package com.matteopelucco.vocalizzapp

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioChunkerTest {
    private val rate = 16_000

    /** Segnale "rumoroso" costante (non silenzio), con eventuali pause. */
    private fun signal(seconds: Int, silences: List<IntRange> = emptyList()): FloatArray {
        val pcm = FloatArray(seconds * rate) { i -> if (i % 2 == 0) 0.3f else -0.3f }
        for (s in silences) for (i in s.first * rate / 10 until s.last * rate / 10) pcm[i] = 0f
        return pcm
    }

    @Test
    fun shortAudioIsOneChunk() {
        val chunks = AudioChunker.split(signal(10))
        assertEquals(listOf(0 to 10 * rate), chunks)
    }

    @Test
    fun chunksAreContiguousAndNotTooLong() {
        val pcm = signal(200)
        val chunks = AudioChunker.split(pcm)
        assertEquals(0, chunks.first().first)
        assertEquals(pcm.size, chunks.last().second)
        chunks.zipWithNext().forEach { (a, b) -> assertEquals(a.second, b.first) }
        chunks.forEach { (s, e) -> assertTrue(e - s <= 28 * rate) }
    }

    @Test
    fun cutsInTheQuietestPoint() {
        // pausa tra 25,0 s e 25,2 s (valori in decimi di secondo: 250..252)
        val pcm = signal(60, silences = listOf(250..252))
        val cut = AudioChunker.split(pcm).first().second
        assertTrue("taglio a ${cut / rate.toFloat()} s", cut in (25 * rate)..(25 * rate + rate / 5))
    }

    @Test
    fun detectsSilence() {
        val pcm = FloatArray(rate)
        assertTrue(AudioChunker.isSilent(pcm, 0, pcm.size))
        pcm[100] = abs(0.5f)
        assertFalse(AudioChunker.isSilent(pcm, 0, pcm.size))
    }
}
