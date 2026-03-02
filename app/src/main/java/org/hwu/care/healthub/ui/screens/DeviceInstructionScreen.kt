package org.hwu.care.healthub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@Composable
fun DeviceInstructionScreen(
    deviceId: String,
    videoId: String,
    ttsLocked: Boolean = false,
    onReady: () -> Unit,
    onVideoEnded: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Instructions for ${deviceId.replaceFirstChar { it.uppercase() }}", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        AndroidView(
            factory = { context ->
                YouTubePlayerView(context).apply {
                    lifecycleOwner.lifecycle.addObserver(this)
                    addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) {
                            youTubePlayer.loadVideo(videoId, 0f)
                        }
                        override fun onStateChange(
                            youTubePlayer: YouTubePlayer,
                            state: PlayerConstants.PlayerState
                        ) {
                            if (state == PlayerConstants.PlayerState.ENDED) {
                                onVideoEnded()
                            }
                        }
                    })
                }
            },
            modifier = Modifier
                .width(600.dp)
                .height(337.5.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onReady,
            enabled = !ttsLocked,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text("Ready", fontSize = 32.sp)
        }
    }
}

@Preview(widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun DeviceInstructionScreenPreview() {
    DeviceInstructionScreen(deviceId = "Oximeter", videoId = "eEzD-Y97ges", onReady = {})
}
