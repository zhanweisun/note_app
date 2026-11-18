package app.notedraft.noteall.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.notedraft.noteall.R
import app.notedraft.noteall.data.NoteType
import app.notedraft.noteall.ui.components.typeIconRes
import app.notedraft.noteall.ui.components.typeLabelRes
import kotlinx.coroutines.launch

/**
 * 「新建」底部菜单：快捷动作 + 全部笔记类型。
 * 结构对应原始 APK 的创建菜单（main_quick_* 与 type_* 两组字符串）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMenuSheet(
    onDismiss: () -> Unit,
    onQuickPick: (QuickCreate) -> Unit,
    onTypePick: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun dismissThen(action: () -> Unit) {
        scope.launch {
            sheetState.hide()
            action()
            onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text(
                stringResource(R.string.main_create),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            QuickCreate.entries.forEach { quick ->
                ListItem(
                    headlineContent = { Text(stringResource(quick.labelRes)) },
                    supportingContent = { Text(stringResource(quick.descRes)) },
                    leadingContent = {
                        Icon(quickIcon(quick), null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.clickable { dismissThen { onQuickPick(quick) } }
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(
                stringResource(R.string.main_templates),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            allCreateTypes().forEach { type ->
                ListItem(
                    headlineContent = { Text(stringResource(typeLabelRes(type))) },
                    leadingContent = {
                        Image(
                            painter = painterResource(typeIconRes(type)),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    modifier = Modifier.clickable { dismissThen { onTypePick(type) } }
                )
            }
        }
    }
}

private fun allCreateTypes(): List<String> = listOf(
    NoteType.NOTE,
    NoteType.LIST,
    NoteType.BOARD,
    NoteType.CALC,
    NoteType.COUNTER,
    NoteType.HABIT,
    NoteType.LINK_LIST,
    NoteType.NAMES,
    NoteType.NOTEBOOK,
    NoteType.PASSWORD,
    NoteType.SHOPPING,
    NoteType.SNIPPETS,
    NoteType.TIMELINE
)

private fun quickIcon(quick: QuickCreate): ImageVector = when (quick) {
    QuickCreate.NOTE -> Icons.Default.Description
    QuickCreate.CHECKLIST -> Icons.Default.Checklist
    QuickCreate.FOLDER -> Icons.Default.Folder
    QuickCreate.WORKFLOW -> Icons.Default.SwapHoriz
}