package com.whit31ister.juassign

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.whit31ister.juassign.ui.theme.JUAssignTheme

class MainActivity : ComponentActivity() {
    private val apiService = LibraryApiService.create()

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val systemDarkTheme = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDarkTheme) }

            JUAssignTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var allAssignments by remember { mutableStateOf<List<AssignmentFile>>(emptyList()) }
                    var isLoading by remember { mutableStateOf(true) }
                    var isRefreshing by remember { mutableStateOf(false) }
                    var errorMessage by remember { mutableStateOf<String?>(null) }
                    
                    var currentPath by remember { mutableStateOf<List<String>>(listOf("assignments")) }
                    var searchQuery by remember { mutableStateOf("") }
                    var isSearching by remember { mutableStateOf(false) }
                    var viewingFile by remember { mutableStateOf<AssignmentFile?>(null) }
                    var sortOrder by remember { mutableStateOf(SortOrder.A_Z) }
                    var showSortMenu by remember { mutableStateOf(false) }
                    
                    val coroutineScope = rememberCoroutineScope()
                    val pullRefreshState = rememberPullRefreshState(
                        refreshing = isRefreshing,
                        onRefresh = {
                            coroutineScope.launch {
                                isRefreshing = true
                                try {
                                    val manifest = withContext(Dispatchers.IO) { apiService.getManifest() }
                                    allAssignments = manifest.files
                                    errorMessage = null
                                } catch (e: Exception) {
                                    errorMessage = e.message
                                } finally {
                                    isRefreshing = false
                                }
                            }
                        }
                    )

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

                    BackHandler(enabled = viewingFile != null || currentPath.size > 1 || isSearching) {
                        if (viewingFile != null) {
                            viewingFile = null
                        } else if (isSearching) {
                            isSearching = false
                            searchQuery = ""
                        } else if (currentPath.size > 1) {
                            currentPath = currentPath.dropLast(1)
                        }
                    }

                    val context = LocalContext.current

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
                                    actions = {
                                        IconButton(onClick = { downloadFile(context, viewingFile!!.path, viewingFile!!.title) }) {
                                            Icon(Icons.Default.Download, contentDescription = "Download File")
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
                                        if (currentPath.size > 1) {
                                            IconButton(onClick = { currentPath = currentPath.dropLast(1) }) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                            }
                                        }
                                    },
                                    actions = {
                                        Box {
                                            IconButton(onClick = { showSortMenu = true }) {
                                                Icon(Icons.Default.Sort, contentDescription = "Sort")
                                            }
                                            DropdownMenu(
                                                expanded = showSortMenu,
                                                onDismissRequest = { showSortMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Name (A to Z)") },
                                                    onClick = { sortOrder = SortOrder.A_Z; showSortMenu = false }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Name (Z to A)") },
                                                    onClick = { sortOrder = SortOrder.Z_A; showSortMenu = false }
                                                )
                                            }
                                        }
                                        IconButton(onClick = { isDarkTheme = !isDarkTheme }) {
                                            Icon(
                                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                                contentDescription = "Toggle Theme"
                                            )
                                        }
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
                                DocumentViewerScreen(path = viewingFile!!.path, isDarkTheme = isDarkTheme)
                            } else {
                                val displayItems = remember(allAssignments, currentPath, searchQuery, sortOrder) {
                                    computeDisplayItems(allAssignments, currentPath, searchQuery, sortOrder)
                                }
                                
                                Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
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
                                    PullRefreshIndicator(
                                        refreshing = isRefreshing,
                                        state = pullRefreshState,
                                        modifier = Modifier.align(Alignment.TopCenter),
                                        backgroundColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.primary
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
fun DocumentViewerScreen(path: String, isDarkTheme: Boolean) {
    val ext = path.substringAfterLast('.', "").lowercase()
    
    // 1. Correctly encode the path pieces (spaces -> %20, + -> %2B)
    val encodedPath = path.split("/").joinToString("/") { android.net.Uri.encode(it) }
    val directFileUrl = "https://whit31ister.github.io/JU_ASSIGN/$encodedPath"
    
    // 2. Safely encode the entire URL to be passed as a query parameter
    val encodedParam = java.net.URLEncoder.encode(directFileUrl, "UTF-8")
    
    val viewerUrl = when (ext) {
        "pdf", "docx", "doc", "pptx", "ppt", "xlsx", "xls" -> "https://docs.google.com/gview?embedded=true&url=$encodedParam"
        "md" -> "about:blank"
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
                // Initial load handled in update to prevent duplicating the md code
            }
        },
        update = { webView ->
            if (ext == "md") {
                val bg = if (isDarkTheme) "#121212" else "#f5f2e9"
                val surface = if (isDarkTheme) "#1e1e1e" else "#fdfcf9"
                val text = if (isDarkTheme) "#e0e0e0" else "#33302a"
                val accent = if (isDarkTheme) "#8b7d72" else "#706359"
                val line = if (isDarkTheme) "#333333" else "#dcd6c6"
                
                val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes">
                        <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
                        <style>
                            body { font-family: sans-serif; padding: 16px; color: $text; background-color: $bg; line-height: 1.6; }
                            img { max-width: 100%; border-radius: 8px; }
                            pre { background: $surface; padding: 12px; overflow-x: auto; border-radius: 8px; border: 1px solid $line; }
                            code { font-family: monospace; background: $surface; padding: 2px 4px; border-radius: 4px; }
                            a { color: $accent; text-decoration: none; }
                            blockquote { border-left: 4px solid $accent; margin: 0; padding-left: 16px; color: $text; opacity: 0.8; }
                            table { border-collapse: collapse; width: 100%; margin: 16px 0; font-size: 0.9em; }
                            th, td { border: 1px solid $line; padding: 10px 12px; text-align: left; }
                            th { background-color: $surface; font-weight: bold; }
                        </style>
                    </head>
                    <body>
                        <div id="content"><p>Loading markdown...</p></div>
                        <script>
                            fetch("$directFileUrl")
                                .then(res => res.text())
                                .then(text => {
                                    document.getElementById('content').innerHTML = marked.parse(text);
                                })
                                .catch(err => {
                                    document.getElementById('content').innerHTML = '<p style="color:red">Failed to load markdown.</p>';
                                });
                        </script>
                    </body>
                    </html>
                """.trimIndent()
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            } else {
                if (webView.url != viewerUrl) {
                    webView.loadUrl(viewerUrl)
                }
            }
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

enum class SortOrder {
    A_Z, Z_A
}

fun computeDisplayItems(
    allAssignments: List<AssignmentFile>,
    currentPath: List<String>,
    searchQuery: String,
    sortOrder: SortOrder
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
        val results = scopedAssignments
            .filter { it.title.lowercase().contains(query) || it.path.lowercase().contains(query) }
            .map { DisplayItem(isFolder = false, name = it.title, description = it.path, file = it) }
        
        return when (sortOrder) {
            SortOrder.A_Z -> results.sortedBy { it.name.lowercase() }
            SortOrder.Z_A -> results.sortedByDescending { it.name.lowercase() }
        }
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

    val displayFolders = folders.map { folderName ->
        DisplayItem(isFolder = true, name = folderName, description = "Folder")
    }
    
    val displayFiles = files.map { file ->
        DisplayItem(isFolder = false, name = file.title, description = file.description, file = file)
    }

    val combined = displayFolders + displayFiles
    return when (sortOrder) {
        SortOrder.A_Z -> combined.sortedBy { it.name.lowercase() }
        SortOrder.Z_A -> combined.sortedByDescending { it.name.lowercase() }
    }
}

fun downloadFile(context: Context, path: String, title: String) {
    val encodedPath = path.split("/").joinToString("/") { android.net.Uri.encode(it) }
    val directFileUrl = "https://whit31ister.github.io/JU_ASSIGN/$encodedPath"
    
    val request = DownloadManager.Request(android.net.Uri.parse(directFileUrl))
        .setTitle(title)
        .setDescription("Downloading assignment...")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, title)
        
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    downloadManager.enqueue(request)
    
    Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
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
