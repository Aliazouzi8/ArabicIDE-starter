package com.aliazouzi.arabicide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliazouzi.arabicide.termux.TermuxStatus
import com.aliazouzi.arabicide.ui.theme.IdeColors
import kotlinx.coroutines.delay

/** أوامر تفعيل الربط: تُلصق مرة واحدة داخل Termux. */
const val TERMUX_SETUP_COMMANDS = """mkdir -p ~/.termux
grep -qxF 'allow-external-apps = true' ~/.termux/termux.properties 2>/dev/null || echo 'allow-external-apps = true' >> ~/.termux/termux.properties
termux-reload-settings"""

const val TERMUX_BASE_PACKAGES = """pkg update -y
pkg install -y python nodejs"""

private val TOOL_COMMANDS = listOf(
    "Python" to "pkg install -y python",
    "Node.js" to "pkg install -y nodejs",
    "C / C++" to "pkg install -y clang",
    "Java" to "pkg install -y openjdk-17",
    "PHP" to "pkg install -y php",
    "Ruby" to "pkg install -y ruby",
    "Go" to "pkg install -y golang",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermuxGuideSheet(
    status: TermuxStatus,
    onCopy: (String) -> Unit,
    onOpenTermux: () -> Unit,
    onOpenInstallPage: () -> Unit,
    onGrantPermission: () -> Unit,
    onRunTest: () -> Unit,
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("ربط ArabicIDE مع Termux", style = MaterialTheme.typography.titleLarge)

            val (statusText, statusColor) = when (status) {
                TermuxStatus.READY -> Pair(
                    "Termux مثبت والصلاحية ممنوحة. أكمل الخطوات 3 و4 إن لم تفعلهما، ثم جرّب الاختبار.",
                    IdeColors.Green,
                )
                TermuxStatus.NO_PERMISSION -> Pair(
                    "Termux مثبت، لكن صلاحية التشغيل لم تُمنح بعد (الخطوة 5).",
                    IdeColors.Yellow,
                )
                TermuxStatus.NOT_INSTALLED -> Pair(
                    "Termux غير مثبت على هذا الجهاز (الخطوة 1).",
                    IdeColors.Red,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(statusColor))
                Spacer(Modifier.width(8.dp))
                Text(statusText, fontSize = 13.sp, color = statusColor)
            }

            StepCard(1, "ثبّت Termux من F-Droid", "نسخة Google Play قديمة ولا تعمل مع هذا الربط.") {
                OutlinedButton(onClick = onOpenInstallPage) { Text("فتح صفحة Termux في F-Droid") }
            }

            StepCard(2, "افتح Termux مرة واحدة", "انتظر حتى تنتهي التهيئة الأولى وتظهر علامة $ ثم ابقَ داخل Termux.") {
                OutlinedButton(onClick = onOpenTermux) { Text("فتح Termux") }
            }

            StepCard(
                3,
                "انسخ الأوامر والصقها في Termux",
                "اضغط نسخ، ثم افتح Termux، واضغط مطولاً في أي مكان واختر لصق. هذه الأوامر تفعّل السماح للتطبيقات الخارجية. لا تظهر رسالة نجاح، وهذا طبيعي.",
            ) {
                CodeBlock(TERMUX_SETUP_COMMANDS)
                CopyButton("نسخ أوامر التفعيل", TERMUX_SETUP_COMMANDS, onCopy)
            }

            StepCard(
                4,
                "أغلق Termux بالكامل ثم افتحه",
                "من إشعار Termux اضغط Exit، ثم افتحه من جديد حتى يُطبَّق الإعداد.",
            )

            StepCard(
                5,
                "امنح ArabicIDE صلاحية تشغيل الأوامر",
                "اضغط الزر ووافق في النافذة. إن لم تظهر النافذة فأعد تثبيت ArabicIDE بعد تثبيت Termux، أو افتح إعدادات الهاتف ← التطبيقات ← ArabicIDE ← الأذونات.",
            ) {
                Button(onClick = onGrantPermission, enabled = status != TermuxStatus.NOT_INSTALLED) {
                    Text(if (status == TermuxStatus.READY) "الصلاحية ممنوحة" else "منح الصلاحية")
                }
            }

            StepCard(
                6,
                "اختبر الاتصال",
                "يرسل الأمر echo إلى Termux. إن ظهرت كلمة ArabicIDE-OK في الطرفية فالربط يعمل.",
            ) {
                Button(onClick = onRunTest, enabled = status == TermuxStatus.READY) { Text("اختبار الاتصال") }
            }

            HorizontalDivider(color = IdeColors.Border)

            Text("تثبيت الأدوات (حسب لغتك)", style = MaterialTheme.typography.titleMedium)
            Text(
                "الأداة تُثبَّت مرة واحدة داخل Termux. الصق الأمر هناك كما في الخطوة 3.",
                fontSize = 13.sp,
                color = IdeColors.TextSecondary,
            )
            CodeBlock(TERMUX_BASE_PACKAGES)
            CopyButton("نسخ تثبيت Python وNode.js", TERMUX_BASE_PACKAGES, onCopy)

            Surface(color = IdeColors.SurfaceHigh, shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    TOOL_COMMANDS.forEach { (name, cmd) -> ToolRow(name, cmd, onCopy) }
                }
            }

            HorizontalDivider(color = IdeColors.Border)

            Text("إذا لم تعمل الأوامر", style = MaterialTheme.typography.titleMedium)
            Text(
                "• لا تصل نتيجة: كرر الخطوة 4، وتأكد أن Termux شُغّل مرة واحدة على الأقل.\n" +
                    "• رمز الخروج 127: الأداة غير مثبتة، وستظهر لك الطرفية الأمر المناسب.\n" +
                    "• لا يظهر Termux في القائمة: ثبّته من F-Droid لا من Google Play.",
                fontSize = 13.sp,
                color = IdeColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun StepCard(
    number: Int,
    title: String,
    body: String,
    content: @Composable () -> Unit = {},
) {
    Surface(color = IdeColors.SurfaceHigh, shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(12.dp)) {
            Box(
                Modifier.size(26.dp).clip(CircleShape).background(IdeColors.Accent),
                contentAlignment = Alignment.Center,
            ) {
                Text("$number", color = Color(0xFF002B52), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(body, color = IdeColors.TextSecondary, fontSize = 13.sp)
                content()
            }
        }
    }
}

@Composable
private fun CodeBlock(code: String) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Surface(color = IdeColors.Panel, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            SelectionContainer {
                Text(
                    code,
                    Modifier.padding(10.dp).horizontalScroll(rememberScrollState()),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = IdeColors.TextPrimary,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun CopyButton(label: String, text: String, onCopy: (String) -> Unit) {
    var done by remember { mutableStateOf(false) }
    LaunchedEffect(done) {
        if (done) {
            delay(1500)
            done = false
        }
    }
    Button(onClick = {
        onCopy(text)
        done = true
    }) {
        Icon(
            if (done) Icons.Filled.Check else Icons.Filled.ContentCopy,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(if (done) "تم النسخ" else label)
    }
}

@Composable
private fun ToolRow(name: String, command: String, onCopy: (String) -> Unit) {
    var done by remember { mutableStateOf(false) }
    LaunchedEffect(done) {
        if (done) {
            delay(1500)
            done = false
        }
    }
    Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(command, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = IdeColors.TextSecondary)
            }
        }
        IconButton(onClick = {
            onCopy(command)
            done = true
        }) {
            Icon(
                if (done) Icons.Filled.Check else Icons.Filled.ContentCopy,
                contentDescription = "نسخ أمر تثبيت $name",
                tint = if (done) IdeColors.Green else IdeColors.TextSecondary,
            )
        }
    }
}
