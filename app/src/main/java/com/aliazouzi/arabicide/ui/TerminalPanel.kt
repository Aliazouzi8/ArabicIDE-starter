package com.aliazouzi.arabicide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliazouzi.arabicide.EntryStatus
import com.aliazouzi.arabicide.TerminalEntry
import com.aliazouzi.arabicide.termux.TermuxStatus
import com.aliazouzi.arabicide.ui.theme.IdeColors

private val QUICK_COMMANDS = listOf(
    "python3 --version",
    "node -v",
    "pwd",
    "ls -la",
    "pkg update",
    "pkg install -y python",
    "pkg install -y nodejs",
)

private val Mono = FontFamily.Monospace

@Composable
fun TerminalPanel(
    log: List<TerminalEntry>,
    status: TermuxStatus,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onClose: () -> Unit,
    onClear: () -> Unit,
    onCopyAll: () -> Unit,
    onRun: (String) -> Unit,
    onCancel: (Long) -> Unit,
    onGrantPermission: () -> Unit,
    onOpenInstallPage: () -> Unit,
    onHelp: () -> Unit,
    onInputFocus: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(log) {
        if (log.isNotEmpty()) listState.animateScrollToItem(log.lastIndex)
    }

    val submit = {
        val c = input.trim()
        if (c.isNotEmpty()) {
            onRun(c)
            input = ""
        }
    }

    Column(modifier.background(IdeColors.Panel)) {
        HorizontalDivider(color = IdeColors.Border)

        // ------------------------------------------------------------ الرأس
        Row(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(IdeColors.Surface)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Terminal, contentDescription = null, tint = IdeColors.Accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("الطرفية", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when (status) {
                            TermuxStatus.READY -> IdeColors.Green
                            TermuxStatus.NO_PERMISSION -> IdeColors.Yellow
                            TermuxStatus.NOT_INSTALLED -> IdeColors.Red
                        },
                    ),
            )
            Spacer(Modifier.weight(1f))
            PanelIcon(Icons.Filled.Info, "دليل ربط Termux", onHelp)
            PanelIcon(Icons.Filled.ContentCopy, "نسخ الناتج", onCopyAll)
            PanelIcon(Icons.Filled.DeleteSweep, "مسح", onClear)
            PanelIcon(
                if (expanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                if (expanded) "تصغير" else "تكبير",
                onToggleExpand,
            )
            PanelIcon(Icons.Filled.Close, "إغلاق", onClose)
        }

        // ------------------------------------------------- تنبيه حالة Termux
        when (status) {
            TermuxStatus.NOT_INSTALLED -> Banner(
                "Termux غير مثبت على هذا الجهاز.",
                "تثبيت من F-Droid",
                onOpenInstallPage,
                onHelp,
            )
            TermuxStatus.NO_PERMISSION -> Banner(
                "يلزم منح ArabicIDE صلاحية تشغيل الأوامر في Termux.",
                "منح الصلاحية",
                onGrantPermission,
                onHelp,
            )
            TermuxStatus.READY -> Unit
        }

        // ------------------------------------------------------------ السجل
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            SelectionContainer(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                ) {
                    if (log.isEmpty()) {
                        item {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    "اضغط ▶ لتشغيل الملف المفتوح، أو اكتب أمراً بالأسفل.",
                                    Modifier.padding(vertical = 10.dp),
                                    color = IdeColors.TextSecondary,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                    items(log, key = { it.id }) { entry ->
                        EntryView(entry, onCancel)
                    }
                }
            }
        }

        // ------------------------------------------------------ أوامر سريعة
        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(QUICK_COMMANDS) { cmd ->
                AssistChip(
                    onClick = { onRun(cmd) },
                    label = { Text(cmd, fontFamily = Mono, fontSize = 12.sp) },
                )
            }
        }

        // ---------------------------------------------------------- إدخال أمر
        Row(
            Modifier
                .fillMaxWidth()
                .background(IdeColors.Surface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { onInputFocus(it.isFocused) },
                    singleLine = true,
                    prefix = { Text("$ ", color = IdeColors.Accent, fontFamily = Mono) },
                    textStyle = TextStyle(fontFamily = Mono, fontSize = 14.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { submit() }),
                )
            }
            IconButton(onClick = { submit() }, enabled = input.isNotBlank()) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "تشغيل الأمر")
            }
        }
    }
}

@Composable
private fun PanelIcon(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = desc, modifier = Modifier.size(20.dp), tint = IdeColors.TextSecondary)
    }
}

@Composable
private fun Banner(text: String, action: String, onAction: () -> Unit, onGuide: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(IdeColors.SurfaceHigh)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, Modifier.weight(1f), color = IdeColors.Yellow, fontSize = 12.sp)
        TextButton(onClick = onGuide) { Text("الدليل") }
        TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun EntryView(e: TerminalEntry, onCancel: (Long) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text("$ " + e.command, color = IdeColors.Accent, fontFamily = Mono, fontSize = 13.sp)

        if (e.stdout.isNotBlank()) {
            Text(e.stdout.trimEnd(), color = IdeColors.TextPrimary, fontFamily = Mono, fontSize = 13.sp)
        }
        if (e.stderr.isNotBlank()) {
            Text(e.stderr.trimEnd(), color = IdeColors.Red, fontFamily = Mono, fontSize = 13.sp)
        }
        if (e.note.isNotBlank()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(e.note, color = IdeColors.Yellow, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
            }
        }

        when (e.status) {
            EntryStatus.RUNNING -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text("جارٍ التنفيذ…", color = IdeColors.TextSecondary, fontSize = 12.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        TextButton(onClick = { onCancel(e.id) }) { Text("إلغاء الانتظار", fontSize = 12.sp) }
                    }
                }
                if (e.slow) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            "لم تصل نتيجة بعد. إن استمر ذلك تأكد من allow-external-apps = true في Termux وأنك شغّلت Termux مرة واحدة.",
                            color = IdeColors.Yellow,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            EntryStatus.DONE -> {
                val code = e.exitCode
                if (code != null) {
                    Text(
                        "[exit $code]",
                        color = if (code == 0) IdeColors.Green else IdeColors.Red,
                        fontFamily = Mono,
                        fontSize = 12.sp,
                    )
                }
            }
            EntryStatus.CANCELLED -> {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text("أُلغي الانتظار", color = IdeColors.TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}
