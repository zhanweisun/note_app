package app.notedraft.noteall.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.notedraft.noteall.R
import app.notedraft.noteall.data.Folder
import app.notedraft.noteall.data.NoteOrList
import app.notedraft.noteall.ui.components.TypeIcon
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    vm: MainViewModel,
    onOpenNote: (NoteOrList) -> Unit,
    onOpenSettings: () -> Unit
) {
    val section by vm.section.collectAsStateWithLifecycle()
    val items by vm.visibleItems.collectAsStateWithLifecycle()
    val folders by vm.childFolders.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val selectionMode by vm.selectionMode.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var titleMenu by remember { mutableStateOf(false) }

    val title = when (section) {
        MainSection.NOTES -> stringResource(R.string.nav_notes)
        MainSection.TASKS -> stringResource(R.string.nav_tasks)
        MainSection.FOLDERS -> stringResource(R.string.nav_folders)
        MainSection.CALENDAR -> stringResource(R.string.nav_calendar)
        MainSection.MAPS -> stringResource(R.string.nav_maps)
        MainSection.TRASH -> stringResource(R.string.nav_trash)
        MainSection.PROFILE -> stringResource(R.string.nav_profile)
        MainSection.SETTINGS -> stringResource(R.string.nav_settings)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                current = section,
                onSelect = {
                    vm.navigate(it)
                    scope.launch { drawerState.close() }
                },
                onSettings = {
                    scope.launch { drawerState.close() }
                    onOpenSettings()
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                if (selectionMode) {
                    TopAppBar(
                        title = { Text(stringResource(R.string.main_items_selected, selected.size)) },
                        navigationIcon = {
                            IconButton(onClick = vm::clearSelection) {
                                Icon(Icons.Default.Close, stringResource(R.string.common_cancel))
                            }
                        },
                        actions = {
                            IconButton(onClick = { showMoveDialog = true }) {
                                Icon(Icons.Default.PushPin, stringResource(R.string.main_move_to_folder))
                            }
                            if (section == MainSection.TRASH) {
                                IconButton(onClick = vm::restoreSelected) {
                                    Icon(Icons.Default.Check, stringResource(R.string.action_unarchive))
                                }
                                IconButton(onClick = vm::deleteSelectedForever) {
                                    Icon(Icons.Default.Delete, stringResource(R.string.common_delete))
                                }
                            } else {
                                IconButton(onClick = { vm.trashSelected() }) {
                                    Icon(Icons.Default.Delete, stringResource(R.string.common_delete))
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                } else {
                    TopAppBar(
                        title = {
                            if (showSearch) {
                                OutlinedTextField(
                                    value = query,
                                    onValueChange = vm::search,
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    placeholder = { Text(stringResource(R.string.common_search)) },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            vm.search("")
                                            showSearch = false
                                        }) { Icon(Icons.Default.Close, null) }
                                    }
                                )
                            } else {
                                Text(title, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, stringResource(R.string.cd_nav_menu))
                            }
                        },
                        actions = {
                            if (!showSearch) {
                                IconButton(onClick = { showSearch = true }) {
                                    Icon(Icons.Default.Search, stringResource(R.string.common_search))
                                }
                            }
                            if (section == MainSection.TRASH) {
                                Box {
                                    IconButton(onClick = { titleMenu = true }) {
                                        Icon(Icons.Default.MoreVert, null)
                                    }
                                    DropdownMenu(expanded = titleMenu, onDismissRequest = { titleMenu = false }) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.main_empty_trash)) },
                                            onClick = {
                                                titleMenu = false
                                                vm.emptyTrash()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
                AnimatedVisibility(visible = selectionMode, exit = shrinkVertically()) {
                    HorizontalDivider()
                }
            },
            floatingActionButton = {
                if (!selectionMode) {
                    ExtendedFloatingActionButton(
                        onClick = { showCreate = true },
                        icon = { Icon(Icons.Default.Add, null) },
                        text = { Text(stringResource(R.string.main_create)) }
                    )
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when {
                    items.isEmpty() && folders.isEmpty() -> EmptyState(section)
                    else -> LazyColumn(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (section == MainSection.FOLDERS || folders.isNotEmpty()) {
                            items(folders, key = { "f${it.id}" }) { folder ->
                                FolderRow(folder, onClick = { vm.openFolder(folder.id) })
                            }
                        }
                        items(items, key = { it.id }) { note ->
                            NoteRow(
                                note = note,
                                selected = note.id in selected,
                                selectionMode = selectionMode,
                                onClick = {
                                    if (selectionMode) vm.toggleSelection(note.id) else onOpenNote(note)
                                },
                                onLongClick = { vm.toggleSelection(note.id) },
                                onPin = { vm.togglePin(note) },
                                onDuplicate = { vm.duplicate(note) },
                                onTrash = { vm.trash(note.id) }
                            )
                        }
                        item { Spacer(Modifier.height(88.dp)) }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateMenuSheet(
            onDismiss = { showCreate = false },
            onQuickPick = { quick ->
                showCreate = false
                when (quick) {
                    QuickCreate.FOLDER -> showFolderDialog = true
                    else -> vm.createNote(quick.type) { id -> vm.openNote(id) }
                }
            },
            onTypePick = { type ->
                showCreate = false
                vm.createNote(type) { id -> vm.openNote(id) }
            }
        )
    }

    if (showFolderDialog) {
        TextInputDialog(
            title = stringResource(R.string.main_new_folder_title),
            hint = stringResource(R.string.main_enter_folder_name),
            onDismiss = { showFolderDialog = false },
            onConfirm = { name ->
                showFolderDialog = false
                vm.createFolder(name)
            }
        )
    }

    if (showMoveDialog) {
        val all by vm.folders.collectAsStateWithLifecycle()
        MoveDialog(
            folders = all,
            onDismiss = { showMoveDialog = false },
            onPick = { folderId ->
                showMoveDialog = false
                vm.moveSelectedTo(folderId)
            }
        )
    }
}

@Composable
private fun NavigationDrawerContent(
    current: MainSection,
    onSelect: (MainSection) -> Unit,
    onSettings: () -> Unit
) {
    ModalDrawerSheet {
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.app_name_short),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
        )
        val entries = listOf(
            MainSection.NOTES to R.string.nav_notes,
            MainSection.TASKS to R.string.nav_tasks,
            MainSection.FOLDERS to R.string.nav_folders,
            MainSection.CALENDAR to R.string.nav_calendar,
            MainSection.MAPS to R.string.nav_maps,
            MainSection.TRASH to R.string.nav_trash,
            MainSection.PROFILE to R.string.nav_profile
        )
        entries.forEach { (section, labelRes) ->
            NavigationDrawerItem(
                label = { Text(stringResource(labelRes)) },
                selected = current == section,
                onClick = { onSelect(section) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.nav_settings)) },
            selected = false,
            onClick = onSettings,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}

@Composable
private fun NoteRow(
    note: NoteOrList,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPin: () -> Unit,
    onDuplicate: () -> Unit,
    onTrash: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selectionMode) {
                    Icon(
                        Icons.Default.Check,
                        null,
                        tint = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                } else {
                    TypeIcon(note.type, size = 22.dp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(
                Modifier.weight(1f).clickable(onClick = onClick)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPinned) {
                        Icon(Icons.Default.PushPin, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = note.title.ifBlank { stringResource(R.string.main_new_note) },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val preview = note.preview.trim()
                if (preview.isNotEmpty()) {
                    Text(
                        text = preview.replace('\n', ' '),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatTime(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { menu = true }, enabled = !selectionMode) {
                    Icon(Icons.Default.MoreVert, null)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(if (note.isPinned) R.string.action_unarchive else R.string.common_pin)) },
                        leadingIcon = { Icon(Icons.Outlined.PushPin, null) },
                        onClick = {
                            menu = false
                            onPin()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_duplicate)) },
                        onClick = {
                            menu = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.common_delete)) },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = {
                            menu = false
                            onTrash()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderRow(folder: Folder, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TypeIcon(NoteTypeFolders.ICON, size = 22.dp)
            Spacer(Modifier.width(12.dp))
            Text(folder.name, style = MaterialTheme.typography.titleMedium)
        }
    }
}

private object NoteTypeFolders {
    const val ICON = "FOLDER"
}

@Composable
private fun EmptyState(section: MainSection) {
    val (titleRes, subRes) = when (section) {
        MainSection.TASKS -> R.string.main_no_tasks_title to R.string.main_no_tasks_sub
        MainSection.FOLDERS -> R.string.main_no_folders_title to R.string.main_no_folders_sub
        MainSection.TRASH -> R.string.main_trash_empty to R.string.main_empty_local
        else -> R.string.main_empty_start_writing to R.string.main_empty_local
    }
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.Add, null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(12.dp))
        Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(subRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TextInputDialog(
    title: String,
    hint: String,
    initial: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { Text(hint) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (value.isNotBlank()) onConfirm(value.trim()) }) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun MoveDialog(
    folders: List<Folder>,
    onDismiss: () -> Unit,
    onPick: (Long?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.main_move_to_folder)) },
        text = {
            LazyColumn {
                item {
                    TextButton(onClick = { onPick(null) }) {
                        Text(stringResource(R.string.main_root_level))
                    }
                }
                items(folders, key = { it.id }) { folder ->
                    TextButton(onClick = { onPick(folder.id) }) { Text(folder.name) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(millis))