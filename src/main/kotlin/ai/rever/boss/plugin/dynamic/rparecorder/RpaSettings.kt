package ai.rever.boss.plugin.dynamic.rparecorder

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Settings for the RPA Recorder plugin
 */
@Serializable
data class RpaRecorderSettings(
    val lastExportPath: String = "",
    val defaultViewMode: String = "CLEAN",
    val autoSaveRecordings: Boolean = true,
    val savedConfigurations: List<String> = emptyList(), // List of configuration names
    val recentConfigurations: List<SavedConfiguration> = emptyList()
)

/**
 * A saved RPA configuration
 */
@Serializable
data class SavedConfiguration(
    val name: String,
    val description: String = "",
    val createdAt: Long,
    val actions: List<RpaActionConfig>
)

/**
 * Manager for RPA recorder settings and configurations
 */
class RpaSettingsManager {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val configDir: File
        get() {
            val homeDir = System.getProperty("user.home")
            val dir = File(homeDir, ".boss/config/rparecorder")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    private val settingsFile: File
        get() = File(configDir, "settings.json")

    private val configurationsDir: File
        get() {
            val dir = File(configDir, "configurations")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    private var cachedSettings: RpaRecorderSettings? = null

    /**
     * Load settings from disk
     */
    fun loadSettings(): RpaRecorderSettings {
        cachedSettings?.let { return it }

        return try {
            if (settingsFile.exists()) {
                val content = settingsFile.readText()
                json.decodeFromString<RpaRecorderSettings>(content).also {
                    cachedSettings = it
                }
            } else {
                RpaRecorderSettings().also {
                    cachedSettings = it
                    saveSettings(it)
                }
            }
        } catch (e: Exception) {
            RpaRecorderSettings().also {
                cachedSettings = it
            }
        }
    }

    /**
     * Save settings to disk
     */
    fun saveSettings(settings: RpaRecorderSettings) {
        try {
            cachedSettings = settings
            settingsFile.writeText(json.encodeToString(RpaRecorderSettings.serializer(), settings))
        } catch (e: Exception) {
            // Log error but continue
        }
    }

    /**
     * Update a specific setting
     */
    fun updateSettings(update: (RpaRecorderSettings) -> RpaRecorderSettings) {
        val current = loadSettings()
        val updated = update(current)
        saveSettings(updated)
    }

    /**
     * Save a configuration to disk
     */
    fun saveConfiguration(config: RpaConfiguration): Boolean {
        return try {
            val filename = sanitizeFilename(config.name) + ".json"
            val file = File(configurationsDir, filename)
            file.writeText(json.encodeToString(RpaConfiguration.serializer(), config))

            // Update settings with saved configuration name
            updateSettings { settings ->
                val configNames = settings.savedConfigurations.toMutableList()
                if (!configNames.contains(config.name)) {
                    configNames.add(config.name)
                }
                settings.copy(savedConfigurations = configNames)
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Load a configuration from disk
     */
    fun loadConfiguration(name: String): RpaConfiguration? {
        return try {
            val filename = sanitizeFilename(name) + ".json"
            val file = File(configurationsDir, filename)
            if (file.exists()) {
                json.decodeFromString<RpaConfiguration>(file.readText())
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * List all saved configurations
     */
    fun listConfigurations(): List<String> {
        return try {
            configurationsDir.listFiles { file -> file.extension == "json" }
                ?.map { it.nameWithoutExtension }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Delete a configuration
     */
    fun deleteConfiguration(name: String): Boolean {
        return try {
            val filename = sanitizeFilename(name) + ".json"
            val file = File(configurationsDir, filename)
            if (file.exists()) {
                file.delete()
                updateSettings { settings ->
                    settings.copy(
                        savedConfigurations = settings.savedConfigurations.filter { it != name }
                    )
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Export configuration to a file
     */
    fun exportConfiguration(config: RpaConfiguration, filePath: String): Boolean {
        return try {
            val file = File(filePath)
            file.writeText(json.encodeToString(RpaConfiguration.serializer(), config))

            // Update last export path
            updateSettings { it.copy(lastExportPath = file.parent ?: "") }

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Import configuration from a file
     */
    fun importConfiguration(filePath: String): RpaConfiguration? {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                json.decodeFromString<RpaConfiguration>(file.readText())
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get the default export directory (Downloads)
     */
    fun getDefaultExportDir(): File {
        val homeDir = System.getProperty("user.home")
        val downloadsDir = File(homeDir, "Downloads")
        return if (downloadsDir.exists()) downloadsDir else File(homeDir)
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(100) // Limit length
    }
}
