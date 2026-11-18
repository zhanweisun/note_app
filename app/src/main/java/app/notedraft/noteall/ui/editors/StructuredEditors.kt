package app.notedraft.noteall.ui.editors

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.notedraft.noteall.R
import app.notedraft.noteall.data.BoardCard
import app.notedraft.noteall.data.BoardColumn
import app.notedraft.noteall.data.BoardPayload
import app.notedraft.noteall.data.CounterEntry
import app.notedraft.noteall.data.CounterPayload
import app.notedraft.noteall.data.HabitPayload
import app.notedraft.noteall.data.LinkEntry
import app.notedraft.noteall.data.LinkListPayload
import app.notedraft.noteall.data.LinkTab
import app.notedraft.noteall.data.NameNotesPayload
import app.notedraft.noteall.data.NamePerson
import app.notedraft.noteall.data.NameTab
import app.notedraft.noteall.data.NoteOrList
import app.notedraft.noteall.data.NotebookNote
import app.notedraft.noteall.data.NotebookPayload
import app.notedraft.noteall.data.NotebookTab
import app.notedraft.noteall.data.PasswordEntry
import app.notedraft.noteall.data.PasswordPayload
import app.notedraft.noteall.data.PasswordTab
import app.notedraft.noteall.data.ShoppingItem
import app.notedraft.noteall.data.ShoppingPayload
import app.notedraft.noteall.data.ShoppingTab
import app.notedraft.noteall.data.SnippetEntry
import app.notedraft.noteall.data.SnippetPayload
import app.notedraft.noteall.data.SnippetTab
import app.notedraft.noteall.data.TimelineEntry
import app.notedraft.noteall.data.TimelinePayload
import app.notedraft.noteall.data.TimelineTab
import app.notedraft.noteall.ui.components.typeLabelRes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 结构化笔记的统一容器。每种类型对应一个专用编辑器，
 * 数据以 JSON 形式保存在 items.body 中（与原 APK 一致）。
 */
@Composable
fun StructuredEditor(
    note: NoteOrList,
    body: String?,
    onSave: (String) -> Unit
) {
    val panel: @Composable (onSave: (String) -> Unit) -> Unit = when (note.type) {
        app.notedraft.noteall.data.NoteType.BOARD -> label@{ save ->
            BoardEditor(decodePayload<BoardPayload>(body, BoardPayload()) ?: BoardPayload()) { save(app.notedraft.noteall.data.PayloadCodec.json.encodeToString(BoardPayload.serializer(), it)) }
        }
        app.notedraft.noteall.data.NoteType.CALC -> label@{ save ->
            CalcEditor(body.orEmpty()) { save(it) }
        }
        app.notedraft.noteall.data.NoteType.COUNTER -> label@{ save ->
            val initial = decodePayload<CounterPayload>(body, CounterPayload()) ?: CounterPayload()
            CounterEditor(initial) { save(app.notedraft.noteall.data.PayloadCodec.json.encodeToString(CounterPayload.serializer(), it)) }
        }
        app.notedraft.noteall.data.NoteType.HABIT -> label@{ save ->
            val initial = decodePayload<HabitPayload>(body, HabitPayload()) ?: HabitPayload()
            HabitEditor(initial) { save(app.notedraft.noteall.data.PayloadCodec.json.encodeToString(HabitPayload.serializer(), it)) }
        }
        app.notedraft.noteall.data.NoteType.LINK_LIST -> label@{ save ->
            LinkListEditor(decodePayload<LinkListPayload>(body, LinkListPayload()) ?: LinkListPayload()) { save(app.notedraft.noteall.data.PayloadCodec.json.encodeToString(LinkListPayload.serializer(), it)) }
        }
        app.notedraft.noteall.data.NoteType.NAMES -> label@{ save ->
            NamesEditor(decodePayload<NameNotesPayload>(body, NameNotesPayload()) ?: NameNotesPayload()) { save(app.notedraft.noteall.data.PayloadCodec.json.encodeToString(NameNotesPayload.serializer(), it)) }
        }
        app.notedraft.noteall.data.NoteType.NOTEBOOK -> label@{ save ->
            NotebookEditor(decodePayload<NotebookPayload>(body, NotebookPayload()) ?: NotebookPayload()) { save(app.notedraft.noteall.data.PayloadCodec.json.encodeToString(NotebookPayload.serializer(), it)) }
        }
        app.notedraft.noteall.data.NoteType.PASSWORD -> label@{ save ->
            PasswordEditor(decodePayload<PasswordPayload>(body, PasswordPayload()) ?: PasswordPayload()) { save(app.notedraft.noteall.data.PayloadCodec.json.encodeToString(PasswordPayload.serializer(), it)) }
        }
        app.notedraft.noteall.data.NoteType.SHOPPING -> label@{ save ->
            ShoppingEditor(decodePayload<ShoppingPayload>(body, ShoppingPayload()) ?: ShoppingPayload()) { save(app.notedraft.noteall.data.PayloadCodec.json.encodeToString(ShoppingPayload.serializer(), it)) }
        }
        app.notedraft.noteall.data.NoteType.SNIPPETS -> label@{ save ->
            SnippetsEditor(decodePayload<SnippetPayload>(body, SnippetPayload()) ?: SnippetPayload()) { save(app.notedraft.noteall.data.PayloadCodec.json.encodeToString(SnippetPayload.serializer(), it)) }
        }
        app.notedraft.noteall.data.NoteType.TIMELINE -> label@{ save ->
            TimelineEditor(decodePayload<TimelinePayload>(body, TimelinePayload()) ?: TimelinePayload()) { save(app.notedraft.noteall.data.PayloadCodec.json.encodeToString(TimelinePayload.serializer(), it)) }
        }
        else -> label@{ _ ->
            TypePlaceholder(note.type)
        }
    }
    Box(Modifier.fillMaxSize().padding(horizontal = 12.dp)) { panel(onSave) }
}

private inline fun <reified T> decodePayload(body: String?, default: T): T? where T : Any =
    body?.let {
        runCatching { app.notedraft.noteall.data.PayloadCodec.json.decodeFromString<T>(it) }.getOrNull()
    }

@Composable
private fun TypePlaceholder(type: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            stringResource(typeLabelRes(type)),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------------------
// 看板 Board
// ---------------------------------------------------------------------------

@Composable
private fun BoardEditor(payload: BoardPayload, onChange: (BoardPayload) -> Unit) {
    var adding by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        LazyRow(
            Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(payload.columns, key = { _, c -> c.id }) { ci, column ->
                Card(
                    Modifier.width(260.dp).fillMaxSize(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text(column.title, style = MaterialTheme.typography.titleSmall)
                        LazyColumn(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            itemsIndexed(column.cards, key = { _, card -> card.id }) { i, card ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Card(Modifier.weight(1f)) {
                                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                card.text.ifBlank { "—" },
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (ci + 1 < payload.columns.size) {
                                                TextButton(onClick = {
                                                    val target = payload.columns[ci + 1]
                                                    val cols = payload.columns.toMutableList()
                                                    cols[ci] = column.copy(cards = column.cards.filterIndexed { idx, _ -> idx != i })
                                                    cols[ci + 1] = target.copy(cards = target.cards + card)
                                                    onChange(payload.copy(columns = cols))
                                                }) { Text("›") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Row {
                            BasicTextField(
                                value = if (ci == payload.columns.size - 1 && adding) draft else "",
                                onValueChange = {
                                    draft = it
                                    adding = true
                                },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier.weight(1f).padding(6.dp),
                                decorationBox = { inner ->
                                    if (draft.isEmpty()) Text(stringResource(R.string.common_add), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    inner()
                                }
                            )
                            IconButton(onClick = {
                                if (draft.isNotBlank()) {
                                    val cols = payload.columns.toMutableList()
                                    cols[ci] = column.copy(cards = column.cards + BoardCard(id = java.util.UUID.randomUUID().toString(), text = draft.trim()))
                                    onChange(payload.copy(columns = cols))
                                    draft = ""
                                }
                            }) { Icon(Icons.Default.Add, null) }
                        }
                    }
                }
            }
            item {
                Column(Modifier.width(160.dp)) {
                    var text by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text(stringResource(R.string.common_add)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(onClick = {
                        if (text.isNotBlank()) {
                            onChange(payload.copy(columns = payload.columns + BoardColumn(java.util.UUID.randomUUID().toString(), text.trim())))
                            text = ""
                        }
                    }) { Text(stringResource(R.string.common_done)) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 计算 Calc
// ---------------------------------------------------------------------------

@Composable
private fun CalcEditor(body: String, onSave: (String) -> Unit) {
    var text by remember(body) { mutableStateOf(body) }
    val result = remember(text) { Evaluate(text) }
    Column(Modifier.fillMaxSize()) {
        BasicTextField(
            value = text,
            onValueChange = {
                text = it
                onSave(it)
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth().weight(1f).padding(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
        )
        Card(
            Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text(
                "= $result",
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

/** 安全的四则运算求值（不使用脚本引擎）。 */
private fun Evaluate(input: String): String {
    val lastLine = input.lines().lastOrNull { it.isNotBlank() } ?: return "0"
    val expr = lastLine.substringAfterLast('=').trim()
    if (expr.isEmpty()) return "0"
    return runCatching {
        val value = ExpressionParser(expr).parse()
        if (value == value.toLong().toDouble()) value.toLong().toString()
        else String.format(Locale.US, "%.6f", value).trimEnd('0').trimEnd('.')
    }.getOrDefault("—")
}

private class ExpressionParser(private val src: String) {
    private var pos = -1
    private var ch = 0

    private fun next() {
        ch = if (++pos < src.length) src[pos].code else -1
    }

    private fun eat(c: Int): Boolean {
        while (ch == ' '.code) next()
        if (ch == c) {
            next()
            return true
        }
        return false
    }

    fun parse(): Double {
        next()
        val x = addSub()
        if (pos < src.length) error("Unexpected: " + ch.toChar())
        return x
    }

    private fun addSub(): Double {
        var x = mulDiv()
        while (true) {
            if (eat('+'.code)) x += mulDiv()
            else if (eat('-'.code)) x -= mulDiv()
            else return x
        }
    }

    private fun mulDiv(): Double {
        var x = unary()
        while (true) {
            if (eat('*'.code)) x *= unary()
            else if (eat('/'.code)) x /= unary()
            else if (eat('%'.code)) x %= unary()
            else return x
        }
    }

    private fun unary(): Double {
        if (eat('-'.code)) return -unary()
        if (eat('+'.code)) return unary()
        return atom()
    }

    private fun atom(): Double {
        if (eat('('.code)) {
            val x = addSub()
            eat(')'.code)
            return x
        }
        val start = pos
        while (ch in '0'.code..'9'.code || ch == '.'.code) next()
        if (start == pos) error("Unexpected end")
        return src.substring(start, pos).toDouble()
    }
}

// ---------------------------------------------------------------------------
// 计数器 Counter
// ---------------------------------------------------------------------------

@Composable
private fun CounterEditor(payload: CounterPayload, onChange: (CounterPayload) -> Unit) {
    var name by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(payload.counters, key = { _, c -> c.id }) { i, counter ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(counter.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                counter.count.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = {
                            val list = payload.counters.toMutableList()
                            list[i] = counter.copy(count = counter.count - 1)
                            onChange(payload.copy(counters = list))
                        }) { Icon(Icons.Default.Remove, null) }
                        IconButton(onClick = {
                            val list = payload.counters.toMutableList()
                            list[i] = counter.copy(count = counter.count + 1)
                            onChange(payload.copy(counters = list))
                        }) { Icon(Icons.Default.Add, null) }
                        IconButton(onClick = {
                            onChange(payload.copy(counters = payload.counters.filterIndexed { idx, _ -> idx != i }))
                        }) { Icon(Icons.Default.Delete, null) }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(stringResource(R.string.common_add)) },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                if (name.isNotBlank()) {
                    onChange(payload.copy(counters = payload.counters + CounterEntry(java.util.UUID.randomUUID().toString(), name.trim())))
                    name = ""
                }
            }) { Icon(Icons.Default.Add, null) }
        }
    }
}

// ---------------------------------------------------------------------------
// 习惯 Habit
// ---------------------------------------------------------------------------

@Composable
private fun HabitEditor(payload: HabitPayload, onChange: (HabitPayload) -> Unit) {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val today = fmt.format(Date())
    val checked = payload.checkedDays.toSet()
    val days = remember { (0 until 35).map { back ->
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -(34 - back)) }.time
    } }
    Column(Modifier.fillMaxSize().padding(top = 8.dp)) {
        Text(
            stringResource(R.string.type_habit),
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(days.chunked(7)) { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { day ->
                        val key = fmt.format(day)
                        val on = key in checked
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(
                                    if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    if (key == today) 2.dp else 0.dp,
                                    MaterialTheme.colorScheme.tertiary,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    val next = if (on) checked - key else checked + key
                                    onChange(payload.copy(checkedDays = next.sorted()))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                key.substring(8),
                                fontSize = 12.sp,
                                color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "${checked.size} / 35",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------------------
// 链接 Links / 名单 Names / 密码 Password / 购物 Shopping / 片段 Snippets
// ---------------------------------------------------------------------------

@Composable
private fun LinkListEditor(payload: LinkListPayload, onChange: (LinkListPayload) -> Unit) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f)) {
            payload.tabs.forEach { tab ->
                item { Text(tab.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 6.dp)) }
                items(tab.links, key = { it.id }) { link ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(link.title.ifBlank { link.url }, style = MaterialTheme.typography.titleSmall)
                            Text(link.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(title, { title = it }, Modifier.weight(1f), singleLine = true, placeholder = { Text(stringResource(R.string.note_title_hint)) })
            Spacer(Modifier.width(4.dp))
            OutlinedTextField(url, { url = it }, Modifier.weight(1f), singleLine = true, placeholder = { Text(stringResource(R.string.link_url)) })
            IconButton(onClick = {
                if (url.isNotBlank()) {
                    val tabs = payload.tabs.ifEmpty { listOf(LinkTab("t1", "", "Links")) }
                    val first = tabs.first()
                    val updated = tabs.toMutableList()
                    updated[0] = first.copy(links = first.links + LinkEntry(java.util.UUID.randomUUID().toString(), title.ifBlank { url }, url.trim()))
                    onChange(payload.copy(tabs = updated))
                    title = ""
                    url = ""
                }
            }) { Icon(Icons.Default.Add, null) }
        }
    }
}

@Composable
private fun NamesEditor(payload: NameNotesPayload, onChange: (NameNotesPayload) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f)) {
            payload.tabs.forEach { tab ->
                items(tab.items, key = { it.id }) { person ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(36.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(person.name.take(1).uppercase(), fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(person.name, style = MaterialTheme.typography.titleSmall)
                                person.phone?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(name, { name = it }, Modifier.weight(1f), singleLine = true, placeholder = { Text(stringResource(R.string.note_title_hint)) })
            Spacer(Modifier.width(4.dp))
            OutlinedTextField(phone, { phone = it }, Modifier.weight(1f), singleLine = true, placeholder = { Text(stringResource(R.string.link_url)) })
            IconButton(onClick = {
                if (name.isNotBlank()) {
                    val tabs = payload.tabs.ifEmpty { listOf(NameTab("n1", "", "Names")) }
                    val updated = tabs.toMutableList()
                    updated[0] = tabs[0].copy(items = tabs[0].items + NamePerson(java.util.UUID.randomUUID().toString(), "", name.trim(), phone = phone.ifBlank { null }))
                    onChange(payload.copy(tabs = updated))
                    name = ""
                    phone = ""
                }
            }) { Icon(Icons.Default.Add, null) }
        }
    }
}

@Composable
private fun PasswordEditor(payload: PasswordPayload, onChange: (PasswordPayload) -> Unit) {
    var revealed by remember { mutableStateOf<Set<String>>(emptySet()) }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            payload.tabs.forEach { tab ->
                items(tab.items, key = { it.id }) { entry ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Column(
                            Modifier.fillMaxWidth().padding(12.dp).clickable {
                                revealed = if (entry.id in revealed) revealed - entry.id else revealed + entry.id
                            }
                        ) {
                            Text(entry.title, style = MaterialTheme.typography.titleSmall)
                            entry.username?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            Text(
                                if (entry.id in revealed) entry.password.orEmpty() else "••••••••",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingEditor(payload: ShoppingPayload, onChange: (ShoppingPayload) -> Unit) {
    var text by remember { mutableStateOf("") }
    val all = payload.tabs.flatMap { it.items }
    val total = all.sumOf { it.price?.toDoubleOrNull() ?: 0.0 }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(all, key = { _, item -> item.id }) { _, item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(item.checked, onCheckedChange = { checked ->
                        val tabs = payload.tabs.map { tab ->
                            tab.copy(items = tab.items.map { if (it.id == item.id) it.copy(checked = checked) else it })
                        }
                        onChange(payload.copy(tabs = tabs))
                    })
                    Text(
                        item.title,
                        modifier = Modifier.weight(1f),
                        textDecoration = if (item.checked) TextDecoration.LineThrough else null
                    )
                    item.price?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
        Text(
            stringResource(R.string.type_shopping) + "  " + String.format(Locale.US, "%.2f", total),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(text, { text = it }, Modifier.weight(1f), placeholder = { Text(stringResource(R.string.common_add)) })
            IconButton(onClick = {
                if (text.isNotBlank()) {
                    val tabs = payload.tabs.ifEmpty { listOf(ShoppingTab("s1", "", "Shopping")) }
                    val updated = tabs.toMutableList()
                    updated[0] = tabs[0].copy(items = tabs[0].items + ShoppingItem(java.util.UUID.randomUUID().toString(), "", text.trim()))
                    onChange(payload.copy(tabs = updated))
                    text = ""
                }
            }) { Icon(Icons.Default.Add, null) }
        }
    }
}

@Composable
private fun SnippetsEditor(payload: SnippetPayload, onChange: (SnippetPayload) -> Unit) {
    var text by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f)) {
            payload.tabs.forEach { tab ->
                items(tab.items, key = { it.id }) { snippet ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(
                            snippet.content,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(text, { text = it }, Modifier.weight(1f), placeholder = { Text(stringResource(R.string.common_add)) })
            IconButton(onClick = {
                if (text.isNotBlank()) {
                    val tabs = payload.tabs.ifEmpty { listOf(SnippetTab("c1", "", "Snippets")) }
                    val updated = tabs.toMutableList()
                    updated[0] = tabs[0].copy(items = tabs[0].items + SnippetEntry(java.util.UUID.randomUUID().toString(), "", text))
                    onChange(payload.copy(tabs = updated))
                    text = ""
                }
            }) { Icon(Icons.Default.Add, null) }
        }
    }
}

// ---------------------------------------------------------------------------
// 笔记本 Notebook
// ---------------------------------------------------------------------------

@Composable
private fun NotebookEditor(payload: NotebookPayload, onChange: (NotebookPayload) -> Unit) {
    var text by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        val tab = payload.tabs.firstOrNull() ?: NotebookTab("nb1", "", "Notes")
        LazyColumn(Modifier.weight(1f)) {
            items(tab.items, key = { it.id }) { note ->
                Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        val first = note.text.lines().firstOrNull().orEmpty()
                        Text(first.ifBlank { "—" }, style = MaterialTheme.typography.titleSmall)
                        val rest = note.text.lines().drop(1).joinToString(" ").trim()
                        if (rest.isNotEmpty()) {
                            Text(rest, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(text, { text = it }, Modifier.weight(1f), placeholder = { Text(stringResource(R.string.nb_new_note)) })
            IconButton(onClick = {
                if (text.isNotBlank()) {
                    val tabs = payload.tabs.ifEmpty { listOf(tab) }
                    val updated = tabs.toMutableList()
                    updated[0] = tabs[0].copy(
                        items = tabs[0].items + NotebookNote(
                            java.util.UUID.randomUUID().toString(), "", text.trim(), System.currentTimeMillis()
                        )
                    )
                    onChange(payload.copy(tabs = updated))
                    text = ""
                }
            }) { Icon(Icons.Default.Add, null) }
        }
    }
}

// ---------------------------------------------------------------------------
// 日程 Timeline
// ---------------------------------------------------------------------------

@Composable
private fun TimelineEditor(payload: TimelinePayload, onChange: (TimelinePayload) -> Unit) {
    var title by remember { mutableStateOf("") }
    val all = payload.tabs.flatMap { it.entries }.sortedWith(compareBy({ it.date }, { it.hour }, { it.minute }))
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f)) {
            items(all, key = { it.id }) { entry ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.width(64.dp)) {
                        Text(entry.date.takeLast(5), style = MaterialTheme.typography.labelMedium)
                        Text(
                            String.format(Locale.US, "%02d:%02d", entry.hour, entry.minute),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(entry.title, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(title, { title = it }, Modifier.weight(1f), placeholder = { Text(stringResource(R.string.common_add)) })
            IconButton(onClick = {
                if (title.isNotBlank()) {
                    val cal = Calendar.getInstance()
                    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val tabs = payload.tabs.ifEmpty { listOf(TimelineTab("tl1", "", "Schedule")) }
                    val updated = tabs.toMutableList()
                    updated[0] = tabs[0].copy(
                        entries = tabs[0].entries + TimelineEntry(
                            java.util.UUID.randomUUID().toString(),
                            fmt.format(cal.time),
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            "",
                            title.trim()
                        )
                    )
                    onChange(payload.copy(tabs = updated))
                    title = ""
                }
            }) { Icon(Icons.Default.Add, null) }
        }
    }
}