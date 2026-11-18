package app.notedraft.noteall.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 数据库结构完全还原自原始 APK 的 SQLiteOpenHelper：
 *  - 版本 4
 *  - onConfigure 中开启外键约束并设置 PRAGMA secure_delete = ON
 *  - v1 -> v4 的增量迁移与原始一致
 */
class NoteDatabase(context: Context, name: String? = DEFAULT_NAME) :
    SQLiteOpenHelper(context.applicationContext, name, null, VERSION) {

    companion object {
        const val DEFAULT_NAME = "notedraft.db"
        const val VERSION = 4
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
        db.rawQuery("PRAGMA secure_delete = ON", null).use { it.moveToFirst() }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE folders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    parent_folder_id INTEGER,
    created_at INTEGER NOT NULL,
    FOREIGN KEY(parent_folder_id) REFERENCES folders(id) ON DELETE CASCADE
)"""
        )
        db.execSQL(
            """CREATE TABLE items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    folder_id INTEGER,
    type TEXT NOT NULL,
    title TEXT NOT NULL,
    body TEXT,
    is_pinned INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_encrypted INTEGER NOT NULL DEFAULT 0,
    encryption_salt TEXT,
    deleted_at INTEGER,
    FOREIGN KEY(folder_id) REFERENCES folders(id) ON DELETE CASCADE
)"""
        )
        db.execSQL(
            """CREATE TABLE list_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id INTEGER NOT NULL,
    content TEXT NOT NULL,
    is_completed INTEGER NOT NULL DEFAULT 0,
    sort_order INTEGER NOT NULL DEFAULT 0,
    due TEXT,
    FOREIGN KEY(item_id) REFERENCES items(id) ON DELETE CASCADE
)"""
        )
        db.execSQL(
            """CREATE TABLE revisions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    body TEXT,
    saved_at INTEGER NOT NULL,
    revision_number INTEGER NOT NULL,
    is_encrypted INTEGER NOT NULL DEFAULT 0,
    encryption_salt TEXT,
    FOREIGN KEY(item_id) REFERENCES items(id) ON DELETE CASCADE
)"""
        )
        db.execSQL(
            """CREATE TABLE settings (
    key TEXT PRIMARY KEY,
    value TEXT
)"""
        )
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS settings (
    key TEXT PRIMARY KEY,
    value TEXT
)"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS revisions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    body TEXT,
    saved_at INTEGER NOT NULL,
    revision_number INTEGER NOT NULL,
    FOREIGN KEY(item_id) REFERENCES items(id) ON DELETE CASCADE
)"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS list_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id INTEGER NOT NULL,
    content TEXT NOT NULL,
    is_completed INTEGER NOT NULL DEFAULT 0,
    sort_order INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(item_id) REFERENCES items(id) ON DELETE CASCADE
)"""
            )
            if (!hasColumn(db, "items", "deleted_at")) {
                db.execSQL("ALTER TABLE items ADD COLUMN deleted_at INTEGER")
            }
        }
        if (oldVersion < 3) {
            val columns = columnNames(db, "revisions")
            if ("is_encrypted" !in columns) {
                db.execSQL("ALTER TABLE revisions ADD COLUMN is_encrypted INTEGER NOT NULL DEFAULT 0")
            }
            if ("encryption_salt" !in columns) {
                db.execSQL("ALTER TABLE revisions ADD COLUMN encryption_salt TEXT")
            }
        }
        if (oldVersion < 4) {
            if (!hasColumn(db, "list_items", "due")) {
                db.execSQL("ALTER TABLE list_items ADD COLUMN due TEXT")
            }
        }
    }

    private fun columnNames(db: SQLiteDatabase, table: String): Set<String> {
        val names = linkedSetOf<String>()
        db.rawQuery("PRAGMA table_info($table)", null).use { c ->
            while (c.moveToNext()) {
                c.getString(c.getColumnIndexOrThrow("name"))?.let(names::add)
            }
        }
        return names
    }

    private fun hasColumn(db: SQLiteDatabase, table: String, column: String) =
        column in columnNames(db, table)
}