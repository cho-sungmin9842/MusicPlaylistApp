package com.example.simplemusicplayer

import android.Manifest
import android.app.*
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.simplemusicplayer.databinding.ActivityMainBinding
import kotlin.properties.Delegates

@UnstableApi
class MainActivity : AppCompatActivity() {
    private var mService: MusicPlayerService? = null
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            // MusicPlayerBinder로 타입 캐스팅 해줍니다.
            mService = (service as MusicPlayerService.MusicPlayerBinder).getService()
            // MediaListAdapter 객체 생성 및 onClick 람다 함수 구현
            val playerAdapter = MediaListAdapter { entity, position ->
                val mediaItems = mService?.makePlayList(entity) ?: return@MediaListAdapter
                mService?.exoPlayer?.setMediaItems(mediaItems)
                binding.songTitleTextView.text = "현재 재생되고 있는 곡 = ${entity.korSongName} "
                if (mService?.state == State.STOP) {
                    mService?.exoPlayer = ExoPlayer.Builder(applicationContext).build()
                    mService?.exoPlayer?.volume = 1.0f
                    mService?.exoPlayer?.setMediaItems(mediaItems)
                    mService?.exoPlayer?.prepare()
                    mService?.exoPlayer?.playWhenReady = true
                    binding.exoplayerView.player = mService?.exoPlayer
                }
                mService?.exoPlayer?.prepare()
                mService?.exoPlayer?.playWhenReady = true
                binding.exoplayerView.player = mService?.exoPlayer
                mService?.exoPlayer?.addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        super.onMediaItemTransition(mediaItem, reason)
                        mService?.startForeground(
                            title = musicList.music[mService?.exoPlayer?.currentMediaItemIndex?.plus(
                                position
                            ) ?: return].singer,
                            text = musicList.music[mService?.exoPlayer?.currentMediaItemIndex?.plus(
                                position
                            ) ?: return].korSongName
                        )
                        if (mService == null)
                            binding.songTitleTextView.text = "현재 재생되고 있는 곡 = ${
                                musicList.music[mService?.exoPlayer?.currentMediaItemIndex?.plus(
                                    position
                                ) ?: return].korSongName
                            }"
                        else
                            binding.songTitleTextView.text = "현재 재생되고 있는 곡 = ${
                                musicList.music[mService?.exoPlayer?.currentMediaItemIndex?.plus(
                                    mService?.start ?: return
                                ) ?: return].korSongName
                            }"
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        super.onIsPlayingChanged(isPlaying)
                        if (isPlaying) {
                            mService?.startForeground(
                                title = musicList.music[mService?.exoPlayer?.currentMediaItemIndex?.plus(
                                    position
                                ) ?: -1].singer,
                                text = musicList.music[mService?.exoPlayer?.currentMediaItemIndex?.plus(
                                    position
                                ) ?: -1].korSongName
                            )
                        }
                        else{
                            mService?.startForeground(
                                title = musicList.music[mService?.exoPlayer?.currentMediaItemIndex?.plus(
                                    mService?.start ?: return
                                ) ?: return].singer,
                                text = musicList.music[mService?.exoPlayer?.currentMediaItemIndex?.plus(
                                    mService?.start ?: return
                                ) ?: return].korSongName
                            )
                        }
                        if (mService?.state == State.STOP && !isPlaying) {
                            binding.songTitleTextView.text = ""
                        }
                    }
                })
                mService?.startForeground(title = entity.singer, text = entity.korSongName)
                startForegroundService(Intent(
                    applicationContext, MusicPlayerService::class.java
                ).apply {
                    action = MEDIA_PLAYER_PLAY
                    putExtra("item", entity)
                })
            }
            if (intent.action == MEDIA_PLAYER_MAIN) {
                binding.exoplayerView.player = mService?.exoPlayer
                binding.songTitleTextView.text = "현재 재생되고 있는 곡 = ${
                    musicList.music[mService?.exoPlayer?.currentMediaItemIndex?.plus(
                        mService?.start ?: return
                    ) ?: return].korSongName
                }"
                mService?.exoPlayer?.addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        super.onMediaItemTransition(mediaItem, reason)
                        mService?.startForeground(
                            title = musicList.music[mService?.exoPlayer?.currentMediaItemIndex?.plus(
                                mService?.start ?: return
                            ) ?: return].singer,
                            text = musicList.music[mService?.exoPlayer?.currentMediaItemIndex?.plus(
                                mService?.start ?: return
                            ) ?: return].korSongName
                        )
                        binding.songTitleTextView.text = "현재 재생되고 있는 곡 = ${
                            musicList.music[mService?.exoPlayer?.currentMediaItemIndex?.plus(
                                mService?.start ?: return
                            ) ?: return].korSongName
                        }"
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        super.onIsPlayingChanged(isPlaying)
                        if (isPlaying) {
                            mService?.startForeground(
                                title = musicList.music[mService?.exoPlayer?.currentMediaItemIndex?.plus(
                                    mService?.start ?: return
                                ) ?: return].singer,
                                text = musicList.music[mService?.exoPlayer?.currentMediaItemIndex?.plus(
                                    mService?.start ?: return
                                ) ?: return].korSongName
                            )
                        }else{
                            mService?.startForeground(
                                title = musicList.music[mService?.exoPlayer?.currentMediaItemIndex?.plus(
                                    mService?.start ?: return
                                ) ?: return].singer,
                                text = musicList.music[mService?.exoPlayer?.currentMediaItemIndex?.plus(
                                    mService?.start ?: return
                                ) ?: return].korSongName
                            )
                        }
                        if (mService?.state == State.STOP && !isPlaying) {
                            binding.songTitleTextView.text = ""
                        }
                    }
                })
            }
            // playerListRecyclerView 설정(layoutManager 및 adapter 연결,아이템 간격 설정,아이템 리스트 전달)
            binding.playerListRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = playerAdapter
                addItemDecoration(VerticalItemDecorator(10))
                playerAdapter.submitList(musicList.music)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null     // 만약 서비스가 끊기면, mService를 null로 만들어줍니다.
        }
    }

    private val notificationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            when (isGranted) {
                // 권한을 허용한 경우 서비스를 실행함
                true -> {
                    startService()
                }
                // 권한을 허용하지 않은 경우
                false -> {
                    // 사용자가 마지막(2번)까지 알림권한을 허용하지 않은 경우
                    // 사용자가 직접 앱에 권한을 부여하는 설정을 띄우는 액티비티를 실행함
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                        getPermissionSettings()
                    }
                }
            }
        }

    private val musicList: MusicList by lazy {
        readData("music.json", MusicList::class.java) ?: MusicList(emptyList())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

    }

    override fun onResume() {
        super.onResume()
        when {
            // 알림 권한이 있다면 서비스를 실행함
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                startService()
            }
            // 사용자가 알림 권한을 한번 허용하지 않음을 누른 경우에 해당권한이 필요한 이유를 설명하는 다이얼로그를 띄움
            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                showDialog()
            }
            // 알림 권한이 없고 이전에 사용자에게 알림 권한이 필요한 이유를 설명한 경우
            else -> {
                // 알림 권한이 없는 경우 알림 권한을 요청함
                if (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_DENIED
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }

    }

    override fun onPause() {
        super.onPause()
        if (mService != null) {
            // 만약 mService가 재생되고 있지 않다면 서비스를 중단해줍니다.
            if (mService?.isPlaying() == false) {
                mService!!.stopForeground(Service.STOP_FOREGROUND_REMOVE)
            }
        }
    }

    private fun startService() {
        if (mService == null) {
            // 안드로이드 O 이상인 경우 startForegroundService를 사용해주어야 합니다.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                startForegroundService(Intent(this, MusicPlayerService::class.java))
            else
                startService(Intent(this, MusicPlayerService::class.java))
            val intent = Intent(this, MusicPlayerService::class.java)
            bindService(intent, mServiceConnection, BIND_AUTO_CREATE)   // 서비스와 바인드합니다.
        }
    }

    // 사용자에게 알림 권한이 필요한 이유를 알려주는 AlertDialog를 띄우는 함수
    private fun showDialog() {
        AlertDialog.Builder(this).apply {
            setTitle("포그라운드 서비스를 이용하기 위해 알림 권한이 필요합니다")
            setPositiveButton("허용") { _, _ ->
                // 알림 권한을 요청함
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            setNegativeButton("취소", null)
            show()
        }
    }

    // 사용자가 알림 권한을 직접 앱에 부여하는 설정 띄우는 액티비티를 실행하는 함수
    private fun getPermissionSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
}