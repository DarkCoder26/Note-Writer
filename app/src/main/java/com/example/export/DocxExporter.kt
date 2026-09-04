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
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocxExporter {

    fun export(document: Document, outputStream: OutputStream) {
        val zip = ZipOutputStream(outputStream)

        // 1. [Content_Types].xml
        zip.putNextEntry(ZipEntry("[Content_Types].xml"))
        zip.write(getContentTypesXml().toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        // 2. _rels/.rels
        zip.putNextEntry(ZipEntry("_rels/.rels"))
        zip.write(getRootRelsXml().toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        // 3. word/_rels/document.xml.rels
        zip.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
        zip.write(getDocumentRelsXml().toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        // 4. word/styles.xml
        zip.putNextEntry(ZipEntry("word/styles.xml"))
        zip.write(getStylesXml().toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        // 5. word/document.xml
        zip.putNextEntry(ZipEntry("word/document.xml"))
        zip.write(buildDocumentXml(document).toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        zip.finish()
        zip.flush()
    }

    fun exportToBytes(document: Document): ByteArray {
        val baos = ByteArrayOutputStream()
        export(document, baos)
        return baos.toByteArray()
    }

    private fun getContentTypesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>"""
    }

    private fun getRootRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
    }

    private fun getDocumentRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
    }

    private fun getStylesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:docDefaults>
    <w:rPrDefault>
      <w:rPr>
        <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/>
        <w:sz w:val="24"/>
        <w:color w:val="1A1A1A"/>
      </w:rPr>
    </w:rPrDefault>
  </w:docDefaults>
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
    <w:name w:val="Normal"/>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading1">
    <w:name w:val="heading 1"/>
    <w:rPr>
      <w:b/>
      <w:sz w:val="36"/>
      <w:color w:val="1565C0"/>
    </w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading2">
    <w:name w:val="heading 2"/>
    <w:rPr>
      <w:b/>
      <w:sz w:val="28"/>
      <w:color w:val="1976D2"/>
    </w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading3">
    <w:name w:val="heading 3"/>
    <w:rPr>
      <w:b/>
      <w:sz w:val="24"/>
      <w:color w:val="1E88E5"/>
    </w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Title">
    <w:name w:val="Title"/>
    <w:rPr>
      <w:b/>
      <w:sz w:val="48"/>
      <w:color w:val="0F4C81"/>
    </w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Subtitle">
    <w:name w:val="Subtitle"/>
    <w:rPr>
      <w:i/>
      <w:sz w:val="28"/>
      <w:color w:val="546E7A"/>
    </w:rPr>
  </w:style>
</w:styles>"""
    }

    private fun buildDocumentXml(doc: Document): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
""")

        for (block in doc.blocks) {
            when (block) {
                is ParagraphBlock -> {
                    sb.append(renderParagraphToXml(block))
                }
                is TableBlock -> {
                    sb.append(renderTableToXml(block))
                }
                is HorizontalRuleBlock -> {
                    sb.append("""    <w:p>
      <w:pPr>
        <w:pBdr>
          <w:bottom w:val="single" w:sz="12" w:space="1" w:color="CCCCCC"/>
        </w:pBdr>
      </w:pPr>
    </w:p>
""")
                }
                is PageBreakBlock -> {
                    sb.append("""    <w:p>
      <w:r>
        <w:br w:type="page"/>
      </w:r>
    </w:p>
""")
                }
                is ImageBlock -> {
                    if (block.caption.isNotBlank()) {
                        sb.append("""    <w:p>
      <w:pPr><w:jc w:val="center"/></w:pPr>
      <w:r><w:rPr><w:i/><w:color w:val="666666"/></w:rPr><w:t>[Image: ${escapeXml(block.caption)}]</w:t></w:r>
    </w:p>
""")
                    }
                }
            }
        }

        // Section properties (standard Letter/A4 portrait page dimensions & 1-inch margins)
        sb.append("""    <w:sectPr>
      <w:pgSz w:w="12240" w:h="15840"/>
      <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/>
    </w:sectPr>
  </w:body>
</w:document>""")

        return sb.toString()
    }

    private fun renderParagraphToXml(p: ParagraphBlock): String {
        val sb = StringBuilder()
        sb.append("    <w:p>\n")
        sb.append("      <w:pPr>\n")

        // Style
        val styleId = when (p.headingLevel) {
            HeadingLevel.TITLE -> "Title"
            HeadingLevel.SUBTITLE -> "Subtitle"
            HeadingLevel.HEADING_1 -> "Heading1"
            HeadingLevel.HEADING_2 -> "Heading2"
            HeadingLevel.HEADING_3 -> "Heading3"
            HeadingLevel.NORMAL -> "Normal"
        }
        sb.append("""        <w:pStyle w:val="$styleId"/>""").append("\n")

        // Alignment
        val jc = when (p.alignment) {
            DocAlignment.CENTER -> "center"
            DocAlignment.RIGHT -> "right"
            DocAlignment.JUSTIFY -> "both"
            DocAlignment.LEFT -> "left"
        }
        sb.append("""        <w:jc w:val="$jc"/>""").append("\n")

        // Indentation
        val indentTwips = (p.indentLevel * 720).coerceAtLeast(0)
        if (indentTwips > 0) {
            sb.append("""        <w:ind w:left="$indentTwips"/>""").append("\n")
        }

        // Line spacing
        val lineSpacingTwips = (p.lineSpacingMultiplier * 240).toInt()
        sb.append("""        <w:spacing w:line="$lineSpacingTwips" w:lineRule="auto" w:after="160"/>""").append("\n")

        sb.append("      </w:pPr>\n")

        // Prefix for lists
        val prefix = when (p.listType) {
            DocListType.BULLET -> "•   "
            DocListType.NUMBER -> "1.  "
            DocListType.CHECKLIST -> if (p.isChecked) "☑   " else "☐   "
            DocListType.NONE -> ""
        }

        val fullText = prefix + p.text
        val cleanColor = p.textColorHex.removePrefix("#")

        sb.append("      <w:r>\n")
        sb.append("        <w:rPr>\n")
        if (p.isBold || p.headingLevel.isBold) sb.append("          <w:b/>\n")
        if (p.isItalic) sb.append("          <w:i/>\n")
        if (p.isUnderline) sb.append("""          <w:u w:val="single"/>""").append("\n")
        if (p.isStrike) sb.append("          <w:strike/>\n")
        if (cleanColor.length == 6) {
            sb.append("""          <w:color w:val="$cleanColor"/>""").append("\n")
        }
        val halfPoints = (p.fontSizeSp * 2).toInt()
        sb.append("""          <w:sz w:val="$halfPoints"/>""").append("\n")
        sb.append("        </w:rPr>\n")

        val lines = fullText.split("\n")
        for (i in lines.indices) {
            sb.append("""        <w:t xml:space="preserve">${escapeXml(lines[i])}</w:t>""").append("\n")
            if (i < lines.size - 1) {
                sb.append("        <w:br/>\n")
            }
        }
        sb.append("      </w:r>\n")
        sb.append("    </w:p>\n")
        return sb.toString()
    }

    private fun renderTableToXml(table: TableBlock): String {
        val sb = StringBuilder()
        sb.append("""    <w:tbl>
      <w:tblPr>
        <w:tblW w:w="0" w:type="auto"/>
        <w:tblBorders>
          <w:top w:val="single" w:sz="6" w:space="0" w:color="CCCCCC"/>
          <w:left w:val="single" w:sz="6" w:space="0" w:color="CCCCCC"/>
          <w:bottom w:val="single" w:sz="6" w:space="0" w:color="CCCCCC"/>
          <w:right w:val="single" w:sz="6" w:space="0" w:color="CCCCCC"/>
          <w:insideH w:val="single" w:sz="4" w:space="0" w:color="E0E0E0"/>
          <w:insideV w:val="single" w:sz="4" w:space="0" w:color="E0E0E0"/>
        </w:tblBorders>
      </w:tblPr>
""")

        for (rowIndex in table.cells.indices) {
            val row = table.cells[rowIndex]
            val isHeader = (rowIndex == 0)
            sb.append("      <w:tr>\n")
            for (cellText in row) {
                sb.append("        <w:tc>\n")
                sb.append("          <w:tcPr>\n")
                sb.append("""            <w:tcW w:w="2400" w:type="dxa"/>""").append("\n")
                if (isHeader) {
                    sb.append("""            <w:shd w:val="clear" w:color="auto" w:fill="F0F4F8"/>""").append("\n")
                }
                sb.append("          </w:tcPr>\n")
                sb.append("          <w:p>\n")
                sb.append("            <w:r>\n")
                if (isHeader) {
                    sb.append("              <w:rPr><w:b/><w:color w:val=\"0F4C81\"/></w:rPr>\n")
                }
                sb.append("""              <w:t>${escapeXml(cellText)}</w:t>""").append("\n")
                sb.append("            </w:r>\n")
                sb.append("          </w:p>\n")
                sb.append("        </w:tc>\n")
            }
            sb.append("      </w:tr>\n")
        }

        sb.append("    </w:tbl>\n")
        return sb.toString()
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
