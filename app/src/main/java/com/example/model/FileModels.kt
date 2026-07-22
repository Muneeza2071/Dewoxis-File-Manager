package com.example.model

import android.graphics.drawable.Drawable
import java.io.File

enum class FileType {
    FOLDER,
    ARCHIVE_ZIP,
    ARCHIVE_7Z,
    ARCHIVE_RAR,
    ARCHIVE_TAR,
    APK,
    IMAGE,
    AUDIO,
    VIDEO,
    CODE_TEXT,
    DOCUMENT,
    UNKNOWN
}

data class FileItem(
    val file: File,
    val name: String,
    val path: String,
    val size: Long,
    val formattedSize: String,
    val lastModified: Long,
    val formattedDate: String,
    val isDirectory: Boolean,
    val extension: String,
    val mimeType: String,
    val fileType: FileType,
    val itemCount: Int? = null,
    val permissionsStr: String = "rw-",
    val isSelected: Boolean = false
)

enum class SortOption {
    NAME,
    SIZE,
    DATE,
    TYPE
}

enum class ViewMode {
    LIST,
    GRID
}

enum class ClipboardMode {
    COPY,
    CUT
}

data class ClipboardData(
    val mode: ClipboardMode,
    val files: List<File>
)

data class StorageInfo(
    val name: String,
    val path: File,
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val usagePercentage: Int
)

data class ApkDetails(
    val label: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdkVersion: Int,
    val targetSdkVersion: Int,
    val icon: Drawable? = null,
    val permissions: List<String> = emptyList(),
    val sizeFormatted: String
)

data class ArchiveEntryItem(
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val formattedSize: String,
    val compressedSize: Long
)
