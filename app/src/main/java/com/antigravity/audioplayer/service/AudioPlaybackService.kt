package com.antigravity.audioplayer.service

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.antigravity.audioplayer.R
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class AudioPlaybackService : MediaLibraryService() {

    companion object {
        // 안드로이드 오토 화면에서 셔플/반복 버튼을 터치했을 때 처리할 커스텀 명령 ID
        const val CUSTOM_CMD_TOGGLE_SHUFFLE = "action.TOGGLE_SHUFFLE"
        const val CUSTOM_CMD_TOGGLE_REPEAT = "action.TOGGLE_REPEAT"
    }

    private var exoPlayer: ExoPlayer? = null
    private var mediaLibrarySession: MediaLibrarySession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // 1. MediaItemTree 초기화 (스마트폰 기기 음악 스캔)
        MediaItemTree.initialize(applicationContext)

        // 2. ExoPlayer 초기화 및 오디오 포커스 설정
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val exoplayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // 자동 오디오 포커스 관리
            .setHandleAudioBecomingNoisy(true)         // 이어폰 분리 시 일시정지
            .build()
        exoPlayer = exoplayer

        // 3. MediaLibrarySession 생성
        val callback = CustomSessionCallback()
        mediaLibrarySession = MediaLibrarySession.Builder(this, exoplayer, callback)
            .build()

        // 4. 플레이어 상태 변경 시 안드로이드 오토 버튼 레이아웃 갱신
        //    (스마트폰 UI에서 셔플/반복을 바꾸면 내비 화면 버튼에도 즉시 반영)
        exoplayer.addListener(object : Player.Listener {
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                updateCustomLayout()
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                updateCustomLayout()
            }
        })

        // 5. 초기 버튼 레이아웃 설정 (셔플 꺼짐, 전체반복 기본값)
        updateCustomLayout()
    }

    /**
     * 안드로이드 오토 미디어 플레이어 화면의 추가 버튼(셔플, 반복)을 현재 상태에 맞게 갱신한다.
     * setCustomLayout()에 등록된 CommandButton 목록이 내비 화면 하단에 표시된다.
     */
    @OptIn(UnstableApi::class)
    private fun updateCustomLayout() {
        val session = mediaLibrarySession ?: return
        val player = exoPlayer ?: return

        val shuffleEnabled = player.shuffleModeEnabled
        val repeatMode = player.repeatMode

        // 셔플 버튼: 현재 상태(ON/OFF)를 표시 이름으로 알림
        val shuffleButton = CommandButton.Builder()
            .setDisplayName(if (shuffleEnabled) "셔플 켜짐" else "셔플 끄기")
            .setIconResId(R.drawable.ic_shuffle)
            .setSessionCommand(SessionCommand(CUSTOM_CMD_TOGGLE_SHUFFLE, Bundle.EMPTY))
            .build()

        // 반복 버튼: OFF → 전체반복 → 한곡반복 순환, 아이콘도 상태에 맞게 변경
        val repeatButton = CommandButton.Builder()
            .setDisplayName(
                when (repeatMode) {
                    Player.REPEAT_MODE_ALL -> "전체 반복"
                    Player.REPEAT_MODE_ONE -> "한 곡 반복"
                    else -> "반복 없음"
                }
            )
            .setIconResId(
                if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.ic_repeat_one
                else R.drawable.ic_repeat
            )
            .setSessionCommand(SessionCommand(CUSTOM_CMD_TOGGLE_REPEAT, Bundle.EMPTY))
            .build()

        session.setCustomLayout(ImmutableList.of(shuffleButton, repeatButton))
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        mediaLibrarySession?.run {
            exoPlayer?.release()
            release()
            mediaLibrarySession = null
            exoPlayer = null
        }
        super.onDestroy()
    }

    // 안드로이드 오토 및 미디어 브라우저 연동을 위한 콜백 구현
    @OptIn(UnstableApi::class)
    private inner class CustomSessionCallback : MediaLibrarySession.Callback {

        /**
         * 컨트롤러(안드로이드 오토 포함) 연결 시 호출.
         * 커스텀 셔플/반복 명령을 허용 목록에 추가하여 내비 화면에서 버튼 터치가 동작하도록 한다.
         */
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands =
                MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                    .add(SessionCommand(CUSTOM_CMD_TOGGLE_SHUFFLE, Bundle.EMPTY))
                    .add(SessionCommand(CUSTOM_CMD_TOGGLE_REPEAT, Bundle.EMPTY))
                    .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        /**
         * 안드로이드 오토 화면에서 셔플/반복 버튼을 터치했을 때 호출되는 핸들러.
         */
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            return when (customCommand.customAction) {
                CUSTOM_CMD_TOGGLE_SHUFFLE -> {
                    // 셔플 ON/OFF 토글
                    session.player.shuffleModeEnabled = !session.player.shuffleModeEnabled
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                CUSTOM_CMD_TOGGLE_REPEAT -> {
                    // REPEAT_OFF → REPEAT_ALL → REPEAT_ONE → REPEAT_OFF 순환
                    val nextMode = when (session.player.repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        else -> Player.REPEAT_MODE_OFF
                    }
                    session.player.repeatMode = nextMode
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                else -> super.onCustomCommand(session, controller, customCommand, args)
            }
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItemTree.getItem(MediaItemTree.ROOT_ID)
                ?: return Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val item = MediaItemTree.getItem(mediaId)
                ?: return Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
            return Futures.immediateFuture(LibraryResult.ofItem(item, null))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            // DHU 연결 시 트리가 비어있을 경우 재초기화
            if (MediaItemTree.getChildren(MediaItemTree.ROOT_ID).isEmpty()) {
                MediaItemTree.initialize(applicationContext)
            }
            val children = MediaItemTree.getChildren(parentId)
            return Futures.immediateFuture(
                LibraryResult.ofItemList(ImmutableList.copyOf(children), params)
            )
        }

        // 재생 아이템 추가 요청 처리 (안드로이드 오토에서 특정 곡이나 폴더를 탭했을 때)
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val updatedItems = mutableListOf<MediaItem>()
            for (item in mediaItems) {
                if (item.mediaId.startsWith("[FOLDER_]") || item.mediaId.startsWith("[EXP_DIR_]")) {
                    val folderSongs = mutableListOf<MediaItem>()
                    collectSongsRecursively(item.mediaId, folderSongs)
                    updatedItems.addAll(folderSongs)
                } else {
                    val fullItem = MediaItemTree.getItem(item.mediaId) ?: item
                    updatedItems.add(fullItem)
                }
            }
            return Futures.immediateFuture(updatedItems)
        }

        // 특정 폴더/디렉토리 하위의 모든 재생 가능한 곡을 재귀적으로 수집
        private fun collectSongsRecursively(parentId: String, songList: MutableList<MediaItem>) {
            val children = MediaItemTree.getChildren(parentId)
            for (child in children) {
                if (child.mediaMetadata.isPlayable == true) {
                    songList.add(child)
                } else {
                    // 서브 디렉토리인 경우 재귀 탐색
                    collectSongsRecursively(child.mediaId, songList)
                }
            }
        }
    }
}
