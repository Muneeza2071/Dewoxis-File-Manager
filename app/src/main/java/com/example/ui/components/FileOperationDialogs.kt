package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MediaRed
import java.io.File

@Composable
fun CreateItemDialog(
    onCreateFolder: (String) -> Unit,
    onCreateFile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var isFolder by remember { mutableStateOf(true) }
    var nameState by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isFolder) "Create New Folder" else "Create New File",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = isFolder,
                        onClick = { isFolder = true },
                        colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                    )
                    Text("Folder")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = !isFolder,
                        onClick = { isFolder = false },
                        colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                    )
                    Text("File")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = nameState,
                    onValueChange = { nameState = it },
                    label = { Text(if (isFolder) "Folder Name" else "File Name (e.g. note.txt)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        focusedLabelColor = EmeraldPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_create_name")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameState.isNotBlank()) {
                        if (isFolder) onCreateFolder(nameState) else onCreateFile(nameState)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("btn_confirm_create")
            ) {
                Text("Create", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RenameDialog(
    currentFile: File,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameState by remember(currentFile) { mutableStateOf(currentFile.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = nameState,
                onValueChange = { nameState = it },
                label = { Text("New Name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_rename")
            )
        },
        confirmButton = {
            Button(
                onClick = { if (nameState.isNotBlank()) onRename(nameState) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("btn_confirm_rename")
            ) {
                Text("Rename", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    selectedCount: Int,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MediaRed) },
        title = { Text("Delete Confirmation", fontWeight = FontWeight.Bold) },
        text = {
            Text("Are you sure you want to permanently delete $selectedCount selected item(s)? This action cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = MediaRed),
                modifier = Modifier.testTag("btn_confirm_delete")
            ) {
                Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CompressDialog(
    selectedCount: Int,
    onCompress: (archiveName: String, password: String?, compressionLevel: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var archiveName by remember { mutableStateOf("Archive_${System.currentTimeMillis() / 1000}") }
    var enablePassword by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var compressionLevel by remember { mutableStateOf(3) } // 1: STORE, 2: FAST, 3: NORMAL, 4: MAX

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.FolderZip, contentDescription = null, tint = EmeraldPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Archive ($selectedCount items)", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = archiveName,
                    onValueChange = { archiveName = it },
                    label = { Text("Archive Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_compress_name")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Password Protection")
                    }
                    Switch(
                        checked = enablePassword,
                        onCheckedChange = { enablePassword = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                    )
                }

                if (enablePassword) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Enter Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_compress_password")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Compression Level:", style = MaterialTheme.typography.bodySmall)
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Store", "Fast", "Normal", "Max").forEachIndexed { idx, label ->
                        val lvl = idx + 1
                        TextButton(onClick = { compressionLevel = lvl }) {
                            Text(
                                text = label,
                                color = if (compressionLevel == lvl) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (compressionLevel == lvl) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (archiveName.isNotBlank()) {
                        onCompress(archiveName, if (enablePassword) password else null, compressionLevel)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("btn_start_compress")
            ) {
                Text("Compress", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ExtractDialog(
    archiveFile: File,
    onExtract: (password: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = EmeraldPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Extract ${archiveFile.name}", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    text = "Extracting to: ${archiveFile.nameWithoutExtension}/",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (Optional)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_extract_password")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onExtract(password.ifEmpty { null }) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("btn_confirm_extract")
            ) {
                Text("Extract Now", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ProgressOverlay(
    message: String,
    progress: Float
) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                CircularProgressIndicator(color = EmeraldPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (progress > 0f) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        color = EmeraldPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldPrimary
                    )
                }
            }
        }
    }
}
