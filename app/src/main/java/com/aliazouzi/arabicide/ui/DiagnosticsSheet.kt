package com.aliazouzi.arabicide.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliazouzi.arabicide.ui.theme.IdeColors

/** عرض تقرير الأخطاء ومشاركته مع المطور. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsSheet(
    report: String,
    hasCrash: Boolean,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onClear: () -> Unit,
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("تقرير الأخطاء", style = MaterialTheme.typography.titleLarge)
            Text(
                if (hasCrash) {
                    "حدث تعطل في تشغيل سابق. شارك التقرير مع المطور ليعالج السبب."
                } else {
                    "لا يوجد تعطل مسجّل. يمكنك مشاركة التقرير عند وجود مشكلة في المحرر أو الطرفية."
                },
                fontSize = 13.sp,
                color = if (hasCrash) IdeColors.Yellow else IdeColors.TextSecondary,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onShare) { Text("مشاركة التقرير") }
                OutlinedButton(onClick = onCopy) { Text("نسخ") }
                TextButton(onClick = onClear) { Text("مسح السجل") }
            }

            Surface(
                color = IdeColors.Panel,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
            ) {
                SelectionContainer {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(10.dp)) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                report,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = IdeColors.TextPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}
