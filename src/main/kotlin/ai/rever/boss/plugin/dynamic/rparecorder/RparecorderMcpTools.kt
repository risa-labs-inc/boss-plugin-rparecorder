package ai.rever.boss.plugin.dynamic.rparecorder

import ai.rever.boss.plugin.api.McpToolDefinition
import ai.rever.boss.plugin.api.McpToolHandler
import ai.rever.boss.plugin.api.McpToolProvider
import ai.rever.boss.plugin.api.McpToolResult

/**
 * MCP tools contributed by the RPA Recorder plugin: read recording status and
 * start/stop/clear a browser-interaction recording.
 *
 * Actions live on the per-panel [RparecorderComponent], so these tools operate
 * on the most recently opened RPA Recorder panel (via [component]); if none is
 * open they report that. A tab must be selected in the panel for recording to
 * capture anything. Registered in [RparecorderDynamicPlugin.register]; removed
 * automatically on disable/unload.
 */
internal class RparecorderMcpToolProvider(
    override val providerId: String,
    private val component: () -> RparecorderComponent?,
) : McpToolProvider {

    override fun tools(): List<McpToolDefinition> = listOf(
        McpToolDefinition(
            name = "rpa_record_status",
            description = "Report RPA Recorder state (recording state, captured action count, current URL).",
            handler = McpToolHandler {
                val c = component() ?: return@McpToolHandler notOpen()
                McpToolResult("state=${c.recordingState.value.name} actions=${c.recordedActions.value.size} url=${c.currentUrl.value}")
            },
        ),
        McpToolDefinition(
            name = "rpa_record_toggle",
            description = "Toggle browser-interaction recording on/off in the RPA Recorder.",
            readOnly = false,
            handler = McpToolHandler {
                val c = component() ?: return@McpToolHandler notOpen()
                c.toggleRecording()
                McpToolResult("Toggled recording (now ${c.recordingState.value.name}).")
            },
        ),
        McpToolDefinition(
            name = "rpa_record_clear",
            description = "Clear all recorded actions in the RPA Recorder.",
            readOnly = false,
            handler = McpToolHandler {
                val c = component() ?: return@McpToolHandler notOpen()
                c.clearRecording()
                McpToolResult("Cleared recorded actions.")
            },
        ),
    )

    private fun notOpen(): McpToolResult =
        McpToolResult("Open the RPA Recorder panel first (no active instance).", isError = true)
}
