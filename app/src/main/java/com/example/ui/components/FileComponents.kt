package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClipboardData
import com.example.model.ClipboardMode
import com.example.model.FileItem
import com.example.model.FileType
import com.example.model.StorageInfo
import com.example.model.ViewMode
import com.example.ui.theme.ApkGreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.FolderYellow
import com.example.ui.theme.MediaRed
import com.example.ui.theme.ZipArchivePurple
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemRow(
    item: FileItem,
    selectionMode: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    onSelectionToggle: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (item.isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onItemLongClick
            )
            .testTag("file_item_${item.name}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = item.isSelected,
                    onCheckedChange = { onSelectionToggle() },
                    colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary),
                    modifier = Modifier.testTag("checkbox_${item.name}")
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            FileIconBadge(fileType = item.fileType)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.formattedDate,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = if (item.isDirectory) "${item.itemCount ?: 0} items" else item.formattedSize,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (item.isDirectory) FolderYellow else EmeraldPrimary
                    )

                    Text(
                        text = item.permissionsStr,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(32.dp).testTag("more_btn_${item.name}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridCard(
    item: FileItem,
    selectionMode: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    onSelectionToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (item.isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .padding(4.dp)
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onItemLongClick
            )
            .testTag("file_grid_${item.name}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                if (selectionMode) {
                    Checkbox(
                        checked = item.isSelected,
                        onCheckedChange = { onSelectionToggle() },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            FileIconBadge(fileType = item.fileType, isGrid = true)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.name,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (item.isDirectory) "${item.itemCount ?: 0} items" else item.formattedSize,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = if (item.isDirectory) FolderYellow else EmeraldPrimary
            )
        }
    }
}

@Composable
fun FileIconBadge(fileType: FileType, isGrid: Boolean = false) {
    val (icon, color) = when (fileType) {
        FileType.FOLDER -> Icons.Default.Folder to FolderYellow
        FileType.ARCHIVE_ZIP, FileType.ARCHIVE_7Z, FileType.ARCHIVE_RAR, FileType.ARCHIVE_TAR -> Icons.Default.FolderZip to ZipArchivePurple
        FileType.APK -> Icons.Default.Android to ApkGreen
        FileType.IMAGE -> Icons.Default.Image to CyberCyan
        FileType.AUDIO -> Icons.Default.MusicNote to MediaRed
        FileType.VIDEO -> Icons.Default.Movie to MediaRed
        FileType.CODE_TEXT -> Icons.Default.Description to EmeraldPrimary
        FileType.DOCUMENT -> Icons.Default.InsertDriveFile to CyberCyan
        FileType.UNKNOWN -> Icons.Default.InsertDriveFile to Color.Gray
    }

    val iconSize = if (isGrid) 38.dp else 24.dp
    val badgeSize = if (isGrid) 56.dp else 40.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(badgeSize)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.15f))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun StorageBarHeader(
    currentStorage: StorageInfo?,
    storageList: List<StorageInfo>,
    onSelectStorage: (File) -> Unit,
    onShowStorageInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Storage",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentStorage?.name ?: "Storage",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onShowStorageInfo,
                    modifier = Modifier.size(28.dp).testTag("storage_info_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Storage Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (currentStorage != null && currentStorage.totalBytes > 0) {
                LinearProgressIndicator(
                    progress = { currentStorage.usagePercentage / 100f },
                    color = if (currentStorage.usagePercentage > 90) MediaRed else EmeraldPrimary,
                    trackColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Used: ${com.example.utils.FileOperationUtils.getFormattedSize(currentStorage.usedBytes)} (${currentStorage.usagePercentage}%)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Free: ${com.example.utils.FileOperationUtils.getFormattedSize(currentStorage.freeBytes)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = EmeraldPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick storage location chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                items(storageList) { storage ->
                    val isSelected = currentStorage?.path?.absolutePath == storage.path.absolutePath
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectStorage(storage.path) },
                        label = {
                            Text(
                                text = storage.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.height(30.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BreadcrumbBar(
    currentDir: File,
    onNavigateToDir: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val pathSegments = mutableListOf<File>()
    var curr: File? = currentDir
    while (curr != null) {
        pathSegments.add(0, curr)
        curr = curr.parentFile
    }

    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        items(pathSegments) { dir ->
            val name = if (dir.path == "/") "Root" else if (dir.path == android.os.Environment.getExternalStorageDirectory().path) "Internal" else dir.name
            TextButton(
                onClick = { onNavigateToDir(dir) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (dir == currentDir) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    ),
                    color = if (dir == currentDir) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (dir != currentDir) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun SelectionActionBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onCompress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClearSelection) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close selection")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$selectedCount selected",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSelectAll, modifier = Modifier.testTag("btn_select_all")) {
                    Icon(imageVector = Icons.Default.SelectAll, contentDescription = "Select all")
                }
                IconButton(onClick = onCopy, modifier = Modifier.testTag("btn_copy")) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy")
                }
                IconButton(onClick = onCut, modifier = Modifier.testTag("btn_cut")) {
                    Icon(imageVector = Icons.Default.ContentCut, contentDescription = "Cut")
                }
                IconButton(onClick = onCompress, modifier = Modifier.testTag("btn_compress")) {
                    Icon(imageVector = Icons.Default.FolderZip, contentDescription = "Compress")
                }
                IconButton(onClick = onDelete, modifier = Modifier.testTag("btn_delete")) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MediaRed)
                }
            }
        }
    }
}

@Composable
fun ClipboardBar(
    clipboardData: ClipboardData,
    onPaste: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = EmeraldPrimary,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (clipboardData.mode == ClipboardMode.COPY) Icons.Default.ContentCopy else Icons.Default.ContentCut,
                    contentDescription = null,
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${clipboardData.files.size} items ready to ${if (clipboardData.mode == ClipboardMode.COPY) "copy" else "move"}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = Color.Black.copy(alpha = 0.8f))
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onPaste,
                    modifier = Modifier
                        .background(Color.Black, CircleShape)
                        .testTag("btn_paste")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste",
                        tint = EmeraldPrimary
                    )
                }
            }
        }
    }
}
