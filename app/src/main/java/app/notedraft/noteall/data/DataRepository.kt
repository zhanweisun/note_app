package app.notedraft.noteall.data

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.util.UUID

/**
 * 唯一的数据访问入口。对应原始 APK 中的 DataRepository。
 *
 * 设计说明：
 *  - 对外暴露 StateFlow + suspend 方法，ViewModel 直接消费
 *  - 所有 SQL 与原始实现保持一致的列名 / 语义
 *  - 结构化类型（看板、计数器、习惯……）的 body 为 JSON 文本，
 *    NOTE / LIST 的 body 为正文 / null（条目存于 list_items 表）
 */
class DataRepository(context: Context, notebookId: String? = null) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val notebookKey: String = notebookId ?: prefs.getString(KEY_ACTIVE_NOTEBOOK, "default") ?: "default"

    private val helper = NoteDatabase(appContext, databaseName(notebookKey))

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        prettyPrint = false
    }

    private val _items = MutableStateFlow<List<NoteOrList>>(emptyList())
    val items: StateFlow<List<NoteOrList>> = _items.asStateFlow()

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings.DEFAULT)
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    /** 当前会话的解锁密码（用于加解密笔记正文）。 */
    var sessionPassword: CharArray? = null
        private set

    // -----------------------------------------------------------------------
    // 加载
    // -----------------------------------------------------------------------

    suspend fun refresh() = withContext(Dispatchers.IO) {
        _folders.value = loadFolders()
        _items.value = loadItems()
        _settings.value = loadSettings()
    }

    suspend fun loadSettingsAsync(): AppSettings = withContext(Dispatchers.IO) { loadSettings() }

    private fun loadFolders(): List<Folder> {
        val list = ArrayList<Folder>()
        helper.readableDatabase.query(
            "folders", null, null, null, null, null, "created_at ASC"
        ).use { c ->
            while (c.moveToNext()) {
                list += Folder(
                    id = c.getLong(c.getColumnIndexOrThrow("id")),
                    name = c.getString(c.getColumnIndexOrThrow("name")) ?: "",
                    parentFolderId = c.getColumnIndexOrThrow("parent_folder_id")
                        .takeIf { !c.isNull(it) }?.let(c::getLong),
                    createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"))
                )
            }
        }
        return list
    }

    private fun loadItems(): List<NoteOrList> {
        val db = helper.readableDatabase
        val raw = ArrayList<NoteOrList>()
        db.query("items", null, null, null, null, null, "is_pinned DESC, updated_at DESC").use { c ->
            while (c.moveToNext()) raw += readItem(c)
        }
        // 批量读取条目，避免 N+1
        val children = HashMap<Long, MutableList<ListItem>>()
        db.query(
            "list_items", null, null, null, null, null, "sort_order ASC, id ASC"
        ).use { c ->
            while (c.moveToNext()) {
                val item = readListItem(c)
                children.getOrPut(item.itemId) { ArrayList() } += item
            }
        }
        return raw.map { it.copy(listItems = children[it.id].orEmpty()) }
    }

    private fun readItem(c: android.database.Cursor): NoteOrList {
        fun longOrNull(col: String): Long? {
            val i = c.getColumnIndexOrThrow(col)
            return if (c.isNull(i)) null else c.getLong(i)
        }

        fun strOrNull(col: String): String? {
            val i = c.getColumnIndexOrThrow(col)
            return if (c.isNull(i)) null else c.getString(i)
        }

        return NoteOrList(
            id = c.getLong(c.getColumnIndexOrThrow("id")),
            folderId = longOrNull("folder_id"),
            type = c.getString(c.getColumnIndexOrThrow("type")) ?: NoteType.NOTE,
            title = c.getString(c.getColumnIndexOrThrow("title")) ?: "",
            body = strOrNull("body"),
            isPinned = c.getInt(c.getColumnIndexOrThrow("is_pinned")) == 1,
            createdAt = c.getLong(c.getColumnIndexOrThrow("created_at")),
            updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at")),
            sortOrder = c.getInt(c.getColumnIndexOrThrow("sort_order")),
            isEncrypted = c.getInt(c.getColumnIndexOrThrow("is_encrypted")) == 1,
            encryptionSalt = strOrNull("encryption_salt"),
            deletedAt = longOrNull("deleted_at")
        )
    }

    private fun readListItem(c: android.database.Cursor): ListItem {
        val dueIdx = c.getColumnIndex("due")
        val dueRaw = if (dueIdx >= 0 && !c.isNull(dueIdx)) c.getString(dueIdx) else null
        return ListItem(
            id = c.getLong(c.getColumnIndexOrThrow("id")),
            itemId = c.getLong(c.getColumnIndexOrThrow("item_id")),
            content = c.getString(c.getColumnIndexOrThrow("content")) ?: "",
            isCompleted = c.getInt(c.getColumnIndexOrThrow("is_completed")) == 1,
            sortOrder = c.getInt(c.getColumnIndexOrThrow("sort_order")),
            due = dueRaw?.let { runCatching { json.decodeFromString(Due.serializer(), it) }.getOrNull() }
        )
    }

    private fun loadSettings(): AppSettings {
        val values = HashMap<String, String?>()
        helper.readableDatabase.query("settings", arrayOf("key", "value"), null, null, null, null, null)
            .use { c ->
                while (c.moveToNext()) {
                    values[c.getString(0) ?: continue] = c.getString(1)
                }
            }
        if (values.isEmpty()) return AppSettings.DEFAULT
        val default = AppSettings.DEFAULT
        return AppSettings(
            theme = values["theme"] ?: default.theme,
            fontScale = values["font_scale"]?.toFloatOrNull() ?: default.fontScale,
            orientation = values["orientation"] ?: default.orientation,
            accentColor = values["accent_color"] ?: default.accentColor,
            lockEnabled = values["lock_enabled"]?.toBooleanStrictOrNull() ?: default.lockEnabled,
            trashRetention = values["trash_retention"] ?: default.trashRetention,
            biometrics = values["biometrics"]?.toBooleanStrictOrNull() ?: default.biometrics,
            language = values["language"] ?: default.language,
            gridView = values["grid_view"]?.toBooleanStrictOrNull() ?: default.gridView,
            sortAscending = values["sort_ascending"]?.toBooleanStrictOrNull() ?: default.sortAscending,
            sortMode = values["sort_mode"] ?: default.sortMode,
            viewMode = values["view_mode"] ?: default.viewMode,
            spellCheck = values["spell_check"]?.toBooleanStrictOrNull() ?: default.spellCheck,
            fontFamily = values["font_family"] ?: default.fontFamily,
            lineHeight = values["line_height"]?.toFloatOrNull() ?: default.lineHeight,
            editorWidth = values["editor_width"] ?: default.editorWidth,
            showWordCount = values["show_word_count"]?.toBooleanStrictOrNull() ?: default.showWordCount,
            profileName = values["profile_name"] ?: default.profileName
        )
    }

    suspend fun saveSettings(settings: AppSettings) = withContext(Dispatchers.IO) {
        val entries = listOf(
            "theme" to settings.theme,
            "font_scale" to settings.fontScale.toString(),
            "orientation" to settings.orientation,
            "accent_color" to settings.accentColor,
            "lock_enabled" to settings.lockEnabled.toString(),
            "trash_retention" to settings.trashRetention,
            "biometrics" to settings.biometrics.toString(),
            "language" to settings.language,
            "grid_view" to settings.gridView.toString(),
            "sort_ascending" to settings.sortAscending.toString(),
            "sort_mode" to settings.sortMode,
            "view_mode" to settings.viewMode,
            "spell_check" to settings.spellCheck.toString(),
            "font_family" to settings.fontFamily,
            "line_height" to settings.lineHeight.toString(),
            "editor_width" to settings.editorWidth,
            "show_word_count" to settings.showWordCount.toString(),
            "profile_name" to settings.profileName
        )
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            for ((k, v) in entries) {
                val cv = ContentValues().apply {
                    put("key", k)
                    if (v == null) putNull("value") else put("value", v)
                }
                db.insertWithOnConflict("settings", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _settings.value = settings
    }

    // -----------------------------------------------------------------------
    // 笔记读写
    // -----------------------------------------------------------------------

    suspend fun createNote(type: String, folderId: Long? = null, title: String = "", body: String? = null): Long =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val cv = ContentValues().apply {
                if (folderId == null) putNull("folder_id") else put("folder_id", folderId)
                put("type", type)
                put("title", title)
                if (body == null) putNull("body") else put("body", body)
                put("is_pinned", 0)
                put("created_at", now)
                put("updated_at", now)
                put("sort_order", nextSortOrder(folderId))
                put("is_encrypted", 0)
            }
            helper.writableDatabase.insert("items", null, cv)
        }

    private fun nextSortOrder(folderId: Long?): Int {
        val where = if (folderId == null) "folder_id IS NULL" else "folder_id = ?"
        val args = if (folderId == null) null else arrayOf(folderId.toString())
        helper.readableDatabase.rawQuery(
            "SELECT MAX(sort_order) FROM items WHERE $where", args
        ).use { c -> return if (c.moveToFirst() && !c.isNull(0)) c.getInt(0) + 1 else 0 }
    }

    suspend fun createFolder(name: String, parentFolderId: Long? = null): Long =
        withContext(Dispatchers.IO) {
            val cv = ContentValues().apply {
                put("name", name)
                if (parentFolderId == null) putNull("parent_folder_id") else put("parent_folder_id", parentFolderId)
                put("created_at", System.currentTimeMillis())
            }
            helper.writableDatabase.insert("folders", null, cv)
        }

    suspend fun renameFolder(id: Long, name: String) = withContext(Dispatchers.IO) {
        helper.writableDatabase.update(
            "folders", ContentValues().apply { put("name", name) }, "id = ?", arrayOf(id.toString())
        )
        Unit
    }

    suspend fun deleteFolder(id: Long) = withContext(Dispatchers.IO) {
        helper.writableDatabase.delete("folders", "id = ?", arrayOf(id.toString()))
        Unit
    }

    /** 更新标题与正文；如涉及清单条目则一并重写 list_items。 */
    suspend fun updateNote(
        id: Long,
        title: String,
        body: String?,
        listItems: List<ListItem>? = null
    ) = withContext(Dispatchers.IO) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val cv = ContentValues().apply {
                put("title", title)
                if (body == null) putNull("body") else put("body", body)
                put("updated_at", System.currentTimeMillis())
            }
            db.update("items", cv, "id = ?", arrayOf(id.toString()))
            if (listItems != null) {
                db.delete("list_items", "item_id = ?", arrayOf(id.toString()))
                listItems.forEachIndexed { index, item ->
                    val icv = ContentValues().apply {
                        put("item_id", id)
                        put("content", item.content)
                        put("is_completed", if (item.isCompleted) 1 else 0)
                        put("sort_order", index)
                        if (item.due == null) putNull("due")
                        else put("due", json.encodeToString(Due.serializer(), item.due))
                    }
                    db.insert("list_items", null, icv)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshFromDb()
        Unit
    }

    suspend fun setPinned(id: Long, pinned: Boolean) = withContext(Dispatchers.IO) {
        helper.writableDatabase.update(
            "items",
            ContentValues().apply { put("is_pinned", if (pinned) 1 else 0) },
            "id = ?", arrayOf(id.toString())
        )
        refreshFromDb()
        Unit
    }

    suspend fun moveToFolder(id: Long, folderId: Long?) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            if (folderId == null) putNull("folder_id") else put("folder_id", folderId)
        }
        helper.writableDatabase.update("items", cv, "id = ?", arrayOf(id.toString()))
        refreshFromDb()
        Unit
    }

    /** 移入回收站（软删除）。 */
    suspend fun trash(id: Long) = withContext(Dispatchers.IO) {
        helper.writableDatabase.update(
            "items",
            ContentValues().apply { put("deleted_at", System.currentTimeMillis()) },
            "id = ?", arrayOf(id.toString())
        )
        refreshFromDb()
        Unit
    }

    suspend fun restore(id: Long) = withContext(Dispatchers.IO) {
        helper.writableDatabase.update(
            "items", ContentValues().apply { putNull("deleted_at") }, "id = ?", arrayOf(id.toString())
        )
        refreshFromDb()
        Unit
    }

    /** 永久删除，连同 list_items 与 revisions 级联清除。 */
    suspend fun deleteForever(id: Long) = withContext(Dispatchers.IO) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("items", "id = ?", arrayOf(id.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshFromDb()
        Unit
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        helper.writableDatabase.delete("items", "deleted_at IS NOT NULL", null)
        refreshFromDb()
        Unit
    }

    suspend fun getNote(id: Long): NoteOrList? = withContext(Dispatchers.IO) {
        val db = helper.readableDatabase
        var base: NoteOrList? = null
        db.query("items", null, "id = ?", arrayOf(id.toString()), null, null, null).use { c ->
            if (c.moveToNext()) base = readItem(c)
        }
        val item = base ?: return@withContext null
        val children = ArrayList<ListItem>()
        db.query(
            "list_items", null, "item_id = ?", arrayOf(id.toString()), null, null,
            "sort_order ASC, id ASC"
        ).use { c ->
            while (c.moveToNext()) children += readListItem(c)
        }
        item.copy(listItems = children)
    }

    /** 写入一次修订记录，revision_number 自增。 */
    suspend fun addRevision(note: NoteOrList) = withContext(Dispatchers.IO) {
        val db = helper.writableDatabase
        val next = db.rawQuery(
            "SELECT MAX(revision_number) FROM revisions WHERE item_id = ?",
            arrayOf(note.id.toString())
        ).use { if (it.moveToFirst() && !it.isNull(0)) it.getInt(0) + 1 else 1 }
        db.insert(
            "revisions", null,
            ContentValues().apply {
                put("item_id", note.id)
                put("title", note.title)
                if (note.body == null) putNull("body") else put("body", note.body)
                put("saved_at", System.currentTimeMillis())
                put("revision_number", next)
                put("is_encrypted", if (note.isEncrypted) 1 else 0)
                if (note.encryptionSalt == null) putNull("encryption_salt")
                else put("encryption_salt", note.encryptionSalt)
            }
        )
        Unit
    }

    suspend fun revisions(itemId: Long): List<Revision> = withContext(Dispatchers.IO) {
        val list = ArrayList<Revision>()
        helper.readableDatabase.query(
            "revisions", null, "item_id = ?", arrayOf(itemId.toString()), null, null,
            "revision_number DESC"
        ).use { c ->
            while (c.moveToNext()) {
                val saltIdx = c.getColumnIndex("encryption_salt")
                list += Revision(
                    id = c.getLong(c.getColumnIndexOrThrow("id")),
                    itemId = c.getLong(c.getColumnIndexOrThrow("item_id")),
                    title = c.getString(c.getColumnIndexOrThrow("title")) ?: "",
                    body = c.getString(c.getColumnIndexOrThrow("body")),
                    savedAt = c.getLong(c.getColumnIndexOrThrow("saved_at")),
                    revisionNumber = c.getLong(c.getColumnIndexOrThrow("revision_number")).toInt(),
                    isEncrypted = c.getInt(c.getColumnIndexOrThrow("is_encrypted")) == 1,
                    encryptionSalt = if (saltIdx >= 0 && !c.isNull(saltIdx)) c.getString(saltIdx) else null
                )
            }
        }
        list
    }

    // -----------------------------------------------------------------------
    // 加锁 / 解锁
    // -----------------------------------------------------------------------

    fun unlock(password: CharArray): Boolean {
        val verifier = items.value.firstOrNull { it.isEncrypted }?.encryptionSalt
        val ok = Crypto.verify(password, verifier)
        if (ok) sessionPassword = password
        return ok
    }

    fun lock() {
        sessionPassword = null
    }

    /** 取出明文正文（自动处理解密）。 */
    fun plainBody(note: NoteOrList): String? {
        val body = note.body ?: return null
        if (!note.isEncrypted) return body
        val pwd = sessionPassword ?: return null
        return Crypto.decrypt(body, pwd)
    }

    // -----------------------------------------------------------------------
    // 结构化负载
    // -----------------------------------------------------------------------

    inline fun <reified T> decodePayload(note: NoteOrList): T? =
        note.body?.let { runCatching { PayloadCodec.json.decodeFromString<T>(it) }.getOrNull() }

    inline fun <reified T> encodePayload(value: T): String =
        PayloadCodec.json.encodeToString(PayloadCodec.json.serializersModule.serializer<T>(), value)

    suspend fun savePayload(id: Long, bodyJson: String) = withContext(Dispatchers.IO) {
        helper.writableDatabase.update(
            "items",
            ContentValues().apply {
                put("body", bodyJson)
                put("updated_at", System.currentTimeMillis())
            },
            "id = ?", arrayOf(id.toString())
        )
        refreshFromDb()
        Unit
    }

    private fun refreshFromDb() {
        _folders.value = loadFolders()
        _items.value = loadItems()
    }

    // -----------------------------------------------------------------------
    // 备份
    // -----------------------------------------------------------------------

    suspend fun exportPayload(): BackupPayload = withContext(Dispatchers.IO) {
        val db = helper.readableDatabase
        val revisions = ArrayList<Revision>()
        db.query("revisions", null, null, null, null, null, "id ASC").use { c ->
            while (c.moveToNext()) {
                revisions += Revision(
                    id = c.getLong(c.getColumnIndexOrThrow("id")),
                    itemId = c.getLong(c.getColumnIndexOrThrow("item_id")),
                    title = c.getString(c.getColumnIndexOrThrow("title")) ?: "",
                    body = c.getString(c.getColumnIndexOrThrow("body")),
                    savedAt = c.getLong(c.getColumnIndexOrThrow("saved_at")),
                    revisionNumber = c.getLong(c.getColumnIndexOrThrow("revision_number")).toInt(),
                    isEncrypted = c.getInt(c.getColumnIndexOrThrow("is_encrypted")) == 1,
                    encryptionSalt = c.getColumnIndex("encryption_salt").takeIf { !c.isNull(it) }
                        ?.let(c::getString)
                )
            }
        }
        BackupPayload(loadFolders(), loadItems(), revisions)
    }

    suspend fun importPayload(payload: BackupPayload) = withContext(Dispatchers.IO) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("list_items", null, null)
            db.delete("revisions", null, null)
            db.delete("items", null, null)
            db.delete("folders", null, null)
            db.execSQL("DELETE FROM sqlite_sequence WHERE name='folders' OR name='items' OR name='list_items' OR name='revisions'")
            payload.folders.forEach { f ->
                db.insert("folders", null, ContentValues().apply {
                    put("id", f.id)
                    put("name", f.name)
                    if (f.parentFolderId == null) putNull("parent_folder_id") else put("parent_folder_id", f.parentFolderId)
                    put("created_at", f.createdAt)
                })
            }
            payload.items.forEach { n ->
                db.insert("items", null, ContentValues().apply {
                    put("id", n.id)
                    if (n.folderId == null) putNull("folder_id") else put("folder_id", n.folderId)
                    put("type", n.type)
                    put("title", n.title)
                    if (n.body == null) putNull("body") else put("body", n.body)
                    put("is_pinned", if (n.isPinned) 1 else 0)
                    put("created_at", n.createdAt)
                    put("updated_at", n.updatedAt)
                    put("sort_order", n.sortOrder)
                    put("is_encrypted", if (n.isEncrypted) 1 else 0)
                    if (n.encryptionSalt == null) putNull("encryption_salt") else put("encryption_salt", n.encryptionSalt)
                    if (n.deletedAt == null) putNull("deleted_at") else put("deleted_at", n.deletedAt)
                })
                n.listItems.forEachIndexed { index, item ->
                    db.insert("list_items", null, ContentValues().apply {
                        put("item_id", n.id)
                        put("content", item.content)
                        put("is_completed", if (item.isCompleted) 1 else 0)
                        put("sort_order", index)
                        if (item.due == null) putNull("due") else put("due", json.encodeToString(Due.serializer(), item.due))
                    })
                }
            }
            payload.revisions.forEach { r ->
                db.insert("revisions", null, ContentValues().apply {
                    put("item_id", r.itemId)
                    put("title", r.title)
                    if (r.body == null) putNull("body") else put("body", r.body)
                    put("saved_at", r.savedAt)
                    put("revision_number", r.revisionNumber)
                    put("is_encrypted", if (r.isEncrypted) 1 else 0)
                    if (r.encryptionSalt == null) putNull("encryption_salt") else put("encryption_salt", r.encryptionSalt)
                })
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshFromDb()
        Unit
    }

    // -----------------------------------------------------------------------
    // 笔记本（多库）
    // -----------------------------------------------------------------------

    fun activeNotebook(): String = notebookKey

    fun setActiveNotebook(id: String) {
        prefs.edit().putString(KEY_ACTIVE_NOTEBOOK, id).apply()
    }

    fun availableNotebooks(): List<String> = NOTEBOOKS.filter { id ->
        id == "default" || appContext.getDatabasePath(databaseName(id)).exists()
    }

    fun newId(): String = UUID.randomUUID().toString()

    companion object {
        const val PREFS_NAME = "notebook_prefs"
        const val KEY_ACTIVE_NOTEBOOK = "active_notebook_id"
        val NOTEBOOKS = listOf("default", "team", "business")

        fun databaseName(notebookId: String): String = when (notebookId) {
            "team" -> "notebook_team.db"
            "business" -> "notebook_business.db"
            else -> NoteDatabase.DEFAULT_NAME
        }
    }
}