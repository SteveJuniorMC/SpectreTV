package com.spectretv.app.presentation

import android.app.PictureInPictureParams
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.spectretv.app.presentation.navigation.AppNavigation
import com.spectretv.app.presentation.player.PlayerManager
import com.spectretv.app.presentation.theme.SpectreTVTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var playerManager: PlayerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SpectreTVTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(playerManager)
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Enter PiP when leaving app if playing video
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            playerManager.currentStream != null &&
            playerManager.isPlaying) {

            // Make sure player is in fullscreen mode for PiP (no UI controls)
            playerManager.enterPipMode()
            playerManager.expand()

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
        playerManager.exitPipMode()
        // When exiting PiP by tapping to expand
        if (!isInPictureInPictureMode && playerManager.currentStream != null) {
            hideKeyboard()
            playerManager.expand()
        }
    }

    override fun onStop() {
        super.onStop()
        // If we're in PiP mode and activity is stopping, PiP was dismissed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode) {
            playerManager.stop()
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            playerManager.release()
        }
    }
}
