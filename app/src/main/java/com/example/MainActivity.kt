package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.security.AppLock
import com.example.security.AppLockStore
import com.example.ui.AppLockScreen
import com.example.ui.MainScreen
import com.example.ui.SplashScreen
import com.example.ui.theme.MyApplicationTheme

import android.content.Intent

// FragmentActivity (a ComponentActivity) is required by the biometric prompt used for the app lock.
class MainActivity : FragmentActivity() {
    private val lockStore by lazy { AppLockStore(this) }
    private val sceneTarget = mutableStateOf<Pair<String, Long>?>(null)
    private val atmosphereTarget = mutableStateOf<String?>(null)
    private val birdSurfaceTarget = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enableHighRefreshRate()
        AppLock.onActivityCreated(this, lockStore)
        com.example.care.TinyCareScheduler.createNotificationChannel(this)
        checkTinyCarePermissionStatus()
        val sc = intent?.getStringExtra("scene")
        val at = intent?.getStringExtra("atmosphere")
        val bs = intent?.getStringExtra("birdSurface")
        at?.let { com.example.data.PreferencesManager(this).atmosphereMode = it }
        atmosphereTarget.value = at
        birdSurfaceTarget.value = bs
        android.util.Log.d("TinyUs", "onCreate intent scene: $sc, atmosphere: $at, birdSurface: $bs")
        sceneTarget.value = sc?.let { it to System.currentTimeMillis() }
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Replace the whole app (and any open dialogs) while locked.
                    if (AppLock.isLocked) {
                        AppLockScreen(lockStore)
                        return@Surface
                    }
                    val target = sceneTarget.value
                    var isSplashVisible by remember { mutableStateOf(target == null) }
                    if (target != null) {
                        isSplashVisible = false
                    }
                    AnimatedContent(
                        targetState = isSplashVisible,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "splashTransition"
                    ) { showSplash ->
                        if (showSplash) {
                            SplashScreen(
                                onSplashComplete = { isSplashVisible = false }
                            )
                        } else {
                            MainScreen(
                                targetScene = sceneTarget.value?.first,
                                targetToken = sceneTarget.value?.second ?: 0L,
                                targetAtmosphere = atmosphereTarget.value,
                                targetBirdSurface = birdSurfaceTarget.value
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val sc = intent.getStringExtra("scene")
        val at = intent.getStringExtra("atmosphere")
        val bs = intent.getStringExtra("birdSurface")
        at?.let { com.example.data.PreferencesManager(this).atmosphereMode = it }
        atmosphereTarget.value = at
        birdSurfaceTarget.value = bs
        android.util.Log.d("TinyUs", "onNewIntent intent scene: $sc, atmosphere: $at, birdSurface: $bs")
        sc?.let {
            sceneTarget.value = it to System.currentTimeMillis()
        }
    }

    private fun enableHighRefreshRate() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                val currentDisplay = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    display
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay
                }
                val modes = currentDisplay?.supportedModes
                if (!modes.isNullOrEmpty()) {
                    val currentMode = currentDisplay.mode
                    val highestRefreshMode = modes
                        .filter { it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight }
                        .maxByOrNull { it.refreshRate }
                    if (highestRefreshMode != null && highestRefreshMode.modeId != currentMode.modeId) {
                        val params = window.attributes
                        params.preferredDisplayModeId = highestRefreshMode.modeId
                        window.attributes = params
                        android.util.Log.d("TinyUs", "Requested display mode ${highestRefreshMode.modeId} @ ${highestRefreshMode.refreshRate}Hz")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("TinyUs", "Unable to set high refresh rate display mode", e)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        AppLock.onAppForegrounded(lockStore)
    }

    override fun onStop() {
        super.onStop()
        // Not when merely rotating/recreating; only when the user actually leaves the app.
        if (!isChangingConfigurations) AppLock.onAppBackgrounded()
    }

    override fun onResume() {
        super.onResume()
        checkTinyCarePermissionStatus()
    }

    private fun checkTinyCarePermissionStatus() {
        val prefs = com.example.data.PreferencesManager(this)
        if (prefs.tinyCareEnabled) {
            val notificationsAllowed = androidx.core.app.NotificationManagerCompat.from(this).areNotificationsEnabled()
            val permissionGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true

            if (!notificationsAllowed || !permissionGranted) {
                prefs.tinyCareEnabled = false
                com.example.care.TinyCareScheduler.cancelAlarm(this)
            } else {
                com.example.care.TinyCareScheduler.scheduleNext(this, forceReschedule = false)
            }
        }
    }
}
