package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.FileItem
import com.example.model.SortOption
import com.example.model.ViewMode
import com.example.ui.components.ApkDetailsDialog
import com.example.ui.components.ArchiveViewerDialog
import com.example.ui.components.BreadcrumbBar
import com.example.ui.components.ClipboardBar
import com.example.ui.components.CompressDialog
import com.example.ui.components.CreateItemDialog
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.ExtractDialog
import com.example.ui.components.FileGridCard
import com.example.ui.components.FileItemRow
import com.example.ui.components.FilePropertiesDialog
import com.example.ui.components.ImageViewerDialog
import com.example.ui.components.ProgressOverlay
import com.example.ui.components.RenameDialog
import com.example.ui.components.SelectionActionBar
import com.example.ui.components.StorageBarHeader
import com.example.ui.components.TextCodeViewerDialog
import com.example.ui.theme.DewoxisTheme
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MediaRed
import com.example.ui.theme.TextPrimary
import com.example.viewmodel.DialogType
import com.example.viewmodel.FileViewModel
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: FileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DewoxisTheme(darkTheme = true) {
                val vm: FileViewModel = viewModel()
                viewModel = vm

                DewoxisApp(viewModel = vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.loadStorageInfo(this)
            viewModel.loadDirectory()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DewoxisApp(viewModel: FileViewModel) {
    val context = LocalContext.current
    val currentDir by viewModel.currentDirectory.collectAsState()
    val fileList by viewModel.fileList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showHidden by viewModel.showHiddenFiles.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val selectedPaths by viewModel.selectedPaths.collectAsState()
    val clipboard by viewModel.clipboard.collectAsState()
    val isOperationInProgress by viewModel.isOperationInProgress.collectAsState()
    val operationMessage by viewModel.operationMessage.collectAsState()
    val operationProgress by viewModel.operationProgress.collectAsState()
    val activeDialog by viewModel.activeDialog.collectAsState()
    val activeDialogFile by viewModel.activeDialogFile.collectAsState()
    val storageList by viewModel.storageList.collectAsState()
    val apkDetails by viewModel.apkDetails.collectAsState()
    val textContent by viewModel.textViewerContent.collectAsState()
    val archiveEntries by viewModel.archiveEntries.collectAsState()
    val checksum by viewModel.fileChecksum.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val hasStoragePermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    LaunchedEffect(key1 = context) {
        viewModel.loadStorageInfo(context)
        viewModel.loadDirectory()
    }

    LaunchedEffect(operationMessage) {
        operationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearOperationMessage()
        }
    }

    BackHandler(enabled = activeDialog != null || selectionMode || searchQuery.isNotEmpty() || currentDir.parentFile != null) {
        when {
            activeDialog != null -> viewModel.closeDialog()
            selectionMode -> viewModel.clearSelection()
            searchQuery.isNotEmpty() -> viewModel.setSearchQuery("")
            else -> viewModel.navigateUp()
        }
    }


    val currentStorageInfo = remember(storageList, currentDir) {
        storageList.firstOrNull { currentDir.path.startsWith(it.path.path) } ?: storageList.firstOrNull()
    }

    Scaffold(
        topBar = {
            if (selectionMode) {
                SelectionActionBar(
                    selectedCount = selectedPaths.size,
                    onSelectAll = { viewModel.selectAll() },
                    onClearSelection = { viewModel.clearSelection() },
                    onCopy = { viewModel.copySelected() },
                    onCut = { viewModel.cutSelected() },
                    onDelete = { viewModel.showDialog(DialogType.DELETE_CONFIRM) },
                    onCompress = { viewModel.showDialog(DialogType.COMPRESS) }
                )
            } else {
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Search files...", fontSize = 14.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("search_text_input")
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Dewoxis",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Manager",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Light,
                                        color = TextPrimary
                                    )
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) viewModel.setSearchQuery("")
                        }, modifier = Modifier.testTag("btn_toggle_search")) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Clear else Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }

                        IconButton(onClick = { viewModel.toggleShowHidden() }, modifier = Modifier.testTag("btn_toggle_hidden")) {
                            Icon(
                                imageVector = if (showHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Hidden Files",
                                tint = if (showHidden) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { showSortMenu = true }, modifier = Modifier.testTag("btn_sort_menu")) {
                            Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort")
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sort by Name") },
                                onClick = {
                                    viewModel.setSortOption(SortOption.NAME)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Size") },
                                onClick = {
                                    viewModel.setSortOption(SortOption.SIZE)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Date") },
                                onClick = {
                                    viewModel.setSortOption(SortOption.DATE)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Type") },
                                onClick = {
                                    viewModel.setSortOption(SortOption.TYPE)
                                    showSortMenu = false
                                }
                            )
                        }

                        IconButton(onClick = { viewModel.toggleViewMode() }, modifier = Modifier.testTag("btn_toggle_view_mode")) {
                            Icon(
                                imageVector = if (viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                                contentDescription = "Toggle View"
                            )
                        }

                        IconButton(onClick = { showOverflowMenu = true }, modifier = Modifier.testTag("btn_overflow_menu")) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Refresh") },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                onClick = {
                                    viewModel.loadStorageInfo(context)
                                    viewModel.loadDirectory()
                                    showOverflowMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("New Folder") },
                                leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                                onClick = {
                                    viewModel.showDialog(DialogType.CREATE_ITEM)
                                    showOverflowMenu = false
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            clipboard?.let { clip ->
                ClipboardBar(
                    clipboardData = clip,
                    onPaste = { viewModel.pasteClipboard() },
                    onCancel = { viewModel.cancelClipboard() }
                )
            }
        },
        floatingActionButton = {
            if (!selectionMode) {
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedVisibility(visible = showFabMenu) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            FloatingActionButton(
                                onClick = {
                                    showFabMenu = false
                                    viewModel.showDialog(DialogType.CREATE_ITEM)
                                },
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = EmeraldPrimary,
                                modifier = Modifier.size(44.dp).testTag("fab_new_item")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "New Item")
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = { showFabMenu = !showFabMenu },
                        containerColor = EmeraldPrimary,
                        contentColor = Color.Black,
                        shape = CircleShape,
                        modifier = Modifier.testTag("fab_main")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Actions")
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!hasStoragePermission) {
                PermissionRequestBanner(context = context)
            }

            StorageBarHeader(
                currentStorage = currentStorageInfo,
                storageList = storageList,
                onSelectStorage = { viewModel.navigateTo(it) },
                onShowStorageInfo = { viewModel.loadStorageInfo(context) }
            )

            BreadcrumbBar(
                currentDir = currentDir,
                onNavigateToDir = { viewModel.navigateTo(it) }
            )

            if (fileList.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Directory is empty",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                if (viewMode == ViewMode.LIST) {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 80.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(fileList, key = { it.path }) { item ->
                            FileItemRow(
                                item = item,
                                selectionMode = selectionMode,
                                onItemClick = {
                                    if (selectionMode) {
                                        viewModel.toggleSelectFile(item.file)
                                    } else {
                                        viewModel.openItem(context, item)
                                    }
                                },
                                onItemLongClick = {
                                    viewModel.toggleSelectFile(item.file)
                                },
                                onSelectionToggle = {
                                    viewModel.toggleSelectFile(item.file)
                                },
                                onMoreClick = {
                                    viewModel.openProperties(item.file)
                                }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(fileList, key = { it.path }) { item ->
                            FileGridCard(
                                item = item,
                                selectionMode = selectionMode,
                                onItemClick = {
                                    if (selectionMode) {
                                        viewModel.toggleSelectFile(item.file)
                                    } else {
                                        viewModel.openItem(context, item)
                                    }
                                },
                                onItemLongClick = {
                                    viewModel.toggleSelectFile(item.file)
                                },
                                onSelectionToggle = {
                                    viewModel.toggleSelectFile(item.file)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Active Dialogs Handling
    when (activeDialog) {
        DialogType.CREATE_ITEM -> {
            CreateItemDialog(
                onCreateFolder = { viewModel.createFolder(it) },
                onCreateFile = { viewModel.createFile(it) },
                onDismiss = { viewModel.closeDialog() }
            )
        }
        DialogType.RENAME_ITEM -> {
            activeDialogFile?.let { file ->
                RenameDialog(
                    currentFile = file,
                    onRename = { viewModel.renameFile(file, it) },
                    onDismiss = { viewModel.closeDialog() }
                )
            }
        }
        DialogType.DELETE_CONFIRM -> {
            DeleteConfirmDialog(
                selectedCount = selectedPaths.size,
                onConfirmDelete = { viewModel.deleteSelected() },
                onDismiss = { viewModel.closeDialog() }
            )
        }
        DialogType.COMPRESS -> {
            CompressDialog(
                selectedCount = selectedPaths.size,
                onCompress = { name, pass, level ->
                    viewModel.compressSelected(name, pass, level)
                },
                onDismiss = { viewModel.closeDialog() }
            )
        }
        DialogType.EXTRACT -> {
            activeDialogFile?.let { file ->
                ExtractDialog(
                    archiveFile = file,
                    onExtract = { pass -> viewModel.extractArchive(file, pass) },
                    onDismiss = { viewModel.closeDialog() }
                )
            }
        }
        DialogType.TEXT_VIEWER -> {
            activeDialogFile?.let { file ->
                TextCodeViewerDialog(
                    file = file,
                    content = textContent ?: "",
                    onSave = { viewModel.saveTextContent(file, it) },
                    onClose = { viewModel.closeDialog() }
                )
            }
        }
        DialogType.IMAGE_VIEWER -> {
            activeDialogFile?.let { file ->
                ImageViewerDialog(
                    file = file,
                    onClose = { viewModel.closeDialog() }
                )
            }
        }

        DialogType.APK_DETAILS -> {
            activeDialogFile?.let { file ->
                ApkDetailsDialog(
                    apkFile = file,
                    details = apkDetails,
                    onClose = { viewModel.closeDialog() }
                )
            }
        }
        DialogType.PROPERTIES -> {
            activeDialogFile?.let { file ->
                FilePropertiesDialog(
                    file = file,
                    checksum = checksum,
                    onCalculateChecksum = { algo -> viewModel.calculateChecksum(file, algo) },
                    onClose = { viewModel.closeDialog() }
                )
            }
        }
        DialogType.ARCHIVE_VIEWER -> {
            activeDialogFile?.let { file ->
                ArchiveViewerDialog(
                    archiveFile = file,
                    entries = archiveEntries,
                    onExtract = {
                        viewModel.closeDialog()
                        viewModel.showDialog(DialogType.EXTRACT, file)
                    },
                    onClose = { viewModel.closeDialog() }
                )
            }
        }
        else -> {}
    }

    if (isOperationInProgress) {
        ProgressOverlay(
            message = operationMessage ?: "Processing...",
            progress = operationProgress
        )
    }
}

@Composable
fun PermissionRequestBanner(context: android.content.Context) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MediaRed.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = MediaRed,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Storage Access Required",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Grant 'All Files Access' permission for full file operations.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                data = Uri.parse("package:" + context.packageName)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            context.startActivity(intent)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("btn_grant_storage_permission")
            ) {
                Text("Grant", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
