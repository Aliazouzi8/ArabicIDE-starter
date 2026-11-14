package com.aliazouzi.arabicide.ui

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aliazouzi.arabicide.OpenTab
import com.aliazouzi.arabicide.PanelMode
import com.aliazouzi.arabicide.WorkspaceViewModel
import com.aliazouzi.arabicide.diagnostics.CrashLogger
import com.aliazouzi.arabicide.editor.EditorView
import com.aliazouzi.arabicide.termux.TermuxRunner
import com.aliazouzi.arabicide.termux.TermuxStatus
import com.aliazouzi.arabicide.ui.theme.IdeColors
import kotlinx.coroutines.launch

private sealed interface Dlg {
    data class NewFile(val parent: String) : Dlg
    data class NewFolder(val parent: String) : Dlg
    data class Rename(val path: String, val name: String) : Dlg
    data class Delete(val path: String, val name: String, val isDir: Boolean) : Dlg
    data object Projects : Dlg
    data object NewProject : Dlg
    data class DeleteProject(val name: String) : Dlg
}

private val SYMBOLS = listOf(
    "{", "}", "(", ")", "[", "]", "<", ">", "=", "\"", "'", ";", ":", ".", ",",
    "/", "\\", "|", "&", "!", "?", "#", "$", "_", "-", "+", "*", "%", "@", "`", "~", "^",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceScreen(vm: WorkspaceViewModel = viewModel()) {
    val context = LocalContext.current
    val controller = vm.controller
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbar = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current

    var dialog by remember { mutableStateOf<Dlg?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }
    var showDiagnostics by remember { mutableStateOf(false) }
    var reportText by remember { mutableStateOf("") }
    var commandFocused by remember { mutableStateOf(false) }
    var termuxStatus by remember { mutableStateOf(TermuxRunner.status(context)) }

    fun openDiagnostics() {
        reportText = CrashLogger.buildReport(
            context,
            extra = listOf(
                "Project: " + vm.project,
                "Open tabs: " + vm.tabs.size,
                "Termux status: " + termuxStatus,
            ),
            editorLog = vm.editorLog.toList(),
        )
        showDiagnostics = true
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        termuxStatus = TermuxRunner.status(context)
        if (!granted) {
            vm.showMessage("لم تُمنح الصلاحية. إن لم تظهر نافذة الإذن فأعد تثبيت ArabicIDE بعد تثبيت Termux.")
        }
    }

    // استقبال نتائج الأوامر القادمة من Termux
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                vm.onTermuxResult(intent)
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(TermuxRunner.RESULT_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { termuxStatus = TermuxRunner.status(context) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.onAppStopped() }

    // أول تشغيل: يظهر دليل ربط Termux مرة واحدة إن لم يكن الربط جاهزاً
    LaunchedEffect(Unit) {
        if (!vm.guideSeen && termuxStatus != TermuxStatus.READY) showGuide = true
        vm.markGuideSeen()
    }

    // تعطل حدث في تشغيل سابق: نعرض التقرير مباشرة لإرساله للمطور
    LaunchedEffect(Unit) {
        if (CrashLogger.hasPendingCrash(context)) openDiagnostics()
    }

    val msg = vm.message
    LaunchedEffect(msg) {
        if (msg != null) {
            snackbar.showSnackbar(msg)
            vm.clearMessage()
        }
    }

    // مزامنة المحرر مع حالة التبويبات
    val ready = controller.ready
    val active = vm.activePath
    LaunchedEffect(ready, active) {
        if (ready && active != null) {
            val tab = vm.tabs.firstOrNull { it.path == active }
            if (tab != null) controller.show(tab.path, tab.text)
        }
    }
    LaunchedEffect(ready, vm.fontSize, vm.wordWrap) {
        if (ready) controller.options(vm.fontSize, vm.wordWrap)
    }
    LaunchedEffect(vm.panelMode) {
        if (vm.panelMode == PanelMode.HIDDEN) commandFocused = false
    }

    BackHandler(enabled = drawerState.isOpen || vm.panelMode != PanelMode.HIDDEN) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            vm.hidePanel()
        }
    }

    val imeVisible = WindowInsets.isImeVisible
    val showKeys = imeVisible && !commandFocused && vm.tabs.isNotEmpty()
    val activeTab = vm.tabs.firstOrNull { it.path == active }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = IdeColors.Surface,
            ) {
                ExplorerPanel(
                    project = vm.project,
                    rows = vm.treeRows,
                    expanded = vm.expanded,
                    activePath = vm.activePath,
                    onProjects = { dialog = Dlg.Projects },
                    onToggleDir = vm::toggleDir,
                    onOpenFile = {
                        vm.openFile(it)
                        scope.launch { drawerState.close() }
                    },
                    onNewFile = { dialog = Dlg.NewFile(it) },
                    onNewFolder = { dialog = Dlg.NewFolder(it) },
                    onRename = { dialog = Dlg.Rename(it.path, it.name) },
                    onDelete = { dialog = Dlg.Delete(it.path, it.name, it.isDir) },
                    onCopyPath = {
                        clipboard.setText(AnnotatedString(it.path))
                        vm.showMessage("نُسخ المسار")
                    },
                    onRefresh = vm::refreshTree,
                )
            }
        },
    ) {
        Box(Modifier.fillMaxSize().background(IdeColors.Surface)) {
            Column(Modifier.fillMaxSize().imePadding()) {

                TopBar(
                    project = vm.project,
                    fileName = activeTab?.name,
                    hasFile = activeTab != null,
                    terminalOpen = vm.panelMode != PanelMode.HIDDEN,
                    onMenu = { scope.launch { drawerState.open() } },
                    onSearch = { vm.editorCmd("find") },
                    onSave = vm::saveActive,
                    onRun = vm::runActive,
                    onTerminal = vm::togglePanel,
                    onNewFile = { dialog = Dlg.NewFile("") },
                    onFormat = { vm.editorCmd("format") },
                    onComment = { vm.editorCmd("comment") },
                    onGoToLine = { vm.editorCmd("gotoLine") },
                    onProjects = { dialog = Dlg.Projects },
                    onSettings = { showSettings = true },
                    onTermuxGuide = { showGuide = true },
                    onDiagnostics = { openDiagnostics() },
                )

                if (vm.tabs.isNotEmpty()) {
                    TabsBar(
                        tabs = vm.tabs,
                        active = active,
                        onSelect = vm::selectTab,
                        onClose = vm::requestCloseTab,
                    )
                }

                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().background(IdeColors.Background)) {
                    val panelHeight by animateDpAsState(
                        targetValue = maxHeight * vm.panelMode.fraction,
                        label = "panelHeight",
                    )
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            EditorView(controller, Modifier.fillMaxSize())

                            if (controller.ready && controller.backend == "textarea") {
                                FallbackBanner(Modifier.align(Alignment.TopCenter)) { showSettings = true }
                            }
                            if (!controller.ready) LoadingOverlay()
                            if (!imeVisible && vm.tabs.isNotEmpty() && controller.ready) {
                                SmallFloatingActionButton(
                                    onClick = vm::focusEditor,
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                                    containerColor = IdeColors.SurfaceHigh,
                                    contentColor = IdeColors.TextPrimary,
                                ) { Icon(Icons.Filled.Keyboard, contentDescription = "إظهار لوحة المفاتيح") }
                            }
                            if (vm.tabs.isEmpty()) {
                                EmptyState(
                                    onOpenExplorer = { scope.launch { drawerState.open() } },
                                    onNewFile = { dialog = Dlg.NewFile("") },
                                )
                            }
                        }

                        if (panelHeight > 1.dp) {
                            TerminalPanel(
                                log = vm.terminalLog,
                                status = termuxStatus,
                                expanded = vm.panelMode == PanelMode.FULL,
                                onToggleExpand = vm::toggleExpandPanel,
                                onClose = vm::hidePanel,
                                onClear = vm::clearTerminal,
                                onCopyAll = {
                                    clipboard.setText(AnnotatedString(vm.terminalText()))
                                    vm.showMessage("نُسخ ناتج الطرفية")
                                },
                                onRun = vm::runCommand,
                                onCancel = vm::cancelEntry,
                                onGrantPermission = { permissionLauncher.launch(TermuxRunner.PERMISSION) },
                                onOpenInstallPage = { openUrl(context, "https://f-droid.org/packages/com.termux/") },
                                onHelp = { showGuide = true },
                                onInputFocus = { commandFocused = it },
                                modifier = Modifier.height(panelHeight),
                            )
                        }
                    }
                }

                if (showKeys) {
                    ExtraKeysBar(onInsert = vm::insertText, onCmd = vm::editorCmd)
                }

                StatusBar(
                    tab = activeTab,
                    line = controller.cursorLine,
                    col = controller.cursorCol,
                    lang = controller.languageLabel,
                    termux = termuxStatus,
                    onTermuxClick = { showGuide = true },
                )
            }

            SnackbarHost(
                snackbar,
                Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(bottom = 60.dp),
            )
        }
    }

    // ------------------------------------------------------------ نوافذ الحوار
    when (val d = dialog) {
        is Dlg.NewFile -> NameDialog("ملف جديد", "", "إنشاء", { dialog = null }) { name ->
            vm.createFile(d.parent, name)
        }
        is Dlg.NewFolder -> NameDialog("مجلد جديد", "", "إنشاء", { dialog = null }) { name ->
            vm.createFolder(d.parent, name)
        }
        is Dlg.Rename -> NameDialog("إعادة تسمية", d.name, "حفظ", { dialog = null }) { name ->
            vm.renameItem(d.path, name)
        }
        is Dlg.Delete -> ConfirmDialog(
            title = "حذف",
            text = "هل تريد حذف «" + d.name + "»" + (if (d.isDir) " وكل ما بداخله؟" else "؟"),
            confirmLabel = "حذف",
            danger = true,
            onDismiss = { dialog = null },
            onConfirm = {
                vm.deleteItem(d.path)
                dialog = null
            },
        )
        Dlg.Projects -> ProjectsDialog(
            projects = vm.projects,
            current = vm.project,
            onSelect = { vm.switchProject(it) },
            onDelete = { dialog = Dlg.DeleteProject(it) },
            onNew = { dialog = Dlg.NewProject },
            onDismiss = { dialog = null },
        )
        Dlg.NewProject -> NameDialog("مشروع جديد", "", "إنشاء", { dialog = null }) { name ->
            vm.createProject(name)
        }
        is Dlg.DeleteProject -> ConfirmDialog(
            title = "حذف المشروع",
            text = "سيُحذف المشروع «" + d.name + "» وكل ملفاته نهائياً.",
            confirmLabel = "حذف",
            danger = true,
            onDismiss = { dialog = null },
            onConfirm = {
                val err = vm.deleteProject(d.name)
                if (err != null) vm.showMessage(err)
                dialog = null
            },
        )
        null -> Unit
    }

    val closing = vm.pendingClose
    if (closing != null) {
        AlertDialog(
            onDismissRequest = vm::cancelClose,
            title = { Text("حفظ التعديلات؟") },
            text = { Text("الملف «" + closing.substringAfterLast('/') + "» فيه تعديلات غير محفوظة.") },
            confirmButton = {
                Row {
                    TextButton(onClick = { vm.resolveClose(false) }) { Text("تجاهل") }
                    TextButton(onClick = { vm.resolveClose(true) }) { Text("حفظ") }
                }
            },
            dismissButton = { TextButton(onClick = vm::cancelClose) { Text("إلغاء") } },
        )
    }

    if (showGuide) {
        TermuxGuideSheet(
            status = termuxStatus,
            onCopy = { text -> clipboard.setText(AnnotatedString(text)) },
            onOpenTermux = { openTermux(context) },
            onOpenInstallPage = { openUrl(context, "https://f-droid.org/packages/com.termux/") },
            onGrantPermission = { permissionLauncher.launch(TermuxRunner.PERMISSION) },
            onRunTest = {
                showGuide = false
                vm.runCommand("echo ArabicIDE-OK")
            },
            onDismiss = { showGuide = false },
        )
    }

    if (showDiagnostics) {
        DiagnosticsSheet(
            report = reportText.takeLast(30_000),
            hasCrash = CrashLogger.hasPendingCrash(context),
            onShare = { shareReport(context, reportText) },
            onCopy = {
                clipboard.setText(AnnotatedString(reportText))
                vm.showMessage("نُسخ التقرير")
            },
            onClear = {
                CrashLogger.clear(context)
                vm.clearEditorLog()
                openDiagnostics()
            },
            onDismiss = {
                CrashLogger.acknowledge(context)
                showDiagnostics = false
            },
        )
    }

    if (showSettings) {
        SettingsSheet(
            fontSize = vm.fontSize,
            wordWrap = vm.wordWrap,
            autoSave = vm.autoSave,
            backend = controller.backend,
            editorLog = vm.editorLog,
            onFontSize = vm::updateFontSize,
            onWordWrap = vm::updateWordWrap,
            onAutoSave = vm::updateAutoSave,
            onCopyLog = {
                clipboard.setText(AnnotatedString(vm.editorLogText()))
                vm.showMessage("نُسخ سجل المحرر")
            },
            onClearLog = vm::clearEditorLog,
            onDismiss = { showSettings = false },
        )
    }
}

private fun shareReport(context: Context, report: String) {
    try {
        val file = CrashLogger.writeReportFile(context, report)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".logs", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "ArabicIDE error report")
            clipData = ClipData.newRawUri("report", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "إرسال التقرير"))
    } catch (e: Exception) {
        CrashLogger.recordError(context, "share", e)
    }
}

private fun openTermux(context: Context) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(TermuxRunner.PACKAGE)
        if (intent != null) context.startActivity(intent)
    } catch (e: Exception) {
        // تعذر فتح Termux
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        // لا يوجد متصفح
    }
}

// ================================================================ الشريط العلوي
@Composable
private fun TopBar(
    project: String,
    fileName: String?,
    hasFile: Boolean,
    terminalOpen: Boolean,
    onMenu: () -> Unit,
    onSearch: () -> Unit,
    onSave: () -> Unit,
    onRun: () -> Unit,
    onTerminal: () -> Unit,
    onNewFile: () -> Unit,
    onFormat: () -> Unit,
    onComment: () -> Unit,
    onGoToLine: () -> Unit,
    onProjects: () -> Unit,
    onSettings: () -> Unit,
    onTermuxGuide: () -> Unit,
    onDiagnostics: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Surface(color = IdeColors.Surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(52.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMenu) { Icon(Icons.Filled.Menu, contentDescription = "مستكشف الملفات") }

            Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(
                    text = fileName ?: "ArabicIDE",
                    style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Ltr),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = project,
                    style = MaterialTheme.typography.labelSmall,
                    color = IdeColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(onClick = onSearch, enabled = hasFile) { Icon(Icons.Filled.Search, contentDescription = "بحث") }
            IconButton(onClick = onSave, enabled = hasFile) { Icon(Icons.Filled.Save, contentDescription = "حفظ") }
            FilledIconButton(
                onClick = onRun,
                enabled = hasFile,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = IdeColors.Green,
                    contentColor = Color(0xFF00281A),
                ),
            ) { Icon(Icons.Filled.PlayArrow, contentDescription = "تشغيل") }
            IconButton(onClick = onTerminal) {
                Icon(
                    Icons.Filled.Terminal,
                    contentDescription = "الطرفية",
                    tint = if (terminalOpen) IdeColors.Accent else LocalContentColor.current,
                )
            }

            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "المزيد") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    MenuItem("ملف جديد", Icons.Filled.NoteAdd) {
                        menuOpen = false
                        onNewFile()
                    }
                    if (hasFile) {
                        MenuItem("تنسيق المستند", Icons.Filled.FormatAlignLeft) {
                            menuOpen = false
                            onFormat()
                        }
                        MenuItem("تعليق / إلغاء تعليق السطر", Icons.Filled.Code) {
                            menuOpen = false
                            onComment()
                        }
                        MenuItem("الانتقال إلى سطر", Icons.Filled.KeyboardArrowDown) {
                            menuOpen = false
                            onGoToLine()
                        }
                    }
                    MenuItem("المشاريع", Icons.Filled.Folder) {
                        menuOpen = false
                        onProjects()
                    }
                    MenuItem("ربط Termux وأوامر التثبيت", Icons.Filled.Terminal) {
                        menuOpen = false
                        onTermuxGuide()
                    }
                    MenuItem("تقرير الأخطاء", Icons.Filled.BugReport) {
                        menuOpen = false
                        onDiagnostics()
                    }
                    MenuItem("الإعدادات", Icons.Filled.Settings) {
                        menuOpen = false
                        onSettings()
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
    )
}

// ================================================================ علامات التبويب
@Composable
private fun TabsBar(
    tabs: List<OpenTab>,
    active: String?,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val activeIndex = tabs.indexOfFirst { it.path == active }
    LaunchedEffect(activeIndex, tabs.size) {
        if (activeIndex >= 0) listState.animateScrollToItem(activeIndex)
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(IdeColors.Panel),
    ) {
        itemsIndexed(tabs, key = { _, t -> t.path }) { _, tab ->
            val selected = tab.path == active
            Column(
                Modifier
                    .fillMaxHeight()
                    .background(if (selected) IdeColors.Background else IdeColors.Panel)
                    .clickable { onSelect(tab.path) },
            ) {
                Row(
                    Modifier.weight(1f).padding(start = 12.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        tab.name,
                        style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
                        color = if (selected) IdeColors.TextPrimary else IdeColors.TextSecondary,
                        maxLines = 1,
                    )
                    if (tab.dirty) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(IdeColors.Yellow),
                        )
                    }
                    Box(
                        Modifier.size(32.dp).clickable { onClose(tab.path) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "إغلاق التبويب",
                            modifier = Modifier.size(14.dp),
                            tint = IdeColors.TextSecondary,
                        )
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(if (selected) IdeColors.Accent else Color.Transparent),
                )
            }
        }
    }
}

// ================================================================ أزرار الرموز
@Composable
private fun ExtraKeysBar(onInsert: (String) -> Unit, onCmd: (String) -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(IdeColors.SurfaceHigh)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KeyIcon(Icons.AutoMirrored.Filled.Undo, "تراجع") { onCmd("undo") }
            KeyIcon(Icons.AutoMirrored.Filled.Redo, "إعادة") { onCmd("redo") }
            KeyText("Tab") { onCmd("tab") }
            KeyText("⇤") { onCmd("outdent") }
            KeyText("←") { onCmd("left") }
            KeyText("→") { onCmd("right") }
            KeyText("↑") { onCmd("up") }
            KeyText("↓") { onCmd("down") }
            SYMBOLS.forEach { s -> KeyText(s) { onInsert(s) } }
        }
    }
}

@Composable
private fun KeyText(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .defaultMinSize(minWidth = 40.dp)
            .height(42.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = IdeColors.TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
    }
}

@Composable
private fun KeyIcon(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier
            .defaultMinSize(minWidth = 40.dp)
            .height(42.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = desc, modifier = Modifier.size(20.dp), tint = IdeColors.TextPrimary)
    }
}

// ================================================================ شريط الحالة
@Composable
private fun StatusBar(
    tab: OpenTab?,
    line: Int,
    col: Int,
    lang: String,
    termux: TermuxStatus,
    onTermuxClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(IdeColors.Surface)
            .navigationBarsPadding()
            .height(26.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (tab != null) {
            Text(
                if (tab.dirty) "غير محفوظ" else "محفوظ",
                color = if (tab.dirty) IdeColors.Yellow else IdeColors.Green,
                fontSize = 11.sp,
            )
            Spacer(Modifier.width(12.dp))
            Text("Ln $line, Col $col", color = IdeColors.TextSecondary, fontSize = 11.sp)
            Spacer(Modifier.width(12.dp))
            Text("UTF-8", color = IdeColors.TextSecondary, fontSize = 11.sp)
            if (lang.isNotEmpty()) {
                Spacer(Modifier.width(12.dp))
                Text(lang, color = IdeColors.TextSecondary, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    when (termux) {
                        TermuxStatus.READY -> IdeColors.Green
                        TermuxStatus.NO_PERMISSION -> IdeColors.Yellow
                        TermuxStatus.NOT_INSTALLED -> IdeColors.Red
                    },
                ),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "Termux",
            modifier = Modifier.clickable(onClick = onTermuxClick).padding(horizontal = 4.dp, vertical = 4.dp),
            color = IdeColors.TextSecondary,
            fontSize = 11.sp,
        )
    }
}

// ================================================================ حالات خاصة
@Composable
private fun EmptyState(onOpenExplorer: () -> Unit, onNewFile: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(IdeColors.Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(56.dp), tint = IdeColors.Accent)
        Spacer(Modifier.height(12.dp))
        Text("ArabicIDE", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            "افتح ملفاً من المستكشف أو أنشئ ملفاً جديداً للبدء",
            color = IdeColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onOpenExplorer) {
            Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("فتح المستكشف")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onNewFile) { Text("ملف جديد") }
    }
}

@Composable
private fun LoadingOverlay() {
    Box(
        Modifier
            .fillMaxSize()
            .background(IdeColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        Text("جارٍ تحميل المحرر…", color = IdeColors.TextSecondary)
    }
}

@Composable
private fun FallbackBanner(modifier: Modifier, onClick: () -> Unit) {
    Surface(
        color = Color(0xFF5A3B00),
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Text(
            "تعذر تحميل Monaco فيعمل محرر نصي بديل. اضغط لعرض السجل.",
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            color = IdeColors.Yellow,
        )
    }
}
