package com.example.simplemusicplayer

import android.app.*
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import com.example.simplemusicplayer.databinding.ActivityMainBinding

@UnstableApi
class MusicPlayerService : Service() {
    var state: State = State.IDLE
    var exoPlayer: ExoPlayer? = null
    var mBinder: MusicPlayerBinder = MusicPlayerBinder()
    private val raw = R.raw::class.java.fields
    private val musicList: MusicList by lazy {
        readData("music.json", MusicList::class.java) ?: MusicList(emptyList())
    }
    var start = -1
    lateinit var notification: Notification.Builder

    inner class MusicPlayerBinder : Binder() {
        fun getService(): MusicPlayerService {
            return this@MusicPlayerService
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(applicationContext).build()
            exoPlayer?.volume = 1.0f   // 볼륨을 지정해줍니다.
        }
        startForegroundService()
    }

    override fun onBind(intent: Intent?): IBinder {   // ❷ 바인더 반환
        return mBinder
    }

    // ❸ startService()를 호출하면 실행되는 콜백 함수
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            // 재생버튼인 경우 미디어 플레이를 생성하고 재생
            MEDIA_PLAYER_PLAY -> {
                state = State.PLAYING
                if (exoPlayer == null) {
                    exoPlayer = ExoPlayer.Builder(applicationContext).build()
                    exoPlayer?.volume = 1.0f    // 볼륨을 지정해줍니다.
                    exoPlayer?.prepare()
                    val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getSerializableExtra("item", MusicEntity::class.java)
                    } else {
                        intent.getSerializableExtra("item") as MusicEntity
                    }
                    val mediaItems = list?.let {
                        makePlayList(it)
                    } ?: emptyList()
                    mediaItems.let { item ->
                        exoPlayer?.setMediaItems(item)
                    }
                    exoPlayer?.addListener(object : Player.Listener {
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            super.onMediaItemTransition(mediaItem, reason)
                            startForeground(
                                title = musicList.music[exoPlayer?.currentMediaItemIndex?.plus(
                                    start
                                ) ?: -1].singer,
                                text = musicList.music[exoPlayer?.currentMediaItemIndex?.plus(
                                    start
                                ) ?: -1].korSongName,
                            )
                        }

                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            super.onIsPlayingChanged(isPlaying)
                            startForeground(
                                title = musicList.music[exoPlayer?.currentMediaItemIndex?.plus(
                                    start
                                )!!].singer,
                                text = musicList.music[exoPlayer?.currentMediaItemIndex?.plus(
                                    start
                                )!!].korSongName,
                            )
                        }
                    })
                }
                if (exoPlayer?.currentMediaItem == null) {
                    Toast.makeText(this, "음악을 먼저 선택하세요.", Toast.LENGTH_SHORT).show()
                } else {
                    play()
                }
            }
            // 일시정지버튼인 경우 미디어 플레이를 일시정지
            MEDIA_PLAYER_PAUSE -> {
                state = State.PAUSE
                pause()
            }
            // 정지버튼인 경우 미디어 플레이를 정지
            MEDIA_PLAYER_CLOSE -> {
                state = State.STOP
                // 미디어 플레이를 정지
                stop()
                // 메모리를 해제
                exoPlayer?.release()
                exoPlayer = null
                // 서비스도 종료 시킴
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
            // 이전 플레이 리스트 플레이
            MEDIA_PLAYER_PRE -> {
                state = State.PLAYING
                if (exoPlayer?.hasPreviousMediaItem() == true) {
                    exoPlayer?.seekToPreviousMediaItem()
                    exoPlayer?.play()
                } else {
                    Toast.makeText(applicationContext, "이전 음악이 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            // 다음 플레이 리스트 플레이
            MEDIA_PLAYER_NEXT -> {
                state = State.PLAYING
                if (exoPlayer?.hasNextMediaItem() == true) {
                    exoPlayer?.seekToNextMediaItem()
                    exoPlayer?.play()
                } else {
                    Toast.makeText(applicationContext, "다음 음악이 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return START_STICKY
    }

    // ❶ 알림 채널 생성
    private fun startForegroundService() {
        // 재생,일시정지,정지 아이콘 생성
        val playIcon =
            Icon.createWithResource(baseContext, R.drawable.ic_baseline_play_arrow_24)
        val pauseIcon = Icon.createWithResource(baseContext, R.drawable.ic_baseline_pause_24)
        val closeIcon = Icon.createWithResource(baseContext, R.drawable.ic_baseline_close_24)
        val nextIcon = Icon.createWithResource(baseContext, R.drawable.ic_baseline_skip_next_24)
        val preIcon = Icon.createWithResource(baseContext, R.drawable.ic_baseline_skip_previous_24)
        // 재생,일시정지,정지 PendingIntent 생성 및 초기화
        val mainPendingIntent = PendingIntent.getActivity(
            baseContext, 0, Intent(baseContext, MainActivity::class.java).apply {
                action = MEDIA_PLAYER_MAIN
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP // 해당액티비티가 stack에 있으면 그것으로 대체함
            }, PendingIntent.FLAG_IMMUTABLE    // 변하지 않음
        )
        val playPendingIntent = PendingIntent.getService(
            baseContext, 0, Intent(baseContext, MusicPlayerService::class.java).apply {
                action = MEDIA_PLAYER_PLAY
            }, PendingIntent.FLAG_IMMUTABLE    // 변하지 않음
        )
        val pausePendingIntent = PendingIntent.getService(
            baseContext, 0, Intent(baseContext, MusicPlayerService::class.java).apply {
                action = MEDIA_PLAYER_PAUSE
            }, PendingIntent.FLAG_IMMUTABLE    // 변하지 않음
        )
        val closePendingIntent = PendingIntent.getService(
            baseContext, 0, Intent(baseContext, MusicPlayerService::class.java).apply {
                action = MEDIA_PLAYER_CLOSE
            }, PendingIntent.FLAG_IMMUTABLE    // 변하지 않음
        )
        val nextPendingIntent = PendingIntent.getService(
            baseContext, 0, Intent(baseContext, MusicPlayerService::class.java).apply {
                action = MEDIA_PLAYER_NEXT
            }, PendingIntent.FLAG_IMMUTABLE    // 변하지 않음
        )
        val prePendingIntent = PendingIntent.getService(
            baseContext, 0, Intent(baseContext, MusicPlayerService::class.java).apply {
                action = MEDIA_PLAYER_PRE
            }, PendingIntent.FLAG_IMMUTABLE    // 변하지 않음
        )
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        // 알림 채널을 생성합니다.
        val mChannel = NotificationChannel(
            "CHANNEL_ID", "CHANNEL_NAME", NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(mChannel)
        // ❷ 알림 생성
        notification = Notification.Builder(this, "CHANNEL_ID")
            .setSmallIcon(R.drawable.ic_baseline_notifications_24) // 알림 아이콘입니다.
            .setStyle(Notification.MediaStyle().setShowActionsInCompactView(0, 1, 2))
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(Notification.Action.Builder(preIcon, "Pre", prePendingIntent).build())
            .addAction(Notification.Action.Builder(pauseIcon, "Pause", pausePendingIntent).build())
            .addAction(Notification.Action.Builder(playIcon, "Play", playPendingIntent).build())
            .addAction(Notification.Action.Builder(nextIcon, "Next", nextPendingIntent).build())
            .addAction(Notification.Action.Builder(closeIcon, "Close", closePendingIntent).build())
            .setContentIntent(mainPendingIntent)    // 알림클릭시 mainPendingIntent로 이동
            .setOngoing(true)   // 사용자가 알림을 스와이프해서 지울수 없게 설정함
    }

    // ❸ 서비스 중단 처리
    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun startForeground(title: String, text: String) {
        notification.setContentTitle(title)
        notification.setContentText(text)
        val notification = notification.build()
        startForeground(1, notification)
    }

    // 재생되고 있는지 확인합니다.
    fun isPlaying(): Boolean {
        return (exoPlayer != null && exoPlayer?.isPlaying ?: false)
    }

    fun play() {
        if (exoPlayer == null) {
            // 음악 파일의 리소스를 가져와 미디어 플레이어 객체를 할당해줍니다.
            exoPlayer = ExoPlayer.Builder(applicationContext).build()
            exoPlayer?.volume = 1.0f    // 볼륨을 지정해줍니다.
            exoPlayer?.prepare()
            exoPlayer?.playWhenReady = true
            exoPlayer?.play() // 음악을 재생합니다.
            state = State.PLAYING
        } else {
            // 음악이 재생 중이라면
            if (exoPlayer?.isPlaying == true)
                Toast.makeText(this, "이미 음악이 실행 중입니다.", Toast.LENGTH_SHORT).show()
            else exoPlayer?.play()  // 음악을 재생합니다.
        }
    }

    fun pause() {
        exoPlayer?.let {
            if (it.isPlaying) {
                it.pause() // 음악을 일시정지합니다.
                state = State.PAUSE
            }
        }
    }

    fun stop() {
        exoPlayer?.let {
            if (it.isPlaying) {
                it.stop()   // 음악을 멈춥니다.
                it.release()    // 미디어 플레이어에 할당된 자원을 해제시켜줍니다.
                exoPlayer = null
                state = State.STOP
            }
        }
    }

    fun makePlayList(mediaEntity: MusicEntity): List<MediaItem> {
        val mediaitems = mutableListOf<MediaItem>()
        raw.forEachIndexed { index, field ->
            if (field.name == mediaEntity.engSongName) {
                start = index

            }
        }
        for (count in start until raw.size) {
            val musicID = this.resources.getIdentifier(raw[count].name, "raw", this.packageName)
            val musicUri: Uri = RawResourceDataSource.buildRawResourceUri(musicID)
            val mediaitem = MediaItem.Builder().setUri(musicUri).build()
            mediaitems.add(mediaitem)
        }
        return mediaitems
    }
}