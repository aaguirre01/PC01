package dev.aaguirre.pc01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.aaguirre.pc01.presentation.navigation.AppNavGraph
import dev.aaguirre.pc01.ui.theme.LoteriaAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoteriaAppTheme {
                AppNavGraph()
            }
        }
    }
}