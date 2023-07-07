package me.ccrama.redditslide.views

import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Tracks
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.video.VideoSize
import ltd.ucode.slide.App
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.isMuted
import me.ccrama.redditslide.util.BlendModeUtil
import me.ccrama.redditslide.util.GifUtils
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.util.NetworkUtil

/**
 * View containing an ExoPlayer
 */
class ExoVideoView @JvmOverloads constructor(
    private val context: Context?,
    attrs: AttributeSet? = null,
    ui: Boolean = true
) : RelativeLayout(
    context, attrs
) {
    private var player: SimpleExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var playerUI: PlayerControlView? = null
    private var muteAttached = false
    private var hqAttached = false
    private var audioFocusHelper: AudioFocusHelper? = null

    constructor(context: Context?, ui: Boolean) : this(context, null, ui) {}

    init {
        setupPlayer()
        if (ui) {
            setupUI()
        }
    }

    /**
     * Initializes the view to render onto and the SimpleExoPlayer instance
     */
    private fun setupPlayer() {
        // Create a view to render the video onto and an AspectRatioFrameLayout to size the video correctly
        val frame = AspectRatioFrameLayout(context!!)
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        params.addRule(CENTER_IN_PARENT, TRUE)
        frame.layoutParams = params
        frame.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        val renderView = SurfaceView(context)
        frame.addView(renderView)
        addView(frame)

        // Create a track selector so we can set specific video quality for DASH
        trackSelector = DefaultTrackSelector(context)
        if (SettingValues.lowResAlways || NetworkUtil.isConnected(context) && !NetworkUtil.isConnectedWifi(
                context
            ) && SettingValues.lowResMobile
            && SettingValues.lqVideos
        ) {
            trackSelector!!.setParameters(
                trackSelector!!.buildUponParameters().setForceLowestBitrate(true)
            )
        } else {
            trackSelector!!.setParameters(
                trackSelector!!.buildUponParameters().setForceHighestSupportedBitrate(true)
            )
        }

        // Create the player, attach it to the view, make it repeat infinitely
        player = SimpleExoPlayer.Builder(context).setTrackSelector(trackSelector!!).build()
        player!!.setVideoSurfaceView(renderView)
        player!!.repeatMode = Player.REPEAT_MODE_ALL

        // Mute by default
        player!!.volume = 0f

        // Create audio focus helper
        audioFocusHelper = AudioFocusHelper(
            ContextCompat.getSystemService(
                context, AudioManager::class.java
            )
        )
        player!!.addListener(object : Player.Listener {
            // Make the video use the correct aspect ratio
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                frame.setAspectRatio(
                    if (videoSize.height == 0 || videoSize.width == 0) 1f
                    else videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                )
            }

            // Logging
            override fun onTracksChanged(tracks: Tracks) {
                val trackGroups = tracks.groups
                val toLog = StringBuilder()
                for (i in trackGroups.indices) {
                    for (j in 0 until trackGroups[i].length) {
                        toLog.append("Format:\t").append(trackGroups[i].getTrackFormat(j))
                            .append("\n")
                    }
                }
                // FIXME: How do I make onTracksChanged work with ExoTrackSelection?
                /*for (TrackSelection i : trackSelections.getAll()) {
                    if (i != null)
                        toLog.append("Selected format:\t").append(i.getSelectedFormat()).append("\n");
                }*/Log.v(LogUtil.getTag(), toLog.toString())
            }
        })
    }

    /**
     * Sets up the player UI
     */
    private fun setupUI() {
        // Create a PlayerControlView for our video controls and add it
        playerUI = PlayerControlView(context!!)
        playerUI!!.player = player
        playerUI!!.showTimeoutMs = 2000
        playerUI!!.hide()
        addView(playerUI)

        // Show/hide the player UI on tap
        setOnClickListener { v: View? ->
            playerUI!!.clearAnimation()
            if (playerUI!!.isVisible) {
                playerUI!!.startAnimation(PlayerUIFadeInAnimation(playerUI!!, false, 300))
            } else {
                playerUI!!.startAnimation(PlayerUIFadeInAnimation(playerUI!!, true, 300))
            }
        }
    }

    /**
     * Sets the player's URI and prepares for playback
     *
     * @param uri      URI
     * @param type     Type of video
     * @param listener EventLister attached to the player, helpful for player state
     */
    fun setVideoURI(uri: Uri, type: VideoType?, listener: Player.Listener?) {
        // Create the data sources used to retrieve and cache the video
        val downloader: DataSource.Factory = OkHttpDataSource.Factory(
            App.client!!
        )
            .setDefaultRequestProperties(GifUtils.AsyncLoadGif.makeHeaderMap(uri.host!!))
        val cacheDataSourceFactory: DataSource.Factory = CacheDataSource.Factory()
            .setCache(App.videoCache!!)
            .setUpstreamDataSourceFactory(downloader)

        // Create an appropriate media source for the video type
        val videoSource: MediaSource = when (type) {
            VideoType.DASH -> DashMediaSource.Factory(cacheDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))

            VideoType.STANDARD -> ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))

            else -> ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))
        }
        player!!.setMediaSource(videoSource)
        player!!.prepare()
        if (listener != null) {
            player!!.addListener(listener)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // If we don't release the player here, hardware decoders won't be released, breaking ExoPlayer device-wide
        stop()
    }

    /**
     * Plays the video
     */
    fun play() {
        player!!.play()
    }

    /**
     * Pauses the video
     */
    fun pause() {
        player!!.pause()
    }

    /**
     * Stops the video and releases the player
     */
    fun stop() {
        player!!.stop()
        player!!.release()
        audioFocusHelper!!.loseFocus() // do this last so audio doesn't overlap
    }

    /**
     * Seeks to a specific timestamp
     *
     * @param time timestamp
     */
    fun seekTo(time: Long) {
        player!!.seekTo(time)
    }

    /**
     * Gets the current timestamp
     *
     * @return current timestamp
     */
    val currentPosition: Long
        get() = player!!.currentPosition

    /**
     * Attach a mute button to the view. The view will then handle hiding/showing that button as appropriate.
     * If this is not called, audio will be permanently muted.
     *
     * @param mute Mute button
     */
    fun attachMuteButton(mute: ImageView) {
        // Hide the mute button by default
        mute.visibility = GONE
        player!!.addListener(object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                val trackGroups = tracks.groups
                // We only need to run this on the first track change, i.e. when the video is loaded
                // Skip this if mute has already been configured, otherwise mark it as configured
                muteAttached = if (muteAttached && trackGroups.size > 0) {
                    return
                } else {
                    true
                }
                val trackSelections = tracks.groups
                // Loop through the tracks and check if any contain audio, if so set up the mute button
                for (i in trackSelections.indices) {
                    val selection = trackSelections[i]
                    if (!selection!!.isSelected) continue
                    if (selection != null && selection.getTrackFormat(0) != null && MimeTypes.isAudio(
                            selection.getTrackFormat(0).sampleMimeType
                        )
                    ) {
                        mute.visibility = VISIBLE
                        // Set initial mute state
                        if (!isMuted) {
                            player!!.volume = 1f
                            BlendModeUtil.tintImageViewAsSrcAtop(mute, Color.WHITE)
                            audioFocusHelper!!.gainFocus()
                        } else {
                            player!!.volume = 0f
                            BlendModeUtil.tintImageViewAsSrcAtop(
                                mute,
                                resources.getColor(R.color.md_red_500)
                            )
                        }
                        mute.setOnClickListener { v: View? ->
                            if (isMuted) {
                                player!!.volume = 1f
                                isMuted = false
                                BlendModeUtil.tintImageViewAsSrcAtop(mute, Color.WHITE)
                                audioFocusHelper!!.gainFocus()
                            } else {
                                player!!.volume = 0f
                                isMuted = true
                                BlendModeUtil.tintImageViewAsSrcAtop(
                                    mute,
                                    resources.getColor(R.color.md_red_500)
                                )
                                audioFocusHelper!!.loseFocus()
                            }
                        }
                        return
                    }
                }
            }
        })
    }

    /**
     * Attach an HQ button to the view. The view will then handle hiding/showing that button as appropriate.
     *
     * @param hq HQ button
     */
    fun attachHqButton(hq: ImageView) {
        // Hidden by default - we don't yet know if we'll have multiple qualities to select from
        hq.visibility = GONE
        player!!.addListener(object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                val trackGroups = tracks.groups
                hqAttached =
                    if (hqAttached || trackGroups.size == 0 || trackSelector!!.parameters.forceHighestSupportedBitrate) {
                        return
                    } else {
                        true
                    }
                // Lopp through the tracks, check if they're video. If we have at least 2 video tracks we can set
                // up quality selection.
                var videoTrackCounter = 0
                for (trackGroup in trackGroups.indices) {
                    for (format in 0 until trackGroups[trackGroup].length) {
                        if (MimeTypes.isVideo(trackGroups[trackGroup].getTrackFormat(format).sampleMimeType)) {
                            videoTrackCounter++
                        }
                        if (videoTrackCounter > 1) {
                            break
                        }
                    }
                    if (videoTrackCounter > 1) {
                        break
                    }
                }
                // If we have enough video tracks to have a quality button, set it up.
                if (videoTrackCounter > 1) {
                    hq.visibility = VISIBLE
                    hq.setOnClickListener { v: View? ->
                        trackSelector!!.setParameters(
                            trackSelector!!.buildUponParameters()
                                .setForceLowestBitrate(false)
                                .setForceHighestSupportedBitrate(true)
                        )
                        hq.visibility = GONE
                    }
                }
            }
        })
    }

    enum class VideoType {
        STANDARD, DASH
    }

    /**
     * Helps manage audio focus
     */
    private inner class AudioFocusHelper internal constructor(private val manager: AudioManager?) :
        AudioManager.OnAudioFocusChangeListener {
        private var wasPlaying = false
        private var request: AudioFocusRequestCompat? = null

        init {
            if (request == null) {
                val audioAttributes = AudioAttributesCompat.Builder()
                    .setContentType(AudioAttributesCompat.CONTENT_TYPE_MOVIE)
                    .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                    .build()
                request =
                    AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN_TRANSIENT) //.setAcceptsDelayedFocusGain(false)
                        .setAudioAttributes(audioAttributes)
                        .setOnAudioFocusChangeListener(this)
                        .setWillPauseWhenDucked(true)
                        .build()
            }
        }

        /**
         * Lose audio focus
         */
        fun loseFocus() {
            AudioManagerCompat.abandonAudioFocusRequest(manager!!, request!!)
        }

        /**
         * Gain audio focus
         */
        fun gainFocus() {
            AudioManagerCompat.requestAudioFocus(manager!!, request!!)
        }

        override fun onAudioFocusChange(focusChange: Int) {
            // Pause on audiofocus loss, play on gain
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                wasPlaying = player!!.playWhenReady
                player!!.pause()
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                player!!.playWhenReady = wasPlaying
            }
        }
    }

    internal class PlayerUIFadeInAnimation(
        private val animationView: PlayerControlView,
        private val toVisible: Boolean,
        duration: Long
    ) : AnimationSet(false) {
        init {
            val startAlpha = (if (toVisible) 0 else 1).toFloat()
            val endAlpha = (if (toVisible) 1 else 0).toFloat()
            val alphaAnimation = AlphaAnimation(startAlpha, endAlpha)
            alphaAnimation.duration = duration
            addAnimation(alphaAnimation)
            setAnimationListener(Listener())
        }

        private inner class Listener : AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                animationView.show()
            }

            override fun onAnimationEnd(animation: Animation) {
                if (toVisible) animationView.show() else animationView.hide()
            }

            override fun onAnimationRepeat(animation: Animation) {
                // Purposefully left blank
            }
        }
    }
}
