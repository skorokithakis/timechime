package com.stavros.timechime

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val PREFS_NAME = "timechime_prefs"
private const val KEY_IS_RUNNING = "is_running"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val wasRunning = prefs.getBoolean(KEY_IS_RUNNING, false)

        // Re-start the service if it was running before the process was killed or the
        // app was restarted, so the user doesn't have to manually tap Start again.
        if (wasRunning) {
            startForegroundService(Intent(this, ChimeService::class.java))
        }

        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TimeChimeScreen(initialRunning = wasRunning)
                }
            }
        }
    }
}

@Composable
fun TimeChimeScreen(initialRunning: Boolean = false) {
    val context = LocalContext.current
    var isRunning by remember { mutableStateOf(initialRunning) }

    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Re-read the persisted state every time the activity resumes, so that changes made
    // from the notification stop action are reflected when the user returns to the app.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isRunning = prefs.getBoolean(KEY_IS_RUNNING, false)
    }

    // Seconds until the next 10-minute boundary, recomputed every second while running.
    var secondsUntilChime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isRunning) {
        while (isRunning) {
            val nowMillis = System.currentTimeMillis()
            val millisIntoTenMinutes = nowMillis % 600_000L
            secondsUntilChime = (600_000L - millisIntoTenMinutes + 999L) / 1_000L
            delay(1_000L)
        }
    }

    // rememberLauncherForActivityResult uses the Activity Result API internally and is
    // the correct Compose-idiomatic alternative to the deprecated requestPermissions().
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        // Permission result is intentionally ignored: the service works without a visible
        // notification, so we proceed regardless of whether it was granted. The callback
        // is where we start the service on API 33+ so that the permission dialog is fully
        // dismissed before the notification is posted — avoiding the race where the
        // notification appears while the dialog is still on screen and gets suppressed.
        context.startForegroundService(Intent(context, ChimeService::class.java))
        prefs.edit().putBoolean(KEY_IS_RUNNING, true).apply()
        isRunning = true
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = {
                    if (isRunning) {
                        context.stopService(Intent(context, ChimeService::class.java))
                        prefs.edit().putBoolean(KEY_IS_RUNNING, false).apply()
                        isRunning = false
                    } else {
                        // POST_NOTIFICATIONS is a runtime permission only on API 33+. On older
                        // APIs there is no dialog, so we start the service inline. On API 33+
                        // the service is started inside the launcher callback (above) so it
                        // only runs after the dialog is fully dismissed.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            context.startForegroundService(Intent(context, ChimeService::class.java))
                            prefs.edit().putBoolean(KEY_IS_RUNNING, true).apply()
                            isRunning = true
                        }
                    }
                },
            ) {
                Text(
                    text = if (isRunning) "Stop" else "Start",
                    fontSize = 24.sp,
                )
            }
            if (isRunning) {
                Spacer(modifier = Modifier.height(16.dp))
                val minutes = secondsUntilChime / 60
                val seconds = secondsUntilChime % 60
                Text(
                    text = "Next chime in ${minutes}m ${seconds}s",
                    fontSize = 18.sp,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TimeChimeScreenStoppedPreview() {
    MaterialTheme {
        TimeChimeScreen(initialRunning = false)
    }
}

@Preview(showBackground = true)
@Composable
fun TimeChimeScreenRunningPreview() {
    MaterialTheme {
        TimeChimeScreen(initialRunning = true)
    }
}
