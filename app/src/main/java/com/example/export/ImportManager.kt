package com.example.export

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.DocAlignment
import com.example.model.DocBlock
import com.example.model.DocListType
import com.example.model.Document
import com.example.model.DocumentSerializer
import com.example.model.ExportFormat
import com.example.model.HeadingLevel
import com.example.model.HorizontalRuleBlock
import com.example.model.PageBreakBlock
import com.example.model.ParagraphBlock
import com.example.model.TableBlock
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.zip.ZipInputStream

object ImportManager {

    fun importFromUri(context: Context, uri: Uri): Document {
        val fileName = getFileName(context, uri) ?: "Imported Document"
        val extension = fileName.substringAfterLast('.', "").lowercase()

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Could not open file stream")

        return inputStream.use { stream ->
            when (extension) {
                "docx" -> parseDocx(fileName, stream)
                "md", "markdown" -> parseMarkdown(fileName, stream)
                "html", "htm" -> parseHtml(fileName, stream)
                "json", "docedit" -> parseJson(fileName, stream)
                else -> parsePlainText(fileName, stream)
            }
        }
    }

    fun parsePlainText(fileName: String, stream: InputStream): Document {
        val title = fileName.substringBeforeLast('.')
        val text = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).readText()
        val paragraphs = text.split("\n\n")

        val blocks = mutableListOf<DocBlock>()
        if (paragraphs.isNotEmpty()) {
            // First paragraph as Title if short, otherwise normal
            val first = paragraphs[0].trim()
            if (first.length < 60 && !first.contains("\n")) {
                blocks.add(ParagraphBlock(text = first, headingLevel = HeadingLevel.TITLE))
            } else {
                blocks.add(ParagraphBlock(text = first))
            }

            for (i in 1 until paragraphs.size) {
                val p = paragraphs[i].trim()
                if (p.isNotBlank()) {
                    blocks.add(ParagraphBlock(text = p))
                }
            }
        }

        if (blocks.isEmpty()) {
            blocks.add(ParagraphBlock())
        }

        return Document(
            id = 0,
            title = title,
            blocks = blocks,
            format = ExportFormat.TXT,
            isModified = true,
            lastSavedMillis = System.currentTimeMillis()
        )
    }

    fun parseMarkdown(fileName: String, stream: InputStream): Document {
        val title = fileName.substringBeforeLast('.')
        val reader = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8))
        val blocks = mutableListOf<DocBlock>()

        var line: String? = reader.readLine()
        while (line != null) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("# ") -> {
                    blocks.add(ParagraphBlock(text = trimmed.removePrefix("# ").trim(), headingLevel = HeadingLevel.TITLE))
                }
                trimmed.startsWith("## ") -> {
                    blocks.add(ParagraphBlock(text = trimmed.removePrefix("## ").trim(), headingLevel = HeadingLevel.HEADING_1))
                }
                trimmed.startsWith("### ") -> {
                    blocks.add(ParagraphBlock(text = trimmed.removePrefix("### ").trim(), headingLevel = HeadingLevel.HEADING_2))
                }
                trimmed.startsWith("#### ") -> {
                    blocks.add(ParagraphBlock(text = trimmed.removePrefix("#### ").trim(), headingLevel = HeadingLevel.HEADING_3))
                }
                trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                    blocks.add(HorizontalRuleBlock())
                }
                trimmed.startsWith("- [ ] ") || trimmed.startsWith("* [ ] ") -> {
                    val content = trimmed.substring(6).trim()
                    blocks.add(ParagraphBlock(text = cleanMarkdownInline(content), listType = DocListType.CHECKLIST, isChecked = false))
                }
                trimmed.startsWith("- [x] ") || trimmed.startsWith("* [x] ") || trimmed.startsWith("- [X] ") -> {
                    val content = trimmed.substring(6).trim()
                    blocks.add(ParagraphBlock(text = cleanMarkdownInline(content), listType = DocListType.CHECKLIST, isChecked = true))
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val content = trimmed.substring(2).trim()
                    blocks.add(ParagraphBlock(text = cleanMarkdownInline(content), listType = DocListType.BULLET))
                }
                trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val content = trimmed.replaceFirst(Regex("^\\d+\\.\\s+"), "")
                    blocks.add(ParagraphBlock(text = cleanMarkdownInline(content), listType = DocListType.NUMBER))
                }
                trimmed.isNotBlank() -> {
                    val isBold = (trimmed.startsWith("**") && trimmed.endsWith("**"))
                    val isItalic = (trimmed.startsWith("*") && trimmed.endsWith("*"))
                    blocks.add(ParagraphBlock(
                        text = cleanMarkdownInline(trimmed),
                        isBold = isBold,
                        isItalic = isItalic
                    ))
                }
            }
            line = reader.readLine()
        }

        if (blocks.isEmpty()) {
            blocks.add(ParagraphBlock())
        }

        return Document(
            id = 0,
            title = title,
            blocks = blocks,
            format = ExportFormat.MARKDOWN,
            isModified = true,
            lastSavedMillis = System.currentTimeMillis()
        )
    }

    private fun cleanMarkdownInline(text: String): String {
        return text
            .replace("**", "")
            .replace("__", "")
            .replace("`", "")
    }

    fun parseHtml(fileName: String, stream: InputStream): Document {
        val title = fileName.substringBeforeLast('.')
        val rawHtml = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).readText()
        val blocks = mutableListOf<DocBlock>()

        // Extract body content or whole HTML
        val bodyMatch = Regex("<body[^>]*>(.*?)</body>", RegexOption.DOT_MATCHES_ALL).find(rawHtml)
        val content = bodyMatch?.groupValues?.get(1) ?: rawHtml

        // Parse tags
        val tagRegex = Regex("<(h[1-6]|p|li|hr|table|br)[^>]*>(.*?)</\\1>|<(hr|br)[^>]*/?>", RegexOption.DOT_MATCHES_ALL)
        val matches = tagRegex.findAll(content).toList()

        if (matches.isNotEmpty()) {
            for (match in matches) {
                val tag = (match.groups[1]?.value ?: match.groups[3]?.value ?: "").lowercase()
                val inner = stripTags(match.groups[2]?.value ?: "").trim()

                when (tag) {
                    "h1" -> blocks.add(ParagraphBlock(text = inner, headingLevel = HeadingLevel.TITLE))
                    "h2" -> blocks.add(ParagraphBlock(text = inner, headingLevel = HeadingLevel.HEADING_1))
                    "h3" -> blocks.add(ParagraphBlock(text = inner, headingLevel = HeadingLevel.HEADING_2))
                    "h4", "h5", "h6" -> blocks.add(ParagraphBlock(text = inner, headingLevel = HeadingLevel.HEADING_3))
                    "hr" -> blocks.add(HorizontalRuleBlock())
                    "li" -> blocks.add(ParagraphBlock(text = inner, listType = DocListType.BULLET))
                    "p" -> if (inner.isNotBlank()) blocks.add(ParagraphBlock(text = inner))
                }
            }
        } else {
            // Plain stripped fallback
            val plain = stripTags(content).trim()
            plain.split("\n\n").forEach {
                if (it.isNotBlank()) blocks.add(ParagraphBlock(text = it.trim()))
            }
        }

        if (blocks.isEmpty()) {
            blocks.add(ParagraphBlock())
        }

        return Document(
            id = 0,
            title = title,
            blocks = blocks,
            format = ExportFormat.HTML,
            isModified = true,
            lastSavedMillis = System.currentTimeMillis()
        )
    }

    private fun stripTags(html: String): String {
        return html
            .replace(Regex("<br\\s*/?>"), "\n")
            .replace(Regex("<[^>]*>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
    }

    fun parseJson(fileName: String, stream: InputStream): Document {
        val json = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).readText()
        return DocumentSerializer.deserialize(json)
    }

    fun parseDocx(fileName: String, stream: InputStream): Document {
        val title = fileName.substringBeforeLast('.')
        val blocks = mutableListOf<DocBlock>()

        val zip = ZipInputStream(stream)
        var entry = zip.nextEntry
        var documentXml: String? = null

        while (entry != null) {
            if (entry.name == "word/document.xml") {
                documentXml = BufferedReader(InputStreamReader(zip, StandardCharsets.UTF_8)).readText()
                break
            }
            entry = zip.nextEntry
        }

        if (documentXml != null) {
            // Extract paragraphs <w:p>
            val pRegex = Regex("<w:p\\b[^>]*>(.*?)</w:p>", RegexOption.DOT_MATCHES_ALL)
            val tRegex = Regex("<w:t\\b[^>]*>(.*?)</w:t>", RegexOption.DOT_MATCHES_ALL)
            val tblRegex = Regex("<w:tbl\\b[^>]*>(.*?)</w:tbl>", RegexOption.DOT_MATCHES_ALL)

            for (pMatch in pRegex.findAll(documentXml)) {
                val pContent = pMatch.groupValues[1]

                // Check for page break
                if (pContent.contains("<w:br w:type=\"page\"/>") || pContent.contains("<w:br w:type=\"page\" />")) {
                    blocks.add(PageBreakBlock())
                    continue
                }

                // Check for heading styles
                val isTitle = pContent.contains("w:val=\"Title\"")
                val isH1 = pContent.contains("w:val=\"Heading1\"") || pContent.contains("w:val=\"heading 1\"")
                val isH2 = pContent.contains("w:val=\"Heading2\"") || pContent.contains("w:val=\"heading 2\"")
                val isH3 = pContent.contains("w:val=\"Heading3\"") || pContent.contains("w:val=\"heading 3\"")

                val isBold = pContent.contains("<w:b/>") || pContent.contains("<w:b />")
                val isItalic = pContent.contains("<w:i/>") || pContent.contains("<w:i />")
                val isUnderline = pContent.contains("<w:u ")

                val textBuilder = StringBuilder()
                for (tMatch in tRegex.findAll(pContent)) {
                    textBuilder.append(unescapeXml(tMatch.groupValues[1]))
                }

                val text = textBuilder.toString().trim()
                if (text.isNotBlank()) {
                    val heading = when {
                        isTitle -> HeadingLevel.TITLE
                        isH1 -> HeadingLevel.HEADING_1
                        isH2 -> HeadingLevel.HEADING_2
                        isH3 -> HeadingLevel.HEADING_3
                        else -> HeadingLevel.NORMAL
                    }

                    blocks.add(
                        ParagraphBlock(
                            text = text,
                            headingLevel = heading,
                            isBold = isBold,
                            isItalic = isItalic,
                            isUnderline = isUnderline
                        )
                    )
                }
            }
        }

        if (blocks.isEmpty()) {
            blocks.add(ParagraphBlock(text = "Imported DOCX: $title"))
        }

        return Document(
            id = 0,
            title = title,
            blocks = blocks,
            format = ExportFormat.DOCX,
            isModified = true,
            lastSavedMillis = System.currentTimeMillis()
        )
    }

    private fun unescapeXml(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        return cursor.getString(nameIndex)
                    }
                }
            }
        }
        return uri.path?.substringAfterLast('/')
    }
}
