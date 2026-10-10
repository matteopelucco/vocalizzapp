package com.matteopelucco.vocalizzapp

import androidx.annotation.StringRes
import java.util.Locale

/** Un file del modello con la sua dimensione esatta (serve a verificare che il download sia completo). */
data class ModelFile(val name: String, val sizeBytes: Long)

/** Un modello Whisper che l'utente puo' scegliere di scaricare. */
data class ModelOption(
    /** Prefisso dei file (`<id>-encoder.int8.onnx`...) e identificativo salvato nelle impostazioni. */
    val id: String,
    @StringRes val title: Int,
    @StringRes val tagline: Int,
    /** Avvertenza facoltativa mostrata sulla scheda. */
    @StringRes val note: Int? = null,
    /** Repository su Hugging Face (utente csukuangfj). */
    val repo: String,
    val files: List<ModelFile>,
    /** Da 1 a 5, misurato sui vocali di prova (vedi docs/DESIGN.md §8). */
    val precision: Int,
    /** Da 1 a 5, misurato su un POCO X6 Pro. */
    val speed: Int,
    /** RAM del telefono sotto la quale il modello puo' risultare lento o instabile. */
    val minRamGb: Int,
    val recommended: Boolean = false,
) {
    val totalBytes: Long get() = files.sumOf { it.sizeBytes }
}

object ModelCatalog {
    private const val TOKENS_BYTES = 816_730L

    private fun int8Files(id: String, encoder: Long, decoder: Long) = listOf(
        ModelFile("$id-encoder.int8.onnx", encoder),
        ModelFile("$id-decoder.int8.onnx", decoder),
        ModelFile("$id-tokens.txt", TOKENS_BYTES),
    )

    val options: List<ModelOption> = listOf(
        ModelOption(
            id = "base",
            title = R.string.model_base_title,
            tagline = R.string.model_base_tagline,
            repo = "sherpa-onnx-whisper-base",
            files = int8Files("base", encoder = 29_120_534L, decoder = 130_672_026L),
            precision = 2,
            speed = 5,
            minRamGb = 3,
        ),
        ModelOption(
            id = "small",
            title = R.string.model_small_title,
            tagline = R.string.model_small_tagline,
            repo = "sherpa-onnx-whisper-small",
            files = int8Files("small", encoder = 112_442_483L, decoder = 262_226_114L),
            precision = 3,
            speed = 3,
            minRamGb = 4,
            recommended = true,
        ),
        ModelOption(
            id = "turbo",
            title = R.string.model_turbo_title,
            tagline = R.string.model_turbo_tagline,
            note = R.string.model_turbo_note,
            repo = "sherpa-onnx-whisper-turbo",
            files = int8Files("turbo", encoder = 674_716_297L, decoder = 361_080_764L),
            precision = 5,
            speed = 2,
            minRamGb = 8,
        ),
    )

    val recommended: ModelOption = options.first { it.recommended }

    fun byId(id: String?): ModelOption? = options.firstOrNull { it.id == id }
}

/** "160 MB" oppure "1,0 GB" (unita' decimali, come mostra il telefono). */
fun formatSize(bytes: Long, locale: Locale = Locale.ITALY): String =
    if (bytes >= 1_000_000_000L) {
        String.format(locale, "%.1f GB", bytes / 1_000_000_000.0)
    } else {
        String.format(locale, "%d MB", Math.round(bytes / 1_000_000.0))
    }
