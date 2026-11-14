package com.aliazouzi.arabicide.diagnostics

import android.content.Context
import android.os.Build
import android.os.Process
import androidx.webkit.WebViewCompat
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * تسجيل التعطل والأخطاء داخل files/logs ثم بناء تقرير نصي يمكن مشاركته مع المطور.
 * التقرير لا يتضمن محتوى ملفات المستخدم عمداً.
 */
object CrashLogger {

    private const val DIR = "logs"
    private const val APP_LOG = "app.log"
    private const val PENDING = "crash_pending"
    private const val REPORT_FILE = "arabicide-report.txt"
    private const val MAX_LOG_CHARS = 200_000
    private const val KEEP_LOG_CHARS = 100_000
    private const val MAX_CRASH_FILES = 5

    private val lock = Any()

    @Volatile
    private var installed = false

    private fun dir(context: Context): File = File(context.filesDir, DIR).also { it.mkdirs() }

    private fun stamp(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

    private fun stackTrace(t: Throwable): String {
        val sw = StringWriter()
        t.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }

    /** يُستدعى مرة واحدة عند بدء التطبيق. */
    fun install(context: Context) {
        if (installed) return
        installed = true
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeCrash(app, thread, throwable)
            } catch (e: Throwable) {
                // لا نسمح لفشل المسجّل نفسه بإخفاء التعطل الأصلي
            }
            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            } else {
                Process.killProcess(Process.myPid())
            }
        }
    }

    private fun writeCrash(context: Context, thread: Thread, t: Throwable) {
        synchronized(lock) {
            val d = dir(context)
            val name = "crash-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".txt"
            File(d, name).writeText("[" + stamp() + "] CRASH on thread \"" + thread.name + "\"\n" + stackTrace(t) + "\n")
            File(d, PENDING).writeText("1")
            crashFiles(context).drop(MAX_CRASH_FILES).forEach { it.delete() }
        }
    }

    private fun crashFiles(context: Context): List<File> =
        dir(context).listFiles()
            ?.filter { it.name.startsWith("crash-") }
            ?.sortedByDescending { it.name }
            ?: emptyList()

    /** سطر في سجل التطبيق (أخطاء غير قاتلة وأحداث مهمة). */
    fun log(context: Context, tag: String, message: String) {
        try {
            synchronized(lock) {
                val f = File(dir(context), APP_LOG)
                val lines = message.trimEnd().split('\n')
                val sb = StringBuilder()
                sb.append(stamp()).append(" [").append(tag).append("] ").append(lines.first()).append('\n')
                lines.drop(1).forEach { sb.append("    ").append(it).append('\n') }
                f.appendText(sb.toString())
                if (f.length() > MAX_LOG_CHARS) {
                    f.writeText(f.readText().takeLast(KEEP_LOG_CHARS))
                }
            }
        } catch (e: Throwable) {
            // تجاهل
        }
    }

    /** خطأ غير قاتل مع تتبّع الاستدعاءات. */
    fun recordError(context: Context, tag: String, t: Throwable) {
        log(context, tag, t.javaClass.name + ": " + t.message + "\n" + stackTrace(t))
    }

    fun hasPendingCrash(context: Context): Boolean = File(dir(context), PENDING).exists()

    fun acknowledge(context: Context) {
        File(dir(context), PENDING).delete()
    }

    fun clear(context: Context) {
        synchronized(lock) {
            dir(context).listFiles()?.forEach { f ->
                if (f.name.startsWith("crash-") || f.name == APP_LOG || f.name == PENDING) f.delete()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun appVersion(context: Context): String = try {
        val pi = context.packageManager.getPackageInfo(context.packageName, 0)
        val code = if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode.toString() else pi.versionCode.toString()
        pi.versionName + " (" + code + ")"
    } catch (e: Exception) {
        "unknown"
    }

    private fun webViewVersion(context: Context): String = try {
        val pkg = WebViewCompat.getCurrentWebViewPackage(context)
        if (pkg == null) "unknown" else pkg.packageName + " " + pkg.versionName
    } catch (e: Throwable) {
        "unknown"
    }

    /** التقرير الكامل: معلومات الجهاز، التعطلات، السجل، وسجل المحرر في هذه الجلسة. */
    fun buildReport(context: Context, extra: List<String>, editorLog: List<String>): String {
        val sb = StringBuilder()
        sb.append("=== ArabicIDE diagnostic report ===\n")
        sb.append("Generated: ").append(stamp()).append(" (").append(TimeZone.getDefault().id).append(")\n")
        sb.append("App: ").append(appVersion(context)).append('\n')
        sb.append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
            .append(" / Android ").append(Build.VERSION.RELEASE)
            .append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n")
        sb.append("WebView: ").append(webViewVersion(context)).append('\n')
        sb.append("Locale: ").append(Locale.getDefault()).append('\n')
        extra.forEach { sb.append(it).append('\n') }

        sb.append("\n--- Crashes (latest first) ---\n")
        val crashes = crashFiles(context)
        if (crashes.isEmpty()) {
            sb.append("none\n")
        } else {
            crashes.forEach { sb.append(it.readText()).append('\n') }
        }

        sb.append("\n--- App log (tail) ---\n")
        val logFile = File(dir(context), APP_LOG)
        if (logFile.exists() && logFile.length() > 0) {
            sb.append(logFile.readText().takeLast(40_000))
        } else {
            sb.append("empty\n")
        }

        sb.append("\n--- Editor console (this session) ---\n")
        if (editorLog.isEmpty()) sb.append("empty\n") else editorLog.forEach { sb.append(it).append('\n') }
        return sb.toString()
    }

    /** يكتب التقرير في ملف داخل files/logs ليُشارَك كمرفق. */
    fun writeReportFile(context: Context, report: String): File {
        val f = File(dir(context), REPORT_FILE)
        f.writeText(report)
        return f
    }
}
