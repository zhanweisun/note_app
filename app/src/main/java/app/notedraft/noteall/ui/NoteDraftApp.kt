package app.notedraft.noteall.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.notedraft.noteall.ui.note.NoteEditorScreen
import app.notedraft.noteall.ui.settings.SettingsScreen
import app.notedraft.noteall.ui.theme.NoteDraftTheme

/** 顶层导航目的地。 */
private sealed interface Route {
    data object Home : Route
    data class Editor(val id: Long) : Route
    data object Settings : Route
}

@Composable
fun NoteDraftApp(vm: MainViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var route by remember { mutableStateOf<Route>(Route.Home) }

    NoteDraftTheme(settings) {
        AnimatedContent(
            targetState = route,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "route"
        ) { target ->
            when (target) {
                is Route.Editor -> NoteEditorScreen(
                    repo = vm.repo,
                    noteId = target.id,
                    onBack = {
                        route = Route.Home
                        vm.refresh()
                    },
                    onDeleted = {
                        route = Route.Home
                        vm.refresh()
                    }
                )
                Route.Settings -> SettingsScreen(vm) { route = Route.Home }
                Route.Home -> MainScreen(
                    vm = vm,
                    onOpenNote = { note -> route = Route.Editor(note.id) },
                    onOpenSettings = { route = Route.Settings }
                )
            }
        }
    }
}