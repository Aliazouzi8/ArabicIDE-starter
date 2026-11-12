package com.aliazouzi.arabicide.termux

import android.util.Base64

/** بناء سكربت bash يزامن ملفات المشروع إلى Termux ثم ينفذ الأمر. */
object ScriptBuilder {

    fun shQuote(s: String): String = "'" + s.replace("'", "'\\''") + "'"

    fun base64Size(text: String): Int =
        ((text.toByteArray(Charsets.UTF_8).size + 2) / 3) * 4 + 40

    /**
     * ينسخ الملفات إلى ‎$HOME/ArabicIDE/<المشروع>‎ داخل Termux (بدون حذف ما هو موجود)،
     * ثم ينفذ الأمر من داخل مجلد المشروع.
     */
    fun build(project: String, files: List<Pair<String, String>>, command: String): String {
        val sb = StringBuilder()
        sb.append("D=\"\$HOME/ArabicIDE\"/").append(shQuote(project)).append('\n')
        sb.append("mkdir -p \"\$D\" && cd \"\$D\" || exit 1\n")
        val dirs = files
            .map { it.first.substringBeforeLast('/', "") }
            .filter { it.isNotEmpty() }
            .toSortedSet()
        for (d in dirs) {
            sb.append("mkdir -p ").append(shQuote(d)).append('\n')
        }
        for (f in files) {
            val b64 = Base64.encodeToString(f.second.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            sb.append("echo '").append(b64).append("' | base64 -d > ").append(shQuote(f.first)).append('\n')
        }
        sb.append(command).append('\n')
        return sb.toString()
    }

    /** أمر تشغيل الملف حسب امتداده، أو null إذا لم يكن معروفاً. */
    fun runLine(path: String): String? {
        val q = shQuote(path)
        val ext = path.substringAfterLast('/').substringAfterLast('.', "").lowercase()
        return when (ext) {
            "py" -> "python3 $q"
            "js", "mjs", "cjs" -> "node $q"
            "sh", "bash" -> "bash $q"
            "rb" -> "ruby $q"
            "php" -> "php $q"
            "lua" -> "lua $q"
            "pl" -> "perl $q"
            "go" -> "go run $q"
            "java" -> "java $q"
            "c" -> "gcc $q -o \"\$TMPDIR/arabicide_out\" && \"\$TMPDIR/arabicide_out\""
            "cpp", "cc", "cxx" -> "g++ $q -o \"\$TMPDIR/arabicide_out\" && \"\$TMPDIR/arabicide_out\""
            else -> null
        }
    }

    private val TOOL_PACKAGES = mapOf(
        "python3" to "python",
        "python" to "python",
        "pip" to "python",
        "node" to "nodejs",
        "npm" to "nodejs",
        "ruby" to "ruby",
        "php" to "php",
        "lua" to "lua54",
        "perl" to "perl",
        "go" to "golang",
        "java" to "openjdk-17",
        "javac" to "openjdk-17",
        "gcc" to "clang",
        "g++" to "clang",
        "git" to "git",
    )

    /** اقتراح تثبيت عند فشل الأمر برمز 127 (أمر غير موجود). */
    fun installHint(command: String): String? {
        for ((tool, pkg) in TOOL_PACKAGES) {
            val re = Regex("(^|[\\s;&|(])" + Regex.escape(tool) + "(\\s|\\z)")
            if (re.containsMatchIn(command)) return "pkg install $pkg"
        }
        return null
    }
}
