package com.hisabkitab.khata.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Full-database backup and restore using the Storage Access Framework (the user picks the location).
 * The backup is a copy of the SQLite file, so it can be moved to another phone and restored there.
 */
class BackupManager(private val context: Context, private val db: AppDatabase) {

    suspend fun export(target: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            checkpoint()
            val src = context.getDatabasePath(AppDatabase.NAME)
            val out = context.contentResolver.openOutputStream(target, "w")
                ?: error("Cannot open destination")
            out.use { o -> src.inputStream().use { it.copyTo(o) } }
            Unit
        }
    }

    /**
     * Validates the chosen file first (SQLite header, integrity_check, expected tables, version).
     * Only when it is healthy is the current database replaced. A safety copy of the current data
     * is kept in the app's private storage. After a successful restore the app must be restarted.
     */
    suspend fun restore(source: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val tmp = File(context.cacheDir, "restore_tmp.db")
            val input = context.contentResolver.openInputStream(source) ?: error("Cannot open file")
            input.use { i -> tmp.outputStream().use { i.copyTo(it) } }
            try {
                validate(tmp)
                checkpoint()
                val dbFile = context.getDatabasePath(AppDatabase.NAME)
                dbFile.copyTo(File(context.filesDir, "pre_restore_backup.db"), overwrite = true)
                db.close()
                File(dbFile.path + "-wal").delete()
                File(dbFile.path + "-shm").delete()
                tmp.copyTo(dbFile, overwrite = true)
            } finally {
                tmp.delete()
            }
            Unit
        }
    }

    private fun checkpoint() {
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
    }

    private fun validate(f: File) {
        val header = ByteArray(16)
        f.inputStream().use { it.read(header) }
        require(String(header, Charsets.US_ASCII).startsWith("SQLite format 3")) { "Not a SQLite file" }
        val sdb = SQLiteDatabase.openDatabase(f.path, null, SQLiteDatabase.OPEN_READWRITE)
        try {
            sdb.rawQuery("PRAGMA integrity_check", null).use { c ->
                require(c.moveToFirst() && c.getString(0) == "ok") { "Integrity check failed" }
            }
            sdb.rawQuery(
                "SELECT name FROM sqlite_master WHERE type = 'table' " +
                    "AND name IN ('shops', 'customers', 'ledger_transactions')", null
            ).use { require(it.count == 3) { "Missing tables" } }
            require(sdb.version in 1..AppDatabase.VERSION) { "Unsupported database version" }
        } finally {
            sdb.close()
        }
    }
}
