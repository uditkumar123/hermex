package com.hermex.app.ui.sessionlist

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hermex.app.data.model.SessionSummary
import com.hermex.app.ui.theme.HermexBlue
import com.hermex.app.util.toRelativeTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    onSessionClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: SessionListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var showSearch by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showProfileMenu by remember { mutableStateOf(false) }
    var showProjectFilter by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var sessionToRename by remember { mutableStateOf<SessionSummary?>(null) }
    var searchDebounceJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadSessions()
        viewModel.loadProfiles()
        viewModel.loadProjects()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sessions") },
                actions = {
                    IconButton(onClick = {
                        showSearch = !showSearch
                        if (!showSearch) viewModel.search("")
                    }) {
                        Icon(
                            if (showSearch) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                    IconButton(onClick = { showProjectFilter = !showProjectFilter }) {
                        Icon(
                            if (uiState.selectedProjectId != null) Icons.Default.FilterListOff else Icons.Default.FilterList,
                            contentDescription = "Filter by project"
                        )
                    }
                    Box {
                        IconButton(onClick = { showProfileMenu = true }) {
                            Icon(Icons.Default.Person, contentDescription = "Profile")
                        }
                        DropdownMenu(
                            expanded = showProfileMenu,
                            onDismissRequest = { showProfileMenu = false }
                        ) {
                            uiState.profiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = profile.displayName,
                                            fontWeight = if (profile.normalizedName == uiState.activeProfile) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        showProfileMenu = false
                                        profile.normalizedName?.let { viewModel.switchProfile(it) }
                                    },
                                    leadingIcon = {
                                        if (profile.normalizedName == uiState.activeProfile) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = HermexBlue
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Session")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            if (showSearch) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { query ->
                        viewModel.search(query)
                        searchDebounceJob?.cancel()
                        searchDebounceJob = scope.launch {
                            delay(350)
                            viewModel.searchSessionsRemote(query)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search sessions...") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.search("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )
            }

            // Project filter chips
            if (showProjectFilter && uiState.projects.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedProjectId == null,
                        onClick = { viewModel.filterByProject(null) },
                        label = { Text("All") }
                    )
                    uiState.projects.take(5).forEach { project ->
                        FilterChip(
                            selected = uiState.selectedProjectId == project.projectId,
                            onClick = { viewModel.filterByProject(project.projectId) },
                            label = { Text(project.name ?: "Unknown") }
                        )
                    }
                }
            }

            // Offline banner
            if (uiState.isOfflineMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Offline mode - showing cached data",
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Error banner
            if (uiState.errorMessage != null && !uiState.isOfflineMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss")
                        }
                    }
                }
            }

            // Content
            when {
                uiState.isLoading && !uiState.isRefreshing -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.sessions.isEmpty() && !uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No sessions yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Tap + to start a new conversation", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                else -> {
                    val sections = viewModel.sectionedSessions()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        sections.forEach { section ->
                            item(key = "header_${section.title}") {
                                Text(
                                    text = section.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp)
                                )
                            }

                            items(
                                items = section.sessions,
                                key = { it.sessionId ?: it.hashCode().toString() }
                            ) { session ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        when (value) {
                                            SwipeToDismissBoxValue.EndToStart -> {
                                                viewModel.deleteSession(session)
                                                true
                                            }
                                            SwipeToDismissBoxValue.StartToEnd -> {
                                                viewModel.pinSession(session)
                                                false // Don't dismiss, just pin
                                            }
                                            else -> false
                                        }
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val direction = dismissState.dismissDirection
                                        val color = when (direction) {
                                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                                            SwipeToDismissBoxValue.StartToEnd -> HermexBlue
                                            else -> MaterialTheme.colorScheme.surface
                                        }
                                        val icon = when (direction) {
                                            SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                                            SwipeToDismissBoxValue.StartToEnd -> Icons.Default.PushPin
                                            else -> Icons.Default.Delete
                                        }
                                        val alignment = when (direction) {
                                            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                            else -> Alignment.CenterStart
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 16.dp),
                                            contentAlignment = alignment
                                        ) {
                                            Icon(
                                                icon,
                                                contentDescription = null,
                                                tint = color
                                            )
                                        }
                                    }
                                ) {
                                    SessionRow(
                                        session = session,
                                        onClick = { session.sessionId?.let { onSessionClick(it) } },
                                        onPin = { viewModel.pinSession(session) },
                                        onArchive = { viewModel.archiveSession(session) },
                                        onDelete = { viewModel.deleteSession(session) },
                                        onRename = {
                                            sessionToRename = session
                                            showRenameDialog = true
                                        },
                                        onDuplicate = { viewModel.duplicateSession(session) },
                                        onMoveToProject = { viewModel.moveToProject(session, null) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create session dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Session") },
            text = { Text("Create a new chat session?") },
            confirmButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    viewModel.createSession(onSessionClick)
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename session dialog
    if (showRenameDialog && sessionToRename != null) {
        var renameText by remember { mutableStateOf(sessionToRename?.title ?: "") }
        AlertDialog(
            onDismissRequest = {
                showRenameDialog = false
                sessionToRename = null
            },
            title = { Text("Rename Session") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Session title") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    sessionToRename?.let { session ->
                        viewModel.renameSession(session, renameText)
                    }
                    showRenameDialog = false
                    sessionToRename = null
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRenameDialog = false
                    sessionToRename = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionRow(
    session: SessionSummary,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveToProject: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }

    ListItem(
        modifier = Modifier
            .clickable(onClick = onClick)
            .animateContentSize(),
        headlineContent = {
            Text(
                text = session.effectiveDisplayTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (session.pinned == true) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        supportingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (session.model != null) {
                    Text(
                        text = session.model,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                if (session.messageCount != null && session.messageCount > 0) {
                    Text(
                        text = "${session.messageCount} msgs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (session.effectiveTimestamp > 0) {
                    Text(
                        text = session.effectiveTimestamp.toRelativeTime(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        leadingContent = {
            if (session.isStreaming == true) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = HermexBlue)
            } else if (session.pinned == true) {
                Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = HermexBlue, modifier = Modifier.size(16.dp))
            }
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (session.pinned == true) "Unpin" else "Pin") },
                        onClick = { showMenu = false; onPin() },
                        leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { showMenu = false; onRename() },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        onClick = { showMenu = false; onDuplicate() },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Move to Project") },
                        onClick = { showMenu = false; onMoveToProject() },
                        leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Archive") },
                        onClick = { showMenu = false; onArchive() },
                        leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    )
    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outline)
}
