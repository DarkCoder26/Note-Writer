package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.model.Document
import com.example.model.ExportFormat
import com.example.viewmodel.SaveStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleBar(
    documentTitle: String,
    documentFormat: ExportFormat,
    saveStatus: SaveStatus,
    canUndo: Boolean,
    canRedo: Boolean,
    onTitleChange: (String) -> Unit,
    onMenuClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSaveAsClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onSearchClick: () -> Unit,
    onPrintClick: () -> Unit,
    onShareClick: () -> Unit,
    onRecentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditingTitle by remember { mutableStateOf(false) }
    var tempTitle by remember { mutableStateOf(documentTitle) }

    Surface(
        color = Color(0xFF0D47A1),
        contentColor = Color.White,
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // Left: App brand & File menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TitleIconButton(
                        icon = Icons.Default.Menu,
                        contentDescription = "File Menu",
                        tooltip = "Open File Menu",
                        testTag = "file_menu_button",
                        onClick = onMenuClick
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // App icon badge
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1565C0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "DocEditor Icon",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Editable Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                tempTitle = documentTitle
                                isEditingTitle = true
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${documentTitle}.${documentFormat.extension}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontSize = 15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Title",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Save Status Badge
                    SaveStatusChip(saveStatus = saveStatus)
                }

                // Right: Quick actions (Save, Undo, Redo, Search, Print, Share)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TitleIconButton(
                        icon = Icons.Default.Save,
                        contentDescription = "Save",
                        tooltip = "Save Document (Ctrl+S)",
                        testTag = "quick_save_button",
                        onClick = onSaveClick
                    )

                    TitleIconButton(
                        icon = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tooltip = "Undo (Ctrl+Z)",
                        enabled = canUndo,
                        testTag = "quick_undo_button",
                        onClick = onUndoClick
                    )

                    TitleIconButton(
                        icon = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tooltip = "Redo (Ctrl+Y)",
                        enabled = canRedo,
                        testTag = "quick_redo_button",
                        onClick = onRedoClick
                    )

                    TitleIconButton(
                        icon = Icons.Default.Search,
                        contentDescription = "Find and Replace",
                        tooltip = "Find & Replace (Ctrl+F)",
                        testTag = "quick_search_button",
                        onClick = onSearchClick
                    )

                    TitleIconButton(
                        icon = Icons.Default.Print,
                        contentDescription = "Print",
                        tooltip = "Print Document (Ctrl+P)",
                        testTag = "quick_print_button",
                        onClick = onPrintClick
                    )

                    TitleIconButton(
                        icon = Icons.Default.Share,
                        contentDescription = "Save As / Export",
                        tooltip = "Export / Share File",
                        testTag = "quick_share_button",
                        onClick = onSaveAsClick
                    )

                    TitleIconButton(
                        icon = Icons.Default.FolderOpen,
                        contentDescription = "Recent Documents",
                        tooltip = "Open Recent Documents",
                        testTag = "quick_recent_button",
                        onClick = onRecentClick
                    )
                }
            }
        }
    }

    // Rename Dialog
    if (isEditingTitle) {
        AlertDialog(
            onDismissRequest = { isEditingTitle = false },
            title = { Text("Rename Document") },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempTitle,
                        onValueChange = { tempTitle = it },
                        label = { Text("Document Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("document_title_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempTitle.isNotBlank()) {
                            onTitleChange(tempTitle.trim())
                        }
                        isEditingTitle = false
                    },
                    modifier = Modifier.testTag("confirm_rename_button")
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditingTitle = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TitleBar(
    document: Document,
    saveStatus: SaveStatus,
    canUndo: Boolean,
    canRedo: Boolean,
    onTitleChange: (String) -> Unit,
    onMenuClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSaveAsClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onSearchClick: () -> Unit,
    onPrintClick: () -> Unit,
    onShareClick: () -> Unit,
    onRecentClick: () -> Unit,
    modifier: Modifier = Modifier
) = TitleBar(
    documentTitle = document.title,
    documentFormat = document.format,
    saveStatus = saveStatus,
    canUndo = canUndo,
    canRedo = canRedo,
    onTitleChange = onTitleChange,
    onMenuClick = onMenuClick,
    onSaveClick = onSaveClick,
    onSaveAsClick = onSaveAsClick,
    onUndoClick = onUndoClick,
    onRedoClick = onRedoClick,
    onSearchClick = onSearchClick,
    onPrintClick = onPrintClick,
    onShareClick = onShareClick,
    onRecentClick = onRecentClick,
    modifier = modifier
)

@Composable
fun SaveStatusChip(saveStatus: SaveStatus) {
    val (bgColor, textColor, icon) = when (saveStatus) {
        SaveStatus.SAVED -> Triple(Color(0xFF1B5E20).copy(alpha = 0.35f), Color(0xFFA5D6A7), Icons.Default.CheckCircle)
        SaveStatus.SAVING -> Triple(Color(0xFF0D47A1).copy(alpha = 0.5f), Color(0xFF90CAF9), Icons.Default.Sync)
        SaveStatus.UNSAVED -> Triple(Color(0xFFE65100).copy(alpha = 0.35f), Color(0xFFFFCC80), null)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        } else {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = saveStatus.label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleIconButton(
    icon: ImageVector,
    contentDescription: String,
    tooltip: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = ""
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState()
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
                .size(36.dp)
                .testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
