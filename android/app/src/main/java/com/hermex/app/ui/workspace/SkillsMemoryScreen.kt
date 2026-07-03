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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.hermex.app.data.api.RetrofitProvider
import com.hermex.app.data.auth.AuthManager
import com.hermex.app.data.auth.AuthState
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsScreen(onBack: () -> Unit) {
    val authManager = AuthManager.getInstance(androidx.compose.ui.platform.LocalContext.current)
    val scope = rememberCoroutineScope()
    var skills by remember { mutableStateOf<List<SkillItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSkill by remember { mutableStateOf<SkillItem?>(null) }
    var skillContent by remember { mutableStateOf<String?>(null) }

    fun loadSkills() {
        val url = (authManager.state.value as? AuthState.LoggedIn)?.serverUrl ?: return
        val api = RetrofitProvider.createApi(url)
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = api.skills()
                skills = response.skills?.mapNotNull { skill ->
                    try {
                        val obj = skill.jsonObject
                        SkillItem(
                            name = obj["name"]?.jsonPrimitive?.content ?: "",
                            description = obj["description"]?.jsonPrimitive?.content
                        )
                    } catch (_: Exception) { null }
                } ?: emptyList()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load skills"
            } finally { isLoading = false }
        }
    }

    fun loadSkillContent(name: String) {
        val url = (authManager.state.value as? AuthState.LoggedIn)?.serverUrl ?: return
        val api = RetrofitProvider.createApi(url)
        scope.launch {
            try {
                val response = api.skillContent(name)
                skillContent = response.content
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load skill content"
            }
        }
    }

    LaunchedEffect(Unit) { loadSkills() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedSkill?.name ?: "Skills") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedSkill != null) { selectedSkill = null; skillContent = null } else onBack()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (selectedSkill != null) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        skillContent ?: "Loading...",
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    Modifier.fillMaxWidth().padding(12.dp),
                    placeholder = { Text("Search skills...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true
                )
                if (isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (errorMessage != null) {
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(errorMessage ?: "", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                val filtered = if (searchQuery.isBlank()) skills
                else skills.filter { it.name.contains(searchQuery, ignoreCase = true) }
                LazyColumn {
                    items(filtered) { skill ->
                        ListItem(
                            headlineContent = { Text(skill.name) },
                            supportingContent = skill.description?.let { { Text(it) } },
                            leadingContent = { Icon(Icons.Default.Build, null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable { selectedSkill = skill; loadSkillContent(skill.name) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(onBack: () -> Unit) {
    val authManager = AuthManager.getInstance(androidx.compose.ui.platform.LocalContext.current)
    val scope = rememberCoroutineScope()
    var notes by remember { mutableStateOf<String?>(null) }
    var profile by remember { mutableStateOf<String?>(null) }
    var sessionNotes by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    fun loadMemory() {
        val url = (authManager.state.value as? AuthState.LoggedIn)?.serverUrl ?: return
        val api = RetrofitProvider.createApi(url)
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = api.memory()
                notes = response.notes
                profile = response.profile
                sessionNotes = response.sessionNotes
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load memory"
            } finally { isLoading = false }
        }
    }

    LaunchedEffect(Unit) { loadMemory() }

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
            TabRow(selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Notes") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Profile") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Session") })
            }
            if (isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (errorMessage != null) {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(errorMessage ?: "", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            when (selectedTab) {
                0 -> Text(notes ?: "No notes", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                1 -> Text(profile ?: "No profile", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                2 -> Text(sessionNotes ?: "No session notes", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

private data class SkillItem(val name: String, val description: String? = null)
