package com.aliazouzi.arabicide.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.aliazouzi.arabicide.ui.theme.IdeColors

/** نافذة إدخال اسم. onConfirm يعيد رسالة خطأ أو null عند النجاح. */
@Composable
fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> String?,
) {
    var field by remember {
        mutableStateOf(TextFieldValue(initial, TextRange(0, initial.substringBeforeLast('.', initial).length)))
    }
    var error by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = field,
                onValueChange = {
                    field = it
                    error = null
                },
                singleLine = true,
                isError = error != null,
                supportingText = { error?.let { Text(it) } },
                textStyle = TextStyle(textDirection = TextDirection.Ltr),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val e = onConfirm(field.text)
                if (e == null) onDismiss() else error = e
            }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    danger: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (danger) IdeColors.Red else Color.Unspecified)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

@Composable
fun ProjectsDialog(
    projects: List<String>,
    current: String,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("المشاريع") },
        text = {
            LazyColumn(Modifier.heightIn(max = 320.dp)) {
                items(projects) { name ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(name)
                                onDismiss()
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            name,
                            Modifier.weight(1f).padding(vertical = 8.dp),
                            color = if (name == current) IdeColors.Accent else Color.Unspecified,
                            fontWeight = if (name == current) FontWeight.Bold else FontWeight.Normal,
                        )
                        if (projects.size > 1) {
                            IconButton(onClick = { onDelete(name) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "حذف المشروع")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onNew) { Text("مشروع جديد") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
    )
}
