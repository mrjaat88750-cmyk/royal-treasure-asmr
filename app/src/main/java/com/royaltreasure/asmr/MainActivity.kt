package com.royaltreasure.asmr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import com.royaltreasure.asmr.state.GameViewModel
import com.royaltreasure.asmr.ui.GameScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val viewModel = remember { GameViewModel(applicationContext) }
                GameScreen(viewModel = viewModel)
            }
        }
    }
}
