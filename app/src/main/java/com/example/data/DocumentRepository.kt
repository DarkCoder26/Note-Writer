package com.example.data

import com.example.model.DocAlignment
import com.example.model.DocBlock
import com.example.model.DocFontFamily
import com.example.model.DocListType
import com.example.model.Document
import com.example.model.DocumentSerializer
import com.example.model.ExportFormat
import com.example.model.HeadingLevel
import com.example.model.HorizontalRuleBlock
import com.example.model.ParagraphBlock
import com.example.model.TableBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DocumentRepository(private val dao: DocumentDao) {

    val allDocuments: Flow<List<DocumentEntity>> = dao.getAllDocuments()

    suspend fun loadDocument(id: Long): Document? = withContext(Dispatchers.IO) {
        val entity = dao.getDocumentById(id) ?: return@withContext null
        DocumentSerializer.deserialize(entity.contentJson).copy(
            id = entity.id,
            title = entity.title,
            format = try { ExportFormat.valueOf(entity.format) } catch (e: Exception) { ExportFormat.DOCX },
            isModified = false,
            lastSavedMillis = entity.lastModified
        )
    }

    suspend fun saveDocument(doc: Document): Long = withContext(Dispatchers.IO) {
        val json = DocumentSerializer.serialize(doc)
        val plainText = extractPlainText(doc)
        val wordCount = calculateWordCount(plainText)
        val charCount = plainText.length

        val entity = DocumentEntity(
            id = if (doc.id > 0) doc.id else 0L,
            title = doc.title,
            format = doc.format.name,
            contentJson = json,
            plainTextPreview = plainText.take(150),
            wordCount = wordCount,
            characterCount = charCount,
            lastModified = System.currentTimeMillis()
        )

        val insertedId = dao.insert(entity)
        insertedId
    }

    suspend fun deleteDocument(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    fun extractPlainText(doc: Document): String {
        val sb = StringBuilder()
        for (block in doc.blocks) {
            when (block) {
                is ParagraphBlock -> {
                    if (block.text.isNotBlank()) {
                        sb.append(block.text).append("\n")
                    }
                }
                is TableBlock -> {
                    for (row in block.cells) {
                        sb.append(row.joinToString(" | ")).append("\n")
                    }
                }
                is HorizontalRuleBlock -> sb.append("---\n")
                else -> {}
            }
        }
        return sb.toString().trim()
    }

    fun calculateWordCount(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split("\\s+".toRegex()).count { it.isNotBlank() }
    }

    fun createBlankDocument(title: String = "Untitled Document"): Document {
        return Document(
            id = 0L,
            title = title,
            format = ExportFormat.DOCX,
            blocks = listOf(
                ParagraphBlock(
                    text = "",
                    headingLevel = HeadingLevel.NORMAL,
                    alignment = DocAlignment.LEFT
                )
            ),
            isModified = false,
            lastSavedMillis = System.currentTimeMillis()
        )
    }

    fun createDefaultDocument(): Document {
        val blocks = listOf<DocBlock>(
            ParagraphBlock(
                text = "Quarterly Business Review & Strategy",
                headingLevel = HeadingLevel.TITLE,
                alignment = DocAlignment.CENTER,
                isBold = true,
                textColorHex = "#0F4C81"
            ),
            ParagraphBlock(
                text = "Prepared for Executive Leadership • Q3 Performance & Growth Outlook",
                headingLevel = HeadingLevel.SUBTITLE,
                alignment = DocAlignment.CENTER,
                isItalic = true,
                textColorHex = "#546E7A"
            ),
            HorizontalRuleBlock(),
            ParagraphBlock(
                text = "Executive Summary",
                headingLevel = HeadingLevel.HEADING_1,
                alignment = DocAlignment.LEFT,
                isBold = true,
                textColorHex = "#1565C0"
            ),
            ParagraphBlock(
                text = "This document highlights our key deliverables, operational milestones, and financial metrics for the quarter. We observed significant progress across all enterprise initiatives, surpassing revenue targets by 18% while optimizing operational overhead.",
                headingLevel = HeadingLevel.NORMAL,
                alignment = DocAlignment.LEFT,
                lineSpacingMultiplier = 1.25f
            ),
            ParagraphBlock(
                text = "Strategic Milestones Achieved",
                headingLevel = HeadingLevel.HEADING_2,
                alignment = DocAlignment.LEFT,
                isBold = true,
                textColorHex = "#1976D2"
            ),
            ParagraphBlock(
                text = "Expanded client onboarding velocity by 34% through automated pipelines.",
                headingLevel = HeadingLevel.NORMAL,
                listType = DocListType.BULLET,
                indentLevel = 0
            ),
            ParagraphBlock(
                text = "Delivered multi-region cloud deployment with 99.99% uptime SLA compliance.",
                headingLevel = HeadingLevel.NORMAL,
                listType = DocListType.BULLET,
                indentLevel = 0
            ),
            ParagraphBlock(
                text = "Implemented end-to-end data security standards and enterprise audits.",
                headingLevel = HeadingLevel.NORMAL,
                listType = DocListType.BULLET,
                indentLevel = 0
            ),
            ParagraphBlock(
                text = "Financial Highlights & Budget Breakdown",
                headingLevel = HeadingLevel.HEADING_2,
                alignment = DocAlignment.LEFT,
                isBold = true,
                textColorHex = "#1976D2"
            ),
            TableBlock(
                rows = 4,
                cols = 4,
                cells = listOf(
                    listOf("Department", "Projected", "Actual Spend", "Variance"),
                    listOf("Product Engineering", "$420,000", "$395,000", "+5.9%"),
                    listOf("Design & Research", "$150,000", "$142,000", "+5.3%"),
                    listOf("Infrastructure & Ops", "$210,000", "$198,000", "+5.7%")
                )
            ),
            ParagraphBlock(
                text = "Action Items for Next Review",
                headingLevel = HeadingLevel.HEADING_2,
                alignment = DocAlignment.LEFT,
                isBold = true,
                textColorHex = "#1976D2"
            ),
            ParagraphBlock(
                text = "Finalize vendor contracts for secondary failover data centers.",
                headingLevel = HeadingLevel.NORMAL,
                listType = DocListType.CHECKLIST,
                isChecked = true
            ),
            ParagraphBlock(
                text = "Complete annual compliance certification and stakeholder signoff.",
                headingLevel = HeadingLevel.NORMAL,
                listType = DocListType.CHECKLIST,
                isChecked = false
            ),
            ParagraphBlock(
                text = "Publish comprehensive developer SDK and documentation portal.",
                headingLevel = HeadingLevel.NORMAL,
                listType = DocListType.CHECKLIST,
                isChecked = false
            )
        )

        return Document(
            id = 0L,
            title = "Quarterly Business Review",
            format = ExportFormat.DOCX,
            blocks = blocks,
            isModified = false,
            lastSavedMillis = System.currentTimeMillis()
        )
    }

    fun createTemplate(templateType: String): Document {
        return when (templateType) {
            "letter" -> Document(
                title = "Business Letter",
                format = ExportFormat.DOCX,
                blocks = listOf(
                    ParagraphBlock(text = "ACME GLOBAL SOLUTIONS", headingLevel = HeadingLevel.TITLE, isBold = true, textColorHex = "#0F4C81"),
                    ParagraphBlock(text = "100 Innovation Blvd • Silicon Valley, CA 94025 • contact@acme.com", headingLevel = HeadingLevel.SUBTITLE, textColorHex = "#666666"),
                    HorizontalRuleBlock(),
                    ParagraphBlock(text = "September 4, 2026\n\nDear Valued Partner,", headingLevel = HeadingLevel.NORMAL),
                    ParagraphBlock(text = "We are pleased to share our collaborative roadmap for the upcoming fiscal cycle. Thank you for your continued partnership and shared dedication to quality.", headingLevel = HeadingLevel.NORMAL),
                    ParagraphBlock(text = "Sincerely,\nExecutive Office\nACME Global Solutions", headingLevel = HeadingLevel.NORMAL, isItalic = true)
                )
            )
            "minutes" -> Document(
                title = "Meeting Minutes",
                format = ExportFormat.DOCX,
                blocks = listOf(
                    ParagraphBlock(text = "Project Sync Meeting Minutes", headingLevel = HeadingLevel.TITLE, isBold = true, textColorHex = "#0F4C81"),
                    ParagraphBlock(text = "Date: September 4, 2026 | Attendees: Alex, Jordan, Sam, Taylor", headingLevel = HeadingLevel.SUBTITLE, isItalic = true),
                    HorizontalRuleBlock(),
                    ParagraphBlock(text = "Agenda Topics", headingLevel = HeadingLevel.HEADING_2, isBold = true),
                    ParagraphBlock(text = "1. Architecture roadmap review", listType = DocListType.NUMBER),
                    ParagraphBlock(text = "2. Cross-platform testing milestones", listType = DocListType.NUMBER),
                    ParagraphBlock(text = "3. Q4 deployment targets", listType = DocListType.NUMBER),
                    ParagraphBlock(text = "Discussion Summary", headingLevel = HeadingLevel.HEADING_2, isBold = true),
                    ParagraphBlock(text = "Team reviewed progress on document export modules. DOCX and PDF generation pipeline validated successfully.", headingLevel = HeadingLevel.NORMAL)
                )
            )
            "resume" -> Document(
                title = "Professional Resume",
                format = ExportFormat.DOCX,
                blocks = listOf(
                    ParagraphBlock(text = "ALEX MORGAN", headingLevel = HeadingLevel.TITLE, alignment = DocAlignment.CENTER, isBold = true, textColorHex = "#0F4C81"),
                    ParagraphBlock(text = "Principal Software Architect • San Francisco, CA • alex.morgan@email.com", headingLevel = HeadingLevel.SUBTITLE, alignment = DocAlignment.CENTER, textColorHex = "#666666"),
                    HorizontalRuleBlock(),
                    ParagraphBlock(text = "Professional Experience", headingLevel = HeadingLevel.HEADING_2, isBold = true, textColorHex = "#1565C0"),
                    ParagraphBlock(text = "Senior Technical Lead — Cloud Systems (2022 - Present)", headingLevel = HeadingLevel.HEADING_3, isBold = true),
                    ParagraphBlock(text = "Architected high-scale document processing microservices supporting millions of active enterprise users worldwide.", listType = DocListType.BULLET),
                    ParagraphBlock(text = "Led cross-functional team of 14 engineers in delivering real-time collaborative editing features.", listType = DocListType.BULLET),
                    ParagraphBlock(text = "Education & Certifications", headingLevel = HeadingLevel.HEADING_2, isBold = true, textColorHex = "#1565C0"),
                    ParagraphBlock(text = "B.S. in Computer Science — Stanford University (2018)", headingLevel = HeadingLevel.NORMAL)
                )
            )
            else -> createBlankDocument()
        }
    }
}
