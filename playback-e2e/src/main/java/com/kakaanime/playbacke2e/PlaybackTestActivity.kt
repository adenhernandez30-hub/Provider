package com.kakaanime.playbacke2e

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.media3.ui.PlayerView

class PlaybackTestActivity : ComponentActivity() {
    val playerView: PlayerView by lazy { PlayerView(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(playerView)
    }
}
