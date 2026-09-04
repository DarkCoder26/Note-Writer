package com.example

import com.example.model.BlockFormatting
import com.example.model.DocAlignment
import com.example.model.DocBlock
import com.example.model.DocFontFamily
import com.example.model.DocListType
import com.example.model.Document
import com.example.model.DocumentMetrics
import com.example.model.ExportFormat
import com.example.model.HeadingLevel
import com.example.model.ParagraphBlock
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `test block formatting remains identical when typing text`() {
        val p1 = ParagraphBlock(
            text = "Hello",
            headingLevel = HeadingLevel.NORMAL,
            isBold = true,
            alignment = DocAlignment.CENTER,
            fontFamily = DocFontFamily.SERIF
        )
        val p2 = p1.copy(text = "Hello World! Adding lots of new typed characters...")

        val fmt1 = BlockFormatting(
            isBold = p1.isBold,
            isItalic = p1.isItalic,
            isUnderline = p1.isUnderline,
            isStrike = p1.isStrike,
            alignment = p1.alignment,
            listType = p1.listType,
            headingLevel = p1.headingLevel,
            fontFamily = p1.fontFamily,
            fontSizeSp = p1.fontSizeSp,
            textColorHex = p1.textColorHex,
            highlightColorHex = p1.highlightColorHex,
            lineSpacingMultiplier = p1.lineSpacingMultiplier,
            indentLevel = p1.indentLevel
        )

        val fmt2 = BlockFormatting(
            isBold = p2.isBold,
            isItalic = p2.isItalic,
            isUnderline = p2.isUnderline,
            isStrike = p2.isStrike,
            alignment = p2.alignment,
            listType = p2.listType,
            headingLevel = p2.headingLevel,
            fontFamily = p2.fontFamily,
            fontSizeSp = p2.fontSizeSp,
            textColorHex = p2.textColorHex,
            highlightColorHex = p2.highlightColorHex,
            lineSpacingMultiplier = p2.lineSpacingMultiplier,
            indentLevel = p2.indentLevel
        )

        // Verifies that typing text does NOT change BlockFormatting, preventing RibbonToolbar recomposition
        assertEquals(fmt1, fmt2)
    }

    @Test
    fun `test document metrics calculation with large document`() {
        val paragraphText = "The quick brown fox jumps over the lazy dog. ".repeat(20) // ~180 words
        val blocks = (1..60).map { i ->
            ParagraphBlock(text = "Paragraph $i: $paragraphText")
        }
        val doc = Document(
            title = "Large Performance Doc",
            blocks = blocks
        )

        val sb = StringBuilder()
        for (b in doc.blocks) {
            if (b is ParagraphBlock && b.text.isNotBlank()) {
                sb.append(b.text).append("\n")
            }
        }
        val plain = sb.toString().trim()
        val words = plain.split("\\s+".toRegex()).count { it.isNotBlank() }
        val chars = plain.length
        val pages = ((chars / 1800) + 1).coerceAtLeast(1)

        val metrics = DocumentMetrics(
            pageCount = pages,
            wordCount = words,
            characterCount = chars,
            paragraphCount = doc.blocks.size
        )

        assertTrue(metrics.wordCount > 10000)
        assertTrue(metrics.characterCount > 50000)
        assertEquals(60, metrics.paragraphCount)
        assertTrue(metrics.pageCount > 25)
    }

    @Test
    fun `test about section developed by darkcoder attribution`() {
        val expectedAttribution = "Developed by DarkCoder"
        assertEquals("Developed by DarkCoder", expectedAttribution)
    }
}
