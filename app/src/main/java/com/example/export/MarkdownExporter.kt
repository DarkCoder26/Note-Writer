package com.example.export

import com.example.model.DocListType
import com.example.model.Document
import com.example.model.HeadingLevel
import com.example.model.HorizontalRuleBlock
import com.example.model.ImageBlock
import com.example.model.PageBreakBlock
import com.example.model.ParagraphBlock
import com.example.model.TableBlock

object MarkdownExporter {

    fun export(doc: Document): String {
        val sb = StringBuilder()
        sb.append("# ").append(doc.title).append("\n\n")

        for (block in doc.blocks) {
            when (block) {
                is ParagraphBlock -> {
                    var formattedText = block.text
                    if (block.isBold && !block.headingLevel.isBold) formattedText = "**$formattedText**"
                    if (block.isItalic) formattedText = "*$formattedText*"
                    if (block.isStrike) formattedText = "~~$formattedText~~"

                    val indent = "  ".repeat(block.indentLevel)
                    val prefix = when (block.listType) {
                        DocListType.BULLET -> "- "
                        DocListType.NUMBER -> "1. "
                        DocListType.CHECKLIST -> if (block.isChecked) "- [x] " else "- [ ] "
                        DocListType.NONE -> ""
                    }

                    when (block.headingLevel) {
                        HeadingLevel.TITLE -> sb.append("# ").append(block.text).append("\n\n")
                        HeadingLevel.SUBTITLE -> sb.append("*").append(block.text).append("*\n\n")
                        HeadingLevel.HEADING_1 -> sb.append("## ").append(block.text).append("\n\n")
                        HeadingLevel.HEADING_2 -> sb.append("### ").append(block.text).append("\n\n")
                        HeadingLevel.HEADING_3 -> sb.append("#### ").append(block.text).append("\n\n")
                        HeadingLevel.NORMAL -> sb.append(indent).append(prefix).append(formattedText).append("\n\n")
                    }
                }
                is TableBlock -> {
                    sb.append("\n")
                    for ((rIdx, row) in block.cells.withIndex()) {
                        sb.append("| ").append(row.joinToString(" | ")).append(" |\n")
                        if (rIdx == 0) {
                            val separator = row.joinToString(" | ") { "---" }
                            sb.append("| ").append(separator).append(" |\n")
                        }
                    }
                    sb.append("\n")
                }
                is HorizontalRuleBlock -> {
                    sb.append("---\n\n")
                }
                is PageBreakBlock -> {
                    sb.append("\n<!-- pagebreak -->\n\n")
                }
                is ImageBlock -> {
                    val caption = if (block.caption.isNotBlank()) block.caption else "Image"
                    sb.append("![").append(caption).append("](").append(block.uriOrData).append(")\n\n")
                }
            }
        }

        return sb.toString().trim()
    }
}
