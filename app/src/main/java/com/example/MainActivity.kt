package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.RouterRepository
import com.example.ui.MainLayout
import com.example.ui.RouterViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Setup local SQLite database via Room
        val database = AppDatabase.getDatabase(this)
        val repository = RouterRepository(
            deviceProfileDao = database.deviceProfileDao(),
            commandLogDao = database.commandLogDao()
        )

        // Initialize ViewModel via simple constructor factory
        val factory = RouterViewModel.Factory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[RouterViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainLayout(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
