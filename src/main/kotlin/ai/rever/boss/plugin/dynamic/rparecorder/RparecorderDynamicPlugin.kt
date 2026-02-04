package ai.rever.boss.plugin.dynamic.rparecorder

import ai.rever.boss.plugin.api.DynamicPlugin
import ai.rever.boss.plugin.api.PluginContext

/**
 * RPA Recorder dynamic plugin - Loaded from external JAR.
 *
 * Record browser interactions for automation.
 * Uses BrowserService and ActiveTabsProvider from PluginContext when available.
 */
class RparecorderDynamicPlugin : DynamicPlugin {
    override val pluginId: String = "ai.rever.boss.plugin.dynamic.rparecorder"
    override val displayName: String = "RPA Recorder (Dynamic)"
    override val version: String = "1.0.4"
    override val description: String = "Record browser interactions for automation"
    override val author: String = "Risa Labs"
    override val url: String = "https://github.com/risa-labs-inc/boss-plugin-rparecorder"

    override fun register(context: PluginContext) {
        val browserService = context.browserService
        val activeTabsProvider = context.activeTabsProvider
        val fileSystemDataProvider = context.fileSystemDataProvider

        context.panelRegistry.registerPanel(RparecorderInfo) { ctx, panelInfo ->
            RparecorderComponent(
                ctx = ctx,
                panelInfo = panelInfo,
                browserService = browserService,
                activeTabsProvider = activeTabsProvider,
                fileSystemDataProvider = fileSystemDataProvider
            )
        }
    }
}
