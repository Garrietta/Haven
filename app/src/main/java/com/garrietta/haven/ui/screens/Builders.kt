package com.garrietta.haven.ui.screens
import androidx.compose.foundation.layout.PaddingValues
import com.garrietta.haven.ui.components.pillClearanceBottom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garrietta.haven.DetailTopBar
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.ui.controls.stateText
import org.json.JSONArray
import org.json.JSONObject

private fun putIfNotBlank(obj: JSONObject, key: String, value: String) {
    if (value.isNotBlank()) obj.put(key, value.trim())
}

@Composable
fun AutomationBuilderScreen(vm: HavenViewModel, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var triggerType by remember { mutableStateOf("state") }
    var triggerTypeExpanded by remember { mutableStateOf(false) }
    var triggerEntity by remember { mutableStateOf("") }
    var triggerFrom by remember { mutableStateOf("") }
    var triggerTo by remember { mutableStateOf("") }
    var triggerAt by remember { mutableStateOf("") }
    var triggerAbove by remember { mutableStateOf("") }
    var triggerBelow by remember { mutableStateOf("") }
    var sunEvent by remember { mutableStateOf("sunrise") }
    var useCondition by remember { mutableStateOf(false) }
    var conditionEntity by remember { mutableStateOf("") }
    var conditionState by remember { mutableStateOf("") }
    var actionDomain by remember { mutableStateOf("") }
    var actionService by remember { mutableStateOf("") }
    var actionEntity by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = pillClearanceBottom()),
    ) {
        item {
            DetailTopBar("New automation", onBack)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Text("When this happens", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { triggerTypeExpanded = true }) { Text(triggerType) }
                DropdownMenu(expanded = triggerTypeExpanded, onDismissRequest = { triggerTypeExpanded = false }) {
                    for (t in listOf("state", "numeric_state", "time", "sun")) {
                        DropdownMenuItem(text = { Text(t) }, onClick = { triggerType = t; triggerTypeExpanded = false })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        when (triggerType) {
            "state", "numeric_state" -> {
                item {
                    OutlinedTextField(value = triggerEntity, onValueChange = { triggerEntity = it }, label = { Text("Entity id") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                }
                if (triggerType == "state") {
                    item {
                        Row {
                            OutlinedTextField(value = triggerFrom, onValueChange = { triggerFrom = it }, label = { Text("From (optional)") }, singleLine = true, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(value = triggerTo, onValueChange = { triggerTo = it }, label = { Text("To") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                } else {
                    item {
                        Row {
                            OutlinedTextField(value = triggerAbove, onValueChange = { triggerAbove = it }, label = { Text("Above (optional)") }, singleLine = true, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(value = triggerBelow, onValueChange = { triggerBelow = it }, label = { Text("Below (optional)") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            "time" -> {
                item {
                    OutlinedTextField(value = triggerAt, onValueChange = { triggerAt = it }, label = { Text("Time (HH:MM:SS)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                }
            }
            "sun" -> {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        var sunExpanded by remember { mutableStateOf(false) }
                        OutlinedButton(onClick = { sunExpanded = true }) { Text(sunEvent) }
                        DropdownMenu(expanded = sunExpanded, onDismissRequest = { sunExpanded = false }) {
                            for (e in listOf("sunrise", "sunset")) {
                                DropdownMenuItem(text = { Text(e) }, onClick = { sunEvent = e; sunExpanded = false })
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Only if…", modifier = Modifier.weight(1f))
                Switch(checked = useCondition, onCheckedChange = { useCondition = it })
            }
            if (useCondition) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = conditionEntity, onValueChange = { conditionEntity = it }, label = { Text("Entity id") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = conditionState, onValueChange = { conditionState = it }, label = { Text("Is state") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(16.dp))
            Text("Then do this", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Row {
                OutlinedTextField(value = actionDomain, onValueChange = { actionDomain = it }, label = { Text("Domain (e.g. light)") }, singleLine = true, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(value = actionService, onValueChange = { actionService = it }, label = { Text("Service (e.g. turn_on)") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = actionEntity, onValueChange = { actionEntity = it }, label = { Text("Target entity id") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val trigger = JSONObject().put("platform", triggerType)
                    when (triggerType) {
                        "state" -> {
                            putIfNotBlank(trigger, "entity_id", triggerEntity)
                            putIfNotBlank(trigger, "from", triggerFrom)
                            putIfNotBlank(trigger, "to", triggerTo)
                        }
                        "numeric_state" -> {
                            putIfNotBlank(trigger, "entity_id", triggerEntity)
                            triggerAbove.toDoubleOrNull()?.let { trigger.put("above", it) }
                            triggerBelow.toDoubleOrNull()?.let { trigger.put("below", it) }
                        }
                        "time" -> putIfNotBlank(trigger, "at", triggerAt)
                        "sun" -> trigger.put("event", sunEvent)
                    }
                    val action = JSONObject().put("service", "${actionDomain.trim()}.${actionService.trim()}")
                    if (actionEntity.isNotBlank()) {
                        action.put("target", JSONObject().put("entity_id", actionEntity.trim()))
                    }
                    val config = JSONObject()
                        .put("alias", name.trim())
                        .put("trigger", JSONArray().put(trigger))
                        .put("action", JSONArray().put(action))
                        .put("mode", "single")
                    if (useCondition) {
                        val cond = JSONObject().put("condition", "state")
                        putIfNotBlank(cond, "entity_id", conditionEntity)
                        putIfNotBlank(cond, "state", conditionState)
                        config.put("condition", JSONArray().put(cond))
                    }
                    vm.saveAutomation(config) { ok ->
                        status = if (ok) "Automation saved to your instance." else "Save failed — check the connection and try again."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save automation") }
            status?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SceneBuilderScreen(vm: HavenViewModel, onBack: () -> Unit) {
    val states by vm.states.collectAsState()
    var name by remember { mutableStateOf("") }
    val checked = remember { mutableStateListOf<String>() }
    var status by remember { mutableStateOf<String?>(null) }
    val candidates = states.values
        .filter { it.domain in listOf("light", "switch", "cover", "climate", "fan", "lock", "media_player") }
        .sortedBy { it.friendlyName }
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = pillClearanceBottom()),
    ) {
        item {
            DetailTopBar("New scene", onBack)
            Text("Tick the devices to capture, then save — their current states become the scene.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Scene name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }
        items(candidates) { entity ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = checked.contains(entity.entityId),
                    onCheckedChange = { on ->
                        if (on) checked.add(entity.entityId) else checked.remove(entity.entityId)
                    },
                )
                Column {
                    Text(entity.friendlyName)
                    Text(entity.stateText(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val entitiesObj = JSONObject()
                    for (id in checked) {
                        val entity = states[id] ?: continue
                        val stateObj = JSONObject().put("state", entity.state)
                        entity.attrInt("brightness")?.let { stateObj.put("brightness", it) }
                        entity.attrInt("color_temp_kelvin")?.let { stateObj.put("color_temp_kelvin", it) }
                        entity.attrInt("current_position")?.let { stateObj.put("current_position", it) }
                        entity.attrDouble("temperature")?.let { stateObj.put("temperature", it) }
                        entity.attrString("hvac_mode")?.let { stateObj.put("hvac_mode", it) }
                        entity.attrInt("percentage")?.let { stateObj.put("percentage", it) }
                        entitiesObj.put(id, stateObj)
                    }
                    val config = JSONObject().put("name", name.trim()).put("entities", entitiesObj)
                    vm.saveScene(config) { ok ->
                        status = if (ok) "Scene saved to your instance." else "Save failed — check the connection and try again."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save scene") }
            status?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun ScriptBuilderScreen(vm: HavenViewModel, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val steps = remember { mutableStateListOf<Triple<String, String, String>>() }
    var status by remember { mutableStateOf<String?>(null) }
    if (steps.isEmpty()) steps.add(Triple("", "", ""))
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = pillClearanceBottom()),
    ) {
        item {
            DetailTopBar("New script", onBack)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Script name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Text("Steps", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
        }
        items(steps.size) { index ->
            val (domain, service, entity) = steps[index]
            Column {
                Row {
                    OutlinedTextField(value = domain, onValueChange = { steps[index] = Triple(it, service, entity) }, label = { Text("Domain") }, singleLine = true, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(value = service, onValueChange = { steps[index] = Triple(domain, it, entity) }, label = { Text("Service") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = entity, onValueChange = { steps[index] = Triple(domain, service, it) }, label = { Text("Target entity id") }, singleLine = true, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { steps.removeAt(index) }) { Text("Remove") }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        item {
            OutlinedButton(onClick = { steps.add(Triple("", "", "")) }) { Text("Add step") }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val sequence = JSONArray()
                    for ((domain, service, entity) in steps) {
                        if (domain.isBlank() || service.isBlank()) continue
                        val step = JSONObject().put("service", "${domain.trim()}.${service.trim()}")
                        if (entity.isNotBlank()) step.put("target", JSONObject().put("entity_id", entity.trim()))
                        sequence.put(step)
                    }
                    val config = JSONObject().put("alias", name.trim()).put("sequence", sequence)
                    vm.saveScript(config) { ok ->
                        status = if (ok) "Script saved to your instance." else "Save failed — check the connection and try again."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save script") }
            status?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun HelperBuilderScreen(vm: HavenViewModel, onBack: () -> Unit) {
    var type by remember { mutableStateOf("input_boolean") }
    var typeExpanded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var min by remember { mutableStateOf("0") }
    var max by remember { mutableStateOf("100") }
    var step by remember { mutableStateOf("1") }
    var options by remember { mutableStateOf("") }
    var hasDate by remember { mutableStateOf(true) }
    var hasTime by remember { mutableStateOf(false) }
    var duration by remember { mutableStateOf("00:05:00") }
    var initial by remember { mutableStateOf("0") }
    var status by remember { mutableStateOf<String?>(null) }
    val types = listOf("input_boolean", "input_number", "input_select", "input_text", "input_datetime", "timer", "counter")
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = pillClearanceBottom()),
    ) {
        item {
            DetailTopBar("New helper", onBack)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Type: ", fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = { typeExpanded = true }) { Text(type) }
                DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    for (t in types) {
                        DropdownMenuItem(text = { Text(t) }, onClick = { type = t; typeExpanded = false })
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            when (type) {
                "input_number" -> {
                    Row {
                        OutlinedTextField(value = min, onValueChange = { min = it }, label = { Text("Min") }, singleLine = true, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(value = max, onValueChange = { max = it }, label = { Text("Max") }, singleLine = true, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(value = step, onValueChange = { step = it }, label = { Text("Step") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                }
                "input_select" -> {
                    OutlinedTextField(value = options, onValueChange = { options = it }, label = { Text("Options (comma-separated)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                "input_datetime" -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Has date", modifier = Modifier.weight(1f))
                        Switch(checked = hasDate, onCheckedChange = { hasDate = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Has time", modifier = Modifier.weight(1f))
                        Switch(checked = hasTime, onCheckedChange = { hasTime = it })
                    }
                }
                "timer" -> {
                    OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration (HH:MM:SS)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                "counter" -> {
                    Row {
                        OutlinedTextField(value = initial, onValueChange = { initial = it }, label = { Text("Initial") }, singleLine = true, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(value = step, onValueChange = { step = it }, label = { Text("Step") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val config = JSONObject().put("name", name.trim())
                    when (type) {
                        "input_number" -> {
                            min.toDoubleOrNull()?.let { config.put("min", it) }
                            max.toDoubleOrNull()?.let { config.put("max", it) }
                            step.toDoubleOrNull()?.let { config.put("step", it) }
                        }
                        "input_select" -> {
                            val opts = JSONArray()
                            for (opt in options.split(",").map { it.trim() }.filter { it.isNotEmpty() }) opts.put(opt)
                            config.put("options", opts)
                        }
                        "input_datetime" -> {
                            config.put("has_date", hasDate)
                            config.put("has_time", hasTime)
                        }
                        "timer" -> config.put("duration", duration.trim())
                        "counter" -> {
                            initial.toIntOrNull()?.let { config.put("initial", it) }
                            step.toIntOrNull()?.let { config.put("step", it) }
                        }
                    }
                    vm.saveHelper(type, config) { ok ->
                        status = if (ok) "Helper saved to your instance." else "Save failed — check the connection and try again."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save helper") }
            status?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
