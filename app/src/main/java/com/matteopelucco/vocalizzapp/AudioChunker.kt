package com.matteopelucco.vocalizzapp

import kotlin.math.sqrt

/**
 * Divide un audio lungo in blocchi che Whisper puo' elaborare (max 30 s), tagliando
 * nel punto piu' silenzioso verso la fine di ogni blocco, per non spezzare le parole.
 */
object AudioChunker {
    private const val FRAME_MS = 20

    /** Intervalli [start, end) in campioni, contigui e senza sovrapposizione. */
    fun split(
        pcm: FloatArray,
        sampleRate: Int = AudioDecoder.TARGET_RATE,
        maxSeconds: Int = 28,
        searchSeconds: Int = 6,
    ): List<Pair<Int, Int>> {
        val max = maxSeconds * sampleRate
        val frame = sampleRate * FRAME_MS / 1000
        val chunks = mutableListOf<Pair<Int, Int>>()
        var start = 0
        while (pcm.size - start > max) {
            val windowEnd = start + max
            val windowStart = windowEnd - searchSeconds * sampleRate
            val cut = quietestPoint(pcm, windowStart, windowEnd, frame)
            chunks += start to cut
            start = cut
        }
        if (start < pcm.size) chunks += start to pcm.size
        return chunks
    }

    /** Centro del frame con energia minima in [from, to). */
    private fun quietestPoint(pcm: FloatArray, from: Int, to: Int, frame: Int): Int {
        var bestPos = to
        var bestEnergy = Float.MAX_VALUE
        var pos = from
        while (pos + frame <= to) {
            var sum = 0f
            for (i in pos until pos + frame) sum += pcm[i] * pcm[i]
            val energy = sqrt(sum / frame)
            if (energy < bestEnergy) {
                bestEnergy = energy
                bestPos = pos + frame / 2
            }
            pos += frame
        }
        return bestPos
    }

    /** Vero se il blocco e' praticamente silenzio (evita le "allucinazioni" di Whisper). */
    fun isSilent(pcm: FloatArray, start: Int, end: Int, threshold: Float = 0.005f): Boolean {
        for (i in start until end) if (kotlin.math.abs(pcm[i]) > threshold) return false
        return true
    }
}
