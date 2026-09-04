package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.ParagraphBlock
import com.example.ui.components.DocumentCanvas
import com.example.ui.components.FindReplaceBar
import com.example.ui.components.RibbonToolbar
import com.example.ui.components.StatusBar
import com.example.ui.components.TitleBar
import com.example.ui.dialogs.AboutDialog
import com.example.ui.dialogs.FileMenuDrawer
import com.example.ui.dialogs.InsertImageDialog
import com.example.ui.dialogs.InsertLinkDialog
import com.example.ui.dialogs.InsertTableDialog
import com.example.ui.dialogs.RecentDocumentsDialog
import com.example.ui.dialogs.SaveAsDialog
import com.example.ui.dialogs.TemplatesDialog
import com.example.ui.dialogs.UnsavedChangesDialog
import com.example.ui.dialogs.WordCountDialog
import com.example.viewmodel.DocumentViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(
    viewModel: DocumentViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    val document by viewModel.currentDoc.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()
    val activeBlockId by viewModel.activeBlockId.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val zoomPercent by viewModel.zoomPercent.collectAsState()
    val activeFormatting by viewModel.activeFormatting.collectAsState()
    val metrics by viewModel.metrics.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportMessage by viewModel.exportMessage.collectAsState()

    val isSearchOpen by viewModel.isSearchOpen.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val replaceQuery by viewModel.replaceQuery.collectAsState()
    val matches by viewModel.matches.collectAsState()
    val currentMatchIndex by viewModel.currentMatchIndex.collectAsState()

    val recentDocs by viewModel.recentDocuments.collectAsState()

    // Dialog state
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showRecentDialog by remember { mutableStateOf(false) }
    var showInsertTableDialog by remember { mutableStateOf(false) }
    var showInsertImageDialog by remember { mutableStateOf(false) }
    var showInsertLinkDialog by remember { mutableStateOf(false) }
    var showWordCountDialog by remember { mutableStateOf(false) }
    var showTemplatesDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Launcher to open files from storage
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.requestImportDocument(
                context = context,
                uri = uri,
                onNeedsConfirmation = { showUnsavedDialog = true },
                onProceed = {}
            )
        }
    }

    // Collect snackbar messages
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val activeBlock = remember(document.blocks, activeBlockId) {
        document.blocks.firstOrNull { it.id == activeBlockId }
    }

    // Clipboard Handlers
    val handleCut: () -> Unit = {
        val text = (activeBlock as? ParagraphBlock)?.text ?: ""
        if (text.isNotEmpty()) {
            clipboardManager.setText(AnnotatedString(text))
            activeBlockId?.let { viewModel.updateBlockText(it, "") }
            coroutineScope.launch { snackbarHostState.showSnackbar("Cut to clipboard") }
        }
    }

    val handleCopy: () -> Unit = {
        val text = (activeBlock as? ParagraphBlock)?.text ?: ""
        if (text.isNotEmpty()) {
            clipboardManager.setText(AnnotatedString(text))
            coroutineScope.launch { snackbarHostState.showSnackbar("Copied to clipboard") }
        }
    }

    val handlePaste: () -> Unit = {
        val text = clipboardManager.getText()?.text ?: ""
        if (text.isNotEmpty()) {
            val activeId = activeBlockId
            if (activeId != null && activeBlock is ParagraphBlock) {
                val current = (activeBlock as ParagraphBlock).text
                viewModel.updateBlockText(activeId, current + text)
            } else {
                viewModel.addParagraphAfter(activeId)
                viewModel.updateBlockText(viewModel.activeBlockId.value ?: "", text)
            }
            coroutineScope.launch { snackbarHostState.showSnackbar("Pasted from clipboard") }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            FileMenuDrawer(
                documentTitle = document.title,
                onNewDocument = {
                    viewModel.requestNewDocument(
                        onNeedsConfirmation = { showUnsavedDialog = true },
                        onProceed = {}
                    )
                },
                onOpenRecent = { showRecentDialog = true },
                onOpenDeviceFile = { openFileLauncher.launch(arrayOf("*/*")) },
                onSave = { viewModel.saveDocument() },
                onSaveAs = { showSaveAsDialog = true },
                onPrint = { viewModel.printCurrent(context) },
                onShare = { viewModel.exportCurrent(context) },
                onWordCount = { showWordCountDialog = true },
                onTemplates = { showTemplatesDialog = true },
                onAbout = { showAboutDialog = true },
                onCloseDrawer = { coroutineScope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    TitleBar(
                        documentTitle = document.title,
                        documentFormat = document.format,
                        saveStatus = saveStatus,
                        canUndo = canUndo,
                        canRedo = canRedo,
                        onTitleChange = { viewModel.updateTitle(it) },
                        onMenuClick = { coroutineScope.launch { drawerState.open() } },
                        onSaveClick = { viewModel.saveDocument() },
                        onSaveAsClick = { showSaveAsDialog = true },
                        onUndoClick = { viewModel.undo() },
                        onRedoClick = { viewModel.redo() },
                        onSearchClick = { viewModel.toggleSearch() },
                        onPrintClick = { viewModel.printCurrent(context) },
                        onShareClick = { showSaveAsDialog = true },
                        onRecentClick = { showRecentDialog = true }
                    )

                    RibbonToolbar(
                        formatting = activeFormatting,
                        onCut = handleCut,
                        onCopy = handleCopy,
                        onPaste = handlePaste,
                        onBoldToggle = { viewModel.toggleBold() },
                        onItalicToggle = { viewModel.toggleItalic() },
                        onUnderlineToggle = { viewModel.toggleUnderline() },
                        onStrikeToggle = { viewModel.toggleStrike() },
                        onHeadingSelect = { viewModel.setHeadingLevel(it) },
                        onAlignmentSelect = { viewModel.setAlignment(it) },
                        onListTypeSelect = { viewModel.setListType(it) },
                        onIndentAdjust = { viewModel.adjustIndent(it) },
                        onTextColorSelect = { viewModel.setTextColor(it) },
                        onHighlightColorSelect = { viewModel.setHighlightColor(it) },
                        onFontSizeDelta = { viewModel.changeFontSizeBy(it) },
                        onFontSizeSelect = { viewModel.setFontSize(it) },
                        onFontFamilySelect = { viewModel.setFontFamily(it) },
                        onLineSpacingSelect = { viewModel.setLineSpacing(it) },
                        onClearFormatting = { viewModel.clearFormatting() },
                        onInsertTable = { showInsertTableDialog = true },
                        onInsertImage = { showInsertImageDialog = true },
                        onInsertLink = { showInsertLinkDialog = true },
                        onInsertHorizontalRule = { viewModel.insertHorizontalRule() },
                        onInsertPageBreak = { viewModel.insertPageBreak() },
                        onOpenSearch = { viewModel.toggleSearch(true) },
                        onOpenWordCount = { showWordCountDialog = true },
                        onOpenTemplates = { showTemplatesDialog = true },
                        zoomPercent = zoomPercent,
                        onZoomChange = { viewModel.setZoom(it) }
                    )

                    AnimatedVisibility(
                        visible = isSearchOpen,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        FindReplaceBar(
                            searchQuery = searchQuery,
                            replaceQuery = replaceQuery,
                            matches = matches,
                            currentMatchIndex = currentMatchIndex,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onReplaceChange = { viewModel.setReplaceQuery(it) },
                            onNextMatch = { viewModel.nextMatch() },
                            onPreviousMatch = { viewModel.previousMatch() },
                            onReplaceCurrent = { viewModel.replaceCurrent() },
                            onReplaceAll = { viewModel.replaceAll() },
                            onClose = { viewModel.toggleSearch(false) }
                        )
                    }
                }
            },
            bottomBar = {
                StatusBar(
                    pageCount = metrics.pageCount,
                    wordCount = metrics.wordCount,
                    characterCount = metrics.characterCount,
                    saveStatus = saveStatus,
                    zoomPercent = zoomPercent,
                    onZoomChange = { viewModel.setZoom(it) },
                    onWordCountClick = { showWordCountDialog = true }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && (event.isCtrlPressed || event.isMetaPressed)) {
                        when (event.key) {
                            Key.S -> {
                                if (event.isShiftPressed) {
                                    showSaveAsDialog = true
                                } else {
                                    viewModel.saveDocument()
                                }
                                true
                            }
                            Key.Z -> {
                                viewModel.undo()
                                true
                            }
                            Key.Y -> {
                                viewModel.redo()
                                true
                            }
                            Key.N -> {
                                viewModel.requestNewDocument(
                                    onNeedsConfirmation = { showUnsavedDialog = true },
                                    onProceed = {}
                                )
                                true
                            }
                            Key.O -> {
                                openFileLauncher.launch(arrayOf("*/*"))
                                true
                            }
                            Key.P -> {
                                viewModel.printCurrent(context)
                                true
                            }
                            Key.F, Key.H -> {
                                viewModel.toggleSearch(true)
                                true
                            }
                            Key.B -> {
                                viewModel.toggleBold()
                                true
                            }
                            Key.I -> {
                                viewModel.toggleItalic()
                                true
                            }
                            Key.U -> {
                                viewModel.toggleUnderline()
                                true
                            }
                            Key.X -> {
                                handleCut()
                                true
                            }
                            Key.C -> {
                                handleCopy()
                                true
                            }
                            Key.V -> {
                                handlePaste()
                                true
                            }
                            else -> false
                        }
                    } else false
                }
                .testTag("editor_screen")
        ) { innerPadding ->
            DocumentCanvas(
                document = document,
                activeBlockId = activeBlockId,
                zoomPercent = zoomPercent,
                onSelectBlock = { viewModel.setActiveBlock(it) },
                onUpdateBlockText = { id, text -> viewModel.updateBlockText(id, text) },
                onAddParagraphAfter = { id -> viewModel.addParagraphAfter(id) },
                onDeleteBlock = { id -> viewModel.deleteBlock(id) },
                onToggleChecklist = { id -> viewModel.toggleChecklist(id) },
                onUpdateTableCell = { id, r, c, txt -> viewModel.updateTableCell(id, r, c, txt) },
                onAddTableRow = { id -> viewModel.addTableRow(id) },
                onRemoveTableRow = { id -> viewModel.removeTableRow(id) },
                onAddTableCol = { id -> viewModel.addTableCol(id) },
                onRemoveTableCol = { id -> viewModel.removeTableCol(id) },
                onUpdateImageWidth = { id, w -> viewModel.updateImageWidth(id, w) },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    // --- Dialogs ---

    if (showSaveAsDialog) {
        SaveAsDialog(
            initialTitle = document.title,
            initialFormat = document.format,
            onDismiss = { showSaveAsDialog = false },
            onSaveAs = { title, format ->
                viewModel.saveAs(title, format, context, shareAfter = true)
            }
        )
    }

    if (showUnsavedDialog) {
        UnsavedChangesDialog(
            documentTitle = document.title,
            onSave = {
                showUnsavedDialog = false
                viewModel.saveDocument {
                    viewModel.pendingAction?.invoke()
                    viewModel.pendingAction = null
                }
            },
            onDiscard = {
                showUnsavedDialog = false
                viewModel.pendingAction?.invoke()
                viewModel.pendingAction = null
            },
            onCancel = {
                showUnsavedDialog = false
                viewModel.pendingAction = null
            }
        )
    }

    if (showRecentDialog) {
        RecentDocumentsDialog(
            documents = recentDocs,
            onSelectDocument = { id ->
                viewModel.requestOpenDocument(
                    docId = id,
                    onNeedsConfirmation = { showUnsavedDialog = true },
                    onProceed = {}
                )
            },
            onDeleteDocument = { id -> viewModel.deleteRecentDocument(id) },
            onDismiss = { showRecentDialog = false }
        )
    }

    if (showInsertTableDialog) {
        InsertTableDialog(
            onDismiss = { showInsertTableDialog = false },
            onInsert = { rows, cols -> viewModel.insertTable(rows, cols) }
        )
    }

    if (showInsertImageDialog) {
        InsertImageDialog(
            onDismiss = { showInsertImageDialog = false },
            onInsert = { uri, caption -> viewModel.insertImage(uri, caption) }
        )
    }

    if (showInsertLinkDialog) {
        InsertLinkDialog(
            onDismiss = { showInsertLinkDialog = false },
            onInsert = { text, url ->
                // Insert as formatted text
                viewModel.addParagraphAfter(activeBlockId)
                viewModel.updateBlockText(viewModel.activeBlockId.value ?: "", "$text ($url)")
                viewModel.setTextColor("#1565C0")
                viewModel.toggleUnderline()
            }
        )
    }

    if (showWordCountDialog) {
        WordCountDialog(
            pageCount = metrics.pageCount,
            wordCount = metrics.wordCount,
            characterCount = metrics.characterCount,
            paragraphCount = metrics.paragraphCount,
            plainText = viewModel.plainText,
            onDismiss = { showWordCountDialog = false }
        )
    }

    if (showTemplatesDialog) {
        TemplatesDialog(
            onSelectTemplate = { templateType ->
                viewModel.applyTemplate(templateType)
            },
            onDismiss = { showTemplatesDialog = false }
        )
    }

    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }

    if (isExporting) {
        Dialog(
            onDismissRequest = { /* prevent dismissal during critical operation */ }
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF0F4C81),
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = exportMessage.ifBlank { "Processing..." },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
        }
    }
}
