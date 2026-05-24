package com.whit31ister.juassign

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.whit31ister.juassign.ui.theme.JUAssignTheme

class MainActivity : ComponentActivity() {
    private val apiService = LibraryApiService.create()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JUAssignTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var allAssignments by remember { mutableStateOf<List<AssignmentFile>>(emptyList()) }
                    var isLoading by remember { mutableStateOf(true) }
                    var errorMessage by remember { mutableStateOf<String?>(null) }
                    
                    var currentPath by remember { mutableStateOf<List<String>>(emptyList()) }
                    var searchQuery by remember { mutableStateOf("") }
                    var isSearching by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        try {
                            val manifest = withContext(Dispatchers.IO) {
                                apiService.getManifest()
                            }
                            allAssignments = manifest.files
                        } catch (e: Exception) {
                            errorMessage = e.message
                        } finally {
                            isLoading = false
                        }
                    }

                    BackHandler(enabled = currentPath.isNotEmpty() || isSearching) {
                        if (isSearching) {
                            isSearching = false
                            searchQuery = ""
                        } else if (currentPath.isNotEmpty()) {
                            currentPath = currentPath.dropLast(1)
                        }
                    }

                    Scaffold(
                        topBar = {
                            if (isSearching) {
                                SearchBarTop(
                                    query = searchQuery,
                                    onQueryChange = { searchQuery = it },
                                    onClose = {
                                        isSearching = false
                                        searchQuery = ""
                                    }
                                )
                            } else {
                                TopAppBar(
                                    title = {
                                        Text(if (currentPath.isEmpty()) "JU Assignments" else currentPath.last())
                                    },
                                    navigationIcon = {
                                        if (currentPath.isNotEmpty()) {
                                            IconButton(onClick = { currentPath = currentPath.dropLast(1) }) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                            }
                                        }
                                    },
                                    actions = {
                                        IconButton(onClick = { isSearching = true }) {
                                            Icon(Icons.Default.Search, contentDescription = "Search")
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.background,
                                        titleContentColor = MaterialTheme.colorScheme.onBackground
                                    )
                                )
                            }
                        }
                    ) { padding ->
                        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            } else if (errorMessage != null) {
                                Text(
                                    text = "Error: $errorMessage",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(16.dp).align(Alignment.Center)
                                )
                            } else {
                                val displayItems = remember(allAssignments, currentPath, searchQuery) {
                                    computeDisplayItems(allAssignments, currentPath, searchQuery)
                                }
                                
                                if (displayItems.isEmpty()) {
                                    Text(
                                        text = "No files found.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                } else {
                                    AssignmentList(
                                        items = displayItems,
                                        onFolderClick = { folderName ->
                                            currentPath = currentPath + folderName
                                        },
                                        onFileClick = { file ->
                                            val url = "https://whit31ister.github.io/JU_ASSIGN/${file.path}"
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            startActivity(intent)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class DisplayItem(
    val isFolder: Boolean,
    val name: String,
    val description: String,
    val file: AssignmentFile? = null
)

fun computeDisplayItems(
    allAssignments: List<AssignmentFile>,
    currentPath: List<String>,
    searchQuery: String
): List<DisplayItem> {
    if (searchQuery.isNotBlank()) {
        val query = searchQuery.lowercase()
        return allAssignments
            .filter { it.title.lowercase().contains(query) || it.path.lowercase().contains(query) }
            .sortedBy { it.title }
            .map { DisplayItem(isFolder = false, name = it.title, description = it.path, file = it) }
    }

    val depth = currentPath.size
    val folders = mutableSetOf<String>()
    val files = mutableListOf<AssignmentFile>()

    allAssignments.forEach { assignment ->
        val parts = assignment.path.split("/")
        
        var matchesPath = true
        for (i in currentPath.indices) {
            if (i >= parts.size || parts[i] != currentPath[i]) {
                matchesPath = false
                break
            }
        }

        if (matchesPath) {
            if (parts.size > depth + 1) {
                folders.add(parts[depth])
            } else if (parts.size == depth + 1) {
                files.add(assignment)
            }
        }
    }

    val displayFolders = folders.sorted().map { folderName ->
        DisplayItem(isFolder = true, name = folderName, description = "Folder")
    }
    
    val displayFiles = files.sortedBy { it.title }.map { file ->
        DisplayItem(isFolder = false, name = file.title, description = file.description, file = file)
    }

    return displayFolders + displayFiles
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarTop(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search by title or file name") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedIndicatorColor = MaterialTheme.colorScheme.background,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.background
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Close search")
            }
        },
        actions = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
fun AssignmentList(
    items: List<DisplayItem>,
    onFolderClick: (String) -> Unit,
    onFileClick: (AssignmentFile) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (item.isFolder) {
                            onFolderClick(item.name)
                        } else {
                            item.file?.let { onFileClick(it) }
                        }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = MaterialTheme.shapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon: ImageVector = if (item.isFolder) Icons.Default.Folder else Icons.Default.Description
                    val iconTint = if (item.isFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(32.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
