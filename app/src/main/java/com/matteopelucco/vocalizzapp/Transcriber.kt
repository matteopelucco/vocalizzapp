package com.matteopelucco.vocalizzapp

import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Trascrive PCM 16 kHz mono con Whisper (sherpa-onnx). Va chiuso con [close]. */
class Transcriber(model: WhisperModel, language: String = "it") : AutoCloseable {
    private val recognizer = OfflineRecognizer(
        assetManager = null,
        config = OfflineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = AudioDecoder.TARGET_RATE, featureDim = 80),
            modelConfig = OfflineModelConfig(
                // Parakeet (transducer NeMo) e' multilingua con rilevamento automatico: niente lingua da impostare.
                whisper = if (model.joiner != null) OfflineWhisperModelConfig() else OfflineWhisperModelConfig(
                    encoder = model.encoder.absolutePath,
                    decoder = model.decoder.absolutePath,
                    language = language,
                    task = "transcribe",
                ),
                transducer = if (model.joiner == null) OfflineTransducerModelConfig() else OfflineTransducerModelConfig(
                    encoder = model.encoder.absolutePath,
                    decoder = model.decoder.absolutePath,
                    joiner = model.joiner.absolutePath,
                ),
                tokens = model.tokens.absolutePath,
                numThreads = 3,
                provider = "cpu",
                modelType = if (model.joiner != null) "nemo_transducer" else "whisper",
            ),
            decodingMethod = "greedy_search",
        ),
    )

    // Il recognizer nativo non e' pensato per usi concorrenti (es. un annullamento seguito da un nuovo audio).
    private val lock = Mutex()

    /**
     * Trascrive blocco per blocco; dopo ognuno chiama [onProgress] con la frazione completata (0..1)
     * e il testo ottenuto finora. L'annullamento ha effetto al termine del blocco in corso.
     */
    suspend fun transcribe(pcm: FloatArray, onProgress: (Float, String) -> Unit = { _, _ -> }): String =
        lock.withLock {
            try {
                check(!released) { "Transcriber chiuso" }
                run(pcm, onProgress)
            } finally {
                // Un close() arrivato mentre lavoravamo ha rimandato il rilascio a questo punto.
                if (closeRequested) releaseNow()
            }
        }

    private suspend fun run(pcm: FloatArray, onProgress: (Float, String) -> Unit): String =
        withContext(Dispatchers.Default) {
            val parts = mutableListOf<String>()
            for ((start, end) in AudioChunker.split(pcm)) {
                coroutineContext.ensureActive()
                if (!AudioChunker.isSilent(pcm, start, end)) {
                    val stream = recognizer.createStream()
                    try {
                        stream.acceptWaveform(pcm.copyOfRange(start, end), AudioDecoder.TARGET_RATE)
                        recognizer.decode(stream)
                        val text = recognizer.getResult(stream).text.trim()
                        if (text.isNotEmpty()) parts += continueSentence(parts.lastOrNull(), text)
                    } finally {
                        stream.release()
                    }
                }
                onProgress(end / pcm.size.toFloat(), parts.joinToString(" "))
            }
            parts.joinToString(" ")
        }

    @Volatile private var closeRequested = false
    private var released = false

    /**
     * Libera il modello. Se e' in uso (blocco in corso), il rilascio avviene appena finisce:
     * liberarlo mentre il codice nativo lavora farebbe crashare l'app.
     */
    override fun close() {
        closeRequested = true
        if (lock.tryLock()) {
            try {
                releaseNow()
            } finally {
                lock.unlock()
            }
        }
    }

    private fun releaseNow() {
        if (!released) {
            released = true
            recognizer.release()
        }
    }

    internal companion object {
        /** Whisper inizia ogni blocco in minuscolo: dopo un punto fermo la prima lettera va maiuscola. */
        fun continueSentence(previous: String?, text: String): String =
            if (previous != null && previous.last() in ".!?" && text.first().isLowerCase()) {
                text.replaceFirstChar { it.uppercase() }
            } else {
                text
            }
    }
}
