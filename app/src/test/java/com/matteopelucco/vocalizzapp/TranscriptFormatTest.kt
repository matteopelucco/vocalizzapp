package com.matteopelucco.vocalizzapp

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptFormatTest {
    // 2026-10-10 19:34:56 UTC
    private val millis = 1_791_660_896_000L

    @Test
    fun formatsDateAndTimeToTheMinute() {
        assertEquals("10 ott 2026, 19:34", formatTranscriptDate(millis, ZoneId.of("UTC")))
    }

    @Test
    fun usesTheGivenTimeZone() {
        // In Italia a ottobre c'e' l'ora legale: UTC+2
        assertEquals("10 ott 2026, 21:34", formatTranscriptDate(millis, ZoneId.of("Europe/Rome")))
    }

    @Test
    fun secondsAreNotShown() {
        assertEquals(false, formatTranscriptDate(millis, ZoneId.of("UTC")).contains("56"))
    }

    @Test
    fun previewIsASingleLine() {
        assertEquals("Ciao Matteo come stai", singleLinePreview("  Ciao Matteo\n come   stai \n"))
        assertEquals("", singleLinePreview("   \n "))
    }
}
