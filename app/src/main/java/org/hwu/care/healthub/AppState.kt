package org.hwu.care.healthub

/**
 * The display state of the app. Driven entirely by the backend via the agreed comms protocol.
 *
 * @param pageId  Which screen to show. See [PageId] for constants matching the state machine.
 * @param data    Key/value payload for the screen to render (e.g. reading values, question text).
 */
data class AppState(
    val pageId: Int,
    val data: Map<String, String> = emptyMap()
)

/**
 * Page IDs matching the backend PAGE_CONFIG in config.py.
 * The backend sends these as zero-padded strings ("01", "02"...) — parse to Int on receipt.
 */
object PageId {
    const val IDLE             = 1
    const val WELCOME          = 2
    const val Q1               = 3
    const val Q2               = 4
    const val Q3               = 5
    const val MEASURE_INTRO    = 6
    const val OXIMETER_INTRO   = 7
    const val OXIMETER_READING = 8
    const val OXIMETER_DONE    = 9
    const val BP_INTRO         = 10
    const val BP_READING       = 11
    const val BP_DONE          = 12
    const val SCALE_INTRO      = 13
    const val SCALE_READING    = 14
    const val SCALE_DONE       = 15
    const val RECAP            = 16
    const val SORRY            = 17
}
