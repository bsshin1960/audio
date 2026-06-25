package com.antigravity.audioplayer.ui

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.antigravity.audioplayer.service.AudioPlaybackService
import com.antigravity.audioplayer.service.MediaItemTree
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    // UI 관찰 상태 정의
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _isShuffleModeEnabled = MutableStateFlow(false)
    val isShuffleModeEnabled: StateFlow<Boolean> = _isShuffleModeEnabled

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode

    private val _folders = MutableStateFlow<List<MediaItem>>(emptyList())
    val folders: StateFlow<List<MediaItem>> = _folders

    private val _songsInSelectedFolder = MutableStateFlow<List<MediaItem>>(emptyList())
    val songsInSelectedFolder: StateFlow<List<MediaItem>> = _songsInSelectedFolder

    private val _selectedFolderName = MutableStateFlow<String?>(null)
    val selectedFolderName: StateFlow<String?> = _selectedFolderName

    private var positionUpdateJob: Job? = null

    @OptIn(UnstableApi::class)
    fun initMediaController(context: Context) {
        val sessionToken = SessionToken(context, ComponentName(context, AudioPlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                mediaController?.let { controller ->
                    _isConnected.value = true
                    _isPlaying.value = controller.isPlaying
                    _currentMediaItem.value = controller.currentMediaItem
                    _duration.value = controller.duration.coerceAtLeast(0L)
                    _isShuffleModeEnabled.value = controller.shuffleModeEnabled
                    _repeatMode.value = controller.repeatMode

                    controller.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _isPlaying.value = isPlaying
                            if (isPlaying) {
                                startTrackingPosition()
                            } else {
                                stopTrackingPosition()
                            }
                        }

                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            _currentMediaItem.value = mediaItem
                            _duration.value = controller.duration.coerceAtLeast(0L)
                        }

                        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                            _isShuffleModeEnabled.value = shuffleModeEnabled
                        }

                        override fun onRepeatModeChanged(repeatMode: Int) {
                            _repeatMode.value = repeatMode
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            _duration.value = controller.duration.coerceAtLeast(0L)
                        }
                    })

                    // 로컬 미디어 정보 로딩
                    loadMediaLibrary()
                    if (controller.isPlaying) {
                        startTrackingPosition()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun loadMediaLibrary() {
        // MediaItemTree에서 가공된 폴더 구조 로드
        val folderItems = MediaItemTree.getChildren(MediaItemTree.FOLDERS_CATEGORY_ID)
        _folders.value = folderItems
    }

    fun selectFolder(folderItem: MediaItem) {
        val folderName = folderItem.mediaMetadata.title.toString()
        _selectedFolderName.value = folderName
        _songsInSelectedFolder.value = MediaItemTree.getSongsInFolder(folderItem.mediaId)
    }

    fun clearFolderSelection() {
        _selectedFolderName.value = null
        _songsInSelectedFolder.value = emptyList()
    }

    fun playSong(mediaItem: MediaItem, songsContext: List<MediaItem>) {
        val controller = mediaController ?: return
        
        // 현재 폴더 안의 곡들로 플레이리스트(큐) 빌드
        val startIndex = songsContext.indexOfFirst { it.mediaId == mediaItem.mediaId }
        val finalIndex = if (startIndex >= 0) startIndex else 0

        controller.setMediaItems(songsContext, finalIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun playOrPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun skipToPrevious() {
        mediaController?.seekToPrevious()
    }

    fun skipToNext() {
        mediaController?.seekToNext()
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val nextMode = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = nextMode
        _isShuffleModeEnabled.value = nextMode
    }

    fun toggleRepeatMode() {
        val controller = mediaController ?: return
        val current = controller.repeatMode
        val nextMode = when (current) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = nextMode
        _repeatMode.value = nextMode
    }

    private fun startTrackingPosition() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (true) {
                mediaController?.let { controller ->
                    _currentPosition.value = controller.currentPosition
                }
                delay(500)
            }
        }
    }

    private fun stopTrackingPosition() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    override fun onCleared() {
        stopTrackingPosition()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        super.onCleared()
    }
}
