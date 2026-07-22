package com.example.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StatFs
import android.webkit.MimeTypeMap
import com.example.model.ApkDetails
import com.example.model.ArchiveEntryItem
import com.example.model.FileItem
import com.example.model.FileType
import com.example.model.SortOption
import com.example.model.StorageInfo
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileOperationUtils {

    fun getStorageLocations(context: Context): List<File> {
        val storageList = mutableListOf<File>()

        // Internal Storage
        val primaryStorage = Environment.getExternalStorageDirectory()
        if (primaryStorage != null && primaryStorage.exists()) {
            storageList.add(primaryStorage)
        }

        // Secondary / SD Card storage
        val externalFilesDirs = context.getExternalFilesDirs(null)
        for (dir in externalFilesDirs) {
            if (dir != null) {
                val root = getRootStorageFromAppDir(dir)
                if (root != null && root.exists() && root != primaryStorage && !storageList.contains(root)) {
                    storageList.add(root)
                }
            }
        }

        // Root storage directory
        val rootDir = File("/")
        if (rootDir.exists() && !storageList.contains(rootDir)) {
            storageList.add(rootDir)
        }

        return storageList
    }

    private fun getRootStorageFromAppDir(appDir: File): File? {
        // e.g. /storage/1234-5678/Android/data/com.dewoxis.filemanager/files -> /storage/1234-5678
        var current: File? = appDir
        while (current != null && current.parentFile != null) {
            if (current.parentFile?.name == "storage" || current.parentFile?.path == "/storage") {
                return current
            }
            current = current.parentFile
        }
        return null
    }

    fun getStorageInfo(storageDir: File): StorageInfo {
        return try {
            val stat = StatFs(storageDir.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = totalBytes - freeBytes
            val percentage = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt() else 0

            val name = when {
                storageDir.path == Environment.getExternalStorageDirectory().path -> "Internal Storage"
                storageDir.path == "/" -> "Root System"
                else -> "SD Card (${storageDir.name})"
            }

            StorageInfo(
                name = name,
                path = storageDir,
                totalBytes = totalBytes,
                freeBytes = freeBytes,
                usedBytes = usedBytes,
                usagePercentage = percentage
            )
        } catch (e: Exception) {
            StorageInfo(
                name = storageDir.name.ifEmpty { "Storage" },
                path = storageDir,
                totalBytes = 0,
                freeBytes = 0,
                usedBytes = 0,
                usagePercentage = 0
            )
        }
    }

    fun listFiles(
        directory: File,
        showHidden: Boolean,
        sortBy: SortOption,
        sortAscending: Boolean,
        searchQuery: String = ""
    ): List<FileItem> {
        if (!directory.exists() || !directory.isDirectory) return emptyList()

        val rawFiles = directory.listFiles() ?: return emptyList()

        val items = rawFiles
            .filter { file ->
                if (!showHidden && file.name.startsWith(".")) return@filter false
                if (searchQuery.isNotEmpty()) {
                    file.name.contains(searchQuery, ignoreCase = true)
                } else {
                    true
                }
            }
            .map { file -> createFileItem(file) }

        val comparator: Comparator<FileItem> = when (sortBy) {
            SortOption.NAME -> compareBy { it.name.lowercase(Locale.getDefault()) }
            SortOption.SIZE -> compareBy { it.size }
            SortOption.DATE -> compareBy { it.lastModified }
            SortOption.TYPE -> compareBy { it.fileType.name }
        }

        val sorted = if (sortAscending) items.sortedWith(comparator) else items.sortedWith(comparator.reversed())

        // Folders always pinned to top like ZArchiver
        return sorted.sortedByDescending { it.isDirectory }
    }

    fun createFileItem(file: File): FileItem {
        val isDir = file.isDirectory
        val size = if (isDir) getDirectorySize(file) else file.length()
        val extension = file.extension.lowercase(Locale.getDefault())
        val fileType = determineFileType(file, isDir, extension)
        val itemCount = if (isDir) file.listFiles()?.size else null

        return FileItem(
            file = file,
            name = file.name,
            path = file.absolutePath,
            size = size,
            formattedSize = getFormattedSize(size),
            lastModified = file.lastModified(),
            formattedDate = getFormattedDate(file.lastModified()),
            isDirectory = isDir,
            extension = extension,
            mimeType = getMimeType(file, extension),
            fileType = fileType,
            itemCount = itemCount,
            permissionsStr = getPermissionsStr(file)
        )
    }

    private fun determineFileType(file: File, isDir: Boolean, extension: String): FileType {
        if (isDir) return FileType.FOLDER
        return when (extension) {
            "zip" -> FileType.ARCHIVE_ZIP
            "7z" -> FileType.ARCHIVE_7Z
            "rar" -> FileType.ARCHIVE_RAR
            "tar", "gz", "bz2", "xz" -> FileType.ARCHIVE_TAR
            "apk" -> FileType.APK
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg" -> FileType.IMAGE
            "mp3", "wav", "aac", "m4a", "flac", "ogg" -> FileType.AUDIO
            "mp4", "mkv", "avi", "mov", "webm", "3gp" -> FileType.VIDEO
            "txt", "json", "xml", "kt", "java", "py", "html", "css", "js", "sh", "log", "properties", "conf", "md", "c", "cpp", "h" -> FileType.CODE_TEXT
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx" -> FileType.DOCUMENT
            else -> FileType.UNKNOWN
        }
    }

    fun getFormattedSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val formatted = String.format(Locale.US, "%.1f", bytes / Math.pow(1024.0, digitGroups.toDouble()))
        return "$formatted ${units[digitGroups.coerceAtMost(units.size - 1)]}"
    }

    fun getFormattedDate(timestamp: Long): String {
        if (timestamp <= 0) return "Unknown"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getPermissionsStr(file: File): String {
        val r = if (file.canRead()) "r" else "-"
        val w = if (file.canWrite()) "w" else "-"
        val x = if (file.canExecute()) "x" else "-"
        return "$r$w$x"
    }

    private fun getMimeType(file: File, extension: String): String {
        val type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        return type ?: "application/octet-stream"
    }

    private fun getDirectorySize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (f in files) {
            if (f.isFile) {
                size += f.length()
            }
        }
        return size
    }

    fun createDirectory(parentDir: File, name: String): Boolean {
        val newDir = File(parentDir, name)
        return if (!newDir.exists()) newDir.mkdirs() else false
    }

    fun createNewFile(parentDir: File, name: String): Boolean {
        val newFile = File(parentDir, name)
        return try {
            if (!newFile.exists()) newFile.createNewFile() else false
        } catch (e: Exception) {
            false
        }
    }

    fun deleteFileOrDirectory(file: File): Boolean {
        if (!file.exists()) return true
        if (file.isDirectory) {
            val children = file.listFiles()
            if (children != null) {
                for (child in children) {
                    deleteFileOrDirectory(child)
                }
            }
        }
        return file.delete()
    }

    fun renameFile(file: File, newName: String): Boolean {
        if (!file.exists()) return false
        val newFile = File(file.parentFile, newName)
        return file.renameTo(newFile)
    }

    fun copyFileOrDirectory(source: File, targetDir: File, onProgress: ((Float) -> Unit)? = null): Boolean {
        return try {
            if (source.isDirectory) {
                val destDir = File(targetDir, source.name)
                if (!destDir.exists()) destDir.mkdirs()
                val children = source.listFiles() ?: return true
                for (child in children) {
                    copyFileOrDirectory(child, destDir, onProgress)
                }
                true
            } else {
                val destFile = File(targetDir, source.name)
                source.inputStream().use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun moveFileOrDirectory(source: File, targetDir: File, onProgress: ((Float) -> Unit)? = null): Boolean {
        val destFile = File(targetDir, source.name)
        if (source.renameTo(destFile)) return true
        val copied = copyFileOrDirectory(source, targetDir, onProgress)
        if (copied) {
            deleteFileOrDirectory(source)
            return true
        }
        return false
    }

    fun createZipArchive(
        filesToCompress: List<File>,
        destZipFile: File,
        password: String? = null,
        compressionLevelInt: Int = 3, // 1: STORE, 2: FAST, 3: NORMAL, 4: MAXIMUM
        onProgress: ((Float) -> Unit)? = null
    ): Boolean {
        return try {
            val zipFile = ZipFile(destZipFile)
            if (!password.isNullOrEmpty()) {
                zipFile.setPassword(password.toCharArray())
            }

            val params = ZipParameters()
            params.compressionMethod = if (compressionLevelInt == 1) CompressionMethod.STORE else CompressionMethod.DEFLATE
            params.compressionLevel = when (compressionLevelInt) {
                1 -> CompressionLevel.NO_COMPRESSION
                2 -> CompressionLevel.FASTEST
                4 -> CompressionLevel.MAXIMUM
                else -> CompressionLevel.NORMAL
            }

            if (!password.isNullOrEmpty()) {
                params.isEncryptFiles = true
                params.encryptionMethod = EncryptionMethod.AES
                params.aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
            }

            val total = filesToCompress.size
            filesToCompress.forEachIndexed { index, file ->
                if (file.isDirectory) {
                    zipFile.addFolder(file, params)
                } else {
                    zipFile.addFile(file, params)
                }
                onProgress?.invoke((index + 1).toFloat() / total)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun extractArchive(
        archiveFile: File,
        destDir: File,
        password: String? = null,
        onProgress: ((Float) -> Unit)? = null
    ): Boolean {
        return try {
            if (!destDir.exists()) destDir.mkdirs()

            val ext = archiveFile.extension.lowercase(Locale.getDefault())
            val lowerName = archiveFile.name.lowercase(Locale.getDefault())
            if (ext == "zip") {
                val zipFile = ZipFile(archiveFile)
                if (zipFile.isEncrypted) {
                    if (!password.isNullOrEmpty()) {
                        zipFile.setPassword(password.toCharArray())
                    } else {
                        return false // Password required
                    }
                }
                zipFile.extractAll(destDir.absolutePath)
                onProgress?.invoke(1.0f)
                return true
            } else if (lowerName.endsWith(".tar.gz") || lowerName.endsWith(".tgz") || ext == "gz") {
                extractTarGzArchive(archiveFile, destDir, onProgress)
                onProgress?.invoke(1.0f)
                return true
            } else if (ext == "tar") {
                extractTarArchive(archiveFile, destDir, onProgress)
                onProgress?.invoke(1.0f)
                return true
            } else {

                // Fallback attempt with Zip4j or error
                if (ZipFile(archiveFile).isValidZipFile) {
                    val zipFile = ZipFile(archiveFile)
                    if (zipFile.isEncrypted && !password.isNullOrEmpty()) {
                        zipFile.setPassword(password.toCharArray())
                    }
                    zipFile.extractAll(destDir.absolutePath)
                    return true
                }
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun extractTarArchive(tarFile: File, destDir: File, onProgress: ((Float) -> Unit)? = null) {
        TarArchiveInputStream(BufferedInputStream(FileInputStream(tarFile))).use { tarIn ->
            var entry = tarIn.nextTarEntry
            while (entry != null) {
                val destPath = File(destDir, entry.name)
                if (entry.isDirectory) {
                    destPath.mkdirs()
                } else {
                    destPath.parentFile?.mkdirs()
                    FileOutputStream(destPath).use { out ->
                        tarIn.copyTo(out)
                    }
                }
                entry = tarIn.nextTarEntry
            }
        }
    }

    private fun extractTarGzArchive(tarGzFile: File, destDir: File, onProgress: ((Float) -> Unit)? = null) {
        TarArchiveInputStream(BufferedInputStream(java.util.zip.GZIPInputStream(FileInputStream(tarGzFile)))).use { tarIn ->
            var entry = tarIn.nextTarEntry
            while (entry != null) {
                val destPath = File(destDir, entry.name)
                if (entry.isDirectory) {
                    destPath.mkdirs()
                } else {
                    destPath.parentFile?.mkdirs()
                    FileOutputStream(destPath).use { out ->
                        tarIn.copyTo(out)
                    }
                }
                entry = tarIn.nextTarEntry
            }
        }
    }

    fun listArchiveEntries(archiveFile: File, password: String? = null): List<ArchiveEntryItem> {
        val ext = archiveFile.extension.lowercase(Locale.getDefault())
        val lowerName = archiveFile.name.lowercase(Locale.getDefault())

        if (ext == "tar") {
            return listTarArchiveEntries(archiveFile, isGz = false)
        } else if (lowerName.endsWith(".tar.gz") || lowerName.endsWith(".tgz") || ext == "gz") {
            return listTarArchiveEntries(archiveFile, isGz = true)
        }

        val list = mutableListOf<ArchiveEntryItem>()
        try {
            val zipFile = ZipFile(archiveFile)
            if (zipFile.isEncrypted && !password.isNullOrEmpty()) {
                zipFile.setPassword(password.toCharArray())
            }
            val headers = zipFile.fileHeaders
            for (header in headers) {
                list.add(
                    ArchiveEntryItem(
                        name = header.fileName,
                        isDirectory = header.isDirectory,
                        size = header.uncompressedSize,
                        formattedSize = getFormattedSize(header.uncompressedSize),
                        compressedSize = header.compressedSize
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun listTarArchiveEntries(archiveFile: File, isGz: Boolean): List<ArchiveEntryItem> {
        val list = mutableListOf<ArchiveEntryItem>()
        try {
            val inputStream = if (isGz) {
                java.util.zip.GZIPInputStream(FileInputStream(archiveFile))
            } else {
                FileInputStream(archiveFile)
            }
            TarArchiveInputStream(BufferedInputStream(inputStream)).use { tarIn ->
                var entry = tarIn.nextTarEntry
                while (entry != null) {
                    list.add(
                        ArchiveEntryItem(
                            name = entry.name,
                            isDirectory = entry.isDirectory,
                            size = entry.size,
                            formattedSize = getFormattedSize(entry.size),
                            compressedSize = entry.size
                        )
                    )
                    entry = tarIn.nextTarEntry
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }


    fun getApkDetails(context: Context, apkFile: File): ApkDetails? {
        return try {
            val pm = context.packageManager
            val info = pm.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.GET_PERMISSIONS
            ) ?: return null

            val appInfo = info.applicationInfo ?: return null
            appInfo.sourceDir = apkFile.absolutePath
            appInfo.publicSourceDir = apkFile.absolutePath

            val label = pm.getApplicationLabel(appInfo).toString()
            val icon = pm.getApplicationIcon(appInfo)
            val reqPerms = info.requestedPermissions?.toList() ?: emptyList()

            val minSdk = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                appInfo.minSdkVersion
            } else 21

            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                info.versionCode.toLong()
            }

            ApkDetails(
                label = if (label.isNotEmpty()) label else apkFile.nameWithoutExtension,
                packageName = info.packageName ?: "Unknown",
                versionName = info.versionName ?: "1.0",
                versionCode = versionCode,
                minSdkVersion = minSdk,
                targetSdkVersion = appInfo.targetSdkVersion,
                icon = icon,
                permissions = reqPerms,
                sizeFormatted = getFormattedSize(apkFile.length())
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun calculateChecksum(file: File, algorithm: String = "MD5"): String {
        return try {
            val digest = MessageDigest.getInstance(algorithm)
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } > 0) {
                    digest.update(buffer, 0, read)
                }
            }
            val mdBytes = digest.digest()
            val sb = StringBuilder()
            for (b in mdBytes) {
                sb.append(String.format("%02x", b))
            }
            sb.toString()
        } catch (e: Exception) {
            "Calculation Error"
        }
    }

    fun readTextContent(file: File, maxBytes: Long = 1024 * 1024): String {
        return try {
            if (file.length() > maxBytes) {
                file.inputStream().use { input ->
                    val buffer = ByteArray(maxBytes.toInt())
                    val read = input.read(buffer)
                    String(buffer, 0, read) + "\n\n... [Truncated: File size exceeds 1MB limit]"
                }
            } else {
                file.readText()
            }
        } catch (e: Exception) {
            "Failed to read file content: ${e.message}"
        }
    }

    fun writeTextContent(file: File, text: String): Boolean {
        return try {
            file.writeText(text)
            true
        } catch (e: Exception) {
            false
        }
    }
}
