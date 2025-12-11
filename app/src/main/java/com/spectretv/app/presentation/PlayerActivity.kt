package com.spectretv.app.presentation

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.spectretv.app.presentation.screens.player.PlayerScreen
import com.spectretv.app.presentation.theme.SpectreTVTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    private var isInPipMode by mutableStateOf(false)

    companion object {
        private const val EXTRA_STREAM_URL = "stream_url"
        private const val EXTRA_TITLE = "title"

        fun createIntent(context: Context, streamUrl: String, title: String): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_STREAM_URL, streamUrl)
                putExtra(EXTRA_TITLE, title)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL) ?: run {
            finish()
            return
        }
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Playing"

        setContent {
            SpectreTVTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PlayerScreen(
                        streamUrl = streamUrl,
                        title = title,
                        isInPipMode = isInPipMode,
                        onBackClick = { enterPipAndFinish() }
                    )
                }
            }
        }
    }

    private fun enterPipAndFinish() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        } else {
            finish()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Auto-enter PiP when user navigates away (home button, recent apps, etc.)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isInPipMode) {
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
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Enter PiP instead of closing
        enterPipAndFinish()
    }
}
