package com.example.export

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.model.Document
import com.example.model.ExportFormat
import java.io.File
import java.io.FileOutputStream

object ExportManager {

    fun exportDocument(context: Context, doc: Document, format: ExportFormat): File {
        val sanitizedTitle = doc.title.replace("[^a-zA-Z0-9._-]".toRegex(), "_").ifBlank { "document" }
        val fileName = "$sanitizedTitle.${format.extension}"

        // Save to app external cache or internal files docs directory
        val docsDir = File(context.cacheDir, "docs").apply { mkdirs() }
        val file = File(docsDir, fileName)

        when (format) {
            ExportFormat.DOCX -> {
                FileOutputStream(file).use { out ->
                    DocxExporter.export(doc, out)
                }
            }
            ExportFormat.PDF -> {
                FileOutputStream(file).use { out ->
                    PdfExporter.export(doc, out)
                }
            }
            ExportFormat.HTML -> {
                file.writeText(HtmlExporter.export(doc))
            }
            ExportFormat.TXT -> {
                file.writeText(TxtExporter.export(doc))
            }
            ExportFormat.MARKDOWN -> {
                file.writeText(MarkdownExporter.export(doc))
            }
        }
        return file
    }

    fun shareExportedFile(context: Context, file: File, format: ExportFormat, title: String) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share or Save '$title'")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun printDocument(context: Context, doc: Document) {
        // Generate PDF first
        val pdfFile = exportDocument(context, doc, ExportFormat.PDF)
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return

        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: android.os.Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = android.print.PrintDocumentInfo.Builder("${doc.title}.pdf")
                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: android.os.ParcelFileDescriptor?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (destination == null) return
                try {
                    val input = pdfFile.inputStream()
                    val output = FileOutputStream(destination.fileDescriptor)
                    input.copyTo(output)
                    input.close()
                    output.close()
                    callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }

        printManager.print(
            "Print ${doc.title}",
            printAdapter,
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build()
        )
    }
}
