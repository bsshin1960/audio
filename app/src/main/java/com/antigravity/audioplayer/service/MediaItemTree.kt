package com.antigravity.audioplayer.service

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.antigravity.audioplayer.utils.AudioScanner
import com.antigravity.audioplayer.utils.AudioScanner.toMediaItem
import java.io.File

object MediaItemTree {

    const val ROOT_ID = "[ROOT_ID]"
    const val FOLDERS_CATEGORY_ID = "[FOLDERS_CATEGORY_ID]"
    const val EXPLORER_CATEGORY_ID = "[EXPLORER_CATEGORY_ID]"
    
    private const val FOLDER_PREFIX = "[FOLDER_]"
    private const val EXPLORER_DIR_PREFIX = "[EXP_DIR_]"

    // 미디어 ID를 키로 하는 모든 MediaItem 맵
    private val treeMap = HashMap<String, MediaItem>()
    // 부모 미디어 ID를 키로 하는 자식 MediaItem 리스트 맵
    private val parentToChildrenMap = HashMap<String, MutableList<MediaItem>>()
    // 곡 미디어 ID를 키로 하는 곡의 파일 경로 맵
    private val songPathMap = HashMap<String, String>()

    // 원본 로컬 곡 목록 보관
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
            .setTitle("GravityMusic Library")
            .setFolderType(MediaMetadata.FOLDER_TYPE_MIXED)
            .setIsPlayable(false)
            .build()
        val rootItem = MediaItem.Builder()
            .setMediaId(ROOT_ID)
            .setMediaMetadata(rootMetadata)
            .build()
        treeMap[ROOT_ID] = rootItem

        // 3. "음악 폴더" 카테고리 아이템 생성 (기존 Flat 뷰)
        val foldersCategoryMetadata = MediaMetadata.Builder()
            .setTitle("음악 폴더 (기본)")
            .setFolderType(MediaMetadata.FOLDER_TYPE_TITLES)
            .setIsPlayable(false)
            .build()
        val foldersCategoryItem = MediaItem.Builder()
            .setMediaId(FOLDERS_CATEGORY_ID)
            .setMediaMetadata(foldersCategoryMetadata)
            .build()
        treeMap[FOLDERS_CATEGORY_ID] = foldersCategoryItem

        // 4. "파일 탐색기" 카테고리 아이템 생성 (신규 계층 뷰)
        val explorerCategoryMetadata = MediaMetadata.Builder()
            .setTitle("파일 탐색기")
            .setFolderType(MediaMetadata.FOLDER_TYPE_TITLES)
            .setIsPlayable(false)
            .build()
        val explorerCategoryItem = MediaItem.Builder()
            .setMediaId(EXPLORER_CATEGORY_ID)
            .setMediaMetadata(explorerCategoryMetadata)
            .build()
        treeMap[EXPLORER_CATEGORY_ID] = explorerCategoryItem

        // 루트의 자식으로 카테고리 2종 추가
        parentToChildrenMap.getOrPut(ROOT_ID) { mutableListOf() }.add(foldersCategoryItem)
        parentToChildrenMap.getOrPut(ROOT_ID) { mutableListOf() }.add(explorerCategoryItem)

        // 5. 기본 평탄화 폴더(Flat Folders) 그룹화 진행
        val foldersGrouped = allSongs.groupBy { it.folderName }
        for ((folderName, songs) in foldersGrouped) {
            val folderMediaId = FOLDER_PREFIX + folderName
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
            parentToChildrenMap.getOrPut(FOLDERS_CATEGORY_ID) { mutableListOf() }.add(folderItem)

            for (song in songs) {
                val songItem = song.toMediaItem()
                treeMap[songItem.mediaId] = songItem
                songPathMap[songItem.mediaId] = song.dataPath
                parentToChildrenMap.getOrPut(folderMediaId) { mutableListOf() }.add(songItem)
            }
        }

        // 6. 실제 파일 시스템 기준 "계층형 파일 탐색기" 트리 구성
        buildExplorerTree()
    }

    /**
     * 음악 파일들의 실제 dataPath를 기준으로 계층적 디렉토리 트리를 빌드합니다.
     */
    private fun buildExplorerTree() {
        // 이미 생성된 음악 MediaItem 목록 재활용
        for (song in allSongs) {
            // 중요: 비디오 파일은 "VIDEO_" 접두사가 붙어있으므로 ID 매핑 분기 처리 필수
            val uniqueMediaId = if (song.isVideo) "VIDEO_${song.id}" else song.id.toString()
            val songItem = treeMap[uniqueMediaId] ?: continue
            val file = File(song.dataPath)
            val parentFile = file.parentFile ?: continue

            // 각 상위 디렉토리 노드를 탐색 및 생성
            val parentDirId = getOrCreateDirNode(parentFile)
            
            // 디렉토리 노드의 자식으로 곡 추가
            val children = parentToChildrenMap.getOrPut(parentDirId) { mutableListOf() }
            if (children.none { it.mediaId == songItem.mediaId }) {
                children.add(songItem)
            }
        }

        // 최상위 경로들을 [EXPLORER_CATEGORY_ID]의 자식으로 링크
        linkTopLevelDirsToExplorerRoot()
    }

    /**
     * 특정 디렉토리에 대한 MediaItem 노드를 생성하고, 그 부모 디렉토리로 올라가며 연결을 재귀적으로 수립합니다.
     */
    private fun getOrCreateDirNode(dir: File): String {
        val dirPath = dir.absolutePath
        val dirMediaId = EXPLORER_DIR_PREFIX + dirPath

        if (treeMap.containsKey(dirMediaId)) {
            return dirMediaId
        }

        // 디렉토리 노드 생성
        val metadata = MediaMetadata.Builder()
            .setTitle(dir.name.ifEmpty { dirPath })
            .setFolderType(MediaMetadata.FOLDER_TYPE_ALBUMS)
            .setIsPlayable(false)
            .build()
        val dirItem = MediaItem.Builder()
            .setMediaId(dirMediaId)
            .setMediaMetadata(metadata)
            .build()

        treeMap[dirMediaId] = dirItem

        // 부모 디렉토리가 있으면 부모 노드를 재귀적으로 확보하여 자식으로 링크
        val parentDir = dir.parentFile
        if (parentDir != null && dirPath != "/" && !dirPath.equals("C:\\", ignoreCase = true)) {
            val parentDirId = getOrCreateDirNode(parentDir)
            val parentChildren = parentToChildrenMap.getOrPut(parentDirId) { mutableListOf() }
            if (parentChildren.none { it.mediaId == dirMediaId }) {
                // 서브폴더들은 목록의 위쪽에 노출되도록 인덱스 0번에 추가
                parentChildren.add(0, dirItem)
            }
        }

        return dirMediaId
    }

    /**
     * 파일 탐색기 카테고리 루트의 자식으로 기본 "Music(음악)" 폴더 내부 리스트를 직접 연결합니다.
     */
    private fun linkTopLevelDirsToExplorerRoot() {
        val rootChildren = parentToChildrenMap.getOrPut(EXPLORER_CATEGORY_ID) { mutableListOf() }
        
        // 1. 표준 안드로이드 Music 폴더 경로 ID 확인
        val targetMusicPathId = EXPLORER_DIR_PREFIX + "/storage/emulated/0/Music"
        
        // 2. 혹은 이름이 "Music" 이거나 "music" 인 디렉토리 ID가 존재하는지 확인
        val matchedMusicDirId = treeMap.keys.firstOrNull { id ->
            id.startsWith(EXPLORER_DIR_PREFIX) && 
            (id == targetMusicPathId || File(id.substring(EXPLORER_DIR_PREFIX.length)).name.equals("Music", ignoreCase = true))
        }

        if (matchedMusicDirId != null) {
            // Music 폴더 내부의 아이템들을 탐색기 루트의 자식으로 등록
            val musicChildren = parentToChildrenMap[matchedMusicDirId]
            if (musicChildren != null) {
                rootChildren.addAll(musicChildren)
                return
            }
        }

        // 3. Fallback: Music 폴더를 찾지 못한 경우, 기존 방식대로 최상위 디렉토리 목록을 노출
        val allDirIds = treeMap.keys.filter { it.startsWith(EXPLORER_DIR_PREFIX) }
        for (dirId in allDirIds) {
            val dirPath = dirId.substring(EXPLORER_DIR_PREFIX.length)
            val dirFile = File(dirPath)
            val parentFile = dirFile.parentFile

            val isTopLevel = parentFile == null || 
                             parentFile.absolutePath == "/" || 
                             parentFile.absolutePath.equals("C:\\", ignoreCase = true) ||
                             dirPath.equals("/storage/emulated/0", ignoreCase = true) ||
                             parentFile.absolutePath.equals("/storage/emulated", ignoreCase = true)

            if (isTopLevel) {
                val dirItem = treeMap[dirId]
                if (dirItem != null && rootChildren.none { it.mediaId == dirId }) {
                    rootChildren.add(dirItem)
                }
            }
        }
        rootChildren.sortBy { it.mediaMetadata.title.toString() }
    }

    fun getItem(mediaId: String): MediaItem? {
        return treeMap[mediaId]
    }

    fun getChildren(parentMediaId: String): List<MediaItem> {
        return parentToChildrenMap[parentMediaId] ?: emptyList()
    }

    fun getSongsInFolder(folderMediaId: String): List<MediaItem> {
        // 계층 구조에 있는 디렉토리 노드에 포함된 직계 곡들 반환
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
