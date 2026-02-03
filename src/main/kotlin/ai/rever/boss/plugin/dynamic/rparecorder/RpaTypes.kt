package ai.rever.boss.plugin.dynamic.rparecorder

import kotlinx.serialization.Serializable
import kotlin.time.Clock

/**
 * Data class for browser tab information
 */
data class BrowserTabInfo(
    val id: String,
    val title: String,
    val url: String
)

/**
 * Selector information for locating elements
 */
@Serializable
data class SelectorInfo(
    val type: String = "xpath", // css, xpath, text, id, none
    val value: String? = null,
    val isUnique: Boolean? = null
)

/**
 * A recorded browser action
 */
@Serializable
data class RecordedAction(
    val type: String,
    val selector: SelectorInfo,
    val value: String? = null,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val elementText: String? = null,
    val url: String? = null,
    val elementType: String? = null
)

/**
 * RPA configuration for export
 */
@Serializable
data class RpaConfiguration(
    val name: String,
    val description: String = "",
    val actions: List<RpaActionConfig>
)

/**
 * RPA action configuration
 */
@Serializable
data class RpaActionConfig(
    val name: String = "",
    val actionType: String = "default", // default, assertion, screenshot, network, custom
    val type: String, // click, input, navigate, wait, select, scroll, switch_frame, run_script, screenshot, assert
    val selector: SelectorInfo,
    val value: String? = null,
    val meta: Map<String, String>? = null
)

/**
 * View modes for displaying recorded actions
 */
enum class ViewMode {
    CLEAN,  // Filtered view without redundant actions
    RAW,    // All captured actions
    EDITOR  // Editable view
}

/**
 * Feedback message types
 */
enum class FeedbackType {
    SUCCESS,
    INFO,
    WARNING,
    ERROR
}

/**
 * Feedback message for user notifications
 */
data class FeedbackMessage(
    val text: String,
    val type: FeedbackType,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

/**
 * Action types available for recording
 */
object ActionTypes {
    const val CLICK = "click"
    const val INPUT = "input"
    const val SELECT = "select"
    const val NAVIGATE = "navigate"
    const val WAIT = "wait"
    const val SCROLL = "scroll"
    const val SCREENSHOT = "screenshot"
    const val ASSERT = "assert"

    val all = listOf(CLICK, INPUT, SELECT, NAVIGATE, WAIT, SCROLL, SCREENSHOT, ASSERT)

    fun getDisplayName(type: String): String = when (type) {
        CLICK -> "Click"
        INPUT -> "Type Input"
        SELECT -> "Select Option"
        NAVIGATE -> "Navigate"
        WAIT -> "Wait"
        SCROLL -> "Scroll"
        SCREENSHOT -> "Screenshot"
        ASSERT -> "Assert"
        else -> type.replaceFirstChar { it.uppercase() }
    }
}

/**
 * Selector types for element location
 */
object SelectorTypes {
    const val XPATH = "xpath"
    const val CSS = "css"
    const val ID = "id"
    const val TEXT = "text"
    const val NONE = "none"

    val all = listOf(XPATH, CSS, ID, TEXT, NONE)

    fun getDisplayName(type: String): String = when (type) {
        XPATH -> "XPath"
        CSS -> "CSS Selector"
        ID -> "Element ID"
        TEXT -> "Text Content"
        NONE -> "None"
        else -> type.uppercase()
    }
}

/**
 * Recording state
 */
enum class RecordingState {
    IDLE,       // Not recording
    RECORDING,  // Actively recording
    PAUSED      // Paused
}
