package com.bodyquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bodyquest.ui.nav.BodyQuestNavHost
import com.bodyquest.ui.theme.BodyQuestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BodyQuestTheme {
                BodyQuestNavHost()
            }
        }
    }
}
