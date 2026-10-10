package com.matteopelucco.vocalizzapp

import android.os.SystemClock
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Running(val progress: Float) : DownloadState
    data class Failed(val message: String) : DownloadState
}

/**
 * Scarica il modello Whisper in `models/`. Unico uso della rete dell'app: l'audio non esce mai dal telefono.
 * Ogni file passa da `.part` (con ripresa se interrotto) e viene rinominato solo se la dimensione e' quella attesa.
 */
object ModelDownloader {
    private const val BASE_URL = "https://huggingface.co/csukuangfj/sherpa-onnx-whisper-small/resolve/main/"

    /** File del modello `small` int8 con la loro dimensione esatta in byte. */
    private val FILES = listOf(
        "small-encoder.int8.onnx" to 112_442_483L,
        "small-decoder.int8.onnx" to 262_226_114L,
        "small-tokens.txt" to 816_730L,
    )

    /** Dimensione totale in MB, per mostrarla all'utente. */
    val totalMegabytes: Int = (FILES.sumOf { it.second } / 1_000_000L).toInt()

    suspend fun download(dir: File, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        dir.mkdirs()
        val total = FILES.sumOf { it.second }
        var done = 0L
        var lastReport = 0L
        for ((name, size) in FILES) {
            val target = File(dir, name)
            if (target.exists() && target.length() == size) {
                done += size
                continue
            }
            val part = File(dir, "$name.part")
            downloadFile(URL(BASE_URL + name), part, size) { read ->
                val now = SystemClock.elapsedRealtime()
                if (now - lastReport > 150) {
                    lastReport = now
                    onProgress((done + read) / total.toFloat())
                }
            }
            check(part.length() == size) { "Download incompleto di $name" }
            check(part.renameTo(target)) { "Impossibile salvare $name" }
            done += size
            onProgress(done / total.toFloat())
        }
    }

    private suspend fun downloadFile(url: URL, part: File, expectedSize: Long, onBytes: (Long) -> Unit) {
        val existing = if (part.exists()) part.length() else 0L
        if (existing == expectedSize) return
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            if (existing in 1 until expectedSize) setRequestProperty("Range", "bytes=$existing-")
        }
        try {
            val code = conn.responseCode
            check(code == 200 || code == 206) { "Errore di rete (HTTP $code)" }
            val append = code == 206 // con 200 il server ha ignorato la ripresa: si riparte da zero
            var read = if (append) existing else 0L
            FileOutputStream(part, append).use { out ->
                conn.inputStream.use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        read += n
                        onBytes(read)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }
}
