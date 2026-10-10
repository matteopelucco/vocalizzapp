package com.matteopelucco.vocalizzapp

import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Trascrive PCM 16 kHz mono con Whisper (sherpa-onnx). Va chiuso con [close]. */
class Transcriber(model: WhisperModel, language: String = "it") : AutoCloseable {
    private val recognizer = OfflineRecognizer(
        assetManager = null,
        config = OfflineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = AudioDecoder.TARGET_RATE, featureDim = 80),
            modelConfig = OfflineModelConfig(
                whisper = OfflineWhisperModelConfig(
                    encoder = model.encoder.absolutePath,
                    decoder = model.decoder.absolutePath,
                    language = language,
                    task = "transcribe",
                ),
                tokens = model.tokens.absolutePath,
                numThreads = 4,
                provider = "cpu",
                modelType = "whisper",
            ),
            decodingMethod = "greedy_search",
        ),
    )

    /** [onProgress] riceve una frazione 0..1 dopo ogni blocco. */
    suspend fun transcribe(pcm: FloatArray, onProgress: (Float) -> Unit = {}): String =
        withContext(Dispatchers.Default) {
            val chunkSize = CHUNK_SECONDS * AudioDecoder.TARGET_RATE
            val parts = mutableListOf<String>()
            var start = 0
            while (start < pcm.size) {
                coroutineContext.ensureActive()
                val end = minOf(start + chunkSize, pcm.size)
                val stream = recognizer.createStream()
                try {
                    stream.acceptWaveform(pcm.copyOfRange(start, end), AudioDecoder.TARGET_RATE)
                    recognizer.decode(stream)
                    val text = recognizer.getResult(stream).text.trim()
                    if (text.isNotEmpty()) parts += text
                } finally {
                    stream.release()
                }
                start = end
                onProgress(start / pcm.size.toFloat())
            }
            parts.joinToString(" ")
        }

    override fun close() = recognizer.release()

    private companion object {
        const val CHUNK_SECONDS = 28 // Whisper accetta al massimo 30 s per volta
    }
}
