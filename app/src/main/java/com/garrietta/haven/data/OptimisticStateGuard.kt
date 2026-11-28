package com.garrietta.haven.data

/**
 * Makes optimistic state flips sticky.
 *
 * The bug it fixes: a tap flips the local state instantly (see
 * HaClient.setLocalState), but the next `state_changed` event from HA
 * blindly overwrote it. When HA emits an intermediate or stale event
 * whose state still says the OLD value — which the lamp's turn_off path
 * does, while its turn_on path does not — the flip visibly reverted and
 * only the later confirming event settled it. From the user's side that
 * reads as "tap off: nothing happens, then it flips off after a delay",
 * while on feels instant, even though both taps run the same optimistic
 * code. The header text and the bulb then also disagree for that window:
 * the header follows the reverted state while the bulb morph is still
 * animating toward the tapped one.
 *
 * The guard records the state a tap expects. While the expectation is
 * live (a few seconds), an incoming server state that CONTRADICTS it is
 * dropped — it is stale or intermediate, not news. An incoming state
 * that MATCHES confirms the flip and clears the expectation, and the
 * expectation expires on its own so a genuinely stuck flip can never
 * block later events forever. A failed service call clears it
 * immediately (the ViewModel reverts the flip itself). Pure Kotlin, no
 * Android types, so it is unit tested on the JVM.
 */
class OptimisticStateGuard(private val windowMs: Long = 4_000L) {
    private data class Expectation(val state: String, val atMs: Long)

    private val expectations = mutableMapOf<String, Expectation>()

    /** Record that [entityId] was just optimistically flipped to [state]. */
    @Synchronized
    fun expect(entityId: String, state: String, nowMs: Long = System.currentTimeMillis()) {
        expectations[entityId] = Expectation(state, nowMs)
    }

    /** Drop a live expectation (service call failed; the flip was reverted). */
    @Synchronized
    fun clear(entityId: String) {
        expectations.remove(entityId)
    }

    /**
     * True when [incomingState] for [entityId] must NOT overwrite the
     * current local state: a live expectation exists, has not expired,
     * and says something different. A matching state confirms the flip
     * (expectation cleared, event applies); an expired expectation stops
     * filtering (cleared, event applies).
     */
    @Synchronized
    fun shouldDrop(entityId: String, incomingState: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        val exp = expectations[entityId] ?: return false
        if (nowMs - exp.atMs > windowMs) {
            expectations.remove(entityId)
            return false
        }
        if (incomingState == exp.state) {
            expectations.remove(entityId)
            return false
        }
        return true
    }
}
