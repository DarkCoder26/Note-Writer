package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FileMenuDrawer(
    documentTitle: String,
    onNewDocument: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenDeviceFile: () -> Unit,
    onSave: () -> Unit,
    onSaveAs: () -> Unit,
    onPrint: () -> Unit,
    onShare: () -> Unit,
    onWordCount: () -> Unit,
    onTemplates: () -> Unit,
    onAbout: () -> Unit = {},
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF0F4C81),
        drawerContentColor = Color.White,
        modifier = modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = onCloseDrawer, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Editor",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "File Menu",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = documentTitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        ),
                        maxLines = 1
                    )
                }
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Menu Options
            FileMenuItem(
                icon = Icons.Default.NoteAdd,
                title = "New Document",
                subtitle = "Create a blank document",
                onClick = {
                    onCloseDrawer()
                    onNewDocument()
                },
                testTag = "file_menu_new"
            )

            FileMenuItem(
                icon = Icons.Default.FolderOpen,
                title = "Open Recent...",
                subtitle = "Browse saved documents",
                onClick = {
                    onCloseDrawer()
                    onOpenRecent()
                },
                testTag = "file_menu_open"
            )

            FileMenuItem(
                icon = Icons.Default.FileUpload,
                title = "Open from Device...",
                subtitle = "DOCX, TXT, MD, HTML, JSON",
                onClick = {
                    onCloseDrawer()
                    onOpenDeviceFile()
                },
                testTag = "file_menu_open_device"
            )

            FileMenuItem(
                icon = Icons.Default.Save,
                title = "Save",
                subtitle = "Save changes to storage",
                onClick = {
                    onCloseDrawer()
                    onSave()
                },
                testTag = "file_menu_save"
            )

            FileMenuItem(
                icon = Icons.Default.SaveAs,
                title = "Save As / Export...",
                subtitle = "DOCX, PDF, HTML, TXT, MD",
                onClick = {
                    onCloseDrawer()
                    onSaveAs()
                },
                testTag = "file_menu_save_as"
            )

            FileMenuItem(
                icon = Icons.Default.Print,
                title = "Print",
                subtitle = "Send to printer or save as PDF",
                onClick = {
                    onCloseDrawer()
                    onPrint()
                },
                testTag = "file_menu_print"
            )

            FileMenuItem(
                icon = Icons.Default.Share,
                title = "Share Document",
                subtitle = "Send via email or apps",
                onClick = {
                    onCloseDrawer()
                    onShare()
                },
                testTag = "file_menu_share"
            )

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            FileMenuItem(
                icon = Icons.Default.Description,
                title = "Starter Templates",
                subtitle = "Letters, resumes, minutes",
                onClick = {
                    onCloseDrawer()
                    onTemplates()
                },
                testTag = "file_menu_templates"
            )

            FileMenuItem(
                icon = Icons.Default.ViewHeadline,
                title = "Word Count & Stats",
                subtitle = "Document metrics",
                onClick = {
                    onCloseDrawer()
                    onWordCount()
                },
                testTag = "file_menu_stats"
            )

            FileMenuItem(
                icon = Icons.Default.Info,
                title = "About",
                subtitle = "App information",
                onClick = {
                    onCloseDrawer()
                    onAbout()
                },
                testTag = "file_menu_about"
            )
        }
    }
}

@Composable
private fun FileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            )
        }
    }
}
