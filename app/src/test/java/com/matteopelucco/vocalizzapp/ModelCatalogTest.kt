package com.matteopelucco.vocalizzapp

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogTest {
    @Test
    fun formatsSizesTheWayThePhoneDoes() {
        assertEquals("161 MB", formatSize(160_609_290L, Locale.ITALY)) // arrotonda: 160,6 -> 161
        assertEquals("375 MB", formatSize(375_485_327L, Locale.ITALY))
        assertEquals("1,0 GB", formatSize(1_036_613_791L, Locale.ITALY))
        assertEquals("24,5 GB", formatSize(24_500_000_000L, Locale.ITALY))
    }

    @Test
    fun hasExactlyOneRecommendedModel() {
        assertEquals(1, ModelCatalog.options.count { it.recommended })
        assertEquals("small", ModelCatalog.recommended.id)
    }

    @Test
    fun everyModelHasTheThreeFilesSherpaNeeds() {
        ModelCatalog.options.forEach { option ->
            val names = option.files.map { it.name }.toSet()
            assertEquals(
                setOf("${option.id}-encoder.int8.onnx", "${option.id}-decoder.int8.onnx", "${option.id}-tokens.txt"),
                names,
            )
            assertTrue(option.files.all { it.sizeBytes > 0 })
            assertTrue(option.precision in 1..5 && option.speed in 1..5)
        }
    }

    @Test
    fun deviceChecksUseRamAndFreeSpaceWithMargin() {
        val turbo = ModelCatalog.byId("turbo")!!
        val gib = 1024L * 1024L * 1024L
        // 12 GB nominali: ~11,2 GiB riportati
        assertTrue(DeviceInfo(totalRamBytes = 11 * gib, freeBytes = 50 * gib).hasEnoughRamFor(turbo))
        // 6 GB nominali: ~5,5 GiB riportati
        assertTrue(!DeviceInfo(totalRamBytes = (5.5 * gib).toLong(), freeBytes = 50 * gib).hasEnoughRamFor(turbo))
        // spazio: serve il 30% in piu' del modello
        assertTrue(!DeviceInfo(1L shl 40, freeBytes = turbo.totalBytes + 1).hasEnoughSpaceFor(turbo))
        assertTrue(DeviceInfo(1L shl 40, freeBytes = (turbo.totalBytes * 1.4).toLong()).hasEnoughSpaceFor(turbo))
    }
}
