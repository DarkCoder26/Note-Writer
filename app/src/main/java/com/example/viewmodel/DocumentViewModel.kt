package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DocumentEntity
import com.example.data.DocumentRepository
import com.example.export.ExportManager
import com.example.export.ImportManager
import com.example.model.BlockFormatting
import com.example.model.DocAlignment
import com.example.model.DocBlock
import com.example.model.DocFontFamily
import com.example.model.DocListType
import com.example.model.Document
import com.example.model.DocumentMetrics
import com.example.model.ExportFormat
import com.example.model.HeadingLevel
import com.example.model.HorizontalRuleBlock
import com.example.model.ImageBlock
import com.example.model.PageBreakBlock
import com.example.model.ParagraphBlock
import com.example.model.TableBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

enum class SaveStatus(val label: String) {
    SAVED("Saved"),
    SAVING("Saving..."),
    UNSAVED("Unsaved changes")
}

data class SearchMatch(
    val blockId: String,
    val startIndex: Int,
    val length: Int
)

class DocumentViewModel(
    private val repository: DocumentRepository
) : ViewModel() {

    private val _currentDoc = MutableStateFlow<Document>(repository.createDefaultDocument())
    val currentDoc: StateFlow<Document> = _currentDoc.asStateFlow()

    private val _saveStatus = MutableStateFlow(SaveStatus.SAVED)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    private val _activeBlockId = MutableStateFlow<String?>(null)
    val activeBlockId: StateFlow<String?> = _activeBlockId.asStateFlow()

    private val _zoomPercent = MutableStateFlow(100)
    val zoomPercent: StateFlow<Int> = _zoomPercent.asStateFlow()

    // Undo / Redo History
    private val undoStack = mutableListOf<Document>()
    private val redoStack = mutableListOf<Document>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Typing burst batching
    private var lastTypingBlockId: String? = null
    private var lastTypingTimestamp: Long = 0L

    // Metrics calculation (asynchronous & debounced)
    private var metricsJob: Job? = null
    private val _metrics = MutableStateFlow(DocumentMetrics(1, 0, 0))
    val metrics: StateFlow<DocumentMetrics> = _metrics.asStateFlow()

    // Active formatting for RibbonToolbar (stable reference while typing text)
    private val _activeFormatting = MutableStateFlow(BlockFormatting())
    val activeFormatting: StateFlow<BlockFormatting> = _activeFormatting.asStateFlow()

    // Non-blocking Export & Loading Progress Feedback
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()
    private val _exportMessage = MutableStateFlow("")
    val exportMessage: StateFlow<String> = _exportMessage.asStateFlow()

    // Auto-save Job
    private var autoSaveJob: Job? = null

    // Search & Replace
    private val _isSearchOpen = MutableStateFlow(false)
    val isSearchOpen: StateFlow<Boolean> = _isSearchOpen.asStateFlow()
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _replaceQuery = MutableStateFlow("")
    val replaceQuery: StateFlow<String> = _replaceQuery.asStateFlow()
    private val _matches = MutableStateFlow<List<SearchMatch>>(emptyList())
    val matches: StateFlow<List<SearchMatch>> = _matches.asStateFlow()
    private val _currentMatchIndex = MutableStateFlow(0)
    val currentMatchIndex: StateFlow<Int> = _currentMatchIndex.asStateFlow()

    // Recent Documents from Room
    val recentDocuments: StateFlow<List<DocumentEntity>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Feedback Message
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // Unsaved Changes Pending Action
    var pendingAction: (() -> Unit)? = null

    init {
        // Automatically set first block as active
        _currentDoc.value.blocks.firstOrNull()?.let {
            _activeBlockId.value = it.id
        }
        updateActiveFormatting()
        scheduleMetricsRecalculation(immediate = true)
    }

    private fun updateActiveFormatting() {
        val activeId = _activeBlockId.value
        val block = _currentDoc.value.blocks.firstOrNull { it.id == activeId }
        val newFormatting = if (block is ParagraphBlock) {
            BlockFormatting(
                isBold = block.isBold || block.headingLevel.isBold,
                isItalic = block.isItalic,
                isUnderline = block.isUnderline,
                isStrike = block.isStrike,
                alignment = block.alignment,
                listType = block.listType,
                headingLevel = block.headingLevel,
                fontFamily = block.fontFamily,
                fontSizeSp = block.fontSizeSp,
                textColorHex = block.textColorHex,
                highlightColorHex = block.highlightColorHex,
                lineSpacingMultiplier = block.lineSpacingMultiplier,
                indentLevel = block.indentLevel
            )
        } else {
            BlockFormatting()
        }
        if (_activeFormatting.value != newFormatting) {
            _activeFormatting.value = newFormatting
        }
    }

    private fun scheduleMetricsRecalculation(immediate: Boolean = false) {
        metricsJob?.cancel()
        metricsJob = viewModelScope.launch(Dispatchers.Default) {
            if (!immediate) {
                delay(450)
            }
            val doc = _currentDoc.value
            val plain = repository.extractPlainText(doc)
            val words = repository.calculateWordCount(plain)
            val chars = plain.length
            val charEstimate = (chars / 1800) + 1
            val pageBreaks = doc.blocks.count { it is PageBreakBlock }
            val pages = (charEstimate + pageBreaks).coerceAtLeast(1)
            val paragraphs = doc.blocks.count { it is ParagraphBlock }.coerceAtLeast(1)

            _metrics.value = DocumentMetrics(
                pageCount = pages,
                wordCount = words,
                characterCount = chars,
                paragraphCount = paragraphs
            )
        }
    }

    private fun pushHistory(previousDoc: Document) {
        undoStack.add(previousDoc)
        if (undoStack.size > 30) undoStack.removeAt(0)
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = false
    }

    private fun updateDocument(newDoc: Document) {
        lastTypingBlockId = null
        val modifiedDoc = newDoc.copy(isModified = true)
        pushHistory(_currentDoc.value)
        _currentDoc.value = modifiedDoc
        _saveStatus.value = SaveStatus.UNSAVED
        updateActiveFormatting()
        scheduleMetricsRecalculation(immediate = false)

        // Debounced auto-save
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1500)
            autoSave()
        }
    }

    private suspend fun autoSave() {
        _saveStatus.value = SaveStatus.SAVING
        try {
            val savedId = repository.saveDocument(_currentDoc.value)
            _currentDoc.value = _currentDoc.value.copy(id = savedId, isModified = false, lastSavedMillis = System.currentTimeMillis())
            _saveStatus.value = SaveStatus.SAVED
        } catch (e: Exception) {
            e.printStackTrace()
            _saveStatus.value = SaveStatus.UNSAVED
        }
    }

    fun setActiveBlock(blockId: String) {
        if (_activeBlockId.value != blockId) {
            _activeBlockId.value = blockId
            updateActiveFormatting()
        }
    }

    fun updateTitle(newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isNotBlank() && trimmed != _currentDoc.value.title) {
            updateDocument(_currentDoc.value.copy(title = trimmed))
        }
    }

    fun updateBlockText(blockId: String, newText: String) {
        val now = System.currentTimeMillis()
        val isNewBurst = blockId != lastTypingBlockId || (now - lastTypingTimestamp) > 1200L
        if (isNewBurst) {
            pushHistory(_currentDoc.value)
        }
        lastTypingBlockId = blockId
        lastTypingTimestamp = now

        val blocks = _currentDoc.value.blocks.map { block ->
            if (block.id == blockId && block is ParagraphBlock) {
                block.copy(text = newText)
            } else block
        }
        _currentDoc.value = _currentDoc.value.copy(blocks = blocks, isModified = true)
        _saveStatus.value = SaveStatus.UNSAVED

        // Debounced metrics update (no lag while typing)
        scheduleMetricsRecalculation(immediate = false)

        // Debounced auto-save (1500ms)
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1500)
            autoSave()
        }
    }

    fun addParagraphAfter(blockId: String?, defaultHeading: HeadingLevel = HeadingLevel.NORMAL) {
        val newBlock = ParagraphBlock(
            text = "",
            headingLevel = defaultHeading,
            alignment = DocAlignment.LEFT
        )
        val currentBlocks = _currentDoc.value.blocks.toMutableList()
        val index = if (blockId != null) {
            currentBlocks.indexOfFirst { it.id == blockId }.takeIf { it != -1 }?.plus(1) ?: currentBlocks.size
        } else {
            currentBlocks.size
        }
        currentBlocks.add(index, newBlock)
        updateDocument(_currentDoc.value.copy(blocks = currentBlocks))
        _activeBlockId.value = newBlock.id
    }

    fun deleteBlock(blockId: String) {
        if (_currentDoc.value.blocks.size <= 1) {
            // Keep at least one empty block
            updateDocument(_currentDoc.value.copy(blocks = listOf(ParagraphBlock())))
            return
        }
        val currentBlocks = _currentDoc.value.blocks.toMutableList()
        val removeIndex = currentBlocks.indexOfFirst { it.id == blockId }
        if (removeIndex != -1) {
            currentBlocks.removeAt(removeIndex)
            updateDocument(_currentDoc.value.copy(blocks = currentBlocks))
            val newActive = currentBlocks.getOrNull(removeIndex.coerceAtMost(currentBlocks.size - 1))
            _activeBlockId.value = newActive?.id
        }
    }

    // --- Formatting Controls on Active Paragraph ---

    private fun updateActiveParagraph(transform: (ParagraphBlock) -> ParagraphBlock) {
        val activeId = _activeBlockId.value ?: return
        val blocks = _currentDoc.value.blocks.map { block ->
            if (block.id == activeId && block is ParagraphBlock) {
                transform(block)
            } else block
        }
        updateDocument(_currentDoc.value.copy(blocks = blocks))
    }

    fun toggleBold() {
        updateActiveParagraph { it.copy(isBold = !it.isBold) }
    }

    fun toggleItalic() {
        updateActiveParagraph { it.copy(isItalic = !it.isItalic) }
    }

    fun toggleUnderline() {
        updateActiveParagraph { it.copy(isUnderline = !it.isUnderline) }
    }

    fun toggleStrike() {
        updateActiveParagraph { it.copy(isStrike = !it.isStrike) }
    }

    fun setHeadingLevel(level: HeadingLevel) {
        updateActiveParagraph {
            it.copy(
                headingLevel = level,
                fontSizeSp = level.fontSizeSp,
                isBold = level.isBold || it.isBold
            )
        }
    }

    fun setAlignment(alignment: DocAlignment) {
        updateActiveParagraph { it.copy(alignment = alignment) }
    }

    fun setListType(listType: DocListType) {
        updateActiveParagraph {
            val nextType = if (it.listType == listType) DocListType.NONE else listType
            it.copy(listType = nextType)
        }
    }

    fun toggleChecklist(blockId: String) {
        val blocks = _currentDoc.value.blocks.map { block ->
            if (block.id == blockId && block is ParagraphBlock) {
                block.copy(isChecked = !block.isChecked)
            } else block
        }
        updateDocument(_currentDoc.value.copy(blocks = blocks))
    }

    fun adjustIndent(delta: Int) {
        updateActiveParagraph {
            val newLevel = (it.indentLevel + delta).coerceIn(0, 5)
            it.copy(indentLevel = newLevel)
        }
    }

    fun setTextColor(colorHex: String) {
        updateActiveParagraph { it.copy(textColorHex = colorHex) }
    }

    fun setHighlightColor(colorHex: String?) {
        updateActiveParagraph { it.copy(highlightColorHex = colorHex) }
    }

    fun setFontSize(sizeSp: Float) {
        updateActiveParagraph { it.copy(fontSizeSp = sizeSp) }
    }

    fun changeFontSizeBy(delta: Float) {
        updateActiveParagraph {
            val newSize = (it.fontSizeSp + delta).coerceIn(9f, 48f)
            it.copy(fontSizeSp = newSize)
        }
    }

    fun setFontFamily(font: DocFontFamily) {
        updateActiveParagraph { it.copy(fontFamily = font) }
    }

    fun setLineSpacing(spacing: Float) {
        updateActiveParagraph { it.copy(lineSpacingMultiplier = spacing) }
    }

    fun clearFormatting() {
        updateActiveParagraph {
            it.copy(
                headingLevel = HeadingLevel.NORMAL,
                isBold = false,
                isItalic = false,
                isUnderline = false,
                isStrike = false,
                textColorHex = "#1A1A1A",
                highlightColorHex = null,
                fontSizeSp = 15f,
                fontFamily = DocFontFamily.SANS_SERIF,
                lineSpacingMultiplier = 1.15f,
                indentLevel = 0,
                listType = DocListType.NONE
            )
        }
    }

    // --- Insert Elements ---

    fun insertTable(rows: Int, cols: Int) {
        val safeRows = rows.coerceIn(1, 10)
        val safeCols = cols.coerceIn(1, 8)
        val cells = List(safeRows) { List(safeCols) { "" } }
        val table = TableBlock(rows = safeRows, cols = safeCols, cells = cells)
        val currentBlocks = _currentDoc.value.blocks.toMutableList()
        val activeId = _activeBlockId.value
        val index = if (activeId != null) {
            currentBlocks.indexOfFirst { it.id == activeId }.takeIf { it != -1 }?.plus(1) ?: currentBlocks.size
        } else currentBlocks.size

        currentBlocks.add(index, table)
        // add a paragraph after table for easy continuing
        currentBlocks.add(index + 1, ParagraphBlock())
        updateDocument(_currentDoc.value.copy(blocks = currentBlocks))
        _activeBlockId.value = table.id
    }

    fun updateTableCell(tableId: String, row: Int, col: Int, text: String) {
        val blocks = _currentDoc.value.blocks.map { block ->
            if (block.id == tableId && block is TableBlock) {
                val newCells = block.cells.mapIndexed { r, rList ->
                    if (r == row) {
                        rList.mapIndexed { c, cellVal ->
                            if (c == col) text else cellVal
                        }
                    } else rList
                }
                block.copy(cells = newCells)
            } else block
        }
        updateDocument(_currentDoc.value.copy(blocks = blocks))
    }

    fun addTableRow(tableId: String) {
        val blocks = _currentDoc.value.blocks.map { block ->
            if (block.id == tableId && block is TableBlock) {
                val newCells = block.cells.toMutableList()
                newCells.add(List(block.cols) { "" })
                block.copy(rows = block.rows + 1, cells = newCells)
            } else block
        }
        updateDocument(_currentDoc.value.copy(blocks = blocks))
    }

    fun removeTableRow(tableId: String) {
        val blocks = _currentDoc.value.blocks.map { block ->
            if (block.id == tableId && block is TableBlock && block.rows > 1) {
                val newCells = block.cells.dropLast(1)
                block.copy(rows = block.rows - 1, cells = newCells)
            } else block
        }
        updateDocument(_currentDoc.value.copy(blocks = blocks))
    }

    fun addTableCol(tableId: String) {
        val blocks = _currentDoc.value.blocks.map { block ->
            if (block.id == tableId && block is TableBlock && block.cols < 8) {
                val newCells = block.cells.map { it + "" }
                block.copy(cols = block.cols + 1, cells = newCells)
            } else block
        }
        updateDocument(_currentDoc.value.copy(blocks = blocks))
    }

    fun removeTableCol(tableId: String) {
        val blocks = _currentDoc.value.blocks.map { block ->
            if (block.id == tableId && block is TableBlock && block.cols > 1) {
                val newCells = block.cells.map { it.dropLast(1) }
                block.copy(cols = block.cols - 1, cells = newCells)
            } else block
        }
        updateDocument(_currentDoc.value.copy(blocks = blocks))
    }

    fun insertImage(uri: String, caption: String = "") {
        val imageBlock = ImageBlock(uriOrData = uri, caption = caption)
        val currentBlocks = _currentDoc.value.blocks.toMutableList()
        val activeId = _activeBlockId.value
        val index = if (activeId != null) {
            currentBlocks.indexOfFirst { it.id == activeId }.takeIf { it != -1 }?.plus(1) ?: currentBlocks.size
        } else currentBlocks.size

        currentBlocks.add(index, imageBlock)
        currentBlocks.add(index + 1, ParagraphBlock())
        updateDocument(_currentDoc.value.copy(blocks = currentBlocks))
        _activeBlockId.value = imageBlock.id
    }

    fun updateImageWidth(imageId: String, widthPercent: Float) {
        val blocks = _currentDoc.value.blocks.map { block ->
            if (block.id == imageId && block is ImageBlock) {
                block.copy(widthPercent = widthPercent.coerceIn(0.25f, 1.0f))
            } else block
        }
        updateDocument(_currentDoc.value.copy(blocks = blocks))
    }

    fun insertHorizontalRule() {
        val hr = HorizontalRuleBlock()
        val currentBlocks = _currentDoc.value.blocks.toMutableList()
        val activeId = _activeBlockId.value
        val index = if (activeId != null) {
            currentBlocks.indexOfFirst { it.id == activeId }.takeIf { it != -1 }?.plus(1) ?: currentBlocks.size
        } else currentBlocks.size

        currentBlocks.add(index, hr)
        currentBlocks.add(index + 1, ParagraphBlock())
        updateDocument(_currentDoc.value.copy(blocks = currentBlocks))
    }

    fun insertPageBreak() {
        val pb = PageBreakBlock()
        val currentBlocks = _currentDoc.value.blocks.toMutableList()
        val activeId = _activeBlockId.value
        val index = if (activeId != null) {
            currentBlocks.indexOfFirst { it.id == activeId }.takeIf { it != -1 }?.plus(1) ?: currentBlocks.size
        } else currentBlocks.size

        currentBlocks.add(index, pb)
        currentBlocks.add(index + 1, ParagraphBlock())
        updateDocument(_currentDoc.value.copy(blocks = currentBlocks))
    }

    // --- Undo / Redo ---

    fun undo() {
        lastTypingBlockId = null
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(_currentDoc.value)
            _currentDoc.value = previous
            _canUndo.value = undoStack.isNotEmpty()
            _canRedo.value = true
            _saveStatus.value = SaveStatus.UNSAVED
            updateActiveFormatting()
            scheduleMetricsRecalculation(immediate = true)
        }
    }

    fun redo() {
        lastTypingBlockId = null
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(_currentDoc.value)
            _currentDoc.value = next
            _canUndo.value = true
            _canRedo.value = redoStack.isNotEmpty()
            _saveStatus.value = SaveStatus.UNSAVED
            updateActiveFormatting()
            scheduleMetricsRecalculation(immediate = true)
        }
    }

    // --- Find & Replace ---

    fun toggleSearch(open: Boolean? = null) {
        val target = open ?: !_isSearchOpen.value
        _isSearchOpen.value = target
        if (!target) {
            _searchQuery.value = ""
            _replaceQuery.value = ""
            _matches.value = emptyList()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        refreshMatches(query)
    }

    fun setReplaceQuery(query: String) {
        _replaceQuery.value = query
    }

    private fun refreshMatches(query: String) {
        if (query.isBlank()) {
            _matches.value = emptyList()
            _currentMatchIndex.value = 0
            return
        }
        val found = mutableListOf<SearchMatch>()
        for (block in _currentDoc.value.blocks) {
            if (block is ParagraphBlock) {
                var start = 0
                while (start < block.text.length) {
                    val idx = block.text.indexOf(query, start, ignoreCase = true)
                    if (idx == -1) break
                    found.add(SearchMatch(block.id, idx, query.length))
                    start = idx + query.length
                }
            }
        }
        _matches.value = found
        _currentMatchIndex.value = if (found.isNotEmpty()) 0 else 0
    }

    fun nextMatch() {
        if (_matches.value.isNotEmpty()) {
            _currentMatchIndex.value = (_currentMatchIndex.value + 1) % _matches.value.size
            highlightMatch(_matches.value[_currentMatchIndex.value])
        }
    }

    fun previousMatch() {
        if (_matches.value.isNotEmpty()) {
            _currentMatchIndex.value = if (_currentMatchIndex.value - 1 < 0) _matches.value.size - 1 else _currentMatchIndex.value - 1
            highlightMatch(_matches.value[_currentMatchIndex.value])
        }
    }

    private fun highlightMatch(match: SearchMatch) {
        _activeBlockId.value = match.blockId
    }

    fun replaceCurrent() {
        val list = _matches.value
        val idx = _currentMatchIndex.value
        if (list.isEmpty() || idx !in list.indices) return

        val match = list[idx]
        val repl = _replaceQuery.value
        val query = _searchQuery.value

        val blocks = _currentDoc.value.blocks.map { block ->
            if (block.id == match.blockId && block is ParagraphBlock) {
                val before = block.text.substring(0, match.startIndex)
                val after = block.text.substring(match.startIndex + match.length)
                block.copy(text = before + repl + after)
            } else block
        }
        updateDocument(_currentDoc.value.copy(blocks = blocks))
        refreshMatches(query)
    }

    fun replaceAll() {
        val query = _searchQuery.value
        if (query.isBlank()) return
        val repl = _replaceQuery.value

        val blocks = _currentDoc.value.blocks.map { block ->
            if (block is ParagraphBlock && block.text.contains(query, ignoreCase = true)) {
                block.copy(text = block.text.replace(query, repl, ignoreCase = true))
            } else block
        }
        updateDocument(_currentDoc.value.copy(blocks = blocks))
        refreshMatches(query)
        viewModelScope.launch {
            _snackbarMessage.emit("Replaced all matches")
        }
    }

    // --- Save, Save As, Export, New, Open ---

    fun saveDocument(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            _saveStatus.value = SaveStatus.SAVING
            try {
                val id = repository.saveDocument(_currentDoc.value)
                _currentDoc.value = _currentDoc.value.copy(id = id, isModified = false, lastSavedMillis = System.currentTimeMillis())
                _saveStatus.value = SaveStatus.SAVED
                _snackbarMessage.emit("Document saved successfully")
                onComplete?.invoke()
            } catch (e: Exception) {
                e.printStackTrace()
                _saveStatus.value = SaveStatus.UNSAVED
                _snackbarMessage.emit("Failed to save: ${e.localizedMessage ?: "Storage error"}")
            }
        }
    }

    fun saveAs(newTitle: String, newFormat: ExportFormat, context: Context, shareAfter: Boolean = true) {
        viewModelScope.launch {
            _isExporting.value = true
            _exportMessage.value = "Preparing ${newFormat.displayName}..."
            try {
                val docToSave = _currentDoc.value.copy(
                    title = newTitle.trim().ifBlank { "Untitled Document" },
                    format = newFormat
                )
                val id = repository.saveDocument(docToSave)
                _currentDoc.value = docToSave.copy(id = id, isModified = false, lastSavedMillis = System.currentTimeMillis())
                _saveStatus.value = SaveStatus.SAVED

                // Generate real export file in background IO thread
                val file = withContext(Dispatchers.IO) {
                    ExportManager.exportDocument(context, docToSave, newFormat)
                }
                if (shareAfter) {
                    ExportManager.shareExportedFile(context, file, newFormat, docToSave.title)
                }
                _snackbarMessage.emit("Exported ${docToSave.title}.${newFormat.extension}")
            } catch (e: Exception) {
                e.printStackTrace()
                _snackbarMessage.emit("Unable to export the document: ${e.localizedMessage ?: "Error"}")
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportCurrent(context: Context) {
        saveAs(_currentDoc.value.title, _currentDoc.value.format, context, shareAfter = true)
    }

    fun printCurrent(context: Context) {
        try {
            ExportManager.printDocument(context, _currentDoc.value)
        } catch (e: Exception) {
            e.printStackTrace()
            viewModelScope.launch {
                _snackbarMessage.emit("Print could not be started: ${e.message}")
            }
        }
    }

    fun requestNewDocument(onNeedsConfirmation: () -> Unit, onProceed: () -> Unit) {
        if (_currentDoc.value.isModified) {
            pendingAction = {
                createNewDocument()
                onProceed()
            }
            onNeedsConfirmation()
        } else {
            createNewDocument()
            onProceed()
        }
    }

    fun createNewDocument() {
        val blank = repository.createBlankDocument("Untitled Document")
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
        _currentDoc.value = blank
        _saveStatus.value = SaveStatus.SAVED
        _activeBlockId.value = blank.blocks.firstOrNull()?.id
        updateActiveFormatting()
        scheduleMetricsRecalculation(immediate = true)
    }

    fun requestOpenDocument(docId: Long, onNeedsConfirmation: () -> Unit, onProceed: () -> Unit) {
        if (_currentDoc.value.isModified) {
            pendingAction = {
                loadDocument(docId)
                onProceed()
            }
            onNeedsConfirmation()
        } else {
            loadDocument(docId)
            onProceed()
        }
    }

    fun loadDocument(docId: Long) {
        viewModelScope.launch {
            _isExporting.value = true
            _exportMessage.value = "Loading document..."
            try {
                val doc = withContext(Dispatchers.IO) {
                    repository.loadDocument(docId)
                }
                if (doc != null) {
                    undoStack.clear()
                    redoStack.clear()
                    _canUndo.value = false
                    _canRedo.value = false
                    _currentDoc.value = doc
                    _saveStatus.value = SaveStatus.SAVED
                    _activeBlockId.value = doc.blocks.firstOrNull()?.id
                    updateActiveFormatting()
                    scheduleMetricsRecalculation(immediate = true)
                    _snackbarMessage.emit("Opened '${doc.title}'")
                } else {
                    _snackbarMessage.emit("Could not load document.")
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun requestImportDocument(context: Context, uri: Uri, onNeedsConfirmation: () -> Unit, onProceed: () -> Unit) {
        if (_currentDoc.value.isModified) {
            pendingAction = {
                importDocumentFromUri(context, uri)
                onProceed()
            }
            onNeedsConfirmation()
        } else {
            importDocumentFromUri(context, uri)
            onProceed()
        }
    }

    fun importDocumentFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isExporting.value = true
            _exportMessage.value = "Opening document..."
            try {
                val importedDoc = withContext(Dispatchers.IO) {
                    ImportManager.importFromUri(context, uri)
                }
                val savedId = withContext(Dispatchers.IO) {
                    repository.saveDocument(importedDoc)
                }
                val finalDoc = importedDoc.copy(id = savedId, isModified = false, lastSavedMillis = System.currentTimeMillis())
                undoStack.clear()
                redoStack.clear()
                _canUndo.value = false
                _canRedo.value = false
                _currentDoc.value = finalDoc
                _saveStatus.value = SaveStatus.SAVED
                _activeBlockId.value = finalDoc.blocks.firstOrNull()?.id
                updateActiveFormatting()
                scheduleMetricsRecalculation(immediate = true)
                _snackbarMessage.emit("Successfully opened '${finalDoc.title}'")
            } catch (e: Exception) {
                e.printStackTrace()
                _snackbarMessage.emit("Could not open file: ${e.localizedMessage ?: "unsupported format"}")
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun applyTemplate(templateType: String) {
        val template = repository.createTemplate(templateType)
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
        _currentDoc.value = template
        _saveStatus.value = SaveStatus.SAVED
        _activeBlockId.value = template.blocks.firstOrNull()?.id
        updateActiveFormatting()
        scheduleMetricsRecalculation(immediate = true)
    }

    fun deleteRecentDocument(id: Long) {
        viewModelScope.launch {
            repository.deleteDocument(id)
            _snackbarMessage.emit("Document removed from recent list")
        }
    }

    fun setZoom(percent: Int) {
        _zoomPercent.value = percent.coerceIn(50, 200)
    }

    fun zoomIn() {
        setZoom(_zoomPercent.value + 15)
    }

    fun zoomOut() {
        setZoom(_zoomPercent.value - 15)
    }

    // --- Statistics Helper (Debounced, O(1) reads) ---
    val plainText: String
        get() = repository.extractPlainText(_currentDoc.value)

    val wordCount: Int
        get() = _metrics.value.wordCount

    val characterCount: Int
        get() = _metrics.value.characterCount

    val pageCount: Int
        get() = _metrics.value.pageCount
}

class DocumentViewModelFactory(private val repository: DocumentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DocumentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DocumentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
