package com.antigravity.audioplayer.service

import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class AudioPlaybackService : MediaLibraryService() {

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
            .setAudioAttributes(audioAttributes, true) // true로 설정하면 자동으로 오디오 포커스 관리 (전화 수신, 네비게이션 감쇠 등)
            .setHandleAudioBecomingNoisy(true)         // 이어폰 분리 시 일시정지
            .build()
        exoPlayer = exoplayer

        // 3. MediaLibrarySession 생성
        val callback = CustomSessionCallback()
        mediaLibrarySession = MediaLibrarySession.Builder(this, exoplayer, callback)
            .build()
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
    private inner class CustomSessionCallback : MediaLibrarySession.Callback {

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
                // 폴더를 선택한 경우 폴더 내부의 모든 곡을 플레이리스트에 추가
                if (item.mediaId.startsWith("[FOLDER_]")) {
                    val folderSongs = MediaItemTree.getSongsInFolder(item.mediaId)
                    updatedItems.addAll(folderSongs)
                } else {
                    // 단일 곡일 경우
                    val fullItem = MediaItemTree.getItem(item.mediaId) ?: item
                    updatedItems.add(fullItem)
                }
            }
            return Futures.immediateFuture(updatedItems)
        }
    }
}
