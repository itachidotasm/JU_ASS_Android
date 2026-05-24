package com.whit31ister.juassign

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
                    var viewingFile by remember { mutableStateOf<AssignmentFile?>(null) }

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

                    BackHandler(enabled = viewingFile != null || currentPath.isNotEmpty() || isSearching) {
                        if (viewingFile != null) {
                            viewingFile = null
                        } else if (isSearching) {
                            isSearching = false
                            searchQuery = ""
                        } else if (currentPath.isNotEmpty()) {
                            currentPath = currentPath.dropLast(1)
                        }
                    }

                    Scaffold(
                        topBar = {
                            if (viewingFile != null) {
                                TopAppBar(
                                    title = { Text(viewingFile!!.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    navigationIcon = {
                                        IconButton(onClick = { viewingFile = null }) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Close Viewer")
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.background,
                                        titleContentColor = MaterialTheme.colorScheme.onBackground
                                    )
                                )
                            } else if (isSearching) {
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
                            } else if (viewingFile != null) {
                                DocumentViewerScreen(path = viewingFile!!.path)
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
                                            viewingFile = file
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

@Composable
fun DocumentViewerScreen(path: String) {
    val ext = path.substringAfterLast('.', "").lowercase()
    
    // 1. Correctly encode the path pieces (spaces -> %20, + -> %2B)
    val encodedPath = path.split("/").joinToString("/") { android.net.Uri.encode(it) }
    val directFileUrl = "https://whit31ister.github.io/JU_ASSIGN/$encodedPath"
    
    // 2. Safely encode the entire URL to be passed as a query parameter
    val encodedParam = java.net.URLEncoder.encode(directFileUrl, "UTF-8")
    
    val viewerUrl = when (ext) {
        "pdf" -> "https://docs.google.com/gview?embedded=true&url=$encodedParam"
        "docx", "doc", "pptx", "ppt", "xlsx", "xls" -> "https://view.officeapps.live.com/op/view.aspx?src=$encodedParam"
        else -> directFileUrl
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Forcibly unlock pinch-to-zoom on embedded viewers
                        view?.evaluateJavascript(
                            "try { document.querySelector('meta[name=\"viewport\"]').setAttribute('content', 'width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes'); } catch(e) {}", 
                            null
                        )
                    }
                }
                loadUrl(viewerUrl)
            }
        },
        update = { webView ->
            webView.loadUrl(viewerUrl)
        },
        modifier = Modifier.fillMaxSize()
    )
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
    val scopedAssignments = allAssignments.filter { assignment ->
        val parts = assignment.path.split("/")
        var matchesPath = true
        for (i in currentPath.indices) {
            if (i >= parts.size || parts[i] != currentPath[i]) {
                matchesPath = false
                break
            }
        }
        matchesPath
    }

    if (searchQuery.isNotBlank()) {
        val query = searchQuery.lowercase()
        return scopedAssignments
            .filter { it.title.lowercase().contains(query) || it.path.lowercase().contains(query) }
            .sortedBy { it.title }
            .map { DisplayItem(isFolder = false, name = it.title, description = it.path, file = it) }
    }

    val depth = currentPath.size
    val folders = mutableSetOf<String>()
    val files = mutableListOf<AssignmentFile>()

    scopedAssignments.forEach { assignment ->
        val parts = assignment.path.split("/")
        if (parts.size > depth + 1) {
            folders.add(parts[depth])
        } else if (parts.size == depth + 1) {
            files.add(assignment)
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
