package app.notedraft.noteall.data

import kotlinx.serialization.Serializable

/**
 * 13 种笔记类型。与原始 APK 中 items.type 列的取值一致。
 */
object NoteType {
    const val NOTE = "NOTE"
    const val LIST = "LIST"
    const val BOARD = "BOARD"
    const val CALC = "CALC"
    const val COUNTER = "COUNTER"
    const val HABIT = "HABIT"
    const val LINK_LIST = "LINK_LIST"
    const val NAMES = "NAMES"
    const val NOTEBOOK = "NOTEBOOK"
    const val PASSWORD = "PASSWORD"
    const val SHOPPING = "SHOPPING"
    const val SNIPPETS = "SNIPPETS"
    const val TIMELINE = "TIMELINE"

    /** 以 JSON 负载存储在 items.body 中的结构化类型。 */
    val STRUCTURED = setOf(
        BOARD, CALC, COUNTER, HABIT, LINK_LIST, NAMES, NOTEBOOK,
        PASSWORD, SHOPPING, SNIPPETS, TIMELINE
    )

    fun isStructured(type: String) = type in STRUCTURED
}

@Serializable
data class Due(
    val date: String,
    val time: String? = null,
    val remind: Boolean = false,
    val repeat: String? = null
)

@Serializable
data class ListItem(
    val id: Long,
    val itemId: Long,
    val content: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    val due: Due? = null
)

/** folders 表。父子关系通过 parent_folder_id 自引用。 */
@Serializable
data class Folder(
    val id: Long,
    val name: String,
    val parentFolderId: Long? = null,
    val createdAt: Long
)

@Serializable
data class Revision(
    val id: Long,
    val itemId: Long,
    val title: String,
    val body: String? = null,
    val savedAt: Long,
    val revisionNumber: Int,
    val isEncrypted: Boolean = false,
    val encryptionSalt: String? = null
)

/** items 表。body 对 NOTE/LIST 为正文，对结构化类型为 JSON。 */
@Serializable
data class NoteOrList(
    val id: Long,
    val folderId: Long? = null,
    val type: String,
    val title: String,
    val body: String? = null,
    val isPinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val sortOrder: Int = 0,
    val listItems: List<ListItem> = emptyList(),
    val isEncrypted: Boolean = false,
    val encryptionSalt: String? = null,
    val deletedAt: Long? = null
) {
    val isBoard get() = type == NoteType.BOARD
    val isCalc get() = type == NoteType.CALC
    val isCounter get() = type == NoteType.COUNTER
    val isHabit get() = type == NoteType.HABIT
    val isTrashed get() = deletedAt != null
    val isChecklist get() = type == NoteType.LIST
    val isNote get() = type == NoteType.NOTE

    val preview: String
        get() = when {
            isEncrypted -> ""
            type == NoteType.LIST -> listItems.joinToString("\n") { it.content }
            else -> body.orEmpty()
        }
}

// ---------------------------------------------------------------------------
// 结构化类型的 JSON 负载
// ---------------------------------------------------------------------------

@Serializable
data class BoardCard(val id: String, val text: String = "")

@Serializable
data class BoardColumn(val id: String, val title: String, val cards: List<BoardCard> = emptyList())

@Serializable
data class BoardPayload(val columns: List<BoardColumn> = emptyList())

@Serializable
data class CounterEntry(val id: String, val name: String, val count: Int = 0)

@Serializable
data class CounterPayload(val counters: List<CounterEntry> = emptyList())

@Serializable
data class HabitPayload(val checkedDays: List<String> = emptyList())

@Serializable
data class LinkEntry(val id: String, val title: String, val url: String)

@Serializable
data class LinkTab(val id: String, val emoji: String = "", val name: String, val links: List<LinkEntry> = emptyList())

@Serializable
data class LinkListPayload(val tabs: List<LinkTab> = emptyList())

@Serializable
data class NamePerson(
    val id: String,
    val emoji: String = "",
    val name: String,
    val note: String? = null,
    val image: String? = null,
    val phone: String? = null
)

@Serializable
data class NameTab(val id: String, val emoji: String = "", val name: String, val items: List<NamePerson> = emptyList())

@Serializable
data class NameNotesPayload(val tabs: List<NameTab> = emptyList())

@Serializable
data class NotebookNote(val id: String, val emoji: String = "", val text: String, val updatedAt: Long = 0L)

@Serializable
data class NotebookTab(val id: String, val emoji: String = "", val name: String, val items: List<NotebookNote> = emptyList())

@Serializable
data class NotebookPayload(val tabs: List<NotebookTab> = emptyList())

@Serializable
data class PasswordEntry(
    val id: String,
    val emoji: String = "",
    val title: String,
    val site: String? = null,
    val username: String? = null,
    val password: String? = null,
    val note: String? = null
)

@Serializable
data class PasswordTab(val id: String, val emoji: String = "", val name: String, val items: List<PasswordEntry> = emptyList())

@Serializable
data class PasswordPayload(val tabs: List<PasswordTab> = emptyList())

@Serializable
data class ShoppingItem(
    val id: String,
    val emoji: String = "",
    val title: String,
    val price: String? = null,
    val brand: String? = null,
    val image: String? = null,
    val checked: Boolean = false
)

@Serializable
data class ShoppingTab(val id: String, val emoji: String = "", val name: String, val items: List<ShoppingItem> = emptyList())

@Serializable
data class ShoppingPayload(val tabs: List<ShoppingTab> = emptyList())

@Serializable
data class SnippetEntry(val id: String, val emoji: String = "", val content: String, val label: String? = null)

@Serializable
data class SnippetTab(val id: String, val emoji: String = "", val name: String, val items: List<SnippetEntry> = emptyList())

@Serializable
data class SnippetPayload(val tabs: List<SnippetTab> = emptyList())

@Serializable
data class TimelineEntry(
    val id: String,
    val date: String,
    val hour: Int,
    val minute: Int,
    val emoji: String = "",
    val title: String,
    val note: String? = null
)

@Serializable
data class TimelineTab(val id: String, val emoji: String = "", val name: String, val entries: List<TimelineEntry> = emptyList())

@Serializable
data class TimelinePayload(val tabs: List<TimelineTab> = emptyList())

// ---------------------------------------------------------------------------
// 笔记内的附件 / 绘图
// ---------------------------------------------------------------------------

@Serializable
data class Attachment(val stored: String, val name: String)

@Serializable
data class SerializablePoint(val x: Float, val y: Float)

@Serializable
data class SerializableDrawingPath(
    val points: List<SerializablePoint> = emptyList(),
    val tool: String = "pen",
    val r: Int = 0,
    val g: Int = 0,
    val b: Int = 0,
    val a: Int = 255,
    val strokeWidth: Float = 2f
)

@Serializable
data class NoteMetadata(
    val title: String,
    val tags: List<String> = emptyList(),
    val drawing: Long? = null,
    val kind: String? = null,
    val themeId: String? = null,
    val attachments: List<Attachment> = emptyList(),
    val isFavorite: Boolean = false,
    val reminderAt: Long? = null,
    val voice: List<Attachment> = emptyList(),
    val wordCount: Int = 0,
    val charCount: Int = 0,
    val createdAtIso: String? = null,
    val updatedAtIso: String? = null,
    val version: String? = null,
    val flags: Int = 0
)

// ---------------------------------------------------------------------------
// 备份 / 设置
// ---------------------------------------------------------------------------

@Serializable
data class BackupPayload(
    val folders: List<Folder> = emptyList(),
    val items: List<NoteOrList> = emptyList(),
    val revisions: List<Revision> = emptyList()
)

/**
 * 应用设置。字段顺序与原始 APK 的 AppSettings 序列化器保持一致，
 * 默认值取自 Dex 中构造函数的缺省分支。
 */
@Serializable
data class AppSettings(
    val theme: String = "System",
    val fontScale: Float = 1.0f,
    val orientation: String = "PORTRAIT",
    val accentColor: String? = null,
    val lockEnabled: Boolean = false,
    val trashRetention: String = "After 30 days",
    val biometrics: Boolean = false,
    val language: String = "English (US)",
    val gridView: Boolean = false,
    val sortAscending: Boolean = false,
    val sortMode: String = "SYSTEM",
    val viewMode: String = "NONE",
    val spellCheck: Boolean = true,
    val fontFamily: String = "SANS",
    val lineHeight: Float = 1.0f,
    val editorWidth: String = "NORMAL",
    val showWordCount: Boolean = true,
    val profileName: String? = null
) {
    companion object {
        val DEFAULT = AppSettings()
    }
}
/** 结构化负载的 JSON 编解码器（与数据库层共享同一套配置）。 */
object PayloadCodec {
    val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        prettyPrint = false
    }
}
