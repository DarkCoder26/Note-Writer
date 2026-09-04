package com.example.export

import com.example.model.DocListType
import com.example.model.Document
import com.example.model.HeadingLevel
import com.example.model.HorizontalRuleBlock
import com.example.model.ImageBlock
import com.example.model.PageBreakBlock
import com.example.model.ParagraphBlock
import com.example.model.TableBlock

object TxtExporter {

    fun export(doc: Document): String {
        val sb = StringBuilder()
        sb.append(doc.title.uppercase()).append("\n")
        sb.append("=".repeat(doc.title.length.coerceAtLeast(20))).append("\n\n")

        for (block in doc.blocks) {
            when (block) {
                is ParagraphBlock -> {
                    val indent = "    ".repeat(block.indentLevel)
                    val prefix = when (block.listType) {
                        DocListType.BULLET -> "• "
                        DocListType.NUMBER -> "1. "
                        DocListType.CHECKLIST -> if (block.isChecked) "[X] " else "[ ] "
                        DocListType.NONE -> ""
                    }

                    when (block.headingLevel) {
                        HeadingLevel.TITLE -> sb.append(block.text.uppercase()).append("\n\n")
                        HeadingLevel.SUBTITLE -> sb.append(block.text).append("\n\n")
                        HeadingLevel.HEADING_1 -> sb.append("\n# ").append(block.text).append("\n")
                        HeadingLevel.HEADING_2 -> sb.append("\n## ").append(block.text).append("\n")
                        HeadingLevel.HEADING_3 -> sb.append("\n### ").append(block.text).append("\n")
                        HeadingLevel.NORMAL -> sb.append(indent).append(prefix).append(block.text).append("\n\n")
                    }
                }
                is TableBlock -> {
                    sb.append("\n")
                    for (row in block.cells) {
                        sb.append(row.joinToString(" | ")).append("\n")
                    }
                    sb.append("\n")
                }
                is HorizontalRuleBlock -> {
                    sb.append("\n----------------------------------------\n\n")
                }
                is PageBreakBlock -> {
                    sb.append("\n=== PAGE BREAK ===\n\n")
                }
                is ImageBlock -> {
                    if (block.caption.isNotBlank()) {
                        sb.append("[Image: ").append(block.caption).append("]\n\n")
                    }
                }
            }
        }

        return sb.toString().trim()
    }
}
