package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.export.DocxExporter
import com.example.export.HtmlExporter
import com.example.export.MarkdownExporter
import com.example.export.TxtExporter
import com.example.model.DocAlignment
import com.example.model.Document
import com.example.model.DocumentSerializer
import com.example.model.ExportFormat
import com.example.model.HeadingLevel
import com.example.model.ParagraphBlock
import com.example.model.TableBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DocEditor", appName)
    }

    @Test
    fun `test document serialization and deserialization`() {
        val doc = Document(
            title = "Test QBR",
            format = ExportFormat.DOCX,
            blocks = listOf(
                ParagraphBlock(text = "Hello World", headingLevel = HeadingLevel.TITLE, isBold = true),
                TableBlock(rows = 2, cols = 2, cells = listOf(listOf("A", "B"), listOf("C", "D")))
            )
        )

        val json = DocumentSerializer.serialize(doc)
        assertTrue(json.contains("Test QBR"))
        assertTrue(json.contains("Hello World"))

        val deserialized = DocumentSerializer.deserialize(json)
        assertEquals("Test QBR", deserialized.title)
        assertEquals(2, deserialized.blocks.size)
        assertTrue(deserialized.blocks[0] is ParagraphBlock)
        assertEquals("Hello World", (deserialized.blocks[0] as ParagraphBlock).text)
    }

    @Test
    fun `test docx export generates valid zip structure`() {
        val doc = Document(
            title = "Export Test",
            format = ExportFormat.DOCX,
            blocks = listOf(
                ParagraphBlock(text = "Export Paragraph", headingLevel = HeadingLevel.NORMAL)
            )
        )
        val bytes = DocxExporter.exportToBytes(doc)
        assertTrue(bytes.isNotEmpty())
        // Standard ZIP magic bytes: PK (0x50, 0x4B)
        assertEquals(0x50.toByte(), bytes[0])
        assertEquals(0x4B.toByte(), bytes[1])
    }

    @Test
    fun `test html export contains formatted html`() {
        val doc = Document(
            title = "HTML Test",
            blocks = listOf(
                ParagraphBlock(text = "Sample Heading", headingLevel = HeadingLevel.HEADING_1, isBold = true)
            )
        )
        val html = HtmlExporter.export(doc)
        assertTrue(html.contains("<html"))
        assertTrue(html.contains("Sample Heading"))
    }

    @Test
    fun `test txt and markdown export`() {
        val doc = Document(
            title = "Markdown Test",
            blocks = listOf(
                ParagraphBlock(text = "Section Title", headingLevel = HeadingLevel.HEADING_1),
                ParagraphBlock(text = "Bold Text", isBold = true)
            )
        )
        val md = MarkdownExporter.export(doc)
        assertTrue(md.contains("## Section Title"))
        assertTrue(md.contains("**Bold Text**"))

        val txt = TxtExporter.export(doc)
        assertTrue(txt.contains("MARKDOWN TEST"))
        assertTrue(txt.contains("Section Title"))
    }

    @Test
    fun `test import plain text`() {
        val raw = "My First Document\n\nThis is paragraph one.\n\nThis is paragraph two."
        val stream = raw.byteInputStream(Charsets.UTF_8)
        val doc = com.example.export.ImportManager.parsePlainText("TestFile.txt", stream)

        assertEquals("TestFile", doc.title)
        assertEquals(3, doc.blocks.size)
        assertTrue(doc.blocks[0] is ParagraphBlock)
        assertEquals("My First Document", (doc.blocks[0] as ParagraphBlock).text)
    }

    @Test
    fun `test import markdown`() {
        val raw = "# Main Title\n\n## Section 1\n\n- Bullet item\n- [x] Done task\n- [ ] Pending task"
        val stream = raw.byteInputStream(Charsets.UTF_8)
        val doc = com.example.export.ImportManager.parseMarkdown("Notes.md", stream)

        assertEquals("Notes", doc.title)
        assertEquals(5, doc.blocks.size)
        val titleBlock = doc.blocks[0] as ParagraphBlock
        assertEquals("Main Title", titleBlock.text)
        assertEquals(HeadingLevel.TITLE, titleBlock.headingLevel)

        val taskBlock = doc.blocks[3] as ParagraphBlock
        assertEquals(com.example.model.DocListType.CHECKLIST, taskBlock.listType)
        assertTrue(taskBlock.isChecked)
    }

    @Test
    fun `test docx export and import round trip`() {
        val original = Document(
            title = "RoundTripDoc",
            blocks = listOf(
                ParagraphBlock(text = "Executive Summary", headingLevel = HeadingLevel.TITLE),
                ParagraphBlock(text = "This is a high priority project.", isBold = true)
            )
        )
        val docxBytes = DocxExporter.exportToBytes(original)
        val imported = com.example.export.ImportManager.parseDocx("RoundTripDoc.docx", docxBytes.inputStream())

        assertEquals("RoundTripDoc", imported.title)
        assertTrue(imported.blocks.isNotEmpty())
        val firstP = imported.blocks.first() as ParagraphBlock
        assertEquals("Executive Summary", firstP.text)
    }
}

