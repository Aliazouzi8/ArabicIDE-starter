package com.aliazouzi.arabicide.data

import android.content.Context

/** إعدادات التطبيق وحالة الجلسة الأخيرة (SharedPreferences). */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("arabicide_settings", Context.MODE_PRIVATE)

    var fontSize: Int
        get() = prefs.getInt("font_size", 15)
        set(value) {
            prefs.edit().putInt("font_size", value).apply()
        }

    var wordWrap: Boolean
        get() = prefs.getBoolean("word_wrap", false)
        set(value) {
            prefs.edit().putBoolean("word_wrap", value).apply()
        }

    var autoSave: Boolean
        get() = prefs.getBoolean("auto_save", true)
        set(value) {
            prefs.edit().putBoolean("auto_save", value).apply()
        }

    var lastProject: String
        get() = prefs.getString("last_project", "") ?: ""
        set(value) {
            prefs.edit().putString("last_project", value).apply()
        }

    var openPaths: List<String>
        get() = (prefs.getString("open_paths", "") ?: "").split("\n").filter { it.isNotEmpty() }
        set(value) {
            prefs.edit().putString("open_paths", value.joinToString("\n")).apply()
        }

    var activePath: String
        get() = prefs.getString("active_path", "") ?: ""
        set(value) {
            prefs.edit().putString("active_path", value).apply()
        }
}
