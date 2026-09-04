package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.DocDatabase
import com.example.data.DocumentRepository
import com.example.ui.EditorScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.DocumentViewModel
import com.example.viewmodel.DocumentViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = DocDatabase.getDatabase(applicationContext)
        val repository = DocumentRepository(database.documentDao())
        val viewModelFactory = DocumentViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) {
                    val viewModel: DocumentViewModel = viewModel(factory = viewModelFactory)
                    EditorScreen(viewModel = viewModel)
                }
            }
        }
    }
}

