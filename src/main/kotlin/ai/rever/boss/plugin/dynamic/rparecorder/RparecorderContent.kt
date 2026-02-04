package ai.rever.boss.plugin.dynamic.rparecorder

import ai.rever.boss.plugin.ui.BossTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Main content for RPA Recorder panel
 */
@Composable
fun RparecorderContent(component: RparecorderComponent) {
    BossTheme {
        val recordingState by component.recordingState.collectAsState()
        val recordedActions by component.recordedActions.collectAsState()
        val viewMode by component.viewMode.collectAsState()
        val selectedIndices by component.selectedActionIndices.collectAsState()
        val feedbackMessage by component.feedbackMessage.collectAsState()
        val showAddDialog by component.showAddActionDialog.collectAsState()
        val showSaveDialog by component.showSaveDialog.collectAsState()
        val showLoadDialog by component.showLoadDialog.collectAsState()
        val savedConfigurations by component.savedConfigurations.collectAsState()
        val configName by component.configurationName.collectAsState()

        // Browser tab selection state
        val availableTabs by component.availableTabs.collectAsState()
        val selectedTab by component.selectedTab.collectAsState()
        val isConnected by component.isConnected.collectAsState()

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Header with recording controls
                HeaderSection(
                    recordingState = recordingState,
                    hasActions = recordedActions.isNotEmpty(),
                    availableTabs = availableTabs,
                    selectedTab = selectedTab,
                    isConnected = isConnected,
                    onTabSelected = { component.selectTab(it) },
                    onToggleRecording = { component.toggleRecording() },
                    onPause = { component.pauseRecording() },
                    onClear = { component.clearRecording() },
                    onExport = { component.exportToFile() },
                    onSave = { component.showSaveDialog() },
                    onLoad = { component.showLoadDialog() },
                    onAddAction = { component.showAddActionDialog() }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // View mode and stats
                ActionControlsSection(
                    totalActions = recordedActions.size,
                    filteredActions = component.getFilteredActions().size,
                    selectedCount = selectedIndices.size,
                    viewMode = viewMode,
                    onViewModeChange = { component.setViewMode(it) },
                    onSelectAll = { component.selectAllActions() },
                    onClearSelection = { component.clearSelection() },
                    onDeleteSelected = { component.removeSelectedActions() }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Actions list
                ActionsListSection(
                    actions = component.getFilteredActions(),
                    selectedIndices = selectedIndices,
                    onToggleSelection = { component.toggleActionSelection(it) },
                    onRemove = { component.removeAction(it) },
                    onEdit = { index, action -> component.editAction(index, action) }
                )

                // Feedback message
                feedbackMessage?.let { feedback ->
                    Spacer(modifier = Modifier.height(8.dp))
                    FeedbackBar(
                        message = feedback,
                        onDismiss = { component.dismissFeedback() }
                    )
                }
            }
        }

        // Dialogs
        if (showAddDialog) {
            AddActionDialog(
                onDismiss = { component.hideAddActionDialog() },
                onAdd = { type, selectorType, selectorValue, value, elementText ->
                    component.addManualAction(type, selectorType, selectorValue, value, elementText)
                }
            )
        }

        if (showSaveDialog) {
            SaveConfigurationDialog(
                currentName = configName,
                onDismiss = { component.hideSaveDialog() },
                onSave = { name, description ->
                    component.saveConfiguration(name, description)
                }
            )
        }

        if (showLoadDialog) {
            LoadConfigurationDialog(
                configurations = savedConfigurations,
                onDismiss = { component.hideLoadDialog() },
                onLoad = { component.loadConfiguration(it) },
                onDelete = { component.deleteConfiguration(it) }
            )
        }
    }
}

/**
 * Compact header matching the bundled RPA Recorder design
 */
@Composable
private fun HeaderSection(
    recordingState: RecordingState,
    hasActions: Boolean,
    availableTabs: List<BrowserTabInfo>,
    selectedTab: BrowserTabInfo?,
    isConnected: Boolean,
    onTabSelected: (BrowserTabInfo) -> Unit,
    onToggleRecording: () -> Unit,
    onPause: () -> Unit,
    onClear: () -> Unit,
    onExport: () -> Unit,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onAddAction: () -> Unit
) {
    val isRecording = recordingState == RecordingState.RECORDING
    val isPaused = recordingState == RecordingState.PAUSED
    var dropdownExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        backgroundColor = if (isRecording)
            Color(0xFFFFEBEE).copy(alpha = 0.3f)
        else
            MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = "RPA Recorder",
                        modifier = Modifier.size(20.dp),
                        tint = if (isRecording) Color(0xFFD32F2F) else MaterialTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "RPA Recorder",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (isRecording) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Recording indicator
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = CircleShape,
                            color = Color(0xFFD32F2F)
                        ) {}
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Controls row with dropdown and buttons (matching bundled design)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab selection dropdown (takes most space)
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .clickable(
                                enabled = !isRecording && availableTabs.isNotEmpty()
                            ) {
                                dropdownExpanded = true
                            },
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colors.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedTab != null && !isRecording)
                                MaterialTheme.colors.primary.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedTab != null)
                                    MaterialTheme.colors.primary
                                else
                                    MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = selectedTab?.title ?: if (availableTabs.isEmpty())
                                    "No tabs"
                                else
                                    "Select tab...",
                                style = MaterialTheme.typography.body2,
                                color = if (selectedTab != null)
                                    MaterialTheme.colors.onSurface
                                else
                                    MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Icon(
                                if (dropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded && availableTabs.isNotEmpty(),
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        availableTabs.forEach { tab ->
                            DropdownMenuItem(
                                onClick = {
                                    onTabSelected(tab)
                                    dropdownExpanded = false
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Language,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (selectedTab?.id == tab.id)
                                            MaterialTheme.colors.primary
                                        else
                                            MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            tab.title,
                                            style = MaterialTheme.typography.body2,
                                            fontWeight = if (selectedTab?.id == tab.id) FontWeight.Medium else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            tab.url,
                                            style = MaterialTheme.typography.caption,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (selectedTab?.id == tab.id) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colors.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Record/Stop button - icon only for compactness
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = if (isRecording)
                        Color(0xFFD32F2F)
                    else if (selectedTab != null)
                        MaterialTheme.colors.primary
                    else
                        MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                    elevation = if (isRecording) 4.dp else 2.dp
                ) {
                    IconButton(
                        onClick = onToggleRecording,
                        enabled = selectedTab != null || isRecording,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (isRecording)
                                Icons.Default.Stop
                            else
                                Icons.Default.FiberManualRecord,
                            contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                    }
                }

                // Clear button
                IconButton(
                    onClick = onClear,
                    enabled = !isRecording && hasActions,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = if (!isRecording && hasActions)
                            MaterialTheme.colors.error
                        else
                            MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Export button
                IconButton(
                    onClick = onExport,
                    enabled = !isRecording && hasActions,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Export",
                        tint = if (!isRecording && hasActions)
                            MaterialTheme.colors.primary
                        else
                            MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // Connection status bar (shown when recording but not connected)
    if (isRecording && !isConnected) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFFF9800),
            shape = RoundedCornerShape(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "No browser connection - Open a Fluck tab",
                    style = MaterialTheme.typography.caption,
                    color = Color.White
                )
            }
        }
    }
}


@Composable
private fun ActionControlsSection(
    totalActions: Int,
    filteredActions: Int,
    selectedCount: Int,
    viewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onRefresh: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            // View mode selector and stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View mode tabs
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ViewMode.values().forEach { mode ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onViewModeChange(mode) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = if (viewMode == mode)
                                MaterialTheme.colors.primary.copy(alpha = 0.2f)
                            else
                                Color.Transparent
                        ) {
                            Text(
                                text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.caption,
                                fontWeight = if (viewMode == mode) FontWeight.Bold else FontWeight.Normal,
                                color = if (viewMode == mode)
                                    MaterialTheme.colors.primary
                                else
                                    MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Action stats
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colors.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "$selectedCount selected",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = if (viewMode == ViewMode.CLEAN && filteredActions < totalActions)
                            "$filteredActions of $totalActions"
                        else
                            "$totalActions actions",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )

                    // Refresh button
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colors.primary
                        )
                    }
                }
            }

            // Selection controls (shown when items exist)
            if (totalActions > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onSelectAll,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Select All", style = MaterialTheme.typography.caption)
                    }

                    if (selectedCount > 0) {
                        TextButton(
                            onClick = onClearSelection,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Clear Selection", style = MaterialTheme.typography.caption)
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun ActionsListSection(
    actions: List<RecordedAction>,
    selectedIndices: Set<Int>,
    onToggleSelection: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onEdit: (Int, RecordedAction) -> Unit
) {
    if (actions.isEmpty()) {
        EmptyStateCard()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(actions) { index, action ->
                ActionItemCard(
                    action = action,
                    index = index,
                    isSelected = selectedIndices.contains(index),
                    onToggleSelection = { onToggleSelection(index) },
                    onRemove = { onRemove(index) },
                    onEdit = { onEdit(index, it) }
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.1f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colors.primary.copy(alpha = 0.1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Default.TouchApp,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colors.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Ready to Record",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Select a browser tab and click Start Recording",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "All your interactions will be captured automatically",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ActionItemCard(
    action: RecordedAction,
    index: Int,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onRemove: () -> Unit,
    onEdit: (RecordedAction) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedValue by remember { mutableStateOf(action.value ?: "") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        backgroundColor = if (isSelected)
            MaterialTheme.colors.primary.copy(alpha = 0.1f)
        else
            MaterialTheme.colors.surface,
        elevation = if (isSelected) 2.dp else 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Selection checkbox
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    modifier = Modifier.padding(end = 8.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colors.primary
                    )
                )

                Column(modifier = Modifier.weight(1f)) {
                    // Action type with icon
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = getActionIcon(action.type),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colors.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${index + 1}. ${ActionTypes.getDisplayName(action.type).uppercase()}",
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Selector info
                    if (action.selector.type != SelectorTypes.NONE && action.selector.value != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SelectorTypeBadge(action.selector.type)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = action.selector.value ?: "",
                                style = MaterialTheme.typography.body2,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colors.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Value (for input, select, etc.)
                    if (action.value != null && action.type != ActionTypes.CLICK) {
                        Spacer(modifier = Modifier.height(4.dp))
                        if (isEditing) {
                            OutlinedTextField(
                                value = editedValue,
                                onValueChange = { editedValue = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.body2
                            )
                        } else {
                            Text(
                                text = "Value: ${action.value}",
                                style = MaterialTheme.typography.body2,
                                color = MaterialTheme.colors.onSurface
                            )
                        }
                    }

                    // Element text
                    if (!action.elementText.isNullOrEmpty()) {
                        Text(
                            text = "Text: ${action.elementText}",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Action buttons
                Row {
                    // Edit (for input/select actions)
                    if (action.type in listOf(ActionTypes.INPUT, ActionTypes.SELECT, ActionTypes.WAIT)) {
                        IconButton(
                            onClick = {
                                if (isEditing) {
                                    onEdit(action.copy(value = editedValue))
                                    isEditing = false
                                } else {
                                    isEditing = true
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = if (isEditing) "Save" else "Edit",
                                modifier = Modifier.size(16.dp),
                                tint = if (isEditing) Color.Green else MaterialTheme.colors.onSurface
                            )
                        }
                    }

                    // Delete
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colors.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectorTypeBadge(type: String) {
    val (bgColor, textColor) = when (type) {
        SelectorTypes.ID -> Color(0xFF4CAF50).copy(alpha = 0.2f) to Color(0xFF4CAF50)
        SelectorTypes.CSS -> Color(0xFF2196F3).copy(alpha = 0.2f) to Color(0xFF2196F3)
        SelectorTypes.XPATH -> Color(0xFFFF9800).copy(alpha = 0.2f) to Color(0xFFFF9800)
        SelectorTypes.TEXT -> Color(0xFF9C27B0).copy(alpha = 0.2f) to Color(0xFF9C27B0)
        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.1f) to MaterialTheme.colors.onSurface
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = SelectorTypes.getDisplayName(type),
            style = MaterialTheme.typography.caption,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun getActionIcon(type: String) = when (type) {
    ActionTypes.CLICK -> Icons.Default.TouchApp
    ActionTypes.INPUT -> Icons.Default.Keyboard
    ActionTypes.SELECT -> Icons.Default.ArrowDropDown
    ActionTypes.NAVIGATE -> Icons.Default.Navigation
    ActionTypes.SCROLL -> Icons.Default.SwapVert
    ActionTypes.WAIT -> Icons.Default.Schedule
    ActionTypes.SCREENSHOT -> Icons.Default.PhotoCamera
    ActionTypes.ASSERT -> Icons.Default.CheckCircle
    else -> Icons.Default.Code
}

@Composable
private fun FeedbackBar(
    message: FeedbackMessage,
    onDismiss: () -> Unit
) {
    val (bgColor, iconTint) = when (message.type) {
        FeedbackType.SUCCESS -> Color(0xFF4CAF50) to Color.White
        FeedbackType.INFO -> MaterialTheme.colors.primary to Color.White
        FeedbackType.WARNING -> Color(0xFFFF9800) to Color.White
        FeedbackType.ERROR -> Color(0xFFD32F2F) to Color.White
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (message.type) {
                    FeedbackType.SUCCESS -> Icons.Default.CheckCircle
                    FeedbackType.INFO -> Icons.Default.Info
                    FeedbackType.WARNING -> Icons.Default.Warning
                    FeedbackType.ERROR -> Icons.Default.Error
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = iconTint
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message.text,
                style = MaterialTheme.typography.body2,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun AddActionDialog(
    onDismiss: () -> Unit,
    onAdd: (type: String, selectorType: String, selectorValue: String?, value: String?, elementText: String?) -> Unit
) {
    var selectedType by remember { mutableStateOf(ActionTypes.CLICK) }
    var selectorType by remember { mutableStateOf(SelectorTypes.CSS) }
    var selectorValue by remember { mutableStateOf("") }
    var actionValue by remember { mutableStateOf("") }
    var elementText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Add Action",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action type dropdown
                Text("Action Type", style = MaterialTheme.typography.caption)
                ActionTypeDropdown(
                    selected = selectedType,
                    onSelected = { selectedType = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Selector type dropdown
                Text("Selector Type", style = MaterialTheme.typography.caption)
                SelectorTypeDropdown(
                    selected = selectorType,
                    onSelected = { selectorType = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Selector value
                if (selectorType != SelectorTypes.NONE) {
                    OutlinedTextField(
                        value = selectorValue,
                        onValueChange = { selectorValue = it },
                        label = { Text("Selector") },
                        placeholder = { Text(getSelectorPlaceholder(selectorType)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Action value (for input, wait, navigate, etc.)
                if (selectedType in listOf(ActionTypes.INPUT, ActionTypes.WAIT, ActionTypes.NAVIGATE, ActionTypes.SELECT, ActionTypes.ASSERT)) {
                    OutlinedTextField(
                        value = actionValue,
                        onValueChange = { actionValue = it },
                        label = { Text(getValueLabel(selectedType)) },
                        placeholder = { Text(getValuePlaceholder(selectedType)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Element text (optional)
                OutlinedTextField(
                    value = elementText,
                    onValueChange = { elementText = it },
                    label = { Text("Element Text (optional)") },
                    placeholder = { Text("Visible text of the element") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onAdd(
                                selectedType,
                                selectorType,
                                selectorValue.takeIf { it.isNotEmpty() },
                                actionValue.takeIf { it.isNotEmpty() },
                                elementText.takeIf { it.isNotEmpty() }
                            )
                        }
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionTypeDropdown(
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(ActionTypes.getDisplayName(selected))
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ActionTypes.all.forEach { type ->
                DropdownMenuItem(onClick = {
                    onSelected(type)
                    expanded = false
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            getActionIcon(type),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colors.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(ActionTypes.getDisplayName(type))
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectorTypeDropdown(
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(SelectorTypes.getDisplayName(selected))
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SelectorTypes.all.forEach { type ->
                DropdownMenuItem(onClick = {
                    onSelected(type)
                    expanded = false
                }) {
                    Text(SelectorTypes.getDisplayName(type))
                }
            }
        }
    }
}

private fun getSelectorPlaceholder(type: String) = when (type) {
    SelectorTypes.CSS -> "e.g., #login-button, .submit-btn"
    SelectorTypes.XPATH -> "e.g., //button[@id='login']"
    SelectorTypes.ID -> "e.g., login-button"
    SelectorTypes.TEXT -> "e.g., Submit"
    else -> "Enter selector"
}

private fun getValueLabel(type: String) = when (type) {
    ActionTypes.INPUT -> "Text to Type"
    ActionTypes.WAIT -> "Wait Time (ms)"
    ActionTypes.NAVIGATE -> "URL"
    ActionTypes.SELECT -> "Option Value"
    ActionTypes.ASSERT -> "Expected Value"
    else -> "Value"
}

private fun getValuePlaceholder(type: String) = when (type) {
    ActionTypes.INPUT -> "Text to enter"
    ActionTypes.WAIT -> "1000"
    ActionTypes.NAVIGATE -> "https://example.com"
    ActionTypes.SELECT -> "option-value"
    ActionTypes.ASSERT -> "Expected text or value"
    else -> "Value"
}

@Composable
private fun SaveConfigurationDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var description by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Save Configuration",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Configuration Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(name, description) },
                        enabled = name.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadConfigurationDialog(
    configurations: List<String>,
    onDismiss: () -> Unit,
    onLoad: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Load Configuration",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (configurations.isEmpty()) {
                    Text(
                        "No saved configurations",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(configurations) { config ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onLoad(config) },
                                backgroundColor = MaterialTheme.colors.surface,
                                elevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colors.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        config,
                                        style = MaterialTheme.typography.body1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onDelete(config) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colors.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
