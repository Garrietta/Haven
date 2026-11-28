package com.garrietta.haven.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Blinds
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Co2
import androidx.compose.material.icons.filled.CoffeeMaker
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Cottage
import androidx.compose.material.icons.filled.Crib
import androidx.compose.material.icons.filled.Curtains
import androidx.compose.material.icons.filled.Deck
import androidx.compose.material.icons.filled.Desk
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.DoorSliding
import androidx.compose.material.icons.filled.Doorbell
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Fence
import androidx.compose.material.icons.filled.Fireplace
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HeatPump
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HotTub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Iron
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Light
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Microwave
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.OutdoorGrill
import androidx.compose.material.icons.filled.Outlet
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RollerShades
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SensorDoor
import androidx.compose.material.icons.filled.SensorOccupied
import androidx.compose.material.icons.filled.SensorWindow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Shower
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Toys
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbIncandescent
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Window
import androidx.compose.material.icons.filled.Yard
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The icons a tile can be given, by group, as the tile options sheet
 * lists them. A key is what settings store for an entity, so keys are
 * frozen: a renamed key silently loses everyone's choice. Labels and
 * groups can change freely. Glyphs Material has no picture of (fan,
 * heater, garage door, sprinkler...) are Haven's own ([HavenGlyphs]).
 * The caller falls back to the default icon whenever a stored key is
 * unknown.
 */
data class CardIconOption(val key: String, val label: String, val icon: ImageVector, val group: String)

/** The sheet's groups, in the order it lists them. */
val CARD_ICON_GROUPS: List<String> = listOf(
    "Lights", "Power", "Heating & cooling", "Doors & windows", "Water & garden",
    "Kitchen & laundry", "Media", "Security & sensors", "Rooms & people", "Other",
)

val CARD_ICON_OPTIONS: List<CardIconOption> = listOf(
    CardIconOption("lightbulb", "Bulb", Icons.Filled.Lightbulb, "Lights"),
    CardIconOption("lamp", "Lamp", HavenGlyphs.Lamp, "Lights"),
    CardIconOption("ceiling_light", "Ceiling", Icons.Filled.Light, "Lights"),
    CardIconOption("incandescent", "Bulb 2", Icons.Filled.WbIncandescent, "Lights"),
    CardIconOption("nightlight", "Night", Icons.Filled.Nightlight, "Lights"),
    CardIconOption("sunny", "Sun", Icons.Filled.WbSunny, "Lights"),

    CardIconOption("power", "Power", Icons.Filled.Power, "Power"),
    CardIconOption("outlet", "Outlet", Icons.Filled.Outlet, "Power"),
    CardIconOption("bolt", "Bolt", Icons.Filled.Bolt, "Power"),
    CardIconOption("electric", "Electric", Icons.Filled.ElectricBolt, "Power"),
    CardIconOption("battery", "Battery", Icons.Filled.BatteryFull, "Power"),
    CardIconOption("solar", "Solar", Icons.Filled.SolarPower, "Power"),
    CardIconOption("ev", "Charger", Icons.Filled.EvStation, "Power"),

    CardIconOption("thermostat", "Thermostat", Icons.Filled.Thermostat, "Heating & cooling"),
    CardIconOption("heater", "Heater", HavenGlyphs.Heater, "Heating & cooling"),
    CardIconOption("fan", "Fan", HavenGlyphs.Fan, "Heating & cooling"),
    CardIconOption("ac", "AC", Icons.Filled.AcUnit, "Heating & cooling"),
    CardIconOption("heat_pump", "Heat pump", Icons.Filled.HeatPump, "Heating & cooling"),
    CardIconOption("fireplace", "Fireplace", Icons.Filled.Fireplace, "Heating & cooling"),
    CardIconOption("flame", "Flame", Icons.Filled.LocalFireDepartment, "Heating & cooling"),
    CardIconOption("air", "Air", Icons.Filled.Air, "Heating & cooling"),
    CardIconOption("humidifier", "Humidifier", HavenGlyphs.Humidifier, "Heating & cooling"),
    CardIconOption("air_purifier", "Air purifier", HavenGlyphs.AirPurifier, "Heating & cooling"),
    CardIconOption("water_heater", "Water heater", HavenGlyphs.WaterHeater, "Heating & cooling"),

    CardIconOption("garage_door", "Garage door", HavenGlyphs.GarageDoor, "Doors & windows"),
    CardIconOption("garage", "Garage", Icons.Filled.Garage, "Doors & windows"),
    CardIconOption("gate", "Gate", HavenGlyphs.Gate, "Doors & windows"),
    CardIconOption("door", "Door", Icons.Filled.DoorFront, "Doors & windows"),
    CardIconOption("sliding_door", "Sliding door", Icons.Filled.DoorSliding, "Doors & windows"),
    CardIconOption("window", "Window", Icons.Filled.Window, "Doors & windows"),
    CardIconOption("blinds", "Blinds", Icons.Filled.Blinds, "Doors & windows"),
    CardIconOption("shades", "Shades", Icons.Filled.RollerShades, "Doors & windows"),
    CardIconOption("curtains", "Curtains", Icons.Filled.Curtains, "Doors & windows"),
    CardIconOption("lock", "Locked", Icons.Filled.Lock, "Doors & windows"),
    CardIconOption("lock_open", "Unlocked", Icons.Filled.LockOpen, "Doors & windows"),
    CardIconOption("doorbell", "Doorbell", Icons.Filled.Doorbell, "Doors & windows"),
    CardIconOption("key", "Key", Icons.Filled.Key, "Doors & windows"),

    CardIconOption("sprinkler", "Sprinklers", HavenGlyphs.Sprinkler, "Water & garden"),
    CardIconOption("valve", "Valve", HavenGlyphs.Valve, "Water & garden"),
    CardIconOption("water", "Water", Icons.Filled.WaterDrop, "Water & garden"),
    CardIconOption("waves", "Waves", Icons.Filled.Water, "Water & garden"),
    CardIconOption("pool", "Pool", Icons.Filled.Pool, "Water & garden"),
    CardIconOption("hot_tub", "Hot tub", Icons.Filled.HotTub, "Water & garden"),
    CardIconOption("shower", "Shower", Icons.Filled.Shower, "Water & garden"),
    CardIconOption("bathtub", "Bath", Icons.Filled.Bathtub, "Water & garden"),
    CardIconOption("grass", "Lawn", Icons.Filled.Grass, "Water & garden"),
    CardIconOption("yard", "Yard", Icons.Filled.Yard, "Water & garden"),
    CardIconOption("spa", "Plant", Icons.Filled.Spa, "Water & garden"),
    CardIconOption("tree", "Tree", Icons.Filled.Park, "Water & garden"),
    CardIconOption("deck", "Patio", Icons.Filled.Deck, "Water & garden"),
    CardIconOption("fence", "Fence", Icons.Filled.Fence, "Water & garden"),
    CardIconOption("grill", "Grill", Icons.Filled.OutdoorGrill, "Water & garden"),

    CardIconOption("fridge", "Fridge", Icons.Filled.Kitchen, "Kitchen & laundry"),
    CardIconOption("oven", "Oven", HavenGlyphs.Oven, "Kitchen & laundry"),
    CardIconOption("microwave", "Microwave", Icons.Filled.Microwave, "Kitchen & laundry"),
    CardIconOption("coffee", "Coffee", Icons.Filled.CoffeeMaker, "Kitchen & laundry"),
    CardIconOption("dishwasher", "Dishwasher", HavenGlyphs.Dishwasher, "Kitchen & laundry"),
    CardIconOption("washer", "Washer", Icons.Filled.LocalLaundryService, "Kitchen & laundry"),
    CardIconOption("iron", "Iron", Icons.Filled.Iron, "Kitchen & laundry"),
    CardIconOption("cleaning", "Cleaning", Icons.Filled.CleaningServices, "Kitchen & laundry"),
    CardIconOption("autorenew", "Vacuum", Icons.Filled.Autorenew, "Kitchen & laundry"),

    CardIconOption("tv", "TV", Icons.Filled.Tv, "Media"),
    CardIconOption("speaker", "Speaker", Icons.Filled.Speaker, "Media"),
    CardIconOption("music", "Music", Icons.Filled.MusicNote, "Media"),
    CardIconOption("volume", "Volume", Icons.Filled.VolumeUp, "Media"),
    CardIconOption("headphones", "Headphones", Icons.Filled.Headphones, "Media"),
    CardIconOption("game", "Games", Icons.Filled.VideogameAsset, "Media"),
    CardIconOption("computer", "Computer", Icons.Filled.Computer, "Media"),
    CardIconOption("router", "Router", Icons.Filled.Router, "Media"),
    CardIconOption("wifi", "Wi-Fi", Icons.Filled.Wifi, "Media"),

    CardIconOption("videocam", "Camera", Icons.Filled.Videocam, "Security & sensors"),
    CardIconOption("camera", "Photo", Icons.Filled.Camera, "Security & sensors"),
    CardIconOption("security", "Security", Icons.Filled.Security, "Security & sensors"),
    CardIconOption("alarm", "Alarm", Icons.Filled.Alarm, "Security & sensors"),
    CardIconOption("visibility", "Motion", Icons.Filled.Visibility, "Security & sensors"),
    CardIconOption("occupancy", "Occupancy", Icons.Filled.SensorOccupied, "Security & sensors"),
    CardIconOption("door_sensor", "Door sensor", Icons.Filled.SensorDoor, "Security & sensors"),
    CardIconOption("window_sensor", "Window sensor", Icons.Filled.SensorWindow, "Security & sensors"),
    CardIconOption("sensors", "Sensor", Icons.Filled.Sensors, "Security & sensors"),
    CardIconOption("co2", "CO₂", Icons.Filled.Co2, "Security & sensors"),
    CardIconOption("warning", "Warning", Icons.Filled.Warning, "Security & sensors"),

    CardIconOption("home", "Home", Icons.Filled.Home, "Rooms & people"),
    CardIconOption("cottage", "House", Icons.Filled.Cottage, "Rooms & people"),
    CardIconOption("bed", "Bed", Icons.Filled.Bed, "Rooms & people"),
    CardIconOption("crib", "Crib", Icons.Filled.Crib, "Rooms & people"),
    CardIconOption("chair", "Chair", Icons.Filled.Chair, "Rooms & people"),
    CardIconOption("desk", "Desk", Icons.Filled.Desk, "Rooms & people"),
    CardIconOption("person", "Person", Icons.Filled.Person, "Rooms & people"),
    CardIconOption("pets", "Pets", Icons.Filled.Pets, "Rooms & people"),
    CardIconOption("toys", "Toys", Icons.Filled.Toys, "Rooms & people"),

    CardIconOption("cloudy", "Cloud", Icons.Filled.WbCloudy, "Other"),
    CardIconOption("cloud", "Cloud 2", Icons.Filled.Cloud, "Other"),
    CardIconOption("umbrella", "Umbrella", Icons.Filled.Umbrella, "Other"),
    CardIconOption("refresh", "Cycle", Icons.Filled.Refresh, "Other"),
    CardIconOption("schedule", "Schedule", Icons.Filled.Schedule, "Other"),
    CardIconOption("play", "Play", Icons.Filled.PlayArrow, "Other"),
    CardIconOption("list", "List", Icons.Filled.List, "Other"),
    CardIconOption("build", "Tools", Icons.Filled.Build, "Other"),
    CardIconOption("info", "Info", Icons.Filled.Info, "Other"),
)

val CARD_ICON_KEYS: Set<String> = CARD_ICON_OPTIONS.map { it.key }.toSet()

/** Picker icon for a key, or null for an unknown key (caller falls back to the default icon). */
fun cardIconForKey(key: String?): ImageVector? =
    key?.let { k -> CARD_ICON_OPTIONS.firstOrNull { it.key == k }?.icon }

/** Words in a device's name that say what it is, and the icon each one gets, first match wins. */
private val NAME_ICONS: List<Pair<Regex, String>> = listOf(
    Regex("""\b(sprinklers?|irrigation|drip)\b""") to "sprinkler",
    Regex("""\bwater heater\b""") to "water_heater",
    Regex("""\b(de)?humidifier\b""") to "humidifier",
    Regex("""\b(air )?purifier\b""") to "air_purifier",
    Regex("""\bheat pump\b""") to "heat_pump",
    Regex("""\b(heaters?|radiator|heat mat|heat lamp)\b""") to "heater",
    Regex("""\bfireplace\b""") to "fireplace",
    Regex("""\b(fans?)\b""") to "fan",
    Regex("""\b(a/?c|air conditioner)\b""") to "ac",
    Regex("""\bgarage\b""") to "garage_door",
    Regex("""\bgate\b""") to "gate",
    Regex("""\bvalve\b""") to "valve",
    Regex("""\bpool\b""") to "pool",
    Regex("""\b(hot tub|jacuzzi)\b""") to "hot_tub",
    Regex("""\bfountain\b""") to "waves",
    Regex("""\b(coffee|espresso)\b""") to "coffee",
    Regex("""\bdishwasher\b""") to "dishwasher",
    Regex("""\b(washer|washing machine|laundry)\b""") to "washer",
    Regex("""\boven\b""") to "oven",
    Regex("""\b(christmas|tree)\b""") to "tree",
    Regex("""\b(lamp)\b""") to "lamp",
    Regex("""\b(router|modem)\b""") to "router",
    Regex("""\b(ev|car) charger\b""") to "ev",
)

/** Domains that say exactly what a device is: their own icon, whatever it is called. */
private val SELF_DESCRIBING = mapOf(
    "fan" to "fan",
    "valve" to "valve",
    "water_heater" to "water_heater",
    "humidifier" to "humidifier",
    "lawn_mower" to "grass",
    "siren" to "alarm",
)

/** What a cover's device class says it is. */
private fun coverIcon(deviceClass: String?): String? = when (deviceClass) {
    "garage" -> "garage_door"
    "gate" -> "gate"
    "door" -> "door"
    "window" -> "window"
    "blind", "shutter" -> "blinds"
    "shade", "awning" -> "shades"
    "curtain" -> "curtains"
    "damper" -> "air"
    else -> null
}

/** The only things a light's name may turn its bulb into. */
private val LIGHT_NAMES = setOf("lamp", "tree")

/**
 * The icon a device gets when nobody has picked one: from what Home
 * Assistant says it is (a fan, a valve, a cover's device class), else,
 * for the things people name after what they power (a switch called
 * "Greenhouse Heater", a cover called "Garage"), from its name. Null
 * keeps the domain's own icon.
 */
fun suggestedIconKey(domain: String, deviceClass: String?, name: String): String? {
    SELF_DESCRIBING[domain]?.let { return it }
    val named = NAME_ICONS.firstOrNull { (pattern, _) -> pattern.containsMatchIn(name.lowercase()) }?.second
    return when (domain) {
        "cover" -> coverIcon(deviceClass) ?: named?.takeIf { it in setOf("garage_door", "gate") }
        "switch", "input_boolean" -> named ?: if (deviceClass == "outlet") "outlet" else null
        "light" -> named?.takeIf { it in LIGHT_NAMES }
        else -> null
    }
}
