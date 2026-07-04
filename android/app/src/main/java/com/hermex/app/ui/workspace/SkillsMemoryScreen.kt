package com.hermex.app.ui.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsScreen(
    onBack: () -> Unit,
    viewModel: SkillsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadSkills() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.selectedSkill?.name ?: "Skills") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.selectedSkill != null) viewModel.selectSkill(null)
                        else onBack()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (uiState.selectedSkill != null) {
                if (uiState.errorMessage != null) {
                    Card(
                        Modifier.fillMaxWidth().padding(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(uiState.errorMessage ?: "", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    TextButton(onClick = { viewModel.loadSkillContent(uiState.selectedSkill!!.name) }) {
                        Text("Retry")
                    }
                } else {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(
                            uiState.skillContent ?: "Loading...",
                            Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = uiState.searchQuery, onValueChange = { viewModel.setSearchQuery(it) },
                    Modifier.fillMaxWidth().padding(12.dp),
                    placeholder = { Text("Search skills...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true
                )
                if (uiState.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (uiState.errorMessage != null) {
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(uiState.errorMessage ?: "", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                val filtered = if (uiState.searchQuery.isBlank()) uiState.skills
                else uiState.skills.filter { it.name.contains(uiState.searchQuery, ignoreCase = true) }
                LazyColumn {
                    items(filtered) { skill ->
                        ListItem(
                            headlineContent = { Text(skill.name) },
                            supportingContent = skill.description?.let { { Text(it) } },
                            leadingContent = { Icon(Icons.Default.Build, null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable { viewModel.selectSkill(skill) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    onBack: () -> Unit,
    viewModel: MemoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadMemory() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memory") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(uiState.selectedTab) {
                Tab(selected = uiState.selectedTab == 0, onClick = { viewModel.selectTab(0) }, text = { Text("Notes") })
                Tab(selected = uiState.selectedTab == 1, onClick = { viewModel.selectTab(1) }, text = { Text("Profile") })
                Tab(selected = uiState.selectedTab == 2, onClick = { viewModel.selectTab(2) }, text = { Text("Session") })
            }
            if (uiState.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (uiState.errorMessage != null) {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(uiState.errorMessage ?: "", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            when (uiState.selectedTab) {
                0 -> Text(uiState.notes ?: "No notes", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                1 -> Text(uiState.profile ?: "No profile", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                2 -> Text(uiState.sessionNotes ?: "No session notes", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
