package com.example.export

import com.example.model.DocAlignment
import com.example.model.DocListType
import com.example.model.Document
import com.example.model.HeadingLevel
import com.example.model.HorizontalRuleBlock
import com.example.model.ImageBlock
import com.example.model.PageBreakBlock
import com.example.model.ParagraphBlock
import com.example.model.TableBlock

object HtmlExporter {

    fun export(doc: Document): String {
        val sb = StringBuilder()
        sb.append("""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${escapeHtml(doc.title)}</title>
  <style>
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
      background-color: #f5f6f8;
      margin: 0;
      padding: 40px 20px;
      color: #222222;
      display: flex;
      justify-content: center;
    }
    .document-page {
      background-color: #ffffff;
      width: 100%;
      max-width: 800px;
      min-height: 1000px;
      padding: 60px 50px;
      box-shadow: 0 4px 18px rgba(0, 0, 0, 0.08);
      border-radius: 4px;
      box-sizing: border-box;
    }
    h1.title { font-size: 2.2rem; font-weight: 700; color: #0F4C81; margin-top: 0; margin-bottom: 8px; }
    p.subtitle { font-size: 1.15rem; color: #546E7A; font-style: italic; margin-top: 0; margin-bottom: 20px; }
    h1 { font-size: 1.6rem; font-weight: 700; color: #1565C0; margin-top: 24px; margin-bottom: 12px; }
    h2 { font-size: 1.35rem; font-weight: 600; color: #1976D2; margin-top: 20px; margin-bottom: 10px; }
    h3 { font-size: 1.15rem; font-weight: 600; color: #1E88E5; margin-top: 16px; margin-bottom: 8px; }
    p { margin-top: 0; margin-bottom: 14px; line-height: 1.6; }
    hr { border: none; border-top: 1px solid #e0e0e0; margin: 24px 0; }
    .page-break { page-break-after: always; border-top: 1px dashed #bdbdbd; margin: 36px 0; position: relative; }
    .page-break::after { content: 'Page Break'; position: absolute; top: -10px; right: 10px; background: #fff; padding: 0 8px; font-size: 11px; color: #9e9e9e; }
    table { width: 100%; border-collapse: collapse; margin: 20px 0; font-size: 14px; }
    th, td { border: 1px solid #d0d7de; padding: 10px 14px; text-align: left; }
    th { background-color: #f6f8fa; font-weight: 600; color: #0F4C81; }
    tr:nth-child(even) td { background-color: #fafbfc; }
    .list-item { margin-bottom: 6px; line-height: 1.5; }
    .checkbox { margin-right: 8px; }
    img.doc-image { max-width: 100%; height: auto; border-radius: 4px; display: block; margin: 16px auto; }
    .image-caption { text-align: center; font-size: 13px; color: #666; margin-top: 6px; font-style: italic; }
  </style>
</head>
<body>
  <div class="document-page">
""")

        for (block in doc.blocks) {
            when (block) {
                is ParagraphBlock -> {
                    val inlineStyles = mutableListOf<String>()
                    val alignStr = when (block.alignment) {
                        DocAlignment.CENTER -> "center"
                        DocAlignment.RIGHT -> "right"
                        DocAlignment.JUSTIFY -> "justify"
                        DocAlignment.LEFT -> "left"
                    }
                    inlineStyles.add("text-align: $alignStr")
                    if (block.textColorHex != "#1A1A1A") {
                        inlineStyles.add("color: ${block.textColorHex}")
                    }
                    if (block.highlightColorHex != null) {
                        inlineStyles.add("background-color: ${block.highlightColorHex}")
                    }
                    if (block.indentLevel > 0) {
                        inlineStyles.add("margin-left: ${block.indentLevel * 28}px")
                    }
                    if (block.lineSpacingMultiplier != 1.15f) {
                        inlineStyles.add("line-height: ${block.lineSpacingMultiplier * 1.4}")
                    }

                    val styleAttr = if (inlineStyles.isNotEmpty()) " style=\"${inlineStyles.joinToString("; ")}\"" else ""

                    var text = escapeHtml(block.text).replace("\n", "<br>")
                    if (block.isBold || block.headingLevel.isBold) text = "<strong>$text</strong>"
                    if (block.isItalic) text = "<em>$text</em>"
                    if (block.isUnderline) text = "<u>$text</u>"
                    if (block.isStrike) text = "<del>$text</del>"

                    when (block.headingLevel) {
                        HeadingLevel.TITLE -> sb.append("    <h1 class=\"title\"$styleAttr>$text</h1>\n")
                        HeadingLevel.SUBTITLE -> sb.append("    <p class=\"subtitle\"$styleAttr>$text</p>\n")
                        HeadingLevel.HEADING_1 -> sb.append("    <h1$styleAttr>$text</h1>\n")
                        HeadingLevel.HEADING_2 -> sb.append("    <h2$styleAttr>$text</h2>\n")
                        HeadingLevel.HEADING_3 -> sb.append("    <h3$styleAttr>$text</h3>\n")
                        HeadingLevel.NORMAL -> {
                            when (block.listType) {
                                DocListType.BULLET -> sb.append("    <div class=\"list-item\"$styleAttr>• $text</div>\n")
                                DocListType.NUMBER -> sb.append("    <div class=\"list-item\"$styleAttr>1. $text</div>\n")
                                DocListType.CHECKLIST -> {
                                    val check = if (block.isChecked) "☑" else "☐"
                                    sb.append("    <div class=\"list-item\"$styleAttr><span class=\"checkbox\">$check</span>$text</div>\n")
                                }
                                DocListType.NONE -> sb.append("    <p$styleAttr>$text</p>\n")
                            }
                        }
                    }
                }
                is TableBlock -> {
                    sb.append("    <table>\n")
                    for ((rIdx, row) in block.cells.withIndex()) {
                        sb.append("      <tr>\n")
                        val tag = if (rIdx == 0) "th" else "td"
                        for (cell in row) {
                            sb.append("        <$tag>${escapeHtml(cell)}</$tag>\n")
                        }
                        sb.append("      </tr>\n")
                    }
                    sb.append("    </table>\n")
                }
                is HorizontalRuleBlock -> sb.append("    <hr>\n")
                is PageBreakBlock -> sb.append("    <div class=\"page-break\"></div>\n")
                is ImageBlock -> {
                    sb.append("    <div style=\"text-align: center; margin: 16px 0;\">\n")
                    if (block.uriOrData.isNotBlank()) {
                        sb.append("      <img class=\"doc-image\" src=\"${escapeHtml(block.uriOrData)}\" alt=\"${escapeHtml(block.caption)}\">\n")
                    } else {
                        sb.append("      <div style=\"background: #f0f0f0; padding: 20px; border-radius: 4px; color: #888;\">🖼 Image</div>\n")
                    }
                    if (block.caption.isNotBlank()) {
                        sb.append("      <div class=\"image-caption\">${escapeHtml(block.caption)}</div>\n")
                    }
                    sb.append("    </div>\n")
                }
            }
        }

        sb.append("""  </div>
</body>
</html>""")

        return sb.toString()
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
