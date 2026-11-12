package com.aliazouzi.arabicide.data

import android.content.Context
import java.io.File

data class TreeRow(val path: String, val name: String, val isDir: Boolean, val depth: Int)

sealed interface ReadResult {
    data class Ok(val text: String) : ReadResult
    data class Fail(val reason: String) : ReadResult
}

/**
 * المشاريع تُخزَّن داخل مساحة التطبيق الخاصة: files/projects/<اسم المشروع>.
 * (إدارة الملفات عبر SAF تأتي في مرحلة لاحقة.)
 */
class ProjectRepository(context: Context) {

    companion object {
        const val DEFAULT_PROJECT = "مشروعي"
        const val MAX_OPEN_BYTES = 2L * 1024 * 1024
        private const val MAX_SYNC_FILE_BYTES = 300_000L
    }

    private val root: File = File(context.filesDir, "projects").also { it.mkdirs() }

    fun projectDir(name: String): File = File(root, name)

    fun listProjects(): List<String> =
        root.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?.sortedBy { it.lowercase() }
            ?: emptyList()

    fun ensureDefaultProject() {
        if (listProjects().isEmpty()) createProject(DEFAULT_PROJECT, withSamples = true)
    }

    /** يعيد رسالة الخطأ، أو null إذا كان الاسم صالحاً. */
    fun validateName(name: String): String? {
        val n = name.trim()
        if (n.isEmpty()) return "الاسم فارغ"
        if (n == "." || n == "..") return "اسم غير صالح"
        if (n.length > 100) return "الاسم طويل جداً"
        if (n.any { it == '/' || it == '\\' || it.code < 32 }) {
            return "الاسم لا يمكن أن يحتوي على / أو \\ أو رموز تحكم"
        }
        return null
    }

    fun createProject(name: String, withSamples: Boolean = false): String? {
        val n = name.trim()
        validateName(n)?.let { return it }
        if (n.startsWith(".")) return "اسم المشروع لا يمكن أن يبدأ بنقطة"
        val dir = File(root, n)
        if (dir.exists()) return "يوجد مشروع بهذا الاسم"
        if (!dir.mkdirs()) return "تعذر إنشاء المشروع"
        if (withSamples) {
            File(dir, "main.py").writeText(SAMPLE_PY)
            File(dir, "hello.js").writeText(SAMPLE_JS)
            File(dir, "README.md").writeText(SAMPLE_MD)
        }
        return null
    }

    fun deleteProject(name: String): Boolean = projectDir(name).deleteRecursively()

    /** أول ملف في جذر المشروع (يفضّل main.*). */
    fun firstFile(project: String): String? {
        val files = projectDir(project).listFiles()?.filter { it.isFile } ?: return null
        val pick = files.firstOrNull { it.name.startsWith("main.") }
            ?: files.minByOrNull { it.name.lowercase() }
        return pick?.name
    }

    private fun resolve(project: String, rel: String): File? = try {
        val base = projectDir(project).canonicalFile
        val f = File(base, rel).canonicalFile
        if (f.path == base.path || f.path.startsWith(base.path + File.separator)) f else null
    } catch (e: Exception) {
        null
    }

    /** الصفوف الظاهرة في الشجرة حسب المجلدات المفتوحة. */
    fun visibleTree(project: String, expanded: Set<String>): List<TreeRow> {
        val out = mutableListOf<TreeRow>()
        fun walk(dir: File, rel: String, depth: Int) {
            val children = dir.listFiles()
                ?.sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() }))
                ?: return
            for (f in children) {
                if (f.name.endsWith(".tmp")) continue
                val p = if (rel.isEmpty()) f.name else rel + "/" + f.name
                out.add(TreeRow(p, f.name, f.isDirectory, depth))
                if (f.isDirectory && p in expanded) walk(f, p, depth + 1)
            }
        }
        walk(projectDir(project), "", 0)
        return out
    }

    fun read(project: String, path: String): ReadResult {
        val f = resolve(project, path) ?: return ReadResult.Fail("مسار غير صالح")
        if (!f.isFile) return ReadResult.Fail("الملف غير موجود")
        if (f.length() > MAX_OPEN_BYTES) return ReadResult.Fail("الملف أكبر من 2 ميغابايت")
        return try {
            val bytes = f.readBytes()
            val n = minOf(bytes.size, 8000)
            for (i in 0 until n) {
                if (bytes[i] == 0.toByte()) return ReadResult.Fail("ملف ثنائي لا يمكن فتحه")
            }
            ReadResult.Ok(String(bytes, Charsets.UTF_8).removePrefix("\uFEFF"))
        } catch (e: Exception) {
            ReadResult.Fail("تعذرت قراءة الملف")
        }
    }

    /** كتابة آمنة: ملف مؤقت ثم استبدال. */
    fun write(project: String, path: String, text: String): Boolean = try {
        val f = resolve(project, path)
        if (f == null) {
            false
        } else {
            f.parentFile?.mkdirs()
            val tmp = File(f.parentFile, f.name + ".tmp")
            tmp.writeText(text, Charsets.UTF_8)
            if (!tmp.renameTo(f)) {
                f.writeText(text, Charsets.UTF_8)
                tmp.delete()
            }
            true
        }
    } catch (e: Exception) {
        false
    }

    fun createFile(project: String, parent: String, name: String): String? {
        validateName(name)?.let { return it }
        val dir = resolve(project, parent) ?: return "مسار غير صالح"
        if (!dir.isDirectory) return "المجلد غير موجود"
        val f = File(dir, name)
        if (f.exists()) return "يوجد عنصر بهذا الاسم"
        return try {
            if (f.createNewFile()) null else "تعذر إنشاء الملف"
        } catch (e: Exception) {
            "تعذر إنشاء الملف"
        }
    }

    fun createDir(project: String, parent: String, name: String): String? {
        validateName(name)?.let { return it }
        val dir = resolve(project, parent) ?: return "مسار غير صالح"
        if (!dir.isDirectory) return "المجلد غير موجود"
        val f = File(dir, name)
        if (f.exists()) return "يوجد عنصر بهذا الاسم"
        return if (f.mkdir()) null else "تعذر إنشاء المجلد"
    }

    fun rename(project: String, path: String, newName: String): String? {
        if (path.isEmpty()) return "مسار غير صالح"
        validateName(newName)?.let { return it }
        val src = resolve(project, path) ?: return "مسار غير صالح"
        if (!src.exists()) return "العنصر غير موجود"
        val dst = File(src.parentFile, newName)
        if (dst.exists()) return "يوجد عنصر بهذا الاسم"
        return if (src.renameTo(dst)) null else "تعذرت إعادة التسمية"
    }

    fun delete(project: String, path: String): Boolean {
        if (path.isEmpty()) return false
        val f = resolve(project, path) ?: return false
        return f.deleteRecursively()
    }

    /** كل الملفات النصية الصغيرة في المشروع (لمزامنتها إلى Termux). */
    fun collectTextFiles(project: String): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        fun walk(dir: File, rel: String) {
            val children = dir.listFiles() ?: return
            for (f in children.sortedBy { it.name }) {
                if (f.name.startsWith(".") || f.name == "node_modules" || f.name == "__pycache__") continue
                if (f.name.endsWith(".tmp")) continue
                val p = if (rel.isEmpty()) f.name else rel + "/" + f.name
                if (f.isDirectory) {
                    walk(f, p)
                } else if (f.length() <= MAX_SYNC_FILE_BYTES) {
                    val r = read(project, p)
                    if (r is ReadResult.Ok) out.add(Pair(p, r.text))
                }
            }
        }
        walk(projectDir(project), "")
        return out
    }
}

private val SAMPLE_PY = """
# مرحباً من ArabicIDE
name = "ArabicIDE"
print(f"مرحباً بك في {name}")

for i in range(1, 4):
    print("السطر", i)
""".trimIndent() + "\n"

private val SAMPLE_JS = """
// مرحباً من Node.js
const name = "ArabicIDE";
console.log("مرحباً بك في " + name);
""".trimIndent() + "\n"

private val SAMPLE_MD = """
# مشروعي

هذا مشروع تجريبي. اضغط زر التشغيل ▶ لتشغيل الملف المفتوح داخل Termux.

تأكد أولاً من تثبيت الأداة المناسبة داخل Termux، مثلاً:

    pkg install python
""".trimIndent() + "\n"
