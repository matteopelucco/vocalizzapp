package com.matteopelucco.vocalizzapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Un testo trascritto e salvato sul telefono. */
data class SavedTranscript(
    val id: Long,
    val text: String,
    /** Quando e' stata completata la trascrizione (millisecondi dall'epoca). */
    val createdAtMillis: Long,
    /** Nome del file audio di origine, se noto. */
    val sourceName: String?,
    val audioSeconds: Float,
)

/**
 * Cronologia dei testi trascritti, nel database privato dell'app (`databases/transcripts.db`):
 * non e' leggibile da altre app e non entra nei backup (`allowBackup=false`).
 * Si usa il SQLite di Android senza librerie aggiuntive: serve una sola tabella.
 *
 * Le letture sono "a cursore" sull'id (che cresce con il tempo) cosi' la paginazione resta stabile
 * anche se nel frattempo arrivano nuovi testi. Chiamare da un thread in background.
 */
class TranscriptStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        // Con "secure_delete" SQLite sovrascrive con zeri il contenuto cancellato: senza, il testo
        // eliminato resterebbe leggibile nel file finche' lo spazio non viene riutilizzato.
        try {
            db.execSQL("PRAGMA secure_delete = ON")
        } catch (_: Exception) {
            // Se non e' disponibile, deleteAll() compatta comunque il file con VACUUM.
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "text TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL, " +
                "source_name TEXT, " +
                "audio_seconds REAL NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Nessuna migrazione finora: alla prima modifica dello schema va scritta qui.
    }

    fun insert(text: String, sourceName: String?, audioSeconds: Float, createdAtMillis: Long = System.currentTimeMillis()): Long {
        val values = ContentValues().apply {
            put("text", text)
            put("created_at", createdAtMillis)
            put("source_name", sourceName)
            put("audio_seconds", audioSeconds)
        }
        return writableDatabase.insertOrThrow(TABLE, null, values)
    }

    /** I [limit] testi piu' recenti; con [beforeId] solo quelli salvati prima di quello. */
    fun latest(limit: Int, beforeId: Long? = null): List<SavedTranscript> {
        val selection = if (beforeId != null) "id < ?" else null
        val args = if (beforeId != null) arrayOf(beforeId.toString()) else null
        val result = ArrayList<SavedTranscript>(limit)
        readableDatabase.query(TABLE, null, selection, args, null, null, "id DESC", limit.toString()).use { c ->
            val id = c.getColumnIndexOrThrow("id")
            val text = c.getColumnIndexOrThrow("text")
            val createdAt = c.getColumnIndexOrThrow("created_at")
            val source = c.getColumnIndexOrThrow("source_name")
            val seconds = c.getColumnIndexOrThrow("audio_seconds")
            while (c.moveToNext()) {
                result += SavedTranscript(
                    id = c.getLong(id),
                    text = c.getString(text),
                    createdAtMillis = c.getLong(createdAt),
                    sourceName = if (c.isNull(source)) null else c.getString(source),
                    audioSeconds = c.getFloat(seconds),
                )
            }
        }
        return result
    }

    /** Elimina un testo. Restituisce vero se esisteva. */
    fun delete(id: Long): Boolean = writableDatabase.delete(TABLE, "id = ?", arrayOf(id.toString())) > 0

    /** Elimina tutti i testi e compatta il file del database, cosi' non ne resta traccia. */
    fun deleteAll(): Int {
        val deleted = writableDatabase.delete(TABLE, null, null)
        try {
            writableDatabase.execSQL("VACUUM")
        } catch (_: Exception) {
            // Facoltativo: i testi sono gia' cancellati e, con secure_delete, sovrascritti.
        }
        return deleted
    }

    fun count(): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE", null).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    private companion object {
        const val DB_NAME = "transcripts.db"
        const val DB_VERSION = 1
        const val TABLE = "transcripts"
    }
}
