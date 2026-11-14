package com.aliazouzi.arabicide

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aliazouzi.arabicide.data.ProjectRepository
import com.aliazouzi.arabicide.diagnostics.CrashLogger
import com.aliazouzi.arabicide.data.ReadResult
import com.aliazouzi.arabicide.data.SettingsStore
import com.aliazouzi.arabicide.data.TreeRow
import com.aliazouzi.arabicide.editor.EditorController
import com.aliazouzi.arabicide.termux.ScriptBuilder
import com.aliazouzi.arabicide.termux.TermuxRunner
import com.aliazouzi.arabicide.termux.TermuxStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class OpenTab(val path: String, val savedText: String, val text: String) {
    val name: String get() = path.substringAfterLast('/')
    val dirty: Boolean get() = text != savedText
}

enum class EntryStatus { RUNNING, DONE, CANCELLED }

data class TerminalEntry(
    val id: Long,
    val command: String,
    val status: EntryStatus = EntryStatus.RUNNING,
    val stdout: String = "",
    val stderr: String = "",
    val note: String = "",
    val exitCode: Int? = null,
    val slow: Boolean = false,
)

enum class PanelMode(val fraction: Float) {
    HIDDEN(0f),
    HALF(0.42f),
    FULL(0.8f),
}

class WorkspaceViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ProjectRepository(app)
    private val store = SettingsStore(app)
    val controller = EditorController()

    // ---------------------------------------------------------------- الحالة
    var projects by mutableStateOf(listOf<String>())
        private set
    var project by mutableStateOf("")
        private set
    var treeRows by mutableStateOf(listOf<TreeRow>())
        private set
    var expanded by mutableStateOf(setOf<String>())
        private set

    var tabs by mutableStateOf(listOf<OpenTab>())
        private set
    var activePath by mutableStateOf<String?>(null)
        private set
    var pendingClose by mutableStateOf<String?>(null)
        private set

    var fontSize by mutableStateOf(store.fontSize)
        private set
    var wordWrap by mutableStateOf(store.wordWrap)
        private set
    var autoSave by mutableStateOf(store.autoSave)
        private set

    var terminalLog by mutableStateOf(listOf<TerminalEntry>())
        private set
    var panelMode by mutableStateOf(PanelMode.HIDDEN)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    val editorLog = mutableStateListOf<String>()

    /** هل ظهر دليل ربط Termux من قبل؟ */
    val guideSeen: Boolean = store.guideSeen

    private val autoSaveJobs = mutableMapOf<String, Job>()
    private var nextExecId = 1L

    init {
        controller.onChanged = { path, text -> onEditorChanged(path, text) }
        controller.onSaveRequested = { saveActive() }
        controller.onLog = {
            addEditorLog(it)
            CrashLogger.log(getApplication(), "editor", it)
        }

        repo.ensureDefaultProject()
        projects = repo.listProjects()
        val last = store.lastProject
        val start = if (last in projects) last else projects.first()
        loadProject(start, restore = true)
    }

    // ------------------------------------------------------------- مساعدات
    private fun joinPath(parent: String, name: String): String =
        if (parent.isEmpty()) name else "$parent/$name"

    private fun parentsOf(path: String): List<String> {
        val parts = path.split('/')
        return (1 until parts.size).map { parts.take(it).joinToString("/") }
    }

    fun markGuideSeen() {
        store.guideSeen = true
    }

    fun showMessage(text: String) {
        message = text
    }

    fun clearMessage() {
        message = null
    }

    private fun persistSession() {
        store.openPaths = tabs.map { it.path }
        store.activePath = activePath ?: ""
    }

    // ------------------------------------------------------------- المشاريع
    private fun loadProject(name: String, restore: Boolean) {
        project = name
        store.lastProject = name
        expanded = setOf()
        tabs = listOf()
        activePath = null

        if (restore) {
            for (p in store.openPaths) {
                val r = repo.read(name, p)
                if (r is ReadResult.Ok) tabs = tabs + OpenTab(p, r.text, r.text)
            }
            val act = store.activePath
            activePath = if (tabs.any { it.path == act }) act else tabs.firstOrNull()?.path
            expanded = tabs.flatMap { parentsOf(it.path) }.toSet()
        }
        if (tabs.isEmpty()) {
            val first = repo.firstFile(name)
            if (first != null) openFile(first)
        }
        refreshTree()
        persistSession()
    }

    fun refreshTree() {
        treeRows = repo.visibleTree(project, expanded)
    }

    fun toggleDir(path: String) {
        expanded = if (path in expanded) expanded - path else expanded + path
        refreshTree()
    }

    fun switchProject(name: String) {
        if (name == project) return
        saveAllDirtyBlocking()
        tabs.forEach { controller.drop(it.path) }
        loadProject(name, restore = false)
    }

    fun createProject(name: String): String? {
        val n = name.trim()
        val err = repo.createProject(n, withSamples = true)
        if (err != null) return err
        projects = repo.listProjects()
        switchProject(n)
        return null
    }

    fun deleteProject(name: String): String? {
        if (projects.size <= 1) return "لا يمكن حذف المشروع الوحيد"
        if (name == project) switchProject(projects.first { it != name })
        if (!repo.deleteProject(name)) return "تعذر حذف المشروع"
        projects = repo.listProjects()
        return null
    }

    // --------------------------------------------------------------- الملفات
    fun createFile(parent: String, name: String): String? {
        val n = name.trim()
        val err = repo.createFile(project, parent, n)
        if (err != null) return err
        if (parent.isNotEmpty()) expanded = expanded + parent + parentsOf(parent)
        refreshTree()
        openFile(joinPath(parent, n))
        return null
    }

    fun createFolder(parent: String, name: String): String? {
        val n = name.trim()
        val err = repo.createDir(project, parent, n)
        if (err != null) return err
        expanded = expanded + joinPath(parent, n) + (if (parent.isNotEmpty()) parentsOf(parent) + parent else emptyList())
        refreshTree()
        return null
    }

    fun renameItem(path: String, newName: String): String? {
        val n = newName.trim()
        val err = repo.rename(project, path, n)
        if (err != null) return err
        val newPath = joinPath(path.substringBeforeLast('/', ""), n)
        remapTabs(path, newPath)
        expanded = expanded.map {
            if (it == path || it.startsWith("$path/")) newPath + it.removePrefix(path) else it
        }.toSet()
        refreshTree()
        return null
    }

    private fun remapTabs(oldPath: String, newPath: String) {
        var changed = false
        tabs = tabs.map {
            if (it.path == oldPath || it.path.startsWith("$oldPath/")) {
                controller.drop(it.path)
                autoSaveJobs.remove(it.path)?.cancel()
                changed = true
                it.copy(path = newPath + it.path.removePrefix(oldPath))
            } else {
                it
            }
        }
        val act = activePath
        if (act != null && (act == oldPath || act.startsWith("$oldPath/"))) {
            activePath = newPath + act.removePrefix(oldPath)
        }
        if (changed) persistSession()
    }

    fun deleteItem(path: String) {
        if (!repo.delete(project, path)) {
            showMessage("تعذر الحذف")
            return
        }
        tabs.filter { it.path == path || it.path.startsWith("$path/") }
            .map { it.path }
            .forEach { closeTabNow(it) }
        expanded = expanded.filterNot { it == path || it.startsWith("$path/") }.toSet()
        refreshTree()
    }

    // ------------------------------------------------------------ التبويبات
    fun openFile(path: String) {
        if (tabs.any { it.path == path }) {
            selectTab(path)
            return
        }
        when (val r = repo.read(project, path)) {
            is ReadResult.Ok -> {
                tabs = tabs + OpenTab(path, r.text, r.text)
                activePath = path
                persistSession()
            }
            is ReadResult.Fail -> showMessage(r.reason)
        }
    }

    fun selectTab(path: String) {
        activePath = path
        persistSession()
    }

    fun requestCloseTab(path: String) {
        val tab = tabs.firstOrNull { it.path == path } ?: return
        if (tab.dirty) pendingClose = path else closeTabNow(path)
    }

    fun resolveClose(save: Boolean) {
        val path = pendingClose ?: return
        pendingClose = null
        if (save) {
            viewModelScope.launch {
                saveTab(path)
                closeTabNow(path)
            }
        } else {
            closeTabNow(path)
        }
    }

    fun cancelClose() {
        pendingClose = null
    }

    private fun closeTabNow(path: String) {
        autoSaveJobs.remove(path)?.cancel()
        controller.drop(path)
        val idx = tabs.indexOfFirst { it.path == path }
        if (idx < 0) return
        tabs = tabs.filterIndexed { i, _ -> i != idx }
        if (activePath == path) {
            activePath = tabs.getOrNull(minOf(idx, tabs.lastIndex))?.path
        }
        persistSession()
    }

    // --------------------------------------------------------------- الحفظ
    private fun applyText(path: String, text: String) {
        val idx = tabs.indexOfFirst { it.path == path }
        if (idx < 0 || tabs[idx].text == text) return
        tabs = tabs.toMutableList().also { it[idx] = it[idx].copy(text = text) }
    }

    private fun onEditorChanged(path: String, text: String) {
        applyText(path, text)
        if (autoSave && tabs.any { it.path == path && it.dirty }) scheduleAutoSave(path)
    }

    private fun scheduleAutoSave(path: String) {
        autoSaveJobs[path]?.cancel()
        autoSaveJobs[path] = viewModelScope.launch {
            delay(1200)
            saveTab(path)
        }
    }

    private fun markSaved(path: String, text: String) {
        tabs = tabs.map { if (it.path == path) it.copy(savedText = text) else it }
    }

    private suspend fun saveTab(path: String): Boolean {
        val tab = tabs.firstOrNull { it.path == path } ?: return false
        val text = tab.text
        val proj = project
        val ok = withContext(Dispatchers.IO) { repo.write(proj, path, text) }
        if (ok) {
            markSaved(path, text)
        } else {
            showMessage("تعذر حفظ " + tab.name)
            CrashLogger.log(getApplication(), "save", "failed: " + proj + "/" + path)
        }
        return ok
    }

    private fun saveAllDirtyBlocking() {
        val proj = project
        for (t in tabs) {
            if (t.dirty && repo.write(proj, t.path, t.text)) markSaved(t.path, t.text)
        }
    }

    /** حفظ الملف النشط بعد قراءة آخر نص من المحرر. */
    fun saveActive() {
        val path = activePath ?: return
        controller.getText(path) { text ->
            if (text != null) applyText(path, text)
            viewModelScope.launch {
                if (saveTab(path)) showMessage("تم الحفظ")
            }
        }
    }

    /** يقرأ نص الملف النشط من المحرر ثم يحفظ كل التبويبات المعدّلة، ثم ينفذ onDone. */
    private fun flushAndSave(onDone: () -> Unit) {
        val path = activePath
        val proceed: () -> Unit = {
            val proj = project
            val dirty = tabs.filter { it.dirty }.map { Pair(it.path, it.text) }
            viewModelScope.launch {
                val results = withContext(Dispatchers.IO) {
                    dirty.map { Triple(it.first, it.second, repo.write(proj, it.first, it.second)) }
                }
                results.forEach { if (it.third) markSaved(it.first, it.second) }
                onDone()
            }
        }
        if (path == null) {
            proceed()
        } else {
            controller.getText(path) { text ->
                if (text != null) applyText(path, text)
                proceed()
            }
        }
    }

    fun onAppStopped() {
        saveAllDirtyBlocking()
        persistSession()
    }

    override fun onCleared() {
        saveAllDirtyBlocking()
        super.onCleared()
    }

    // ------------------------------------------------------------- الإعدادات
    fun updateFontSize(v: Int) {
        fontSize = v
        store.fontSize = v
    }

    fun updateWordWrap(v: Boolean) {
        wordWrap = v
        store.wordWrap = v
    }

    fun updateAutoSave(v: Boolean) {
        autoSave = v
        store.autoSave = v
    }

    // ----------------------------------------------------------------- المحرر
    fun editorCmd(name: String) = controller.cmd(name)

    fun insertText(text: String) = controller.insert(text)

    fun focusEditor() = controller.focusEditor()

    private fun addEditorLog(line: String) {
        editorLog.add(line)
        if (editorLog.size > 120) editorLog.removeAt(0)
    }

    fun clearEditorLog() {
        editorLog.clear()
    }

    fun editorLogText(): String = editorLog.joinToString("\n")

    // ---------------------------------------------------------------- الطرفية
    fun togglePanel() {
        panelMode = if (panelMode == PanelMode.HIDDEN) PanelMode.HALF else PanelMode.HIDDEN
    }

    fun toggleExpandPanel() {
        panelMode = if (panelMode == PanelMode.FULL) PanelMode.HALF else PanelMode.FULL
    }

    fun hidePanel() {
        panelMode = PanelMode.HIDDEN
    }

    fun clearTerminal() {
        terminalLog = listOf()
    }

    fun terminalText(): String = terminalLog.joinToString("\n\n") { e ->
        buildString {
            append("$ ").append(e.command)
            if (e.stdout.isNotBlank()) append('\n').append(e.stdout.trimEnd())
            if (e.stderr.isNotBlank()) append('\n').append(e.stderr.trimEnd())
            if (e.note.isNotBlank()) append('\n').append(e.note)
            if (e.exitCode != null) append("\n[exit ").append(e.exitCode).append(']')
        }
    }

    fun runActive() {
        val path = activePath
        if (path == null) {
            showMessage("افتح ملفاً أولاً")
            return
        }
        val line = ScriptBuilder.runLine(path)
        if (line == null) {
            showMessage("لا يوجد أمر تشغيل لهذا النوع من الملفات")
            return
        }
        flushAndSave { execute(line) }
    }

    fun runCommand(command: String) {
        flushAndSave { execute(command) }
    }

    fun cancelEntry(id: Long) {
        updateEntry(id) { if (it.status == EntryStatus.RUNNING) it.copy(status = EntryStatus.CANCELLED) else it }
    }

    private fun addEntry(e: TerminalEntry) {
        terminalLog = (terminalLog + e).takeLast(60)
    }

    private fun updateEntry(id: Long, change: (TerminalEntry) -> TerminalEntry) {
        terminalLog = terminalLog.map { if (it.id == id) change(it) else it }
    }

    private fun buildScript(proj: String, active: String?, command: String): Pair<String, String> {
        val all = repo.collectTextFiles(proj)
        val total = all.sumOf { ScriptBuilder.base64Size(it.second) }
        if (total <= TermuxRunner.MAX_SYNC_BASE64) {
            return Pair(ScriptBuilder.build(proj, all, command), "")
        }
        val only = all.filter { it.first == active }
        val onlySize = only.sumOf { ScriptBuilder.base64Size(it.second) }
        return if (onlySize <= TermuxRunner.MAX_SYNC_BASE64) {
            Pair(ScriptBuilder.build(proj, only, command), "المشروع كبير؛ أُرسل الملف النشط فقط إلى Termux.")
        } else {
            Pair(ScriptBuilder.build(proj, emptyList(), command), "الملف أكبر من حد الإرسال إلى Termux (نحو 65 كيلوبايت).")
        }
    }

    private fun execute(command: String) {
        val ctx = getApplication<Application>()
        if (panelMode == PanelMode.HIDDEN) panelMode = PanelMode.HALF
        val id = nextExecId++

        when (TermuxRunner.status(ctx)) {
            TermuxStatus.NOT_INSTALLED -> {
                addEntry(TerminalEntry(id, command, EntryStatus.DONE, note = "Termux غير مثبت. ثبّته من F-Droid ثم أعد المحاولة."))
                return
            }
            TermuxStatus.NO_PERMISSION -> {
                addEntry(TerminalEntry(id, command, EntryStatus.DONE, note = "لم تُمنح صلاحية تشغيل الأوامر. اضغط «منح الصلاحية» أعلى الطرفية."))
                return
            }
            TermuxStatus.READY -> Unit
        }

        val proj = project
        val active = activePath
        viewModelScope.launch {
            val built = withContext(Dispatchers.IO) { buildScript(proj, active, command) }
            addEntry(TerminalEntry(id, command, note = built.second))
            try {
                TermuxRunner.send(ctx, id, built.first)
            } catch (e: Exception) {
                CrashLogger.recordError(ctx, "termux", e)
                updateEntry(id) {
                    it.copy(status = EntryStatus.DONE, note = "تعذر إرسال الأمر إلى Termux: " + e.message)
                }
                return@launch
            }
            delay(15_000)
            updateEntry(id) { if (it.status == EntryStatus.RUNNING) it.copy(slow = true) else it }
        }
    }

    fun onTermuxResult(intent: Intent) {
        val res = TermuxRunner.parse(intent) ?: return
        updateEntry(res.execId) { e ->
            if (e.status != EntryStatus.RUNNING) {
                e
            } else {
                val hint = if (res.exitCode == 127) ScriptBuilder.installHint(e.command) else null
                val notes = listOfNotNull(
                    e.note.takeIf { it.isNotBlank() },
                    res.errMsg.takeIf { it.isNotBlank() },
                    hint?.let { "الأداة غير مثبتة. جرّب في الطرفية: $it" },
                )
                e.copy(
                    status = EntryStatus.DONE,
                    stdout = res.stdout,
                    stderr = res.stderr,
                    exitCode = res.exitCode,
                    note = notes.joinToString("\n"),
                    slow = false,
                )
            }
        }
    }
}
