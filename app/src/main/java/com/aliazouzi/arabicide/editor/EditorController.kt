package com.aliazouzi.arabicide.editor

import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import org.json.JSONTokener

/**
 * جسر بين Kotlin ومحرر Monaco داخل WebView.
 * الرسائل القادمة من JavaScript تُقبل بأنواع محددة فقط.
 */
class EditorController {

    var webView: WebView? = null

    /** true بعد أن يرسل المحرر رسالة ready. */
    var ready by mutableStateOf(false)
        private set

    /** "monaco" أو "textarea" (بديل عند فشل تحميل Monaco). */
    var backend by mutableStateOf("")
        private set

    var languageLabel by mutableStateOf("")
        private set
    var cursorLine by mutableStateOf(1)
        private set
    var cursorCol by mutableStateOf(1)
        private set

    var onChanged: (String, String) -> Unit = { _, _ -> }
    var onSaveRequested: () -> Unit = {}
    var onLog: (String) -> Unit = {}

    fun markDetached() {
        ready = false
        backend = ""
    }

    fun handleMessage(raw: String) {
        try {
            val o = JSONObject(raw)
            when (o.optString("type")) {
                "ready" -> {
                    backend = o.optString("backend")
                    val reason = o.optString("reason")
                    if (reason.isNotEmpty()) onLog(reason)
                    ready = true
                }
                "changed" -> onChanged(o.optString("path"), o.optString("text"))
                "cursor" -> {
                    cursorLine = o.optInt("line", 1)
                    cursorCol = o.optInt("col", 1)
                    languageLabel = o.optString("lang")
                }
                "save" -> onSaveRequested()
                "log" -> onLog(o.optString("message"))
                else -> onLog("رسالة من المحرر بنوع غير معروف")
            }
        } catch (e: Exception) {
            onLog("رسالة غير مفهومة من المحرر")
        }
    }

    private fun q(s: String): String = JSONObject.quote(s)

    private fun eval(js: String) {
        val wv = webView ?: return
        if (!ready) return
        wv.evaluateJavascript(js, null)
    }

    fun show(path: String, text: String) = eval("ArabicIDEEditor.show(" + q(path) + "," + q(text) + ")")

    fun drop(path: String) = eval("ArabicIDEEditor.drop(" + q(path) + ")")

    fun options(fontSize: Int, wordWrap: Boolean) =
        eval("ArabicIDEEditor.options({fontSize:" + fontSize + ",wordWrap:" + wordWrap + "})")

    fun cmd(name: String) {
        webView?.requestFocus()
        eval("ArabicIDEEditor.cmd(" + q(name) + ")")
    }

    fun insert(text: String) {
        webView?.requestFocus()
        eval("ArabicIDEEditor.insert(" + q(text) + ")")
    }

    /** يقرأ النص الحالي من المحرر. يعيد null إذا لم يكن المحرر جاهزاً. */
    fun getText(path: String, callback: (String?) -> Unit) {
        val wv = webView
        if (wv == null || !ready) {
            callback(null)
            return
        }
        wv.evaluateJavascript("ArabicIDEEditor.getText(" + q(path) + ")") { raw ->
            val text = try {
                JSONTokener(raw ?: "null").nextValue() as? String
            } catch (e: Exception) {
                null
            }
            callback(text)
        }
    }
}
