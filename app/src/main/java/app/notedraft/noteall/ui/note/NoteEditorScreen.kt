package app.notedraft.noteall.ui.note

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.notedraft.noteall.R
import app.notedraft.noteall.data.DataRepository
import app.notedraft.noteall.data.ListItem
import app.notedraft.noteall.data.NoteOrList
import app.notedraft.noteall.data.NoteType
import app.notedraft.noteall.ui.editors.StructuredEditor
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 笔记编辑器。对应原始 APK 的 NoteEditorScreen：
 *  - NOTE：标题 + 纯文本正文，自动保存（去抖 600ms）并保留修订
 *  - LIST：标题 + 清单条目，勾选后文字加删除线
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    repo: DataRepository,
    noteId: Long,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var note by remember(noteId) { mutableStateOf<NoteOrList?>(null) }
    var title by remember(noteId) { mutableStateOf("") }
    var body by remember(noteId) { mutableStateOf("") }
    val listItems = remember(noteId) { mutableStateListOf<ListItem>() }
    var dirty by remember(noteId) { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val bodyFocus = remember { FocusRequester() }

    LaunchedEffect(noteId) {
        val loaded = repo.getNote(noteId)
        note = loaded
        title = loaded?.title.orEmpty()
        body = repo.plainBody(loaded ?: return@LaunchedEffect).orEmpty()
        listItems.clear()
        listItems.addAll(loaded?.listItems.orEmpty())
    }

    // 去抖自动保存
    LaunchedEffect(dirty, title, body, listItems.toList()) {
        if (!dirty) return@LaunchedEffect
        delay(600)
        note?.let { current ->
            repo.updateNote(
                id = current.id,
                title = title,
                body = if (current.isChecklist) current.body else body,
                listItems = if (current.isChecklist) listItems.toList() else null
            )
            dirty = false
        }
    }

    val current = note
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            current?.let {
                                repo.updateNote(
                                    it.id, title,
                                    if (it.isChecklist) it.body else body,
                                    if (it.isChecklist) listItems.toList() else null
                                )
                                repo.takeIf { _ -> it.title != title || !it.isChecklist }?.let { r ->
                                    r.addRevision(it.copy(title = title, body = body))
                                }
                            }
                            onBack()
                        }
                    }) { Icon(Icons.Default.ArrowBack, stringResource(R.string.common_close)) }
                },
                actions = {
                    current?.let {
                        IconButton(onClick = {
                            scope.launch { repo.setPinned(it.id, !it.isPinned) }
                            note = it.copy(isPinned = !it.isPinned)
                        }) {
                            Icon(
                                if (it.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                stringResource(R.string.common_pin)
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Default.MoreVert, null)
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.common_delete)) },
                                leadingIcon = { Icon(Icons.Default.Delete, null) },
                                onClick = {
                                    menu = false
                                    current?.let {
                                        scope.launch {
                                            repo.trash(it.id)
                                            onDeleted()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .imePadding()
        ) {
            BasicTextField(
                value = title,
                onValueChange = {
                    title = it
                    dirty = true
                },
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                decorationBox = { inner ->
                    if (title.isEmpty()) {
                        Text(
                            stringResource(R.string.note_title_hint),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    inner()
                }
            )
            HorizontalDivider()
            when {
                current.isChecklist -> ChecklistEditor(
                    items = listItems,
                    onChange = { dirty = true },
                    requestFocus = noteId
                )
                NoteType.isStructured(current.type) -> StructuredEditor(
                    note = current,
                    body = if (current.isEncrypted) repo.plainBody(current) else current.body
                ) { json ->
                    scope.launch {
                        repo.savePayload(current.id, json)
                    }
                }
                else -> BasicTextField(
                    value = body,
                    onValueChange = {
                        body = it
                        dirty = true
                    },
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 12.dp)
                        .focusRequester(bodyFocus),
                    decorationBox = { inner ->
                        if (body.isEmpty()) {
                            Text(
                                stringResource(R.string.main_empty_start_writing),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                        }
                        inner()
                    }
                )
            }
        }
    }
}

@Composable
private fun ChecklistEditor(
    items: MutableList<ListItem>,
    onChange: () -> Unit,
    requestFocus: Long
) {
    var newText by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = item.isCompleted,
                        onCheckedChange = { checked ->
                            items[index] = item.copy(isCompleted = checked)
                            onChange()
                        }
                    )
                    BasicTextField(
                        value = item.content,
                        onValueChange = {
                            items[index] = item.copy(content = it)
                            onChange()
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null
                        ),
                        modifier = Modifier.weight(1f).padding(vertical = 8.dp)
                    )
                    IconButton(onClick = {
                        items.removeAt(index)
                        onChange()
                    }) { Icon(Icons.Default.Delete, stringResource(R.string.common_delete)) }
                }
            }
        }
        HorizontalDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = newText,
                onValueChange = { newText = it },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f).padding(vertical = 16.dp).focusRequester(focus),
                decorationBox = { inner ->
                    if (newText.isEmpty()) {
                        Text(
                            stringResource(R.string.common_add),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    inner()
                }
            )
            IconButton(
                enabled = newText.isNotBlank(),
                onClick = {
                    items.add(
                        ListItem(
                            id = System.nanoTime(),
                            itemId = requestFocus,
                            content = newText.trim(),
                            sortOrder = items.size
                        )
                    )
                    newText = ""
                    onChange()
                }
            ) { Icon(Icons.Default.Add, stringResource(R.string.common_add)) }
        }
        Spacer(Modifier.navigationBarsPadding().height(8.dp))
    }
}