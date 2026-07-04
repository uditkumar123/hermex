package com.hermex.app.ui.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.hermex.app.data.api.RetrofitProvider
import com.hermex.app.data.auth.AuthManager
import com.hermex.app.data.auth.AuthState
import com.hermex.app.data.model.FileEntry
import com.hermex.app.data.model.FileListResponse
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    sessionId: String,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    val authManager = AuthManager.getInstance(androidx.compose.ui.platform.LocalContext.current)
    val scope = rememberCoroutineScope()
    var files by remember { mutableStateOf<List<FileEntry>>(emptyList()) }
    var currentPath by remember { mutableStateOf("/") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFile by remember { mutableStateOf<FileEntry?>(null) }
    var fileContent by remember { mutableStateOf<String?>(null) }
    var fileContentLoading by remember { mutableStateOf(false) }

    fun loadFiles(path: String) {
        val url = (authManager.state.value as? AuthState.LoggedIn)?.serverUrl ?: return
        val api = RetrofitProvider.createApi(url)
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = api.listFiles(sessionId, path)
                files = response.files ?: emptyList()
                currentPath = response.currentPath ?: path
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun loadFileContent(file: FileEntry) {
        val url = (authManager.state.value as? AuthState.LoggedIn)?.serverUrl ?: return
        val api = RetrofitProvider.createApi(url)
        scope.launch {
            fileContentLoading = true
            selectedFile = file
            try {
                val response = api.getFile(sessionId, file.path ?: file.name ?: return@launch)
                fileContent = response.content
            } catch (e: Exception) {
                fileContent = "Error loading file: ${e.message}"
            } finally {
                fileContentLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadFiles("/") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = selectedFile?.name ?: "Files",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (selectedFile == null && currentPath != "/") {
                            Text(
                                text = currentPath,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedFile != null) {
                            selectedFile = null
                            fileContent = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (selectedFile != null) {
                        IconButton(onClick = { selectedFile = null; fileContent = null }) {
                            Icon(Icons.Default.Close, "Close file")
                        }
                    } else {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, "Close")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (selectedFile != null) {
                if (fileContentLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (isImageFile(selectedFile?.name)) {
                    val imageUrl = buildString {
                        val base = (authManager.state.value as? AuthState.LoggedIn)?.serverUrl?.trimEnd('/')
                        if (base != null) append("$base/api/file?session_id=$sessionId&path=${selectedFile?.path ?: selectedFile?.name ?: ""}")
                    }
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = selectedFile?.name ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                        selectedFile?.size?.let { Text(formatFileSize(it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Spacer(Modifier.height(8.dp))
                        val ctx = LocalContext.current
                        AsyncImage(
                            model = ImageRequest.Builder(ctx)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = selectedFile?.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = fileContent ?: "(empty)",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    placeholder = { Text("Search files...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true
                )

                if (isLoading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }

                if (errorMessage != null) {
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(errorMessage!!, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }

                val filteredFiles = if (searchQuery.isBlank()) files
                else files.filter { it.name?.contains(searchQuery, ignoreCase = true) == true }

                LazyColumn {
                    val dirs = filteredFiles.filter { it.isDir == true }
                    val regular = filteredFiles.filter { it.isDir != true }

                    if (dirs.isNotEmpty()) {
                        item { Subheader("Directories") }
                        items(dirs, key = { it.path ?: it.name ?: it.hashCode().toString() }) { file ->
                            FileRow(file = file, isDir = true, onClick = {
                                loadFiles(file.path ?: "/")
                                searchQuery = ""
                            })
                        }
                    }
                    if (regular.isNotEmpty()) {
                        if (dirs.isNotEmpty()) item { Divider(modifier = Modifier.padding(horizontal = 12.dp)) }
                        item { Subheader("Files") }
                        items(regular, key = { it.path ?: it.name ?: it.hashCode().toString() }) { file ->
                            FileRow(file = file, isDir = false, onClick = { loadFileContent(file) })
                        }
                    }
                    if (filteredFiles.isEmpty() && !isLoading) {
                        item {
                            Text(
                                "No files found",
                                Modifier.padding(32.dp).fillMaxWidth(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Subheader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun FileRow(file: FileEntry, isDir: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(file.name ?: "(unknown)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            if (!isDir && file.size != null) {
                Text(formatFileSize(file.size))
            }
        },
        leadingContent = {
            Icon(
                if (isDir) Icons.Default.Folder else fileIcon(file.name),
                contentDescription = null,
                tint = if (isDir) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun fileIcon(name: String?) = when {
    name == null -> Icons.Default.InsertDriveFile
    name.endsWith(".png", true) || name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) -> Icons.Default.Image
    name.endsWith(".py", true) -> Icons.Default.Code
    name.endsWith(".kt", true) -> Icons.Default.Code
    name.endsWith(".js", true) -> Icons.Default.Code
    name.endsWith(".json", true) -> Icons.Default.DataObject
    name.endsWith(".md", true) -> Icons.Default.Description
    name.endsWith(".txt", true) -> Icons.Default.TextSnippet
    name.endsWith(".pdf", true) -> Icons.Default.PictureAsPdf
    else -> Icons.Default.InsertDriveFile
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
    else -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
}

private fun isImageFile(name: String?): Boolean = name?.let {
    it.endsWith(".png", true) || it.endsWith(".jpg", true) || it.endsWith(".jpeg", true) || it.endsWith(".gif", true) || it.endsWith(".webp", true) || it.endsWith(".bmp", true)
} ?: false
