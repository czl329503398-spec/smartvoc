package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.ui.VocabTrackerApp
import com.example.ui.WordViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Obtain the ViewModel via our custom application Factory
    val viewModel: WordViewModel by viewModels {
      WordViewModel.Factory(application)
    }

    setContent {
      MyApplicationTheme {
        VocabTrackerApp(viewModel = viewModel)
      }
    }
  }
}

