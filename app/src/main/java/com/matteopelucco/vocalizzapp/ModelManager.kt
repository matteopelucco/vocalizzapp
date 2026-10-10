package com.matteopelucco.vocalizzapp

import android.content.Context
import java.io.File

/** File di un modello Whisper in formato sherpa-onnx. */
data class WhisperModel(
    val name: String,
    val encoder: File,
    val decoder: File,
    val tokens: File,
)

/**
 * I modelli vivono fuori dall'APK, in `getExternalFilesDir("models")`,
 * cosi' possono essere aggiornati senza reinstallare l'app.
 *
 * Un modello e' una terna `<nome>-encoder[.int8].onnx`, `<nome>-decoder[.int8].onnx`,
 * `<nome>-tokens.txt`, direttamente in `models/` (utile con `adb push`) o in una sua sottocartella.
 */
class ModelManager(context: Context) {
    // Creata dall'app stessa: una cartella creata da adb appartiene a "shell" e l'app non la puo' leggere.
    val modelsDir: File = File(context.getExternalFilesDir(null), "models").also { it.mkdirs() }

    /** Descrizione di cio' che l'app vede nella cartella dei modelli (per diagnostica). */
    fun describe(): String {
        if (!modelsDir.exists()) return "cartella models inesistente"
        val dirs = modelsDir.listFiles() ?: return "cartella models non leggibile"
        if (dirs.isEmpty()) return "cartella models vuota"
        return dirs.joinToString("; ") { d ->
            if (d.isDirectory) {
                val inner = d.listFiles()?.joinToString { "${it.name} (${it.length()} B)" } ?: "non leggibile"
                "${d.name}/: $inner"
            } else {
                "${d.name} (${d.length()} B)"
            }
        }
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

    /** Il modello chiamato [name], altrimenti [DEFAULT_MODEL], altrimenti il primo disponibile. */
    fun findModel(name: String? = null): WhisperModel? {
        val models = findModels()
        return models.firstOrNull { it.name == name }
            ?: models.firstOrNull { it.name == DEFAULT_MODEL }
            ?: models.firstOrNull()
    }

    companion object {
        const val DEFAULT_MODEL = "small"
    }

    // Preferisce la variante int8 (piu' piccola e veloce).
    private fun pick(files: List<File>, base: String): File? =
        files.firstOrNull { it.name == "$base.int8.onnx" } ?: files.firstOrNull { it.name == "$base.onnx" }
}
