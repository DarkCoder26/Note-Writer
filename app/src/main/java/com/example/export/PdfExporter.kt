package com.example.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
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

object PdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_LEFT = 45f
    private const val MARGIN_RIGHT = 45f
    private const val MARGIN_TOP = 50f
    private const val MARGIN_BOTTOM = 50f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT

    fun export(document: Document, outputStream: OutputStream) {
        val pdfDoc = PdfDocument()

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        val tableBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F4F6F8")
            style = Paint.Style.FILL
        }

        var pageNumber = 1
        var currentPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var currentPage = pdfDoc.startPage(currentPageInfo)
        var canvas = currentPage.canvas
        var cursorY = MARGIN_TOP

        fun advanceToNextPage() {
            // Draw page footer
            drawFooter(canvas, pageNumber)
            pdfDoc.finishPage(currentPage)
            pageNumber++
            currentPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            currentPage = pdfDoc.startPage(currentPageInfo)
            canvas = currentPage.canvas
            cursorY = MARGIN_TOP
        }

        for (block in document.blocks) {
            when (block) {
                is PageBreakBlock -> {
                    advanceToNextPage()
                }
                is HorizontalRuleBlock -> {
                    if (cursorY + 20 > PAGE_HEIGHT - MARGIN_BOTTOM) {
                        advanceToNextPage()
                    }
                    cursorY += 10f
                    canvas.drawLine(MARGIN_LEFT, cursorY, PAGE_WIDTH - MARGIN_RIGHT, cursorY, linePaint)
                    cursorY += 15f
                }
                is ParagraphBlock -> {
                    // Configure text paint
                    val fontSize = when (block.headingLevel) {
                        HeadingLevel.TITLE -> 22f
                        HeadingLevel.SUBTITLE -> 14f
                        HeadingLevel.HEADING_1 -> 18f
                        HeadingLevel.HEADING_2 -> 15f
                        HeadingLevel.HEADING_3 -> 13f
                        HeadingLevel.NORMAL -> block.fontSizeSp.coerceIn(9f, 24f)
                    }

                    val isBold = block.isBold || block.headingLevel.isBold
                    val tfStyle = when {
                        isBold && block.isItalic -> Typeface.BOLD_ITALIC
                        isBold -> Typeface.BOLD
                        block.isItalic -> Typeface.ITALIC
                        else -> Typeface.NORMAL
                    }

                    textPaint.textSize = fontSize
                    textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, tfStyle)
                    textPaint.color = try {
                        Color.parseColor(block.textColorHex)
                    } catch (e: Exception) {
                        Color.DKGRAY
                    }
                    textPaint.isUnderlineText = block.isUnderline
                    textPaint.isStrikeThruText = block.isStrike

                    val prefix = when (block.listType) {
                        DocListType.BULLET -> "• "
                        DocListType.NUMBER -> "1. "
                        DocListType.CHECKLIST -> if (block.isChecked) "[✓] " else "[  ] "
                        DocListType.NONE -> ""
                    }

                    val fullText = prefix + block.text
                    val indent = (block.indentLevel * 18f).coerceAtLeast(0f)
                    val availableWidth = CONTENT_WIDTH - indent

                    // Word wrap
                    val lines = wrapText(fullText, textPaint, availableWidth)
                    val lineHeight = fontSize * (block.lineSpacingMultiplier.coerceIn(1.0f, 2.0f)) + 3f

                    for (line in lines) {
                        if (cursorY + lineHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                            advanceToNextPage()
                        }

                        val lineX = when (block.alignment) {
                            DocAlignment.CENTER -> {
                                val textWidth = textPaint.measureText(line)
                                MARGIN_LEFT + indent + (availableWidth - textWidth) / 2f
                            }
                            DocAlignment.RIGHT -> {
                                val textWidth = textPaint.measureText(line)
                                PAGE_WIDTH - MARGIN_RIGHT - textWidth
                            }
                            DocAlignment.LEFT, DocAlignment.JUSTIFY -> {
                                MARGIN_LEFT + indent
                            }
                        }

                        // If highlight
                        if (block.highlightColorHex != null) {
                            val hlPaint = Paint().apply {
                                color = try { Color.parseColor(block.highlightColorHex) } catch (e: Exception) { Color.YELLOW }
                                style = Paint.Style.FILL
                            }
                            val textWidth = textPaint.measureText(line)
                            canvas.drawRect(lineX - 2, cursorY - fontSize + 2, lineX + textWidth + 2, cursorY + 4, hlPaint)
                        }

                        canvas.drawText(line, lineX, cursorY, textPaint)
                        cursorY += lineHeight
                    }
                    cursorY += 6f // paragraph gap
                }
                is TableBlock -> {
                    val colCount = block.cols.coerceAtLeast(1)
                    val colWidth = CONTENT_WIDTH / colCount
                    val cellPadding = 6f
                    val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 10f
                        color = Color.DKGRAY
                    }
                    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 10f
                        typeface = Typeface.DEFAULT_BOLD
                        color = Color.parseColor("#0F4C81")
                    }

                    val estimatedRowHeight = 22f
                    for ((rIdx, row) in block.cells.withIndex()) {
                        if (cursorY + estimatedRowHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                            advanceToNextPage()
                        }

                        val isHeader = (rIdx == 0)
                        if (isHeader) {
                            canvas.drawRect(
                                MARGIN_LEFT,
                                cursorY,
                                PAGE_WIDTH - MARGIN_RIGHT,
                                cursorY + estimatedRowHeight,
                                tableBgPaint
                            )
                        }

                        // Draw borders
                        canvas.drawRect(
                            MARGIN_LEFT,
                            cursorY,
                            PAGE_WIDTH - MARGIN_RIGHT,
                            cursorY + estimatedRowHeight,
                            Paint().apply {
                                color = Color.LTGRAY
                                style = Paint.Style.STROKE
                                strokeWidth = 0.8f
                            }
                        )

                        for (cIdx in 0 until colCount) {
                            val cellText = if (cIdx < row.size) row[cIdx] else ""
                            val cellX = MARGIN_LEFT + (cIdx * colWidth)
                            val p = if (isHeader) headerPaint else cellPaint
                            val clippedText = if (p.measureText(cellText) > colWidth - (cellPadding * 2)) {
                                clipText(cellText, p, colWidth - (cellPadding * 2))
                            } else {
                                cellText
                            }
                            canvas.drawText(clippedText, cellX + cellPadding, cursorY + 15f, p)

                            // Vertical border
                            if (cIdx > 0) {
                                canvas.drawLine(cellX, cursorY, cellX, cursorY + estimatedRowHeight, linePaint)
                            }
                        }
                        cursorY += estimatedRowHeight
                    }
                    cursorY += 12f
                }
                is ImageBlock -> {
                    if (cursorY + 30 > PAGE_HEIGHT - MARGIN_BOTTOM) {
                        advanceToNextPage()
                    }
                    val imgBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#E8ECEF")
                        style = Paint.Style.FILL
                    }
                    val imgH = 60f
                    val imgW = CONTENT_WIDTH * block.widthPercent.coerceIn(0.3f, 1.0f)
                    val imgX = MARGIN_LEFT + (CONTENT_WIDTH - imgW) / 2f
                    canvas.drawRect(imgX, cursorY, imgX + imgW, cursorY + imgH, imgBoxPaint)

                    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.GRAY
                        textSize = 10f
                        textAlign = Paint.Align.CENTER
                    }
                    val label = if (block.caption.isNotBlank()) "🖼 " + block.caption else "🖼 [Image Asset]"
                    canvas.drawText(label, imgX + imgW / 2f, cursorY + 35f, labelPaint)
                    cursorY += imgH + 12f
                }
            }
        }

        // Final footer
        drawFooter(canvas, pageNumber)
        pdfDoc.finishPage(currentPage)

        pdfDoc.writeTo(outputStream)
        pdfDoc.close()
    }

    fun exportToBytes(document: Document): ByteArray {
        val baos = ByteArrayOutputStream()
        export(document, baos)
        return baos.toByteArray()
    }

    private fun drawFooter(canvas: Canvas, page: Int) {
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            textSize = 9f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Page $page", PAGE_WIDTH - MARGIN_RIGHT, PAGE_HEIGHT - 20f, footerPaint)

        val leftFooterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            textSize = 9f
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("DocEditor", MARGIN_LEFT, PAGE_HEIGHT - 20f, leftFooterPaint)
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        val paragraphs = text.split("\n")
        for (para in paragraphs) {
            if (para.isEmpty()) {
                lines.add("")
                continue
            }
            val words = para.split(" ")
            var currentLine = StringBuilder()
            for (word in words) {
                val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    currentLine = StringBuilder(candidate)
                } else {
                    if (currentLine.isNotEmpty()) {
                        lines.add(currentLine.toString())
                    }
                    currentLine = StringBuilder(word)
                }
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
            }
        }
        return lines
    }

    private fun clipText(text: String, paint: Paint, maxWidth: Float): String {
        var clipped = text
        while (clipped.isNotEmpty() && paint.measureText("$clipped...") > maxWidth) {
            clipped = clipped.dropLast(1)
        }
        return if (clipped.length < text.length) "$clipped..." else text
    }
}
