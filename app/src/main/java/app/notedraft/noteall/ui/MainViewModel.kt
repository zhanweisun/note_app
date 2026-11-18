package app.notedraft.noteall.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.notedraft.noteall.data.AppSettings
import app.notedraft.noteall.data.DataRepository
import app.notedraft.noteall.data.Folder
import app.notedraft.noteall.data.NoteOrList
import app.notedraft.noteall.data.NoteType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 主界面的状态与动作。对应原始 APK 中 MainActivity 持有的
 * mutableStateOf 组合 + DataRepository 调用。
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    val repo = DataRepository(app)

    /** 当前导航到的顶层分区。对应底部/侧边导航的 7 个目的地。 */
    private val _section = MutableStateFlow(MainSection.NOTES)
    val section: StateFlow<MainSection> = _section

    private val _currentFolder = MutableStateFlow<Long?>(null)
    val currentFolder: StateFlow<Long?> = _currentFolder

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode

    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    val selected: StateFlow<Set<Long>> = _selected

    /** 已打开的笔记 id（对应原始 MainActivity 的 widget.itemId 状态）。 */
    private val _openNoteId = MutableStateFlow<Long?>(null)
    val openNoteId: StateFlow<Long?> = _openNoteId

    private val _dragging = MutableStateFlow(false)
    val dragging: StateFlow<Boolean> = _dragging

    init {
        viewModelScope.launch { repo.refresh() }
    }

    val settings: StateFlow<AppSettings> = repo.settings

    /** 当前分区过滤 + 搜索后的可见列表。 */
    val visibleItems: StateFlow<List<NoteOrList>> = combine(
        repo.items, _section, _currentFolder, _query
    ) { items, section, folder, query ->
        items.filter { note ->
            when (section) {
                MainSection.NOTES -> !note.isTrashed &&
                    !note.isChecklist &&
                    note.folderId == folder
                MainSection.TASKS -> !note.isTrashed && note.isChecklist
                MainSection.CALENDAR -> !note.isTrashed
                MainSection.TRASH -> note.isTrashed
                else -> !note.isTrashed
            }
        }.let { filtered ->
            if (query.isBlank()) filtered
            else filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.preview.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val folders: StateFlow<List<Folder>> = repo.folders

    val childFolders: StateFlow<List<Folder>> = combine(repo.folders, _currentFolder) { all, parent ->
        all.filter { it.parentFolderId == parent }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun navigate(section: MainSection) {
        _section.value = section
        _query.value = ""
        if (section != MainSection.FOLDERS) _currentFolder.value = null
    }

    fun openFolder(id: Long?) {
        _currentFolder.value = id
        _section.value = MainSection.NOTES
    }

    fun search(text: String) {
        _query.value = text
    }

    fun openNote(id: Long?) {
        _openNoteId.value = id
    }

    fun toggleSelection(id: Long) {
        val current = _selected.value
        _selected.value = if (id in current) current - id else current + id
        _selectionMode.value = _selected.value.isNotEmpty()
    }

    fun clearSelection() {
        _selected.value = emptySet()
        _selectionMode.value = false
    }

    fun selectAll(ids: Collection<Long>) {
        _selected.value = ids.toSet()
        _selectionMode.value = ids.isNotEmpty()
    }

    // -----------------------------------------------------------------------
    // 动作
    // -----------------------------------------------------------------------

    fun createNote(type: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repo.createNote(type, _currentFolder.value)
            repo.refresh()
            onCreated(id)
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            repo.createFolder(name, _currentFolder.value)
            repo.refresh()
        }
    }

    fun togglePin(note: NoteOrList) {
        viewModelScope.launch { repo.setPinned(note.id, !note.isPinned) }
    }

    fun duplicate(note: NoteOrList) {
        viewModelScope.launch {
            val id = repo.createNote(note.type, note.folderId, note.title, note.body)
            repo.refresh()
            onDuplicate(note, id)
        }
    }

    private var onDuplicate: (NoteOrList, Long) -> Unit = { _, _ -> }

    fun setDuplicateHook(hook: (NoteOrList, Long) -> Unit) {
        onDuplicate = hook
    }

    fun trash(id: Long) {
        viewModelScope.launch { repo.trash(id) }
    }

    fun trashSelected() {
        viewModelScope.launch {
            _selected.value.forEach { repo.trash(it) }
            clearSelection()
        }
    }

    fun restoreSelected() {
        viewModelScope.launch {
            _selected.value.forEach { repo.restore(it) }
            clearSelection()
        }
    }

    fun deleteSelectedForever() {
        viewModelScope.launch {
            _selected.value.forEach { repo.deleteForever(it) }
            clearSelection()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch { repo.emptyTrash() }
    }

    fun moveSelectedTo(folderId: Long?) {
        viewModelScope.launch {
            _selected.value.forEach { repo.moveToFolder(it, folderId) }
            clearSelection()
        }
    }

    fun saveSettings(settings: AppSettings) {
        viewModelScope.launch { repo.saveSettings(settings) }
    }

    fun refresh() {
        viewModelScope.launch { repo.refresh() }
    }

    /** 与原始实现的销毁清理一致（清空拖拽状态）。 */
    fun setDragging(value: Boolean) {
        _dragging.value = value
    }
}

enum class MainSection { NOTES, TASKS, FOLDERS, CALENDAR, MAPS, TRASH, PROFILE, SETTINGS }

/** 新建菜单中的快捷入口。与 strings.xml 的 main_quick_* 一一对应。 */
enum class QuickCreate(val type: String, val labelRes: Int, val descRes: Int) {
    NOTE(NoteType.NOTE, app.notedraft.noteall.R.string.main_new_note, app.notedraft.noteall.R.string.main_quick_new_note_desc),
    CHECKLIST(NoteType.LIST, app.notedraft.noteall.R.string.main_new_checklist, app.notedraft.noteall.R.string.main_quick_new_checklist_desc),
    FOLDER("FOLDER", app.notedraft.noteall.R.string.main_new_folder, app.notedraft.noteall.R.string.main_quick_new_folder_desc),
    WORKFLOW(NoteType.BOARD, app.notedraft.noteall.R.string.main_new_workflow, app.notedraft.noteall.R.string.main_quick_new_workflow_desc)
}