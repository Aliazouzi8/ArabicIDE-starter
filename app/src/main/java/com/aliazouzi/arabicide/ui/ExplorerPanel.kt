package com.aliazouzi.arabicide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aliazouzi.arabicide.data.TreeRow
import com.aliazouzi.arabicide.ui.theme.IdeColors

/** لون أيقونة الملف حسب الامتداد. */
internal fun fileTint(name: String): Color =
    when (name.substringAfterLast('.', "").lowercase()) {
        "py" -> IdeColors.Yellow
        "js", "mjs", "ts", "json" -> IdeColors.Yellow
        "kt", "java" -> IdeColors.Purple
        "html", "css" -> IdeColors.Orange
        "md", "txt" -> IdeColors.Accent
        "sh" -> IdeColors.Green
        else -> IdeColors.TextSecondary
    }

@Composable
fun ExplorerPanel(
    project: String,
    rows: List<TreeRow>,
    expanded: Set<String>,
    activePath: String?,
    onProjects: () -> Unit,
    onToggleDir: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    onNewFile: (String) -> Unit,
    onNewFolder: (String) -> Unit,
    onRename: (TreeRow) -> Unit,
    onDelete: (TreeRow) -> Unit,
    onCopyPath: (TreeRow) -> Unit,
    onRefresh: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        // رأس المشروع: الضغط يفتح قائمة المشاريع
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onProjects)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("المشروع", style = MaterialTheme.typography.labelSmall, color = IdeColors.TextSecondary)
                Text(project, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "تبديل المشروع")
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onNewFile("") }) { Icon(Icons.Filled.NoteAdd, contentDescription = "ملف جديد") }
            IconButton(onClick = { onNewFolder("") }) { Icon(Icons.Filled.CreateNewFolder, contentDescription = "مجلد جديد") }
            IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, contentDescription = "تحديث") }
        }
        HorizontalDivider(color = IdeColors.Border)

        if (rows.isEmpty()) {
            Text(
                "المشروع فارغ. أنشئ ملفاً جديداً من الأزرار أعلاه.",
                Modifier.padding(16.dp),
                color = IdeColors.TextSecondary,
            )
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(rows, key = { it.path }) { row ->
                TreeItem(
                    row = row,
                    isOpen = row.path in expanded,
                    selected = row.path == activePath,
                    onToggleDir = onToggleDir,
                    onOpenFile = onOpenFile,
                    onNewFile = onNewFile,
                    onNewFolder = onNewFolder,
                    onRename = onRename,
                    onDelete = onDelete,
                    onCopyPath = onCopyPath,
                )
            }
        }
    }
}

@Composable
private fun TreeItem(
    row: TreeRow,
    isOpen: Boolean,
    selected: Boolean,
    onToggleDir: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    onNewFile: (String) -> Unit,
    onNewFolder: (String) -> Unit,
    onRename: (TreeRow) -> Unit,
    onDelete: (TreeRow) -> Unit,
    onCopyPath: (TreeRow) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(if (selected) IdeColors.SurfaceHigh else Color.Transparent)
            .clickable { if (row.isDir) onToggleDir(row.path) else onOpenFile(row.path) }
            .padding(start = (10 + row.depth * 16).dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (row.isDir) {
            Icon(
                if (isOpen) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = IdeColors.TextSecondary,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                if (isOpen) Icons.Filled.FolderOpen else Icons.Filled.Folder,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = IdeColors.Accent,
            )
        } else {
            Spacer(Modifier.width(22.dp))
            Icon(
                Icons.Filled.Description,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = fileTint(row.name),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            row.name,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "خيارات", modifier = Modifier.size(20.dp))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (row.isDir) {
                    DropdownMenuItem(
                        text = { Text("ملف جديد") },
                        leadingIcon = { Icon(Icons.Filled.NoteAdd, null) },
                        onClick = {
                            menuOpen = false
                            onNewFile(row.path)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("مجلد جديد") },
                        leadingIcon = { Icon(Icons.Filled.CreateNewFolder, null) },
                        onClick = {
                            menuOpen = false
                            onNewFolder(row.path)
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text("إعادة تسمية") },
                    leadingIcon = { Icon(Icons.Filled.DriveFileRenameOutline, null) },
                    onClick = {
                        menuOpen = false
                        onRename(row)
                    },
                )
                DropdownMenuItem(
                    text = { Text("نسخ المسار") },
                    leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                    onClick = {
                        menuOpen = false
                        onCopyPath(row)
                    },
                )
                DropdownMenuItem(
                    text = { Text("حذف", color = IdeColors.Red) },
                    leadingIcon = { Icon(Icons.Filled.Delete, null, tint = IdeColors.Red) },
                    onClick = {
                        menuOpen = false
                        onDelete(row)
                    },
                )
            }
        }
    }
}
