package com.garrietta.haven.data

import java.util.Locale

/**
 * A sensor's reading the way a person reads it: rounded to what the
 * sensor can tell ("66.4 °F", not "66.38 °F"), a percentage without a
 * space ("54%"), anything that is not a number as it is.
 */
fun formatReading(state: String, unit: String?, deviceClass: String?): String {
    val value = state.toDoubleOrNull() ?: return if (unit != null) "$state $unit" else state
    val decimals = when {
        deviceClass in setOf("humidity", "battery", "moisture", "illuminance") || unit == "%" -> 0
        deviceClass == "temperature" || unit in setOf("°F", "°C") -> 1
        kotlin.math.abs(value) >= 100 -> 0
        kotlin.math.abs(value) >= 10 -> 1
        else -> 2
    }
    var number = String.format(Locale.US, "%.${decimals}f", value)
    if (number.contains('.')) number = number.trimEnd('0').trimEnd('.')
    if (number == "-0") number = "0"
    return when {
        unit == null -> number
        unit == "%" -> "$number%"
        else -> "$number $unit"
    }
}
