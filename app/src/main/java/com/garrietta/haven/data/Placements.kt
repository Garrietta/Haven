package com.garrietta.haven.data

/**
 * Feedback round 19, item 5: one placement per placeable group.
 * A room or a custom tab lives in exactly one surface: the Home
 * chip row, the navigation bar/rail, or neither (still manageable
 * from the unified Rooms & tabs screen). This REPLACES the old
 * independent pinned/hidden flags for placement purposes: a tab
 * pinned to the bar is never also a chip, and a hidden tab is
 * never on the bar — the selector is the single source of truth.
 * One enum serves rooms and tabs alike (their placements mean
 * the same thing and share storage keys); the mapping functions
 * below are where the two kinds differ. Pure (no Android types)
 * so it is unit tested.
 */
enum class Placement {
    ROOM_CHIP,
    NAV_TAB,
    HIDDEN,
}

/** Stored key for a placement; unknown keys load as ROOM_CHIP (the historical room default). */
fun Placement.storageKey(): String = when (this) {
    Placement.ROOM_CHIP -> "room_chip"
    Placement.NAV_TAB -> "nav_tab"
    Placement.HIDDEN -> "hidden"
}

fun placementFromKey(key: String?): Placement = when (key) {
    "nav_tab" -> Placement.NAV_TAB
    "hidden" -> Placement.HIDDEN
    else -> Placement.ROOM_CHIP
}

/**
 * A tab's placement, derived from its stored fields: pinned wins
 * over chip (a tab cannot sit in two surfaces at once), chip
 * beats hidden, and a tab with neither flag is HIDDEN — which
 * matches its historical visibility exactly (browse list only,
 * never bar or chips), so existing unpinned tabs do not suddenly
 * flood the chip row.
 */
fun tabPlacementFor(pinned: Boolean, chip: Boolean, hidden: Boolean): Placement = when {
    pinned -> Placement.NAV_TAB
    chip -> Placement.ROOM_CHIP
    hidden -> Placement.HIDDEN
    else -> Placement.HIDDEN
}
