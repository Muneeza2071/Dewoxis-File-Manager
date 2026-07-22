package com.example.viewmodel

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ApkDetails
import com.example.model.ArchiveEntryItem
import com.example.model.ClipboardData
import com.example.model.ClipboardMode
import com.example.model.FileItem
import com.example.model.FileType
import com.example.model.SortOption
import com.example.model.StorageInfo
import com.example.model.ViewMode
import com.example.utils.FileOperationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class DialogType {
    CREATE_ITEM,
    RENAME_ITEM,
    DELETE_CONFIRM,
    COMPRESS,
    EXTRACT,
    PROPERTIES,
    TEXT_VIEWER,
    IMAGE_VIEWER,
    APK_DETAILS,
    ARCHIVE_VIEWER,
    STORAGE_INFO,
    PASSWORD_PROMPT
}

class FileViewModel : ViewModel() {

    private val defaultStartDir = Environment.getExternalStorageDirectory() ?: File("/storage/emulated/0")

    private val _currentDirectory = MutableStateFlow<File>(defaultStartDir)
    val currentDirectory: StateFlow<File> = _currentDirectory.asStateFlow()

    private val _fileList = MutableStateFlow<List<FileItem>>(emptyList())
    val fileList: StateFlow<List<FileItem>> = _fileList.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showHiddenFiles = MutableStateFlow(false)
    val showHiddenFiles: StateFlow<Boolean> = _showHiddenFiles.asStateFlow()

    private val _sortBy = MutableStateFlow(SortOption.NAME)
    val sortBy: StateFlow<SortOption> = _sortBy.asStateFlow()

    private val _sortAscending = MutableStateFlow(true)
    val sortAscending: StateFlow<Boolean> = _sortAscending.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()

    private val _selectedPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedPaths: StateFlow<Set<String>> = _selectedPaths.asStateFlow()

    private val _clipboard = MutableStateFlow<ClipboardData?>(null)
    val clipboard: StateFlow<ClipboardData?> = _clipboard.asStateFlow()

    private val _isOperationInProgress = MutableStateFlow(false)
    val isOperationInProgress: StateFlow<Boolean> = _isOperationInProgress.asStateFlow()

    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    private val _operationProgress = MutableStateFlow(0f)
    val operationProgress: StateFlow<Float> = _operationProgress.asStateFlow()

    private val _activeDialog = MutableStateFlow<DialogType?>(null)
    val activeDialog: StateFlow<DialogType?> = _activeDialog.asStateFlow()

    private val _activeDialogFile = MutableStateFlow<File?>(null)
    val activeDialogFile: StateFlow<File?> = _activeDialogFile.asStateFlow()

    private val _storageList = MutableStateFlow<List<StorageInfo>>(emptyList())
    val storageList: StateFlow<List<StorageInfo>> = _storageList.asStateFlow()

    private val _apkDetails = MutableStateFlow<ApkDetails?>(null)
    val apkDetails: StateFlow<ApkDetails?> = _apkDetails.asStateFlow()

    private val _textViewerContent = MutableStateFlow<String?>(null)
    val textViewerContent: StateFlow<String?> = _textViewerContent.asStateFlow()

    private val _archiveEntries = MutableStateFlow<List<ArchiveEntryItem>>(emptyList())
    val archiveEntries: StateFlow<List<ArchiveEntryItem>> = _archiveEntries.asStateFlow()

    private val _fileChecksum = MutableStateFlow<String?>(null)
    val fileChecksum: StateFlow<String?> = _fileChecksum.asStateFlow()

    fun loadStorageInfo(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val locations = FileOperationUtils.getStorageLocations(context)
            val infoList = locations.map { FileOperationUtils.getStorageInfo(it) }
            _storageList.value = infoList
        }
    }

    fun loadDirectory(directory: File = _currentDirectory.value) {
        viewModelScope.launch(Dispatchers.IO) {
            _currentDirectory.value = directory
            val items = FileOperationUtils.listFiles(
                directory = directory,
                showHidden = _showHiddenFiles.value,
                sortBy = _sortBy.value,
                sortAscending = _sortAscending.value,
                searchQuery = _searchQuery.value
            )
            _fileList.value = items.map { item ->
                item.copy(isSelected = _selectedPaths.value.contains(item.path))
            }
        }
    }

    fun navigateTo(directory: File) {
        if (directory.exists() && directory.isDirectory && directory.canRead()) {
            clearSelection()
            loadDirectory(directory)
        } else {
            _operationMessage.value = "Cannot access directory: ${directory.name}"
        }
    }

    fun navigateUp(): Boolean {
        val parent = _currentDirectory.value.parentFile
        if (parent != null && parent.exists() && parent.canRead()) {
            navigateTo(parent)
            return true
        }
        return false
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        loadDirectory()
    }

    fun toggleShowHidden() {
        _showHiddenFiles.value = !_showHiddenFiles.value
        loadDirectory()
    }

    fun setSortOption(option: SortOption) {
        if (_sortBy.value == option) {
            _sortAscending.value = !_sortAscending.value
        } else {
            _sortBy.value = option
            _sortAscending.value = true
        }
        loadDirectory()
    }

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
    }

    fun toggleSelectFile(file: File) {
        val currentSet = _selectedPaths.value.toMutableSet()
        if (currentSet.contains(file.absolutePath)) {
            currentSet.remove(file.absolutePath)
        } else {
            currentSet.add(file.absolutePath)
        }
        _selectedPaths.value = currentSet
        _selectionMode.value = currentSet.isNotEmpty()

        _fileList.value = _fileList.value.map {
            it.copy(isSelected = currentSet.contains(it.path))
        }
    }

    fun selectAll() {
        val allPaths = _fileList.value.map { it.path }.toSet()
        _selectedPaths.value = allPaths
        _selectionMode.value = allPaths.isNotEmpty()
        _fileList.value = _fileList.value.map { it.copy(isSelected = true) }
    }

    fun clearSelection() {
        _selectedPaths.value = emptySet()
        _selectionMode.value = false
        _fileList.value = _fileList.value.map { it.copy(isSelected = false) }
    }

    fun copySelected() {
        val files = getSelectedFiles()
        if (files.isNotEmpty()) {
            _clipboard.value = ClipboardData(ClipboardMode.COPY, files)
            _operationMessage.value = "${files.size} items copied to clipboard"
            clearSelection()
        }
    }

    fun cutSelected() {
        val files = getSelectedFiles()
        if (files.isNotEmpty()) {
            _clipboard.value = ClipboardData(ClipboardMode.CUT, files)
            _operationMessage.value = "${files.size} items cut to clipboard"
            clearSelection()
        }
    }

    fun cancelClipboard() {
        _clipboard.value = null
    }

    fun pasteClipboard() {
        val clip = _clipboard.value ?: return
        val targetDir = _currentDirectory.value

        viewModelScope.launch(Dispatchers.IO) {
            _isOperationInProgress.value = true
            _operationMessage.value = "Processing files..."
            _operationProgress.value = 0f

            val total = clip.files.size
            var successCount = 0

            clip.files.forEachIndexed { index, file ->
                val ok = if (clip.mode == ClipboardMode.COPY) {
                    FileOperationUtils.copyFileOrDirectory(file, targetDir)
                } else {
                    FileOperationUtils.moveFileOrDirectory(file, targetDir)
                }
                if (ok) successCount++
                _operationProgress.value = (index + 1).toFloat() / total
            }

            if (clip.mode == ClipboardMode.CUT) {
                _clipboard.value = null
            }

            _isOperationInProgress.value = false
            _operationMessage.value = "$successCount / $total items pasted"
            loadDirectory()
        }
    }

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val success = FileOperationUtils.createDirectory(_currentDirectory.value, name.trim())
            if (success) {
                _operationMessage.value = "Folder created: $name"
                loadDirectory()
            } else {
                _operationMessage.value = "Failed to create folder"
            }
            closeDialog()
        }
    }

    fun createFile(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val success = FileOperationUtils.createNewFile(_currentDirectory.value, name.trim())
            if (success) {
                _operationMessage.value = "File created: $name"
                loadDirectory()
            } else {
                _operationMessage.value = "Failed to create file"
            }
            closeDialog()
        }
    }

    fun deleteSelected() {
        val files = getSelectedFiles()
        if (files.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _isOperationInProgress.value = true
            _operationMessage.value = "Deleting items..."

            var deleted = 0
            files.forEach { file ->
                if (FileOperationUtils.deleteFileOrDirectory(file)) {
                    deleted++
                }
            }

            _isOperationInProgress.value = false
            _operationMessage.value = "Deleted $deleted items"
            clearSelection()
            closeDialog()
            loadDirectory()
        }
    }

    fun renameFile(file: File, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val success = FileOperationUtils.renameFile(file, newName.trim())
            if (success) {
                _operationMessage.value = "Renamed to $newName"
                loadDirectory()
            } else {
                _operationMessage.value = "Failed to rename"
            }
            closeDialog()
        }
    }

    fun compressSelected(archiveName: String, password: String?, compressionLevel: Int) {
        val files = getSelectedFiles()
        if (files.isEmpty()) return

        val finalName = if (!archiveName.endsWith(".zip")) "$archiveName.zip" else archiveName
        val destZipFile = File(_currentDirectory.value, finalName)

        viewModelScope.launch(Dispatchers.IO) {
            _isOperationInProgress.value = true
            _operationMessage.value = "Creating archive..."
            _operationProgress.value = 0f

            val ok = FileOperationUtils.createZipArchive(
                filesToCompress = files,
                destZipFile = destZipFile,
                password = password,
                compressionLevelInt = compressionLevel,
                onProgress = { progress -> _operationProgress.value = progress }
            )

            _isOperationInProgress.value = false
            if (ok) {
                _operationMessage.value = "Archive created: $finalName"
                clearSelection()
            } else {
                _operationMessage.value = "Failed to create archive"
            }
            closeDialog()
            loadDirectory()
        }
    }

    fun extractArchive(archiveFile: File, password: String? = null) {
        val outputDir = File(archiveFile.parentFile, archiveFile.nameWithoutExtension)

        viewModelScope.launch(Dispatchers.IO) {
            _isOperationInProgress.value = true
            _operationMessage.value = "Extracting archive..."
            _operationProgress.value = 0f

            val ok = FileOperationUtils.extractArchive(
                archiveFile = archiveFile,
                destDir = outputDir,
                password = password,
                onProgress = { progress -> _operationProgress.value = progress }
            )

            _isOperationInProgress.value = false
            if (ok) {
                _operationMessage.value = "Extracted to ${outputDir.name}"
            } else {
                _operationMessage.value = "Extraction failed or invalid password"
            }
            closeDialog()
            loadDirectory()
        }
    }

    fun openItem(context: Context, item: FileItem) {
        val file = item.file
        if (item.isDirectory) {
            navigateTo(file)
        } else {
            when (item.fileType) {
                FileType.APK -> openApkViewer(context, file)
                FileType.IMAGE -> openImageViewer(file)
                FileType.CODE_TEXT, FileType.DOCUMENT -> openTextViewer(file)
                FileType.ARCHIVE_ZIP, FileType.ARCHIVE_7Z, FileType.ARCHIVE_RAR, FileType.ARCHIVE_TAR -> {
                    openArchiveViewer(file)
                }
                else -> {
                    // Default to text viewer or properties
                    openTextViewer(file)
                }
            }
        }
    }

    fun openApkViewer(context: Context, file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val details = FileOperationUtils.getApkDetails(context, file)
            _apkDetails.value = details
            _activeDialogFile.value = file
            _activeDialog.value = DialogType.APK_DETAILS
        }
    }

    fun openImageViewer(file: File) {
        _activeDialogFile.value = file
        _activeDialog.value = DialogType.IMAGE_VIEWER
    }

    fun openTextViewer(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val content = FileOperationUtils.readTextContent(file)
            _textViewerContent.value = content
            _activeDialogFile.value = file
            _activeDialog.value = DialogType.TEXT_VIEWER
        }
    }

    fun saveTextContent(file: File, newContent: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = FileOperationUtils.writeTextContent(file, newContent)
            if (ok) {
                _operationMessage.value = "File saved successfully"
                _textViewerContent.value = newContent
            } else {
                _operationMessage.value = "Failed to save file"
            }
        }
    }

    fun openArchiveViewer(file: File, password: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val entries = FileOperationUtils.listArchiveEntries(file, password)
            _archiveEntries.value = entries
            _activeDialogFile.value = file
            _activeDialog.value = DialogType.ARCHIVE_VIEWER
        }
    }

    fun openProperties(file: File) {
        _activeDialogFile.value = file
        _fileChecksum.value = null
        _activeDialog.value = DialogType.PROPERTIES
    }

    fun calculateChecksum(file: File, algorithm: String = "MD5") {
        viewModelScope.launch(Dispatchers.IO) {
            _fileChecksum.value = "Calculating $algorithm..."
            val checksum = FileOperationUtils.calculateChecksum(file, algorithm)
            _fileChecksum.value = "$algorithm: $checksum"
        }
    }

    fun showDialog(type: DialogType, file: File? = null) {
        _activeDialogFile.value = file
        _activeDialog.value = type
    }

    fun closeDialog() {
        _activeDialog.value = null
        _activeDialogFile.value = null
        _apkDetails.value = null
        _textViewerContent.value = null
        _archiveEntries.value = emptyList()
        _fileChecksum.value = null
    }

    fun clearOperationMessage() {
        _operationMessage.value = null
    }

    private fun getSelectedFiles(): List<File> {
        return _selectedPaths.value.map { File(it) }.filter { it.exists() }
    }
}
