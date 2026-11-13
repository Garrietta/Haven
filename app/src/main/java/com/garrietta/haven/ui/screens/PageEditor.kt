package com.garrietta.haven.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.ui.components.ColumnReorderHandle
import com.garrietta.haven.ui.components.ScreenTitle
import com.garrietta.haven.ui.components.SectionTitle
import com.garrietta.haven.ui.components.columnReorderRow
import com.garrietta.haven.ui.components.rememberColumnReorder
import com.garrietta.haven.ui.controls.stateText

/** One thing on a tab, as its editor lists it. */
internal class EditorItem(val id: String, val title: String, val subtitle: String)

/** One section of a tab in its editor: what it shows, in order, and what was taken off it. */
internal class EditorSection(val key: String, val title: String, val shown: List<EditorItem>, val takenOff: List<EditorItem>)

/** An entity as an editor row: its name, and what kind of thing it is and its state. */
internal fun editorItem(entity: HaEntity?, id: String): EditorItem =
    EditorItem(id, entity?.friendlyName ?: id, entity?.let { "${it.domain.replace('_', ' ')} · ${it.stateText()}" } ?: id)

/**
 * A tab's title with Edit beside it, which turns the tab into its editor
 * and back.
 */
@Composable
internal fun EditableTitle(title: String, editing: Boolean, onToggle: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ScreenTitle(title, Modifier.weight(1f))
        TextButton(onClick = onToggle) { Text(if (editing) "Done" else "Edit") }
    }
}

/**
 * A tab as its editor: each section's items with grips to drag them into
 * order and a cross to take them off, what was taken off with Show to
 * bring it back, and a search to add any entity, which stays open so
 * several can be added in a row. [onAdd] decides which section an added
 * entity joins.
 */
internal fun LazyListScope.pageEditor(
    vm: HavenViewModel,
    sections: List<EditorSection>,
    candidates: (String) -> List<HaEntity>,
    query: String,
    onQuery: (String) -> Unit,
    onAdd: (HaEntity) -> Unit,
) {
    for (section in sections) {
        item(key = "editor-" + section.key) {
            Spacer(Modifier.height(12.dp))
            SectionTitle(section.title)
            Spacer(Modifier.height(4.dp))
            if (section.shown.isEmpty()) {
                Text("Nothing here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val ids = section.shown.map { it.id }
            val reorder = rememberColumnReorder(section.shown.size) { from, to ->
                vm.updateCuration(section.key) { it.moved(ids, from, to) }
            }
            Column {
                section.shown.forEachIndexed { index, item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .columnReorderRow(reorder, index)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(vertical = 2.dp),
                    ) {
                        ColumnReorderHandle(
                            reorder, index,
                            label = item.title,
                            onStep = { delta -> vm.updateCuration(section.key) { it.moved(ids, index, index + delta) } },
                            modifier = Modifier.size(40.dp).padding(8.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { vm.updateCuration(section.key) { it.remove(item.id) } }) {
                            Icon(Icons.Filled.Close, contentDescription = "Take ${item.title} off")
                        }
                    }
                }
            }
            if (section.takenOff.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Taken off", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                section.takenOff.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(item.title, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(start = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        TextButton(onClick = { vm.updateCuration(section.key) { it.show(item.id) } }) { Text("Show") }
                    }
                }
            }
        }
    }
    item(key = "editor-add") {
        Spacer(Modifier.height(16.dp))
        SectionTitle("Add to this tab")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            label = { Text("Search entities") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        val matches = candidates(query.trim().lowercase())
        if (matches.isEmpty()) {
            Text(
                if (query.isBlank()) "Type to find an entity." else "No entities match \"$query\".",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        matches.forEach { entity ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(entity.friendlyName, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(entity.entityId, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(onClick = { onAdd(entity) }) { Text("Add") }
            }
        }
    }
}
