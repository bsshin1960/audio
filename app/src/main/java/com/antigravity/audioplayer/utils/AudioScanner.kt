package com.antigravity.audioplayer.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaItem
import java.io.File

import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.os.Build

data class LocalSong(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val dataPath: String,
    val folderName: String,
    val uri: Uri,
    val isVideo: Boolean = false
)

object AudioScanner {

    fun scanAudioFiles(context: Context): List<LocalSong> {
        val songs = mutableListOf<LocalSong>()
        
        // 1. MediaStore.Audio.Media (오디오 파일 스캔)
        scanAudio(context, songs)
        
        // 2. MediaStore.Video.Media (MP4 파일 스캔 - 비디오 권한이 허용된 경우에만 실행)
        if (hasVideoPermission(context)) {
            scanVideoAsAudio(context, songs)
        }

        // 가나다순 정렬
        songs.sortBy { it.title.lowercase() }
        return songs
    }

    private fun hasVideoPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun scanAudio(context: Context, songs: MutableList<LocalSong>) {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown Title"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val duration = cursor.getLong(durationColumn)
                val dataPath = cursor.getString(dataColumn) ?: ""

                if (dataPath.isEmpty()) continue

                val file = File(dataPath)
                val folderName = file.parentFile?.name ?: "Root"
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                songs.add(
                    LocalSong(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        dataPath = dataPath,
                        folderName = folderName,
                        uri = contentUri,
                        isVideo = false
                    )
                )
            }
        }
    }

    private fun scanVideoAsAudio(context: Context, songs: MutableList<LocalSong>) {
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.ARTIST,
            MediaStore.Video.Media.ALBUM,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATA
        )

        // DATA(파일 경로)가 .mp4 로 끝나는 파일들을 수집
        val selection = "${MediaStore.Video.Media.DATA} LIKE '%.mp4' OR ${MediaStore.Video.Media.DATA} LIKE '%.MP4'"

        context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown Video"
                val artist = cursor.getString(artistColumn) ?: "동영상 음악"
                val album = cursor.getString(albumColumn) ?: "비디오 앨범"
                val duration = cursor.getLong(durationColumn)
                val dataPath = cursor.getString(dataColumn) ?: ""

                if (dataPath.isEmpty()) continue

                val file = File(dataPath)
                val folderName = file.parentFile?.name ?: "Root"
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                songs.add(
                    LocalSong(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        dataPath = dataPath,
                        folderName = folderName,
                        uri = contentUri,
                        isVideo = true
                    )
                )
            }
        }
    }

    fun LocalSong.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setFolderType(MediaMetadata.FOLDER_TYPE_NONE)
            .setIsPlayable(true)
            .build()

        // 오디오 ID와 비디오 ID의 충돌을 방지하기 위해 비디오 파일은 접두사 추가
        val uniqueMediaId = if (isVideo) "VIDEO_$id" else id.toString()

        return MediaItem.Builder()
            .setMediaId(uniqueMediaId)
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }
}
