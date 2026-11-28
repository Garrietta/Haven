package com.garrietta.haven.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.garrietta.haven.DetailTopBar
import com.garrietta.haven.ui.components.choiceChipColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/*
 * The pieces every settings page is made of, so each page reads as the
 * same kind of thing: a back bar and title, an optional line under it,
 * an optional row of tabs, and a list. Lists load on demand and say
 * plainly when Home Assistant could not be reached, rather than showing
 * an empty list as though it were the answer.
 */

/** A settings page: the back bar, a line saying what the page is for, and its list. */
@Composable
fun SettingsPage(
    title: String,
    onBack: () -> Unit,
    intro: String? = null,
    listState: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().background(MaterialTheme.colorScheme.background)) {
        DetailTopBar(title, onBack)
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            state = listState,
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            if (intro != null) {
                item(key = "intro") {
                    Text(intro, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                }
            }
            content()
        }
    }
}

/** One of a page's tabs, drawn as the lime capsule choice like every other one-of-several. */
@Composable
fun SettingsTabs(tabs: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for ((key, label) in tabs) {
            FilterChip(
                selected = key == selected,
                onClick = { onSelect(key) },
                label = { Text(label) },
                colors = choiceChipColors(),
            )
        }
    }
}

/** A search field over a list. */
@Composable
fun SettingsSearch(query: String, onQuery: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    )
}

/** A list loaded from Home Assistant: still loading, could not load, or loaded. */
sealed class Loaded<out T> {
    data object Loading : Loaded<Nothing>()
    data object Failed : Loaded<Nothing>()
    data class Ready<T>(val value: T) : Loaded<T>()
}

/** Holds a load and lets the page run it again after a change. */
class LoadHandle<T> internal constructor(private val scope: CoroutineScope, private val fetch: suspend () -> T?) {
    var state by mutableStateOf<Loaded<T>>(Loaded.Loading)
        private set

    /** Reloads without blanking what is already shown. */
    fun reload() {
        scope.launch {
            val value = fetch()
            state = if (value == null) (state as? Loaded.Ready) ?: Loaded.Failed else Loaded.Ready(value)
        }
    }

    internal suspend fun first() {
        val value = fetch()
        state = if (value == null) Loaded.Failed else Loaded.Ready(value)
    }
}

@Composable
fun <T> rememberLoad(vararg keys: Any?, fetch: suspend () -> T?): LoadHandle<T> {
    val scope = rememberCoroutineScope()
    val handle = remember(*keys) { LoadHandle(scope, fetch) }
    LaunchedEffect(handle) { handle.first() }
    return handle
}

/** What a list says while loading or when Home Assistant did not answer. */
fun <T> LazyListScope.loadedItems(
    load: LoadHandle<T>,
    what: String,
    content: LazyListScope.(T) -> Unit,
) {
    when (val state = load.state) {
        Loaded.Loading -> item(key = "loading") { SettingsNote("Loading $what…") }
        Loaded.Failed -> item(key = "failed") {
            Column {
                SettingsNote("Could not load $what from Home Assistant. Check the connection, or that your account is an administrator.")
                TextButton(onClick = { load.reload() }) { Text("Try again") }
            }
        }
        is Loaded.Ready -> content(state.value)
    }
}

/** A quiet line of explanation. */
@Composable
fun SettingsNote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

/**
 * The outcome of the last change on a page, shown under its title:
 * what was done, or Home Assistant's reason for refusing it.
 */
class SettingsStatus {
    var text by mutableStateOf<String?>(null)
        private set
    var isError by mutableStateOf(false)
        private set
    private var serial by mutableIntStateOf(0)

    /** [error] null means [done] happened. */
    fun report(error: String?, done: String) {
        text = error ?: done
        isError = error != null
        serial++
    }
}

@Composable
fun rememberSettingsStatus(): SettingsStatus = remember { SettingsStatus() }

fun LazyListScope.statusItem(status: SettingsStatus) {
    val text = status.text ?: return
    item(key = "status") {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = if (status.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
}

/** Asks before something that cannot be undone. */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            com.garrietta.haven.ui.controls.DialogActionButtons(
                onCancel = onDismiss,
                onConfirm = {
                    onDismiss()
                    onConfirm()
                },
                confirmLabel = confirmLabel,
            )
        },
    )
}

/** Asks for one line of text: a name, an icon, an id. */
@Composable
fun TextPromptDialog(
    title: String,
    label: String,
    initial: String,
    onDismiss: () -> Unit,
    confirmLabel: String = "Save",
    supporting: String? = null,
    numeric: Boolean = false,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(label) },
                    singleLine = true,
                    keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (supporting != null) SettingsNote(supporting)
            }
        },
        confirmButton = {
            com.garrietta.haven.ui.controls.DialogActionButtons(
                onCancel = onDismiss,
                onConfirm = {
                    onDismiss()
                    onConfirm(value.trim())
                },
                confirmLabel = confirmLabel,
            )
        },
    )
}

/** Picks one of several; [options] are (value, label), and a null value is allowed ("none"). */
@Composable
fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                for ((value, label) in options) {
                    Box(Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = value == selected,
                            onClick = {
                                onDismiss()
                                onSelect(value)
                            },
                            label = { Text(label) },
                            colors = choiceChipColors(),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

/** A text field for a config flow or a form: password fields hide what is typed. */
@Composable
fun FormTextField(label: String, value: String, onValue: (String) -> Unit, password: Boolean = false, numeric: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = when {
            password -> KeyboardOptions(keyboardType = KeyboardType.Password)
            numeric -> KeyboardOptions(keyboardType = KeyboardType.Decimal)
            else -> KeyboardOptions.Default
        },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    )
}

/**
 * A long list drawn as one group, a row per lazy item: the first row
 * carries the group's top corners and the last its bottom ones, with a
 * hairline between, so hundreds of entities scroll as cheaply as a few.
 */
fun <T> LazyListScope.groupedRows(
    rows: List<T>,
    key: (T) -> Any,
    row: @Composable (T) -> Unit,
) {
    val count = rows.size
    rows.forEachIndexed { index, value ->
        item(key = key(value)) {
            val top = if (index == 0) com.garrietta.haven.ui.theme.Corner.xl else 0.dp
            val bottom = if (index == count - 1) com.garrietta.haven.ui.theme.Corner.xl else 0.dp
            Column(
                Modifier.fillMaxWidth().background(
                    MaterialTheme.colorScheme.surfaceContainer,
                    androidx.compose.foundation.shape.RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom),
                ),
            ) {
                if (index > 0) com.garrietta.haven.ui.components.MenuDivider()
                row(value)
            }
        }
    }
    item(key = "group-end-${rows.hashCode()}") { Spacer(Modifier.height(16.dp)) }
}
