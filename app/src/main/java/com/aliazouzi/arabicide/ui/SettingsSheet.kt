package com.aliazouzi.arabicide.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliazouzi.arabicide.ui.theme.IdeColors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    fontSize: Int,
    wordWrap: Boolean,
    autoSave: Boolean,
    backend: String,
    editorLog: List<String>,
    onFontSize: (Int) -> Unit,
    onWordWrap: (Boolean) -> Unit,
    onAutoSave: (Boolean) -> Unit,
    onCopyLog: () -> Unit,
    onClearLog: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = IdeColors.Surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("الإعدادات", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            Text("حجم الخط: $fontSize", style = MaterialTheme.typography.bodyLarge)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Slider(
                    value = fontSize.toFloat(),
                    onValueChange = { onFontSize(it.roundToInt()) },
                    valueRange = 12f..24f,
                    steps = 11,
                )
            }

            SettingSwitch("التفاف الأسطر الطويلة", wordWrap, onWordWrap)
            SettingSwitch("حفظ تلقائي بعد التوقف عن الكتابة", autoSave, onAutoSave)

            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = IdeColors.Border)

            val editorName = when (backend) {
                "monaco" -> "Monaco (محلي)"
                "textarea" -> "محرر نصي بديل (تعذر تحميل Monaco)"
                else -> "قيد التحميل"
            }
            Text("المحرر: $editorName", style = MaterialTheme.typography.bodyMedium)
            Text("الإصدار 0.2.2", style = MaterialTheme.typography.bodySmall, color = IdeColors.TextSecondary)

            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = IdeColors.Border)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("سجل المحرر", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onCopyLog, enabled = editorLog.isNotEmpty()) { Text("نسخ") }
                TextButton(onClick = onClearLog, enabled = editorLog.isNotEmpty()) { Text("مسح") }
            }
            if (editorLog.isEmpty()) {
                Text("لا توجد رسائل.", color = IdeColors.TextSecondary, fontSize = 13.sp)
            } else {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column {
                        editorLog.takeLast(40).forEach { line ->
                            Text(line, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = IdeColors.TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
