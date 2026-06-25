package com.antigravity.audioplayer.service

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.antigravity.audioplayer.utils.AudioScanner
import com.antigravity.audioplayer.utils.AudioScanner.toMediaItem

object MediaItemTree {

    const val ROOT_ID = "[ROOT_ID]"
    const val FOLDERS_CATEGORY_ID = "[FOLDERS_CATEGORY_ID]"
    private const val FOLDER_PREFIX = "[FOLDER_]"

    // 미디어 ID를 키로 하는 모든 MediaItem 맵
    private val treeMap = HashMap<String, MediaItem>()
    // 부모 미디어 ID를 키로 하는 자식 MediaItem 리스트 맵
    private val parentToChildrenMap = HashMap<String, MutableList<MediaItem>>()
    // 곡 미디어 ID를 키로 하는 곡의 파일 경로 맵
    private val songPathMap = HashMap<String, String>()

    // 원본 로컬 곡 목록 보관 (순차/랜덤 재생 처리를 위해 필요)
    private var allSongs = listOf<com.antigravity.audioplayer.utils.LocalSong>()

    @Synchronized
    fun initialize(context: Context) {
        treeMap.clear()
        parentToChildrenMap.clear()
        songPathMap.clear()

        // 1. 로컬 곡 스캔
        allSongs = AudioScanner.scanAudioFiles(context)

        // 2. 루트 아이템 생성
        val rootMetadata = MediaMetadata.Builder()
            .setTitle("BsshinMusic Library")
            .setFolderType(MediaMetadata.FOLDER_TYPE_MIXED)
            .setIsPlayable(false)
            .build()
        val rootItem = MediaItem.Builder()
            .setMediaId(ROOT_ID)
            .setMediaMetadata(rootMetadata)
            .build()
        treeMap[ROOT_ID] = rootItem

        // 3. "음악 폴더" 카테고리 아이템 생성
        val foldersCategoryMetadata = MediaMetadata.Builder()
            .setTitle("음악 폴더")
            .setFolderType(MediaMetadata.FOLDER_TYPE_TITLES)
            .setIsPlayable(false)
            .build()
        val foldersCategoryItem = MediaItem.Builder()
            .setMediaId(FOLDERS_CATEGORY_ID)
            .setMediaMetadata(foldersCategoryMetadata)
            .build()
        treeMap[FOLDERS_CATEGORY_ID] = foldersCategoryItem

        // 루트의 자식으로 카테고리 추가
        parentToChildrenMap.getOrPut(ROOT_ID) { mutableListOf() }.add(foldersCategoryItem)

        // 4. 폴더별 그룹화 진행
        val foldersGrouped = allSongs.groupBy { it.folderName }

        for ((folderName, songs) in foldersGrouped) {
            val folderMediaId = FOLDER_PREFIX + folderName
            
            // 폴더 노드 생성
            val folderMetadata = MediaMetadata.Builder()
                .setTitle(folderName)
                .setFolderType(MediaMetadata.FOLDER_TYPE_ALBUMS)
                .setIsPlayable(false)
                .build()
            val folderItem = MediaItem.Builder()
                .setMediaId(folderMediaId)
                .setMediaMetadata(folderMetadata)
                .build()

            treeMap[folderMediaId] = folderItem
            // "음악 폴더" 카테고리의 자식으로 폴더 추가
            parentToChildrenMap.getOrPut(FOLDERS_CATEGORY_ID) { mutableListOf() }.add(folderItem)

            // 폴더 내 개별 곡들 추가
            for (song in songs) {
                val songItem = song.toMediaItem()
                treeMap[songItem.mediaId] = songItem
                songPathMap[songItem.mediaId] = song.dataPath
                
                // 폴더의 자식으로 곡 추가
                parentToChildrenMap.getOrPut(folderMediaId) { mutableListOf() }.add(songItem)
            }
        }
    }

    fun getItem(mediaId: String): MediaItem? {
        return treeMap[mediaId]
    }

    fun getChildren(parentMediaId: String): List<MediaItem> {
        return parentToChildrenMap[parentMediaId] ?: emptyList()
    }

    fun getSongsInFolder(folderMediaId: String): List<MediaItem> {
        return parentToChildrenMap[folderMediaId]?.filter { 
            it.mediaMetadata.isPlayable == true 
        } ?: emptyList()
    }

    fun getAllSongs(): List<com.antigravity.audioplayer.utils.LocalSong> {
        return allSongs
    }
    
    fun getSongPath(mediaId: String): String? {
        return songPathMap[mediaId]
    }
}
