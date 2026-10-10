package com.matteopelucco.vocalizzapp

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

data class AudioInfo(
    val uri: Uri,
    val name: String?,
    val sizeBytes: Long?,
    val mimeType: String?,
)

fun readAudioInfo(context: Context, uri: Uri): AudioInfo {
    var name: String? = null
    var size: Long? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val s = c.getColumnIndex(OpenableColumns.SIZE)
            if (n >= 0) name = c.getString(n)
            if (s >= 0 && !c.isNull(s)) size = c.getLong(s)
        }
    }
    return AudioInfo(uri, name, size, context.contentResolver.getType(uri))
}
