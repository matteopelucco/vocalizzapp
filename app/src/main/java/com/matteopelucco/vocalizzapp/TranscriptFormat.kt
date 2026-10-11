package com.matteopelucco.vocalizzapp

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ITALY)

/** Data e ora fino ai minuti, per esempio "10 ott 2026, 21:34". */
fun formatTranscriptDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    DATE_FORMAT.format(Instant.ofEpochMilli(millis).atZone(zone))

/** Il testo su una sola riga (a capo e spazi multipli diventano uno spazio): la UI lo tronca con i puntini. */
fun singleLinePreview(text: String): String = text.trim().replace(Regex("\\s+"), " ")
