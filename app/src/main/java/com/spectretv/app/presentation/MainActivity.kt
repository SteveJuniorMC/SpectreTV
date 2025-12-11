package com.spectretv.app.presentation

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.spectretv.app.presentation.navigation.AppNavigation
import com.spectretv.app.presentation.theme.SpectreTVTheme
import dagger.hilt.android.AndroidEntryPoint

// Composition local to provide PiP functionality to composables
val LocalPipHandler = compositionLocalOf<PipHandler> { error("No PipHandler provided") }

interface PipHandler {
    val isInPipMode: Boolean
    fun enterPipMode()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity(), PipHandler {

    override var isInPipMode by mutableStateOf(false)
        private set

    private var shouldEnterPipOnUserLeave = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CompositionLocalProvider(LocalPipHandler provides this) {
                SpectreTVTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation()
                    }
                }
            }
        }
    }

    override fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            shouldEnterPipOnUserLeave = true
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (shouldEnterPipOnUserLeave && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
        if (!isInPictureInPictureMode) {
            shouldEnterPipOnUserLeave = false
        }
    }

    fun setShouldEnterPipOnLeave(should: Boolean) {
        shouldEnterPipOnUserLeave = should
    }
}
