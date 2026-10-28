package com.garrietta.haven.ui.controls

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.garrietta.haven.HavenViewModel
import com.garrietta.haven.data.HaEntity
import com.garrietta.haven.ui.components.Dial
import com.garrietta.haven.ui.components.PillToggle
import com.garrietta.haven.ui.components.accentFor
import org.json.JSONArray
import org.json.JSONObject

/** Second half of the domain dispatch (called from EntityControlCard). */
@Composable
fun MoreControl(entity: HaEntity, vm: HavenViewModel) {
    when (entity.domain) {
        "sensor", "binary_sensor" -> SensorTile(entity)
        "weather" -> WeatherControl(entity, vm)
        "camera" -> CameraControl(entity, vm)
        "scene" -> FilledTonalButton(onClick = { vm.turnOn(entity.entityId) }) { Text("Activate scene") }
        "script" -> FilledTonalButton(onClick = { vm.turnOn(entity.entityId) }) { Text("Run script") }
        "automation" -> AutomationControl(entity, vm)
        "todo" -> TodoControl(entity, vm)
        "input_boolean" -> Row(verticalAlignment = Alignment.CenterVertically) {
            PillToggle(entity.state == "on", { vm.toggle(entity.entityId) }, accentFor(entity.entityId))
            Spacer(Modifier.width(12.dp))
            Text(if (entity.state == "on") "On" else "Off", fontWeight = FontWeight.SemiBold)
        }
        "input_number" -> InputNumberControl(entity, vm)
        "input_select" -> InputSelectControl(entity, vm)
        "input_text" -> InputTextControl(entity, vm)
        "input_datetime" -> InputDatetimeControl(entity, vm)
        "timer" -> TimerControl(entity, vm)
        "counter" -> CounterControl(entity, vm)
        "alarm_control_panel" -> AlarmControl(entity, vm)
        "person", "device_tracker" -> Text(
            if (entity.state == "home") "Home" else entity.stateText(),
            fontWeight = FontWeight.SemiBold,
        )
        else -> Text(entity.stateText(), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SensorTile(entity: HaEntity) {
    val unit = entity.attrString("unit_of_measurement")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            entity.state,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = accentFor(entity.entityId),
        )
        if (unit != null) {
            Spacer(Modifier.width(6.dp))
            Text(unit, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        val deviceClass = entity.attrString("device_class")
        if (deviceClass != null) Text(deviceClass.replace('_', ' '), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun WeatherControl(entity: HaEntity, vm: HavenViewModel) {
    val accent = accentFor(entity.entityId)
    val temp = entity.attrDouble("temperature") ?: 0.0
    val humidity = entity.attrDouble("humidity")
    var forecast by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    LaunchedEffect(entity.entityId) {
        val attr = entity.attrList("forecast")?.mapNotNull { it as? Map<String, Any?> }
        if (!attr.isNullOrEmpty()) {
            forecast = attr
        } else {
            vm.client.sendCommand(
                "call_service",
                build = {
                    it.put("domain", "weather")
                    it.put("service", "get_forecasts")
                    it.put("target", JSONObject().put("entity_id", entity.entityId))
                    it.put("service_data", JSONObject().put("type", "hourly"))
                    it.put("return_response", true)
                },
                onResult = { msg ->
                    val response = msg.optJSONObject("result")?.optJSONObject("response")
                    val arr: JSONArray? = when {
                        response == null -> null
                        response.has(entity.entityId) -> response.optJSONObject(entity.entityId)?.optJSONArray("forecast")
                        response.has("forecast") -> response.optJSONArray("forecast")
                        else -> null
                    }
                    if (arr != null) {
                        val list = mutableListOf<Map<String, Any?>>()
                        for (i in 0 until arr.length()) {
                            val item = HaEntity.jsonToAny(arr.optJSONObject(i)) as? Map<String, Any?>
                            if (item != null) list.add(item)
                        }
                        Handler(Looper.getMainLooper()).post { forecast = list }
                    }
                },
            )
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Dial(
            value = temp.toFloat(),
            range = -10f..40f,
            onValueChange = {},
            centerText = temp.formatTemp(),
            centerSub = entity.state.replace('_', ' ').replaceFirstChar { it.uppercase() },
            accent = accent,
        )
        if (humidity != null) {
            Text("Humidity ${humidity.toInt()}%", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        for (item in forecast.take(6)) {
            val whenRaw = (item["datetime"] as? String) ?: ""
            val whenLabel = whenRaw.replace('T', ' ').take(16)
            val fTemp = (item["temperature"] as? Number)?.toDouble()
            val condition = (item["condition"] as? String) ?: ""
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(whenLabel, fontSize = 13.sp)
                Text(condition.replace('_', ' '), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (fTemp != null) Text(fTemp.formatTemp(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
fun CameraControl(entity: HaEntity, vm: HavenViewModel) {
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var fullscreen by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    LaunchedEffect(entity.entityId, refreshKey) {
        bitmap = vm.client.fetchCameraBitmap(entity.entityId)
    }
    Column {
        if (bitmap != null) {
            Box(
                Modifier.fillMaxWidth().height(180.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                    .clickable { fullscreen = true },
            ) {
                Image(
                    bitmap!!.asImageBitmap(),
                    contentDescription = entity.friendlyName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            Box(
                Modifier.fillMaxWidth().height(120.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("No snapshot yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { refreshKey++ }) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Refresh snapshot")
        }
    }
    if (fullscreen && bitmap != null) {
        Dialog(onDismissRequest = { fullscreen = false }) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Image(
                    bitmap!!.asImageBitmap(),
                    contentDescription = entity.friendlyName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().clickable { fullscreen = false },
                )
                IconButton(onClick = { fullscreen = false }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
fun AutomationControl(entity: HaEntity, vm: HavenViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PillToggle(entity.state == "on", { vm.toggle(entity.entityId) }, accentFor(entity.entityId))
        Spacer(Modifier.weight(1f))
        FilledTonalButton(onClick = { vm.callService("automation", "trigger", entity.entityId) }) { Text("Run") }
    }
}

@Composable
fun TodoControl(entity: HaEntity, vm: HavenViewModel) {
    var items by remember { mutableStateOf<JSONArray?>(null) }
    var newItem by remember { mutableStateOf("") }
    var refreshKey by remember { mutableStateOf(0) }
    LaunchedEffect(entity.entityId, refreshKey) {
        vm.client.fetchTodoItems(entity.entityId) { arr ->
            Handler(Looper.getMainLooper()).post { items = arr }
        }
    }
    Column {
        val arr = items
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val uid = item.optString("uid")
                val summary = item.optString("summary")
                val done = item.optString("status") == "completed"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = done,
                        onCheckedChange = { checked ->
                            vm.client.todoUpdate(entity.entityId, uid, if (checked) "completed" else "needs_action")
                            refreshKey++
                        },
                    )
                    Text(summary, modifier = Modifier.weight(1f), fontSize = 15.sp)
                    IconButton(onClick = {
                        vm.client.todoDelete(entity.entityId, uid)
                        refreshKey++
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp))
                    }
                }
            }
        } else {
            Text("Loading tasks…", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newItem,
                onValueChange = { newItem = it },
                label = { Text("New task") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (newItem.isNotBlank()) {
                    vm.client.todoCreate(entity.entityId, newItem.trim())
                    newItem = ""
                    refreshKey++
                }
            }) { Text("Add") }
        }
    }
}

@Composable
fun InputNumberControl(entity: HaEntity, vm: HavenViewModel) {
    val value = entity.state.toDoubleOrNull() ?: 0.0
    val min = entity.attrDouble("min")?.toFloat() ?: 0f
    val max = entity.attrDouble("max")?.toFloat() ?: 100f
    val step = entity.attrDouble("step")?.toFloat() ?: 1f
    Column {
        Text("$value", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        ServiceSlider(value.toFloat(), min..max, {
            vm.callService("input_number", "set_value", entity.entityId, JSONObject().put("value", it.toDouble()))
        }, accentFor(entity.entityId))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                vm.callService("input_number", "set_value", entity.entityId, JSONObject().put("value", (value - step).coerceAtLeast(min.toDouble())))
            }) { Text("−") }
            OutlinedButton(onClick = {
                vm.callService("input_number", "set_value", entity.entityId, JSONObject().put("value", (value + step).coerceAtMost(max.toDouble())))
            }) { Text("+") }
        }
    }
}

@Composable
fun InputSelectControl(entity: HaEntity, vm: HavenViewModel) {
    val options = entity.attrList("options")?.mapNotNull { it as? String } ?: emptyList()
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(entity.state) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (option in options) {
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        vm.callService("input_select", "select_option", entity.entityId, JSONObject().put("option", option))
                    },
                )
            }
        }
    }
}

@Composable
fun InputTextControl(entity: HaEntity, vm: HavenViewModel) {
    var text by remember(entity.state) { mutableStateOf(entity.state) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f), singleLine = true)
        Spacer(Modifier.width(8.dp))
        Button(onClick = {
            vm.callService("input_text", "set_value", entity.entityId, JSONObject().put("value", text))
        }) { Text("Set") }
    }
}

@Composable
fun InputDatetimeControl(entity: HaEntity, vm: HavenViewModel) {
    var text by remember(entity.state) { mutableStateOf(entity.state) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f), singleLine = true)
        Spacer(Modifier.width(8.dp))
        Button(onClick = {
            vm.callService("input_datetime", "set_datetime", entity.entityId, JSONObject().put("datetime", text))
        }) { Text("Set") }
    }
}

@Composable
fun TimerControl(entity: HaEntity, vm: HavenViewModel) {
    val remaining = entity.attrString("remaining") ?: entity.state
    Column {
        Text(remaining, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(entity.stateText(), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { vm.callService("timer", "start", entity.entityId) }) { Text("Start") }
            FilledTonalButton(onClick = { vm.callService("timer", "pause", entity.entityId) }) { Text("Pause") }
            OutlinedButton(onClick = { vm.callService("timer", "cancel", entity.entityId) }) { Text("Cancel") }
        }
    }
}

@Composable
fun CounterControl(entity: HaEntity, vm: HavenViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(entity.state, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = { vm.callService("counter", "decrement", entity.entityId) }) { Text("−") }
        OutlinedButton(onClick = { vm.callService("counter", "increment", entity.entityId) }) { Text("+") }
        OutlinedButton(onClick = { vm.callService("counter", "reset", entity.entityId) }) { Text("Reset") }
    }
}

@Composable
fun AlarmControl(entity: HaEntity, vm: HavenViewModel) {
    var code by remember { mutableStateOf("") }
    Column {
        Text(entity.stateText(), fontWeight = FontWeight.SemiBold)
        Text("Code: ${"•".repeat(code.length)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        val digits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "⌫")
        for (row in digits.chunked(3)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (digit in row) {
                    FilledTonalButton(
                        onClick = {
                            when (digit) {
                                "C" -> code = ""
                                "⌫" -> code = code.dropLast(1)
                                else -> code += digit
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(digit) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val codeData = if (code.isNotEmpty()) JSONObject().put("code", code) else null
            Button(onClick = { vm.callService("alarm_control_panel", "alarm_arm_home", entity.entityId, codeData) }) { Text("Arm home") }
            Button(onClick = { vm.callService("alarm_control_panel", "alarm_arm_away", entity.entityId, codeData) }) { Text("Arm away") }
            OutlinedButton(onClick = { vm.callService("alarm_control_panel", "alarm_disarm", entity.entityId, codeData) }) { Text("Disarm") }
        }
    }
}
