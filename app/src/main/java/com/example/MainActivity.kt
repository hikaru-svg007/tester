package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.RoleplayAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.RoleplayViewModel
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    // Set global crash catcher
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      try {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stacktrace = sw.toString()
        
        val sharedPrefs = getSharedPreferences("crash_reports", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("last_crash_trace", stacktrace).commit()
      } catch (e: Exception) {
        e.printStackTrace()
      }
      defaultHandler?.uncaughtException(thread, throwable)
    }

    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        var crashTrace by remember { mutableStateOf<String?>(null) }
        val sharedPrefs = remember { getSharedPreferences("crash_reports", Context.MODE_PRIVATE) }
        
        LaunchedEffect(Unit) {
          crashTrace = sharedPrefs.getString("last_crash_trace", null)
        }

        Box(modifier = Modifier.fillMaxSize()) {
          val viewModel: RoleplayViewModel = viewModel()
          RoleplayAppScreen(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
          )
        }

          crashTrace?.let { trace ->
            val clipboardManager = LocalClipboardManager.current
            AlertDialog(
              onDismissRequest = {
                // Do not dismiss automatically to ensure copy is made
              },
              title = { Text("App Diagnostic / Crash Report") },
              text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                  Text(
                    text = "Aplikasi terhenti secara tidak terduga sebelumnya. Salin laporan di bawah ini untuk membantu kami memperbaikinya:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Spacer(modifier = Modifier.height(12.dp))
                  Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
                  ) {
                    Text(
                      text = trace,
                      fontSize = 10.sp,
                      fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                      modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())
                    )
                  }
                }
              },
              confirmButton = {
                Button(
                  onClick = {
                    clipboardManager.setText(AnnotatedString(trace))
                    sharedPrefs.edit().remove("last_crash_trace").apply()
                    crashTrace = null
                  }
                ) {
                  Text("Copy & Continue")
                }
              },
              dismissButton = {
                TextButton(
                  onClick = {
                    sharedPrefs.edit().remove("last_crash_trace").apply()
                    crashTrace = null
                  }
                ) {
                  Text("Dismiss")
                }
              }
            )
          }
      }
    }
  }
}

