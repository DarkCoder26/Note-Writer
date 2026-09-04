package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.IndeterminateCheckBox
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.DocAlignment
import com.example.model.DocBlock
import com.example.model.DocFontFamily
import com.example.model.DocListType
import com.example.model.Document
import com.example.model.HeadingLevel
import com.example.model.HorizontalRuleBlock
import com.example.model.ImageBlock
import com.example.model.PageBreakBlock
import com.example.model.ParagraphBlock
import com.example.model.TableBlock

@Composable
fun DocumentCanvas(
    document: Document,
    activeBlockId: String?,
    zoomPercent: Int,
    onSelectBlock: (String) -> Unit,
    onUpdateBlockText: (String, String) -> Unit,
    onAddParagraphAfter: (String?) -> Unit,
    onDeleteBlock: (String) -> Unit,
    onToggleChecklist: (String) -> Unit,
    onUpdateTableCell: (String, Int, Int, String) -> Unit,
    onAddTableRow: (String) -> Unit,
    onRemoveTableRow: (String) -> Unit,
    onAddTableCol: (String) -> Unit,
    onRemoveTableCol: (String) -> Unit,
    onUpdateImageWidth: (String, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val scale = zoomPercent / 100f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE2E8F0)) // Modern desktop gray canvas
            .verticalScroll(scrollState)
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = (820 * scale).dp)
        ) {
            // Document Margin Ruler
            DocumentRuler(scale = scale)

            Spacer(modifier = Modifier.height(8.dp))

            // White Realistic Paper Sheet
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1D5DB)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(min = 340.dp, max = 800.dp)
                    .wrapContentHeight()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                    )
                    .testTag("document_paper_sheet")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 54.dp)
                ) {
                    var pageNumber = 1

                    document.blocks.forEachIndexed { index, block ->
                        val isActive = block.id == activeBlockId

                        key(block.id) {
                            when (block) {
                                is ParagraphBlock -> {
                                    ParagraphBlockItem(
                                        block = block,
                                        isActive = isActive,
                                        onSelect = { onSelectBlock(block.id) },
                                        onTextChange = { onUpdateBlockText(block.id, it) },
                                        onAddAfter = { onAddParagraphAfter(block.id) },
                                        onDelete = { onDeleteBlock(block.id) },
                                        onToggleCheck = { onToggleChecklist(block.id) }
                                    )
                                }
                                is TableBlock -> {
                                    TableBlockItem(
                                        table = block,
                                        isActive = isActive,
                                        onSelect = { onSelectBlock(block.id) },
                                        onUpdateCell = { r, c, txt -> onUpdateTableCell(block.id, r, c, txt) },
                                        onAddRow = { onAddTableRow(block.id) },
                                        onRemoveRow = { onRemoveTableRow(block.id) },
                                        onAddCol = { onAddTableCol(block.id) },
                                        onRemoveCol = { onRemoveTableCol(block.id) },
                                        onDeleteTable = { onDeleteBlock(block.id) }
                                    )
                                }
                                is ImageBlock -> {
                                    ImageBlockItem(
                                        image = block,
                                        isActive = isActive,
                                        onSelect = { onSelectBlock(block.id) },
                                        onWidthChange = { onUpdateImageWidth(block.id, it) },
                                        onDelete = { onDeleteBlock(block.id) }
                                    )
                                }
                                is HorizontalRuleBlock -> {
                                    HorizontalRuleItem(
                                        isActive = isActive,
                                        onSelect = { onSelectBlock(block.id) },
                                        onDelete = { onDeleteBlock(block.id) }
                                    )
                                }
                                is PageBreakBlock -> {
                                    pageNumber++
                                    PageBreakItem(
                                        pageNumber = pageNumber,
                                        isActive = isActive,
                                        onSelect = { onSelectBlock(block.id) },
                                        onDelete = { onDeleteBlock(block.id) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Add Paragraph Button at bottom
                    OutlinedButton(
                        onClick = { onAddParagraphAfter(null) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F4C81)),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .testTag("add_paragraph_bottom_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Paragraph", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// --- Paragraph Block Composable ---

@Composable
fun ParagraphBlockItem(
    block: ParagraphBlock,
    isActive: Boolean,
    onSelect: () -> Unit,
    onTextChange: (String) -> Unit,
    onAddAfter: () -> Unit,
    onDelete: () -> Unit,
    onToggleCheck: () -> Unit
) {
    val fontFamily = when (block.fontFamily) {
        DocFontFamily.SANS_SERIF -> FontFamily.SansSerif
        DocFontFamily.SERIF -> FontFamily.Serif
        DocFontFamily.MONOSPACE -> FontFamily.Monospace
        DocFontFamily.CURSIVE -> FontFamily.Cursive
    }

    val textAlign = when (block.alignment) {
        DocAlignment.LEFT -> TextAlign.Left
        DocAlignment.CENTER -> TextAlign.Center
        DocAlignment.RIGHT -> TextAlign.Right
        DocAlignment.JUSTIFY -> TextAlign.Justify
    }

    val fontStyle = if (block.isItalic) FontStyle.Italic else FontStyle.Normal
    val isBold = block.isBold || block.headingLevel.isBold
    val fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal

    val textDecoration = when {
        block.isUnderline && block.isStrike -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
        block.isUnderline -> TextDecoration.Underline
        block.isStrike -> TextDecoration.LineThrough
        else -> TextDecoration.None
    }

    val textColor = try {
        Color(android.graphics.Color.parseColor(block.textColorHex))
    } catch (e: Exception) {
        Color(0xFF1A1A1A)
    }

    val highlightColor = if (block.highlightColorHex != null) {
        try { Color(android.graphics.Color.parseColor(block.highlightColorHex)) } catch (e: Exception) { null }
    } else null

    val fontSize = block.fontSizeSp.sp
    val lineHeight = (block.fontSizeSp * block.lineSpacingMultiplier * 1.35f).sp
    val indentPadding = (block.indentLevel * 24).dp

    var textFieldValue by remember(block.id) {
        mutableStateOf(TextFieldValue(text = block.text, selection = TextRange(block.text.length)))
    }

    LaunchedEffect(block.text) {
        if (block.text != textFieldValue.text) {
            val currentSel = textFieldValue.selection
            val newSel = TextRange(
                start = currentSel.start.coerceIn(0, block.text.length),
                end = currentSel.end.coerceIn(0, block.text.length)
            )
            textFieldValue = textFieldValue.copy(text = block.text, selection = newSel)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) Color(0xFFF1F5F9) else Color.Transparent)
            .border(
                width = if (isActive) 1.dp else 0.dp,
                color = if (isActive) Color(0xFF93C5FD) else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Block action pill when active
        if (isActive) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp)
            ) {
                Text(
                    text = block.headingLevel.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF0F4C81),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )

                IconButton(
                    onClick = onAddAfter,
                    modifier = Modifier.size(24.dp).testTag("block_add_after_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Paragraph Below",
                        tint = Color(0xFF0F4C81),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp).testTag("block_delete_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Paragraph",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = indentPadding)
        ) {
            // List Prefix Icon / Number
            when (block.listType) {
                DocListType.BULLET -> {
                    Text(
                        text = "• ",
                        style = TextStyle(
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        ),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                DocListType.NUMBER -> {
                    Text(
                        text = "1. ",
                        style = TextStyle(
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        ),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                DocListType.CHECKLIST -> {
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp, top = 2.dp)
                            .size(18.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .border(1.5.dp, if (block.isChecked) Color(0xFF1565C0) else Color.Gray, RoundedCornerShape(3.dp))
                            .background(if (block.isChecked) Color(0xFF1565C0) else Color.Transparent)
                            .clickable(onClick = onToggleCheck),
                        contentAlignment = Alignment.Center
                    ) {
                        if (block.isChecked) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Checked",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                DocListType.NONE -> {}
            }

            // Editable Text Field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (highlightColor != null) {
                            Modifier.background(highlightColor)
                        } else Modifier
                    )
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newTfv ->
                        textFieldValue = newTfv
                        if (newTfv.text != block.text) {
                            onTextChange(newTfv.text)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) onSelect() }
                        .testTag("paragraph_input_${block.id.take(6)}"),
                    textStyle = TextStyle(
                        fontFamily = fontFamily,
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        fontStyle = fontStyle,
                        textDecoration = textDecoration,
                        color = textColor,
                        textAlign = textAlign,
                        lineHeight = lineHeight
                    ),
                    cursorBrush = SolidColor(Color(0xFF0F4C81)),
                    decorationBox = { innerTextField ->
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                text = if (block.headingLevel == HeadingLevel.TITLE) "Type Document Title..." else "Type text here...",
                                style = TextStyle(
                                    fontFamily = fontFamily,
                                    fontSize = fontSize,
                                    color = Color(0xFF94A3B8),
                                    textAlign = textAlign
                                )
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}

// --- Table Block Composable ---

@Composable
fun TableBlockItem(
    table: TableBlock,
    isActive: Boolean,
    onSelect: () -> Unit,
    onUpdateCell: (Int, Int, String) -> Unit,
    onAddRow: () -> Unit,
    onRemoveRow: () -> Unit,
    onAddCol: () -> Unit,
    onRemoveCol: () -> Unit,
    onDeleteTable: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) Color(0xFFF1F5F9) else Color.Transparent)
            .border(
                width = if (isActive) 1.dp else 0.dp,
                color = if (isActive) Color(0xFF93C5FD) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onSelect)
            .padding(8.dp)
    ) {
        // Table Action Controls Toolbar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        ) {
            Text(
                text = "Table (${table.rows} × ${table.cols})",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F4C81)
                )
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                TableControlButton("+ Row", onAddRow)
                TableControlButton("- Row", onRemoveRow, enabled = table.rows > 1)
                TableControlButton("+ Col", onAddCol, enabled = table.cols < 8)
                TableControlButton("- Col", onRemoveCol, enabled = table.cols > 1)

                IconButton(
                    onClick = onDeleteTable,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Table",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Table Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
        ) {
            table.cells.forEachIndexed { rIdx, row ->
                val isHeader = rIdx == 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isHeader) Color(0xFFF0F4F8) else if (rIdx % 2 == 1) Color(0xFFFAFAFA) else Color.White)
                ) {
                    row.forEachIndexed { cIdx, cellValue ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(0.5.dp, Color(0xFFE2E8F0))
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            BasicTextField(
                                value = cellValue,
                                onValueChange = { onUpdateCell(rIdx, cIdx, it) },
                                textStyle = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isHeader) Color(0xFF0F4C81) else Color(0xFF1E293B)
                                ),
                                cursorBrush = SolidColor(Color(0xFF0F4C81)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("table_cell_${rIdx}_$cIdx"),
                                decorationBox = { inner ->
                                    if (cellValue.isEmpty()) {
                                        Text(
                                            text = if (isHeader) "Header ${cIdx + 1}" else "Cell",
                                            style = TextStyle(fontSize = 12.sp, color = Color(0xFF94A3B8))
                                        )
                                    }
                                    inner()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TableControlButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (enabled) Color(0xFFE2E8F0) else Color(0xFFF1F5F9))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) Color(0xFF334155) else Color(0xFF94A3B8)
        )
    }
}

// --- Image Block Composable ---

@Composable
fun ImageBlockItem(
    image: ImageBlock,
    isActive: Boolean,
    onSelect: () -> Unit,
    onWidthChange: (Float) -> Unit,
    onDelete: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) Color(0xFFF1F5F9) else Color.Transparent)
            .border(
                width = if (isActive) 1.dp else 0.dp,
                color = if (isActive) Color(0xFF93C5FD) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onSelect)
            .padding(8.dp)
    ) {
        if (isActive) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Size: ", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    listOf(0.4f to "40%", 0.7f to "70%", 1.0f to "100%").forEach { (ratio, label) ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (image.widthPercent == ratio) Color(0xFF0F4C81) else Color(0xFFE2E8F0))
                                .clickable { onWidthChange(ratio) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = if (image.widthPercent == ratio) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Image",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(image.widthPercent)
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (image.uriOrData.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(image.uriOrData)
                        .crossfade(true)
                        .build(),
                    contentDescription = image.caption,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFFE2E8F0))
                ) {
                    Text("🖼 Image Asset", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }

        if (image.caption.isNotBlank()) {
            Text(
                text = image.caption,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontStyle = FontStyle.Italic,
                    color = Color.Gray
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// --- Horizontal Rule Composable ---

@Composable
fun HorizontalRuleItem(
    isActive: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable(onClick = onSelect)
    ) {
        HorizontalDivider(
            color = if (isActive) Color(0xFF3B82F6) else Color(0xFFCBD5E1),
            thickness = if (isActive) 2.dp else 1.dp,
            modifier = Modifier.weight(1f)
        )
        if (isActive) {
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Rule",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// --- Page Break Composable ---

@Composable
fun PageBreakItem(
    pageNumber: Int,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
            .border(1.dp, if (isActive) Color(0xFF3B82F6) else Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F4C81))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "PAGE BREAK  •  Page $pageNumber",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F4C81),
                    letterSpacing = 1.sp
                )
            )
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Page Break",
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// --- Document Margin Ruler ---

@Composable
fun DocumentRuler(scale: Float) {
    Surface(
        color = Color(0xFFF8F9FA),
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1D5DB)),
        shape = RoundedCornerShape(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 800.dp)
            .height(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // Simulated inches tab stops
            (1..12).forEach { inch ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(10.dp)
                            .background(Color(0xFF94A3B8))
                    )
                    Text(
                        text = "$inch",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            color = Color(0xFF64748B)
                        ),
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }
    }
}
