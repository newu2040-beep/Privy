package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.PrivyApp
import com.example.viewmodel.PrivyViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PrivyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingIntent(intent)

        setContent {
            PrivyApp(viewModel = viewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("image/") == true) {
                    val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
                    }
                    imageUri?.let { viewModel.loadFromUri(it) }
                }
            }
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    viewModel.loadFromUri(uri)
                }
            }
        }
    }
}
