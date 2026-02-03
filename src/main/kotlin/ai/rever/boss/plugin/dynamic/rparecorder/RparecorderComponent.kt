package ai.rever.boss.plugin.dynamic.rparecorder

import ai.rever.boss.plugin.api.ActiveTabsProvider
import ai.rever.boss.plugin.api.PanelComponentWithUI
import ai.rever.boss.plugin.api.PanelInfo
import ai.rever.boss.plugin.browser.BrowserService
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * RPA Recorder panel component (Dynamic Plugin)
 *
 * Provides browser interaction recording functionality.
 * Works with or without BrowserService - manual action entry is always available.
 */
class RparecorderComponent(
    ctx: ComponentContext,
    override val panelInfo: PanelInfo,
    private val browserService: BrowserService? = null,
    private val activeTabsProvider: ActiveTabsProvider? = null
) : PanelComponentWithUI, ComponentContext by ctx {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val settingsManager = RpaSettingsManager()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // State flows
    private val _recordedActions = MutableStateFlow<List<RecordedAction>>(emptyList())
    val recordedActions: StateFlow<List<RecordedAction>> = _recordedActions.asStateFlow()

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.CLEAN)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _selectedActionIndices = MutableStateFlow<Set<Int>>(emptySet())
    val selectedActionIndices: StateFlow<Set<Int>> = _selectedActionIndices.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<FeedbackMessage?>(null)
    val feedbackMessage: StateFlow<FeedbackMessage?> = _feedbackMessage.asStateFlow()

    private val _configurationName = MutableStateFlow("Recorded RPA Process")
    val configurationName: StateFlow<String> = _configurationName.asStateFlow()

    private val _configurationDescription = MutableStateFlow("")
    val configurationDescription: StateFlow<String> = _configurationDescription.asStateFlow()

    private val _savedConfigurations = MutableStateFlow<List<String>>(emptyList())
    val savedConfigurations: StateFlow<List<String>> = _savedConfigurations.asStateFlow()

    private val _showAddActionDialog = MutableStateFlow(false)
    val showAddActionDialog: StateFlow<Boolean> = _showAddActionDialog.asStateFlow()

    private val _showSaveDialog = MutableStateFlow(false)
    val showSaveDialog: StateFlow<Boolean> = _showSaveDialog.asStateFlow()

    private val _showLoadDialog = MutableStateFlow(false)
    val showLoadDialog: StateFlow<Boolean> = _showLoadDialog.asStateFlow()

    private val _editingActionIndex = MutableStateFlow<Int?>(null)
    val editingActionIndex: StateFlow<Int?> = _editingActionIndex.asStateFlow()

    // Browser service availability
    val hasBrowserService: Boolean get() = browserService != null

    init {
        lifecycle.doOnDestroy {
            scope.cancel()
        }

        // Load saved configurations list
        loadSavedConfigurations()

        // Load settings
        scope.launch {
            val settings = settingsManager.loadSettings()
            _viewMode.value = try {
                ViewMode.valueOf(settings.defaultViewMode)
            } catch (e: Exception) {
                ViewMode.CLEAN
            }
        }
    }

    @Composable
    override fun Content() {
        RparecorderContent(this)
    }

    /**
     * Toggle recording state
     */
    fun toggleRecording() {
        when (_recordingState.value) {
            RecordingState.IDLE -> startRecording()
            RecordingState.RECORDING -> stopRecording()
            RecordingState.PAUSED -> resumeRecording()
        }
    }

    /**
     * Start recording browser interactions
     */
    private fun startRecording() {
        _recordingState.value = RecordingState.RECORDING

        if (browserService != null) {
            // TODO: When browser integration is available, inject event listeners
            showFeedback("Recording started - browser integration active", FeedbackType.SUCCESS)
        } else {
            showFeedback("Recording mode active - add actions manually", FeedbackType.INFO)
        }
    }

    /**
     * Stop recording
     */
    private fun stopRecording() {
        _recordingState.value = RecordingState.IDLE
        showFeedback("Recording stopped", FeedbackType.INFO)
    }

    /**
     * Resume recording from paused state
     */
    private fun resumeRecording() {
        _recordingState.value = RecordingState.RECORDING
        showFeedback("Recording resumed", FeedbackType.INFO)
    }

    /**
     * Pause recording
     */
    fun pauseRecording() {
        if (_recordingState.value == RecordingState.RECORDING) {
            _recordingState.value = RecordingState.PAUSED
            showFeedback("Recording paused", FeedbackType.INFO)
        }
    }

    /**
     * Clear all recorded actions
     */
    fun clearRecording() {
        _recordedActions.value = emptyList()
        _selectedActionIndices.value = emptySet()
        showFeedback("All actions cleared", FeedbackType.INFO)
    }

    /**
     * Add a new action
     */
    fun addAction(action: RecordedAction) {
        _recordedActions.value = _recordedActions.value + action
    }

    /**
     * Add a manually created action
     */
    fun addManualAction(
        type: String,
        selectorType: String,
        selectorValue: String?,
        value: String? = null,
        elementText: String? = null
    ) {
        val action = RecordedAction(
            type = type,
            selector = SelectorInfo(
                type = selectorType,
                value = selectorValue,
                isUnique = null
            ),
            value = value,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            elementText = elementText,
            url = _currentUrl.value.takeIf { it.isNotEmpty() }
        )
        addAction(action)
        _showAddActionDialog.value = false
        showFeedback("Action added", FeedbackType.SUCCESS)
    }

    /**
     * Remove a specific action
     */
    fun removeAction(index: Int) {
        _recordedActions.value = _recordedActions.value.toMutableList().apply {
            if (index in indices) removeAt(index)
        }
        _selectedActionIndices.value = _selectedActionIndices.value.filter { it != index }.toSet()
    }

    /**
     * Remove selected actions
     */
    fun removeSelectedActions() {
        val indicesToRemove = _selectedActionIndices.value.sortedDescending()
        _recordedActions.value = _recordedActions.value.toMutableList().apply {
            indicesToRemove.forEach { index ->
                if (index in indices) removeAt(index)
            }
        }
        _selectedActionIndices.value = emptySet()
        showFeedback("${indicesToRemove.size} actions removed", FeedbackType.INFO)
    }

    /**
     * Edit a specific action
     */
    fun editAction(index: Int, newAction: RecordedAction) {
        _recordedActions.value = _recordedActions.value.toMutableList().apply {
            if (index in indices) set(index, newAction)
        }
        _editingActionIndex.value = null
    }

    /**
     * Start editing an action
     */
    fun startEditingAction(index: Int) {
        _editingActionIndex.value = index
    }

    /**
     * Cancel editing
     */
    fun cancelEditing() {
        _editingActionIndex.value = null
    }

    /**
     * Move action up in the list
     */
    fun moveActionUp(index: Int) {
        if (index > 0) {
            _recordedActions.value = _recordedActions.value.toMutableList().apply {
                val action = removeAt(index)
                add(index - 1, action)
            }
        }
    }

    /**
     * Move action down in the list
     */
    fun moveActionDown(index: Int) {
        if (index < _recordedActions.value.size - 1) {
            _recordedActions.value = _recordedActions.value.toMutableList().apply {
                val action = removeAt(index)
                add(index + 1, action)
            }
        }
    }

    /**
     * Toggle action selection
     */
    fun toggleActionSelection(index: Int) {
        _selectedActionIndices.value = if (_selectedActionIndices.value.contains(index)) {
            _selectedActionIndices.value - index
        } else {
            _selectedActionIndices.value + index
        }
    }

    /**
     * Select all visible actions
     */
    fun selectAllActions() {
        val visibleActions = getFilteredActions()
        _selectedActionIndices.value = visibleActions.indices.toSet()
    }

    /**
     * Clear all selections
     */
    fun clearSelection() {
        _selectedActionIndices.value = emptySet()
    }

    /**
     * Set view mode
     */
    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
        // Persist preference
        scope.launch {
            settingsManager.updateSettings { it.copy(defaultViewMode = mode.name) }
        }
    }

    /**
     * Update configuration name
     */
    fun updateConfigurationName(name: String) {
        _configurationName.value = name
    }

    /**
     * Update configuration description
     */
    fun updateConfigurationDescription(description: String) {
        _configurationDescription.value = description
    }

    /**
     * Update current URL
     */
    fun updateCurrentUrl(url: String) {
        _currentUrl.value = url
    }

    /**
     * Show add action dialog
     */
    fun showAddActionDialog() {
        _showAddActionDialog.value = true
    }

    /**
     * Hide add action dialog
     */
    fun hideAddActionDialog() {
        _showAddActionDialog.value = false
    }

    /**
     * Show save dialog
     */
    fun showSaveDialog() {
        _showSaveDialog.value = true
    }

    /**
     * Hide save dialog
     */
    fun hideSaveDialog() {
        _showSaveDialog.value = false
    }

    /**
     * Show load dialog
     */
    fun showLoadDialog() {
        loadSavedConfigurations()
        _showLoadDialog.value = true
    }

    /**
     * Hide load dialog
     */
    fun hideLoadDialog() {
        _showLoadDialog.value = false
    }

    /**
     * Get filtered actions based on view mode
     */
    fun getFilteredActions(): List<RecordedAction> {
        return getFilteredActions(_recordedActions.value, _viewMode.value)
    }

    private fun getFilteredActions(actions: List<RecordedAction>, viewMode: ViewMode): List<RecordedAction> {
        return when (viewMode) {
            ViewMode.CLEAN -> {
                // Group actions by selector and keep only the most relevant ones
                val actionGroups = mutableMapOf<String, MutableList<RecordedAction>>()
                val otherActions = mutableListOf<RecordedAction>()

                for (action in actions) {
                    when (action.type) {
                        ActionTypes.INPUT -> {
                            val key = action.selector.value ?: "unknown"
                            actionGroups.getOrPut(key) { mutableListOf() }.add(action)
                        }
                        ActionTypes.CLICK -> {
                            otherActions.add(action)
                        }
                        ActionTypes.SCROLL -> {
                            if (otherActions.isNotEmpty() && otherActions.last().type != ActionTypes.SCROLL) {
                                otherActions.add(action)
                            }
                        }
                        else -> otherActions.add(action)
                    }
                }

                // For each input group, keep only the last (final) value
                val cleanedInputs = actionGroups.values.map { group ->
                    group.last()
                }

                (otherActions + cleanedInputs).sortedBy { it.timestamp }
            }
            ViewMode.RAW -> actions
            ViewMode.EDITOR -> actions
        }
    }

    /**
     * Generate RPA configuration from recorded actions
     */
    fun generateConfiguration(): RpaConfiguration {
        val actionsToExport = if (_selectedActionIndices.value.isNotEmpty()) {
            val allActions = getFilteredActions()
            allActions.filterIndexed { index, _ -> _selectedActionIndices.value.contains(index) }
        } else {
            getFilteredActions()
        }

        return generateRpaConfiguration(actionsToExport)
    }

    private fun generateRpaConfiguration(actions: List<RecordedAction>): RpaConfiguration {
        val rpaActions = actions.mapIndexed { index, action ->
            when (action.type) {
                ActionTypes.CLICK -> RpaActionConfig(
                    name = "Click on ${action.elementText ?: action.selector.value ?: "element"}",
                    actionType = "default",
                    type = "click",
                    selector = action.selector,
                    value = null,
                    meta = buildMap {
                        put("button", "left")
                        action.elementText?.let { put("text", it) }
                    }
                )
                ActionTypes.INPUT -> RpaActionConfig(
                    name = "Type into ${action.selector.value ?: "input field"}",
                    actionType = "default",
                    type = "input",
                    selector = action.selector,
                    value = action.value
                )
                ActionTypes.SELECT -> RpaActionConfig(
                    name = "Select ${action.value ?: "option"} in ${action.selector.value ?: "dropdown"}",
                    actionType = "default",
                    type = "select",
                    selector = action.selector,
                    value = action.value
                )
                ActionTypes.NAVIGATE -> RpaActionConfig(
                    name = "Navigate to ${action.url ?: action.value}",
                    actionType = "default",
                    type = "navigate",
                    selector = SelectorInfo("none", null),
                    value = action.url ?: action.value
                )
                ActionTypes.WAIT -> RpaActionConfig(
                    name = "Wait for ${action.selector.value ?: "${action.value}ms"}",
                    actionType = "default",
                    type = "wait",
                    selector = action.selector,
                    value = action.value ?: "1000"
                )
                ActionTypes.SCROLL -> RpaActionConfig(
                    name = "Scroll to position",
                    actionType = "default",
                    type = "scroll",
                    selector = SelectorInfo("none", null),
                    value = action.value ?: "0,0"
                )
                ActionTypes.SCREENSHOT -> RpaActionConfig(
                    name = "Take screenshot",
                    actionType = "screenshot",
                    type = "screenshot",
                    selector = SelectorInfo("none", null),
                    value = action.value
                )
                ActionTypes.ASSERT -> RpaActionConfig(
                    name = "Assert ${action.value ?: "condition"}",
                    actionType = "assertion",
                    type = "assert",
                    selector = action.selector,
                    value = action.value
                )
                else -> RpaActionConfig(
                    name = "Action ${index + 1}",
                    actionType = "default",
                    type = action.type,
                    selector = action.selector,
                    value = action.value
                )
            }
        }

        return RpaConfiguration(
            name = _configurationName.value,
            description = _configurationDescription.value,
            actions = rpaActions
        )
    }

    /**
     * Export configuration as JSON string
     */
    fun exportConfigurationJson(): String {
        val config = generateConfiguration()
        return json.encodeToString(RpaConfiguration.serializer(), config)
    }

    /**
     * Save configuration to disk
     */
    fun saveConfiguration(name: String, description: String) {
        scope.launch {
            _configurationName.value = name
            _configurationDescription.value = description

            val config = generateConfiguration()
            val success = settingsManager.saveConfiguration(config)

            if (success) {
                showFeedback("Configuration saved: $name", FeedbackType.SUCCESS)
                loadSavedConfigurations()
            } else {
                showFeedback("Failed to save configuration", FeedbackType.ERROR)
            }

            _showSaveDialog.value = false
        }
    }

    /**
     * Load configuration from disk
     */
    fun loadConfiguration(name: String) {
        scope.launch {
            val config = settingsManager.loadConfiguration(name)

            if (config != null) {
                _configurationName.value = config.name
                _configurationDescription.value = config.description
                _recordedActions.value = config.actions.map { rpaAction ->
                    RecordedAction(
                        type = rpaAction.type,
                        selector = rpaAction.selector,
                        value = rpaAction.value,
                        timestamp = Clock.System.now().toEpochMilliseconds(),
                        elementText = rpaAction.meta?.get("text")
                    )
                }
                _selectedActionIndices.value = emptySet()
                showFeedback("Configuration loaded: $name", FeedbackType.SUCCESS)
            } else {
                showFeedback("Failed to load configuration", FeedbackType.ERROR)
            }

            _showLoadDialog.value = false
        }
    }

    /**
     * Delete a saved configuration
     */
    fun deleteConfiguration(name: String) {
        scope.launch {
            val success = settingsManager.deleteConfiguration(name)
            if (success) {
                showFeedback("Configuration deleted: $name", FeedbackType.INFO)
                loadSavedConfigurations()
            } else {
                showFeedback("Failed to delete configuration", FeedbackType.ERROR)
            }
        }
    }

    /**
     * Export configuration to file
     */
    fun exportToFile() {
        scope.launch {
            val config = generateConfiguration()
            val timestamp = Clock.System.now().toEpochMilliseconds()
            val filename = "rpa_${sanitizeFilename(_configurationName.value)}_$timestamp.json"
            val exportDir = settingsManager.getDefaultExportDir()
            val filePath = "${exportDir.absolutePath}/$filename"

            val success = settingsManager.exportConfiguration(config, filePath)

            if (success) {
                val actionCount = config.actions.size
                showFeedback("Exported $actionCount actions to Downloads", FeedbackType.SUCCESS)
            } else {
                showFeedback("Failed to export configuration", FeedbackType.ERROR)
            }
        }
    }

    /**
     * Load saved configurations list
     */
    private fun loadSavedConfigurations() {
        scope.launch {
            _savedConfigurations.value = settingsManager.listConfigurations()
        }
    }

    /**
     * Show feedback message
     */
    private fun showFeedback(message: String, type: FeedbackType) {
        _feedbackMessage.value = FeedbackMessage(message, type)
        scope.launch {
            kotlinx.coroutines.delay(3000)
            if (_feedbackMessage.value?.text == message) {
                _feedbackMessage.value = null
            }
        }
    }

    /**
     * Dismiss feedback message
     */
    fun dismissFeedback() {
        _feedbackMessage.value = null
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(50)
    }
}
