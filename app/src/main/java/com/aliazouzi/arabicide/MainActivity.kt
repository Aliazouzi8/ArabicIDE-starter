package com.aliazouzi.arabicide

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.aliazouzi.arabicide.ui.WorkspaceScreen
import com.aliazouzi.arabicide.ui.theme.ArabicIDETheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // شريط الحالة والتنقل شفافان مع أيقونات فاتحة (الواجهة داكنة)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            ArabicIDETheme {
                // الواجهة RTL، ومساحات الكود تفرض LTR بنفسها
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    WorkspaceScreen()
                }
            }
        }
    }
}
