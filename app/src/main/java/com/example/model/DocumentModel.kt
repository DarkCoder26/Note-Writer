package com.example.model

import java.util.UUID

enum class ExportFormat(val displayName: String, val extension: String, val mimeType: String) {
    DOCX("Word Document (.docx)", "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    PDF("PDF Document (.pdf)", "pdf", "application/pdf"),
    TXT("Plain Text (.txt)", "txt", "text/plain"),
    HTML("HTML Document (.html)", "html", "text/html"),
    MARKDOWN("Markdown Document (.md)", "md", "text/markdown");

    val label: String get() = displayName
}


enum class HeadingLevel(val label: String, val fontSizeSp: Float, val isBold: Boolean) {
    NORMAL("Normal Text", 15f, false),
    TITLE("Document Title", 28f, true),
    SUBTITLE("Subtitle", 19f, false),
    HEADING_1("Heading 1", 22f, true),
    HEADING_2("Heading 2", 18f, true),
    HEADING_3("Heading 3", 16f, true)
}

enum class DocAlignment {
    LEFT, CENTER, RIGHT, JUSTIFY
}

enum class DocListType {
    NONE, BULLET, NUMBER, CHECKLIST
}

enum class DocFontFamily(val displayName: String) {
    SANS_SERIF("Sans-Serif (Modern)"),
    SERIF("Serif (Classic)"),
    MONOSPACE("Monospace (Code)"),
    CURSIVE("Cursive (Script)")
}

sealed interface DocBlock {
    val id: String
}

data class ParagraphBlock(
    override val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val headingLevel: HeadingLevel = HeadingLevel.NORMAL,
    val alignment: DocAlignment = DocAlignment.LEFT,
    val listType: DocListType = DocListType.NONE,
    val isChecked: Boolean = false,
    val indentLevel: Int = 0,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrike: Boolean = false,
    val textColorHex: String = "#1A1A1A",
    val highlightColorHex: String? = null,
    val fontSizeSp: Float = 15f,
    val fontFamily: DocFontFamily = DocFontFamily.SANS_SERIF,
    val lineSpacingMultiplier: Float = 1.15f
) : DocBlock

data class TableBlock(
    override val id: String = UUID.randomUUID().toString(),
    val rows: Int = 3,
    val cols: Int = 3,
    val cells: List<List<String>> = List(3) { List(3) { "" } }
) : DocBlock

data class ImageBlock(
    override val id: String = UUID.randomUUID().toString(),
    val uriOrData: String = "",
    val caption: String = "",
    val widthPercent: Float = 0.8f,
    val alignment: DocAlignment = DocAlignment.CENTER
) : DocBlock

data class HorizontalRuleBlock(
    override val id: String = UUID.randomUUID().toString()
) : DocBlock

data class PageBreakBlock(
    override val id: String = UUID.randomUUID().toString()
) : DocBlock

data class Document(
    val id: Long = 0L,
    val title: String = "Untitled Document",
    val format: ExportFormat = ExportFormat.DOCX,
    val blocks: List<DocBlock> = emptyList(),
    val isModified: Boolean = false,
    val lastSavedMillis: Long = System.currentTimeMillis()
)

data class BlockFormatting(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrike: Boolean = false,
    val alignment: DocAlignment = DocAlignment.LEFT,
    val listType: DocListType = DocListType.NONE,
    val headingLevel: HeadingLevel = HeadingLevel.NORMAL,
    val fontFamily: DocFontFamily = DocFontFamily.SANS_SERIF,
    val fontSizeSp: Float = 15f,
    val textColorHex: String = "#1A1A1A",
    val highlightColorHex: String? = null,
    val lineSpacingMultiplier: Float = 1.15f,
    val indentLevel: Int = 0
)

data class DocumentMetrics(
    val pageCount: Int = 1,
    val wordCount: Int = 0,
    val characterCount: Int = 0,
    val paragraphCount: Int = 1
)
