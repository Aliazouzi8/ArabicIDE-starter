package com.aliazouzi.arabicide.termux

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

enum class TermuxStatus { NOT_INSTALLED, NO_PERMISSION, READY }

data class TermuxResult(
    val execId: Long,
    val stdout: String,
    val stderr: String,
    val exitCode: Int?,
    val errCode: Int?,
    val errMsg: String,
)

/** إرسال أوامر غير تفاعلية إلى Termux عبر RUN_COMMAND واستقبال نتيجتها. */
object TermuxRunner {

    const val PACKAGE = "com.termux"
    const val PERMISSION = "com.termux.permission.RUN_COMMAND"
    const val RESULT_ACTION = "com.aliazouzi.arabicide.TERMUX_RESULT"
    const val MAX_SYNC_BASE64 = 90_000

    private const val EXTRA_EXEC_ID = "arabicide_exec_id"
    private const val SERVICE = "com.termux.app.RunCommandService"
    private const val ACTION_RUN = "com.termux.RUN_COMMAND"
    private const val BASH = "/data/data/com.termux/files/usr/bin/bash"
    private const val HOME = "/data/data/com.termux/files/home"

    fun status(context: Context): TermuxStatus {
        val installed = try {
            context.packageManager.getPackageInfo(PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
        if (!installed) return TermuxStatus.NOT_INSTALLED
        val granted = ContextCompat.checkSelfPermission(context, PERMISSION) ==
            PackageManager.PERMISSION_GRANTED
        return if (granted) TermuxStatus.READY else TermuxStatus.NO_PERMISSION
    }

    /** يرسل السكربت إلى bash داخل Termux بوضع الخلفية. قد يرمي استثناءً عند الفشل. */
    fun send(context: Context, execId: Long, script: String) {
        val resultIntent = Intent(RESULT_ACTION)
            .setPackage(context.packageName)
            .putExtra(EXTRA_EXEC_ID, execId)
        val pending = PendingIntent.getBroadcast(
            context,
            (execId and 0x7fffffffL).toInt(),
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

        val intent = Intent(ACTION_RUN).apply {
            setClassName(PACKAGE, SERVICE)
            putExtra("com.termux.RUN_COMMAND_PATH", BASH)
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", script))
            putExtra("com.termux.RUN_COMMAND_WORKDIR", HOME)
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", pending)
        }
        context.startService(intent)
    }

    /** يحوّل النتيجة العائدة من Termux. يعيد null إذا لم تكن رسالة تخصنا. */
    fun parse(intent: Intent): TermuxResult? {
        val execId = intent.getLongExtra(EXTRA_EXEC_ID, -1L)
        if (execId < 0) return null
        val b = intent.getBundleExtra("result")
            ?: return TermuxResult(execId, "", "", null, null, "وصلت رسالة من Termux بدون نتيجة")
        return TermuxResult(
            execId = execId,
            stdout = b.getString("stdout").orEmpty(),
            stderr = b.getString("stderr").orEmpty(),
            exitCode = if (b.containsKey("exitCode")) b.getInt("exitCode") else null,
            errCode = if (b.containsKey("err")) b.getInt("err") else null,
            errMsg = b.getString("errmsg").orEmpty(),
        )
    }
}
