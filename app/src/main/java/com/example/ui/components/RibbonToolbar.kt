package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlockFormatting
import com.example.model.DocAlignment
import com.example.model.DocBlock
import com.example.model.DocFontFamily
import com.example.model.DocListType
import com.example.model.HeadingLevel
import com.example.model.ParagraphBlock

@Composable
fun RibbonToolbar(
    formatting: BlockFormatting,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onBoldToggle: () -> Unit,
    onItalicToggle: () -> Unit,
    onUnderlineToggle: () -> Unit,
    onStrikeToggle: () -> Unit,
    onHeadingSelect: (HeadingLevel) -> Unit,
    onAlignmentSelect: (DocAlignment) -> Unit,
    onListTypeSelect: (DocListType) -> Unit,
    onIndentAdjust: (Int) -> Unit,
    onTextColorSelect: (String) -> Unit,
    onHighlightColorSelect: (String?) -> Unit,
    onFontSizeDelta: (Float) -> Unit,
    onFontSizeSelect: (Float) -> Unit,
    onFontFamilySelect: (DocFontFamily) -> Unit,
    onLineSpacingSelect: (Float) -> Unit,
    onClearFormatting: () -> Unit,
    onInsertTable: () -> Unit,
    onInsertImage: () -> Unit,
    onInsertLink: () -> Unit,
    onInsertHorizontalRule: () -> Unit,
    onInsertPageBreak: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenWordCount: () -> Unit,
    onOpenTemplates: () -> Unit,
    zoomPercent: Int,
    onZoomChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Home", "Insert", "Tools", "View")

    val isBold = formatting.isBold
    val isItalic = formatting.isItalic
    val isUnderline = formatting.isUnderline
    val isStrike = formatting.isStrike
    val alignment = formatting.alignment
    val listType = formatting.listType
    val currentHeading = formatting.headingLevel
    val currentFontFamily = formatting.fontFamily
    val currentFontSize = formatting.fontSizeSp

    Surface(
        color = Color(0xFFF8F9FA),
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFFF0F4F8),
                contentColor = Color(0xFF0F4C81),
                divider = { HorizontalDivider(color = Color(0xFFE2E8F0)) },
                modifier = Modifier.height(38.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            )
                        },
                        modifier = Modifier.testTag("ribbon_tab_${title.lowercase()}")
                    )
                }
            }

            // Tab Content
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // HOME TAB
                        // Clipboard
                        RibbonAction(
                            icon = Icons.Default.ContentCut,
                            tooltip = "Cut (Ctrl+X)",
                            onClick = onCut,
                            testTag = "ribbon_cut"
                        )
                        RibbonAction(
                            icon = Icons.Default.ContentCopy,
                            tooltip = "Copy (Ctrl+C)",
                            onClick = onCopy,
                            testTag = "ribbon_copy"
                        )
                        RibbonAction(
                            icon = Icons.Default.ContentPaste,
                            tooltip = "Paste (Ctrl+V)",
                            onClick = onPaste,
                            testTag = "ribbon_paste"
                        )
                        ToolbarDivider()

                        // Headings Dropdown
                        HeadingSelector(currentHeading, onHeadingSelect)
                        ToolbarDivider()

                        // Font Family Dropdown
                        FontFamilySelector(currentFontFamily, onFontFamilySelect)
                        ToolbarDivider()

                        // Font Size Controls
                        FontSizeControls(currentFontSize, onFontSizeDelta, onFontSizeSelect)
                        ToolbarDivider()

                        // Basic Formatting (B, I, U, S)
                        RibbonToggle(
                            icon = Icons.Default.FormatBold,
                            tooltip = "Bold (Ctrl+B)",
                            isActive = isBold,
                            onClick = onBoldToggle,
                            testTag = "ribbon_bold"
                        )
                        RibbonToggle(
                            icon = Icons.Default.FormatItalic,
                            tooltip = "Italic (Ctrl+I)",
                            isActive = isItalic,
                            onClick = onItalicToggle,
                            testTag = "ribbon_italic"
                        )
                        RibbonToggle(
                            icon = Icons.Default.FormatUnderlined,
                            tooltip = "Underline (Ctrl+U)",
                            isActive = isUnderline,
                            onClick = onUnderlineToggle,
                            testTag = "ribbon_underline"
                        )
                        RibbonToggle(
                            icon = Icons.Default.FormatStrikethrough,
                            tooltip = "Strikethrough",
                            isActive = isStrike,
                            onClick = onStrikeToggle,
                            testTag = "ribbon_strike"
                        )
                        ToolbarDivider()

                        // Colors
                        TextColorSelector(formatting.textColorHex, onTextColorSelect)
                        HighlightColorSelector(formatting.highlightColorHex, onHighlightColorSelect)
                        ToolbarDivider()

                        // Paragraph Alignment
                        RibbonToggle(
                            icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                            tooltip = "Align Left",
                            isActive = alignment == DocAlignment.LEFT,
                            onClick = { onAlignmentSelect(DocAlignment.LEFT) },
                            testTag = "ribbon_align_left"
                        )
                        RibbonToggle(
                            icon = Icons.Default.FormatAlignCenter,
                            tooltip = "Align Center",
                            isActive = alignment == DocAlignment.CENTER,
                            onClick = { onAlignmentSelect(DocAlignment.CENTER) },
                            testTag = "ribbon_align_center"
                        )
                        RibbonToggle(
                            icon = Icons.AutoMirrored.Filled.FormatAlignRight,
                            tooltip = "Align Right",
                            isActive = alignment == DocAlignment.RIGHT,
                            onClick = { onAlignmentSelect(DocAlignment.RIGHT) },
                            testTag = "ribbon_align_right"
                        )
                        RibbonToggle(
                            icon = Icons.Default.FormatAlignJustify,
                            tooltip = "Justify",
                            isActive = alignment == DocAlignment.JUSTIFY,
                            onClick = { onAlignmentSelect(DocAlignment.JUSTIFY) },
                            testTag = "ribbon_align_justify"
                        )
                        ToolbarDivider()

                        // Lists
                        RibbonToggle(
                            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                            tooltip = "Bulleted List",
                            isActive = listType == DocListType.BULLET,
                            onClick = { onListTypeSelect(DocListType.BULLET) },
                            testTag = "ribbon_bullet_list"
                        )
                        RibbonToggle(
                            icon = Icons.Default.FormatListNumbered,
                            tooltip = "Numbered List",
                            isActive = listType == DocListType.NUMBER,
                            onClick = { onListTypeSelect(DocListType.NUMBER) },
                            testTag = "ribbon_numbered_list"
                        )
                        RibbonToggle(
                            icon = Icons.Default.Checklist,
                            tooltip = "Checklist / Tasks",
                            isActive = listType == DocListType.CHECKLIST,
                            onClick = { onListTypeSelect(DocListType.CHECKLIST) },
                            testTag = "ribbon_checklist"
                        )
                        ToolbarDivider()

                        // Indent & Spacing
                        RibbonAction(
                            icon = Icons.AutoMirrored.Filled.FormatIndentDecrease,
                            tooltip = "Decrease Indent",
                            onClick = { onIndentAdjust(-1) },
                            testTag = "ribbon_indent_dec"
                        )
                        RibbonAction(
                            icon = Icons.AutoMirrored.Filled.FormatIndentIncrease,
                            tooltip = "Increase Indent",
                            onClick = { onIndentAdjust(1) },
                            testTag = "ribbon_indent_inc"
                        )
                        LineSpacingSelector(formatting.lineSpacingMultiplier, onLineSpacingSelect)
                        ToolbarDivider()

                        // Clear Formatting
                        RibbonAction(
                            icon = Icons.Default.FormatClear,
                            tooltip = "Clear All Formatting",
                            onClick = onClearFormatting,
                            testTag = "ribbon_clear_fmt"
                        )
                    }
                    1 -> {
                        // INSERT TAB
                        RibbonTextButton(
                            icon = Icons.Default.TableChart,
                            label = "Table",
                            tooltip = "Insert Table (Rows x Cols)",
                            onClick = onInsertTable,
                            testTag = "ribbon_insert_table"
                        )
                        ToolbarDivider()

                        RibbonTextButton(
                            icon = Icons.Default.Image,
                            label = "Image",
                            tooltip = "Insert Picture / Photo",
                            onClick = onInsertImage,
                            testTag = "ribbon_insert_image"
                        )
                        ToolbarDivider()

                        RibbonTextButton(
                            icon = Icons.Default.Link,
                            label = "Link",
                            tooltip = "Insert Hyperlink",
                            onClick = onInsertLink,
                            testTag = "ribbon_insert_link"
                        )
                        ToolbarDivider()

                        RibbonTextButton(
                            icon = Icons.Default.HorizontalRule,
                            label = "Divider",
                            tooltip = "Insert Horizontal Line",
                            onClick = onInsertHorizontalRule,
                            testTag = "ribbon_insert_hr"
                        )
                        ToolbarDivider()

                        RibbonTextButton(
                            icon = Icons.Default.InsertDriveFile,
                            label = "Page Break",
                            tooltip = "Insert Page Break",
                            onClick = onInsertPageBreak,
                            testTag = "ribbon_insert_pb"
                        )
                    }
                    2 -> {
                        // TOOLS TAB
                        RibbonTextButton(
                            icon = Icons.Default.Search,
                            label = "Find & Replace",
                            tooltip = "Search and replace text in document",
                            onClick = onOpenSearch,
                            testTag = "ribbon_tools_search"
                        )
                        ToolbarDivider()

                        RibbonTextButton(
                            icon = Icons.Default.Numbers,
                            label = "Word Count",
                            tooltip = "View document statistics",
                            onClick = onOpenWordCount,
                            testTag = "ribbon_tools_wc"
                        )
                        ToolbarDivider()

                        RibbonTextButton(
                            icon = Icons.Default.Description,
                            label = "Templates",
                            tooltip = "Choose starter document template",
                            onClick = onOpenTemplates,
                            testTag = "ribbon_tools_templates"
                        )
                    }
                    3 -> {
                        // VIEW TAB
                        Text(
                            text = "Zoom:",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        listOf(75, 100, 125, 150).forEach { preset ->
                            RibbonPresetChip(
                                label = "$preset%",
                                isSelected = zoomPercent == preset,
                                onClick = { onZoomChange(preset) }
                            )
                        }

                        ToolbarDivider()

                        RibbonAction(
                            icon = Icons.Default.ZoomOut,
                            tooltip = "Zoom Out (-15%)",
                            onClick = { onZoomChange((zoomPercent - 15).coerceAtLeast(50)) }
                        )

                        Text(
                            text = "$zoomPercent%",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        RibbonAction(
                            icon = Icons.Default.ZoomIn,
                            tooltip = "Zoom In (+15%)",
                            onClick = { onZoomChange((zoomPercent + 15).coerceAtMost(200)) }
                        )
                    }
                }
            }
        }
    }
}

// --- Sub-components for Ribbon ---

@Composable
fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .width(1.dp)
            .height(26.dp)
            .background(Color(0xFFCBD5E1))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RibbonToggle(
    icon: ImageVector,
    tooltip: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(34.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isActive) Color(0xFFC7D2FE) else Color.Transparent)
                .border(
                    width = if (isActive) 1.dp else 0.dp,
                    color = if (isActive) Color(0xFF3B82F6) else Color.Transparent,
                    shape = RoundedCornerShape(4.dp)
                )
                .clickable(onClick = onClick)
                .testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tooltip,
                tint = if (isActive) Color(0xFF1D4ED8) else Color(0xFF334155),
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RibbonAction(
    icon: ImageVector,
    tooltip: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState()
    ) {
        IconButton(
            onClick = onClick,
            modifier = modifier
                .size(34.dp)
                .testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tooltip,
                tint = Color(0xFF334155),
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
fun RibbonTextButton(
    icon: ImageVector,
    label: String,
    tooltip: String,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tooltip,
            tint = Color(0xFF0F4C81),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E293B)
            )
        )
    }
}

@Composable
fun RibbonPresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) Color(0xFF0F4C81) else Color(0xFFE2E8F0))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isSelected) Color.White else Color(0xFF334155),
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
fun HeadingSelector(
    currentHeading: HeadingLevel,
    onSelect: (HeadingLevel) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .testTag("ribbon_heading_dropdown")
        ) {
            Text(
                text = currentHeading.label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F4C81)
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("▼", fontSize = 9.sp, color = Color.Gray)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            HeadingLevel.values().forEach { level ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = level.label,
                            fontWeight = if (level.isBold) FontWeight.Bold else FontWeight.Normal,
                            fontSize = if (level == HeadingLevel.TITLE) 16.sp else 14.sp
                        )
                    },
                    onClick = {
                        onSelect(level)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun FontFamilySelector(
    currentFont: DocFontFamily,
    onSelect: (DocFontFamily) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .testTag("ribbon_font_family_dropdown")
        ) {
            Text(
                text = currentFont.displayName.substringBefore(" ("),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF334155)
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("▼", fontSize = 9.sp, color = Color.Gray)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DocFontFamily.values().forEach { font ->
                DropdownMenuItem(
                    text = { Text(font.displayName) },
                    onClick = {
                        onSelect(font)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun FontSizeControls(
    fontSize: Float,
    onDelta: (Float) -> Unit,
    onSelect: (Float) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        RibbonAction(
            icon = Icons.Default.Remove,
            tooltip = "Decrease Font Size",
            onClick = { onDelta(-1f) }
        )

        Box {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "${fontSize.toInt()} pt",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                listOf(10f, 11f, 12f, 14f, 16f, 18f, 20f, 24f, 28f, 32f, 36f, 48f).forEach { size ->
                    DropdownMenuItem(
                        text = { Text("${size.toInt()} pt") },
                        onClick = {
                            onSelect(size)
                            expanded = false
                        }
                    )
                }
            }
        }

        RibbonAction(
            icon = Icons.Default.Add,
            tooltip = "Increase Font Size",
            onClick = { onDelta(1f) }
        )
    }
}

@Composable
fun TextColorSelector(
    currentColorHex: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = listOf(
        "#1A1A1A" to "Black",
        "#0D47A1" to "Navy",
        "#1565C0" to "Blue",
        "#C62828" to "Red",
        "#2E7D32" to "Green",
        "#6A1B9A" to "Purple",
        "#E65100" to "Orange",
        "#546E7A" to "Slate"
    )

    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { expanded = true }
                .padding(4.dp)
                .testTag("ribbon_text_color")
        ) {
            Icon(
                imageVector = Icons.Default.FormatColorText,
                contentDescription = "Text Color",
                tint = Color(0xFF334155),
                modifier = Modifier.size(18.dp)
            )
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(3.dp)
                    .background(
                        try { Color(android.graphics.Color.parseColor(currentColorHex)) } catch (e: Exception) { Color.Black }
                    )
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(
                "Text Color",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            colors.forEach { (hex, name) ->
                DropdownMenuItem(
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                        )
                    },
                    text = { Text(name) },
                    onClick = {
                        onSelect(hex)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun HighlightColorSelector(
    currentHighlightHex: String?,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val highlights = listOf(
        null to "No Highlight",
        "#FFF59D" to "Yellow",
        "#C8E6C9" to "Light Green",
        "#B3E5FC" to "Light Blue",
        "#F8BBD0" to "Pink",
        "#FFE0B2" to "Light Orange"
    )

    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { expanded = true }
                .padding(4.dp)
                .testTag("ribbon_highlight_color")
        ) {
            Icon(
                imageVector = Icons.Default.FormatColorFill,
                contentDescription = "Highlight Color",
                tint = Color(0xFF334155),
                modifier = Modifier.size(18.dp)
            )
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(3.dp)
                    .background(
                        if (currentHighlightHex != null) {
                            try { Color(android.graphics.Color.parseColor(currentHighlightHex)) } catch (e: Exception) { Color.Yellow }
                        } else Color.Transparent
                    )
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(
                "Text Highlight",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            highlights.forEach { (hex, name) ->
                DropdownMenuItem(
                    leadingIcon = {
                        if (hex != null) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                            )
                        } else {
                            Text("∅", fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                    },
                    text = { Text(name) },
                    onClick = {
                        onSelect(hex)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun LineSpacingSelector(
    currentSpacing: Float,
    onSelect: (Float) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val spacings = listOf(1.0f, 1.15f, 1.5f, 2.0f)

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(34.dp).testTag("ribbon_line_spacing")
        ) {
            Icon(
                imageVector = Icons.Default.FormatLineSpacing,
                contentDescription = "Line Spacing",
                tint = Color(0xFF334155),
                modifier = Modifier.size(19.dp)
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(
                "Line Spacing",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            spacings.forEach { s ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${s}x ${if (currentSpacing == s) "✓" else ""}",
                            fontWeight = if (currentSpacing == s) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelect(s)
                        expanded = false
                    }
                )
            }
        }
    }
}
