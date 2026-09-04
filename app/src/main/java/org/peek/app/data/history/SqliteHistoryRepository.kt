package org.peek.app.data.history

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.peek.app.domain.model.ExtractionResult
import org.peek.app.domain.model.HistoryEntry
import org.peek.app.domain.model.HistoryMediaKind
import org.peek.app.domain.repository.HistoryRepository

class SqliteHistoryRepository(
    context: Context,
) : HistoryRepository {
    private val database = HistoryDatabase(context.applicationContext)
    private val changes = MutableStateFlow(0L)
    private val writeMutex = Mutex()

    override fun observeHistory(): Flow<List<HistoryEntry>> = changes
        .map { database.readHistory() }
        .flowOn(Dispatchers.IO)

    override suspend fun recordView(result: ExtractionResult) {
        val entry = HistoryEntryMapper.fromExtraction(
            result = result,
            viewedAtEpochMillis = System.currentTimeMillis(),
        )
        withContext(Dispatchers.IO) {
            writeMutex.withLock {
                database.insert(entry)
                changes.value += 1
            }
        }
    }

    override suspend fun remove(id: Long) {
        withContext(Dispatchers.IO) {
            writeMutex.withLock {
                database.delete(id)
                changes.value += 1
            }
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            writeMutex.withLock {
                database.clear()
                changes.value += 1
            }
        }
    }
}

private class HistoryDatabase(
    context: Context,
) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE $TABLE_HISTORY (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_SOURCE_URL TEXT NOT NULL,
                $COLUMN_PLATFORM TEXT,
                $COLUMN_TITLE TEXT,
                $COLUMN_AUTHOR TEXT,
                $COLUMN_MEDIA_KIND TEXT NOT NULL,
                $COLUMN_MEDIA_COUNT INTEGER NOT NULL,
                $COLUMN_DURATION_SECONDS INTEGER,
                $COLUMN_VIEWED_AT INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE INDEX history_viewed_at ON $TABLE_HISTORY " +
                "($COLUMN_VIEWED_AT DESC, $COLUMN_ID DESC)",
        )
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun insert(entry: HistoryEntry) {
        val values = ContentValues().apply {
            put(COLUMN_SOURCE_URL, entry.sourceUrl)
            put(COLUMN_PLATFORM, entry.platform)
            put(COLUMN_TITLE, entry.title)
            put(COLUMN_AUTHOR, entry.author)
            put(COLUMN_MEDIA_KIND, entry.mediaKind.name)
            put(COLUMN_MEDIA_COUNT, entry.mediaCount)
            entry.durationSeconds?.let { put(COLUMN_DURATION_SECONDS, it) }
            put(COLUMN_VIEWED_AT, entry.viewedAtEpochMillis)
        }
        writableDatabase.insertOrThrow(TABLE_HISTORY, null, values)
    }

    fun delete(id: Long) {
        writableDatabase.delete(
            TABLE_HISTORY,
            "$COLUMN_ID = ?",
            arrayOf(id.toString()),
        )
    }

    fun clear() {
        writableDatabase.delete(TABLE_HISTORY, null, null)
    }

    fun readHistory(): List<HistoryEntry> = readableDatabase.query(
        TABLE_HISTORY,
        HISTORY_COLUMNS,
        null,
        null,
        null,
        null,
        "$COLUMN_VIEWED_AT DESC, $COLUMN_ID DESC",
    ).use { cursor ->
        buildList {
            val idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID)
            val sourceUrlIndex = cursor.getColumnIndexOrThrow(COLUMN_SOURCE_URL)
            val platformIndex = cursor.getColumnIndexOrThrow(COLUMN_PLATFORM)
            val titleIndex = cursor.getColumnIndexOrThrow(COLUMN_TITLE)
            val authorIndex = cursor.getColumnIndexOrThrow(COLUMN_AUTHOR)
            val mediaKindIndex = cursor.getColumnIndexOrThrow(COLUMN_MEDIA_KIND)
            val mediaCountIndex = cursor.getColumnIndexOrThrow(COLUMN_MEDIA_COUNT)
            val durationIndex = cursor.getColumnIndexOrThrow(COLUMN_DURATION_SECONDS)
            val viewedAtIndex = cursor.getColumnIndexOrThrow(COLUMN_VIEWED_AT)

            while (cursor.moveToNext()) {
                add(
                    HistoryEntry(
                        id = cursor.getLong(idIndex),
                        sourceUrl = cursor.getString(sourceUrlIndex),
                        platform = cursor.nullableString(platformIndex),
                        title = cursor.nullableString(titleIndex),
                        author = cursor.nullableString(authorIndex),
                        mediaKind = cursor.getString(mediaKindIndex).toHistoryKind(),
                        mediaCount = cursor.getInt(mediaCountIndex),
                        durationSeconds = cursor.nullableLong(durationIndex),
                        viewedAtEpochMillis = cursor.getLong(viewedAtIndex),
                    ),
                )
            }
        }
    }

    private fun android.database.Cursor.nullableString(index: Int): String? =
        if (isNull(index)) null else getString(index)

    private fun android.database.Cursor.nullableLong(index: Int): Long? =
        if (isNull(index)) null else getLong(index)

    private fun String.toHistoryKind(): HistoryMediaKind =
        HistoryMediaKind.entries.firstOrNull { it.name == this } ?: HistoryMediaKind.Mixed

    private companion object {
        const val DATABASE_NAME = "peek-history.db"
        const val DATABASE_VERSION = 1
        const val TABLE_HISTORY = "history"
        const val COLUMN_ID = "id"
        const val COLUMN_SOURCE_URL = "source_url"
        const val COLUMN_PLATFORM = "platform"
        const val COLUMN_TITLE = "title"
        const val COLUMN_AUTHOR = "author"
        const val COLUMN_MEDIA_KIND = "media_kind"
        const val COLUMN_MEDIA_COUNT = "media_count"
        const val COLUMN_DURATION_SECONDS = "duration_seconds"
        const val COLUMN_VIEWED_AT = "viewed_at"
        val HISTORY_COLUMNS = arrayOf(
            COLUMN_ID,
            COLUMN_SOURCE_URL,
            COLUMN_PLATFORM,
            COLUMN_TITLE,
            COLUMN_AUTHOR,
            COLUMN_MEDIA_KIND,
            COLUMN_MEDIA_COUNT,
            COLUMN_DURATION_SECONDS,
            COLUMN_VIEWED_AT,
        )
    }
}
