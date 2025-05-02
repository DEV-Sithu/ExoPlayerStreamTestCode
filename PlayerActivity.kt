package dev.mk.exoplayertester

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import dev.mk.exoplayertester.databinding.ActivityPlayerBinding
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.dash.DefaultDashChunkSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.exoplayer.smoothstreaming.SsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource

class PlayerActivity : AppCompatActivity(){

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var data: String
    private val playbackStateListener: Player.Listener = playbackStateListener()
    private var player: Player? = null

    private var playWhenReady = true
    private var mediaItemIndex = 0
    private var playbackPosition = 0L
    private var spinnerIndex = 0


    companion object {
        fun newIntent(context: Context , url : String,spinnerIndex: Int) = Intent(context, PlayerActivity::class.java).
        apply {
            putExtra("url", url)
            putExtra("spinnerIndex", spinnerIndex)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        data = intent.getStringExtra("url") ?: ""
        spinnerIndex = intent.getIntExtra("spinnerIndex",0)

        supportActionBar?.apply {
            title =  data
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun playbackStateListener() = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateString: String = when (playbackState) {
                ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
                ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
                ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY     -"
                ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
                else -> "UNKNOWN_STATE             -"
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT > 23) {
            initializePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
         hideSystemUi()
        if (Build.VERSION.SDK_INT <= 23 || player == null) {
            initializePlayer()
        }
    }

    public override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    public override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT > 23) {
            releasePlayer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()// Cleanup when the activity is destroyed
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun initializePlayer() {
        try {
            // ExoPlayer implements the Player interface
            var mediaItem = when (spinnerIndex) {
                1 ->
                    MediaItem.Builder().setUri(data)
                        .setMimeType(MimeTypes.APPLICATION_MPD).build()

                2 -> MediaItem.Builder().setUri(data)
                    .setMimeType(MimeTypes.APPLICATION_M3U8).build()

                3 -> MediaItem.Builder().setUri(data)
                    .setMimeType(MimeTypes.APPLICATION_SS).build()

                4 -> MediaItem.Builder().setUri(data)
                    .setMimeType(MimeTypes.APPLICATION_MP4).build()

                5 -> MediaItem.Builder().setUri(data)
                    .setMimeType(MimeTypes.APPLICATION_RTSP).build()

                else -> MediaItem.Builder().setUri(data)
                    .setMimeType(MimeTypes.APPLICATION_MP4).build()
            }
            var mediaSource = when (spinnerIndex) {
                1 -> DashMediaSource.Factory( DefaultDashChunkSource.Factory( DefaultDataSource.Factory(this)),  DefaultDataSource.Factory(this))
                    .createMediaSource(mediaItem)


                2 -> HlsMediaSource.Factory(DefaultDataSource.Factory(this))
                    .createMediaSource(mediaItem)


                3 -> SsMediaSource.Factory(DefaultDataSource.Factory(this)).createMediaSource(mediaItem)


                4 -> ProgressiveMediaSource.Factory(DefaultDataSource.Factory(this)).createMediaSource(mediaItem)


                5 ->RtspMediaSource.Factory().createMediaSource(mediaItem)


                else ->  ProgressiveMediaSource.Factory(DefaultDataSource.Factory(this))
                    .createMediaSource(mediaItem)

            }

            player = ExoPlayer.Builder(this).build().also { exoPlayer ->
                binding.videoView.player = exoPlayer
                // Update the track selection parameters to only pick standard definition tracks
                exoPlayer.trackSelectionParameters =
                    exoPlayer.trackSelectionParameters.buildUpon().setMaxVideoSizeSd().build()

                exoPlayer.setMediaItems(listOf(mediaItem), mediaItemIndex, playbackPosition)
                exoPlayer.playWhenReady = playWhenReady
                exoPlayer.addListener(playbackStateListener)

                exoPlayer.setMediaSource(mediaSource)
                exoPlayer.prepare()
            }
        }catch (e: Exception)
        {
            Log.e("TAG", "Error initializing player: ${e.message}")
            Toast.makeText(this, "Error initializing player: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        finally {
            // Optional: Show feedback to user
            Toast.makeText(this, "Player initialized", Toast.LENGTH_SHORT).show()
        }
    }

    private fun releasePlayer() {
        player?.let { player ->
            playbackPosition = player.currentPosition
            mediaItemIndex = player.currentMediaItemIndex
            playWhenReady = player.playWhenReady
            player.removeListener(playbackStateListener)
            player.release()
        }
        player = null
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        // Get activity window safely
        val activityWindow = window ?: return
        WindowCompat.setDecorFitsSystemWindows(activityWindow, false)
        WindowInsetsControllerCompat(activityWindow, binding.videoView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

}