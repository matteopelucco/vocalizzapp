package com.matteopelucco.vocalizzapp

import android.app.ActivityManager
import android.content.Context
import android.os.StatFs
import java.io.File

/** File di un modello Whisper in formato sherpa-onnx. */
data class WhisperModel(
    val name: String,
    val encoder: File,
    val decoder: File,
    val tokens: File,
)

/** Risorse del telefono che contano per la scelta del modello. */
data class DeviceInfo(val totalRamBytes: Long, val freeBytes: Long) {
    /** `totalMem` e' sempre un po' sotto la RAM nominale (un telefono da 8 GB ne riporta ~7,5). */
    fun hasEnoughRamFor(option: ModelOption): Boolean = totalRamBytes >= option.minRamGb * GIB * 0.85

    /** Margine del 30% per i file temporanei durante il download. */
    fun hasEnoughSpaceFor(option: ModelOption): Boolean = freeBytes >= option.totalBytes * 1.3

    private companion object {
        const val GIB = 1024.0 * 1024.0 * 1024.0
    }
}

/**
 * I modelli vivono fuori dall'APK, in `getExternalFilesDir("models")`,
 * cosi' possono essere aggiornati o cambiati senza reinstallare l'app.
 *
 * Un modello e' una terna `<nome>-encoder[.int8].onnx`, `<nome>-decoder[.int8].onnx`,
 * `<nome>-tokens.txt`, direttamente in `models/` (utile con `adb push`) o in una sua sottocartella.
 */
class ModelManager(private val context: Context) {
    // Creata dall'app stessa: una cartella creata da adb appartiene a "shell" e l'app non la puo' leggere.
    val modelsDir: File = File(context.getExternalFilesDir(null), "models").also { it.mkdirs() }

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Modello scelto dall'utente (null = nessuna scelta: si usa il consigliato, se presente). */
    var activeId: String?
        get() = prefs.getString(KEY_ACTIVE, null)
        set(value) = prefs.edit().putString(KEY_ACTIVE, value).apply()

    fun deviceInfo(): DeviceInfo {
        val memory = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java).getMemoryInfo(memory)
        return DeviceInfo(memory.totalMem, StatFs(modelsDir.path).availableBytes)
    }

    /** Tutti i modelli completi trovati, in ordine alfabetico. */
    fun findModels(): List<WhisperModel> {
        val subDirs = modelsDir.listFiles { f -> f.isDirectory }?.toList().orEmpty()
        return (listOf(modelsDir) + subDirs).flatMap { dir ->
            val files = dir.listFiles { f -> f.isFile }?.toList().orEmpty()
            files.filter { it.name.contains("-encoder") && it.name.endsWith(".onnx") }
                .mapNotNull { encoder ->
                    val prefix = encoder.name.substringBefore("-encoder")
                    val decoder = pick(files, "$prefix-decoder")
                    val tokens = files.firstOrNull { it.name == "$prefix-tokens.txt" }
                    if (decoder != null && tokens != null) WhisperModel(prefix, encoder, decoder, tokens) else null
                }
        }.distinctBy { it.name }.sortedBy { it.name }
    }

    fun installedIds(): Set<String> = findModels().map { it.name }.toSet()

    /** Il modello chiamato [name], altrimenti quello scelto dall'utente, il consigliato, o il primo disponibile. */
    fun findModel(name: String? = null): WhisperModel? {
        val models = findModels()
        return models.firstOrNull { it.name == name }
            ?: models.firstOrNull { it.name == activeId }
            ?: models.firstOrNull { it.name == ModelCatalog.recommended.id }
            ?: models.firstOrNull()
    }

    /** Elimina i file di un modello (compresi i download interrotti) per liberare spazio. */
    fun delete(id: String) {
        val subDirs = modelsDir.listFiles { f -> f.isDirectory }?.toList().orEmpty()
        (listOf(modelsDir) + subDirs).forEach { dir ->
            dir.listFiles { f -> f.isFile && f.name.startsWith("$id-") }?.forEach { it.delete() }
        }
        if (activeId == id) activeId = null
    }

    // Preferisce la variante int8 (piu' piccola e veloce).
    private fun pick(files: List<File>, base: String): File? =
        files.firstOrNull { it.name == "$base.int8.onnx" } ?: files.firstOrNull { it.name == "$base.onnx" }

    private companion object {
        const val KEY_ACTIVE = "active_model"
    }
}
