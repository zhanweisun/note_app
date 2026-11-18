package app.notedraft.noteall.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.notedraft.noteall.R
import app.notedraft.noteall.data.NoteType

/**
 * 16 种笔记类型对应的 Phosphor 图标资源名。
 * 资源名直接来自原始 APK：res/drawable/nd_*.xml 与 ph_*_line.xml
 */
private val typeIcons = mapOf(
    NoteType.NOTE to R.drawable.nd_note,
    NoteType.LIST to R.drawable.nd_checklist,
    NoteType.BOARD to R.drawable.nd_board,
    NoteType.CALC to R.drawable.nd_calc,
    NoteType.COUNTER to R.drawable.nd_counter,
    NoteType.HABIT to R.drawable.nd_habit,
    NoteType.LINK_LIST to R.drawable.nd_link,
    NoteType.NAMES to R.drawable.nd_contacts,
    NoteType.NOTEBOOK to R.drawable.nd_notebook,
    NoteType.PASSWORD to R.drawable.nd_password,
    NoteType.SHOPPING to R.drawable.nd_shopping,
    NoteType.SNIPPETS to R.drawable.nd_snippets,
    NoteType.TIMELINE to R.drawable.nd_timeline
)

fun typeIconRes(type: String): Int = typeIcons[type] ?: R.drawable.nd_note

fun typeLabelRes(type: String): Int = when (type) {
    NoteType.NOTE -> R.string.type_note
    NoteType.LIST -> R.string.type_list
    NoteType.BOARD -> R.string.type_board
    NoteType.CALC -> R.string.type_calc
    NoteType.COUNTER -> R.string.type_counter
    NoteType.HABIT -> R.string.type_habit
    NoteType.LINK_LIST -> R.string.type_linklist
    NoteType.NAMES -> R.string.type_names
    NoteType.NOTEBOOK -> R.string.type_notebook
    NoteType.PASSWORD -> R.string.type_password
    NoteType.SHOPPING -> R.string.type_shopping
    NoteType.SNIPPETS -> R.string.type_snippets
    NoteType.TIMELINE -> R.string.type_timeline
    else -> R.string.type_note
}

@Composable
fun TypeIcon(type: String, size: Dp = 24.dp, tint: Color = LocalContentColor.current) {
    Image(
        painter = painterResource(id = typeIconRes(type)),
        contentDescription = null,
        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(tint),
        modifier = Modifier.size(size)
    )
}

@Composable
fun PhIcon(resId: Int, contentDescription: String? = null, size: Dp = 24.dp) {
    Icon(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        modifier = Modifier.size(size)
    )
}