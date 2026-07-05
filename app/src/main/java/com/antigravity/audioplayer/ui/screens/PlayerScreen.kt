package com.antigravity.audioplayer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.antigravity.audioplayer.ui.MainViewModel
import com.antigravity.audioplayer.ui.theme.*

@Composable
fun PlayerScreen(viewModel: MainViewModel) {
    val folders by viewModel.folders.collectAsState()
    val selectedFolderSongs by viewModel.songsInSelectedFolder.collectAsState()
    val selectedFolderName by viewModel.selectedFolderName.collectAsState()
    val currentSong by viewModel.currentMediaItem.collectAsState()
    val isShuffle by viewModel.isShuffleModeEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    // 신규 추가: 탐색기 모드 관련 상태 구독
    val explorationMode by viewModel.explorationMode.collectAsState()
    val explorerItems by viewModel.explorerCurrentItems.collectAsState()
    val explorerPathHistory by viewModel.explorerPathHistory.collectAsState()
    val explorerDirName by viewModel.explorerCurrentDirName.collectAsState()

    var isPlayerDetailOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepBlack, DarkGrey)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (explorationMode == MainViewModel.MODE_FLAT) {
                // 1. 기존 Flat 폴더 탐색 뷰
                HeaderSection(
                    title = selectedFolderName ?: "음악 라이브러리",
                    showBackButton = selectedFolderName != null,
                    onBackClicked = { viewModel.clearFolderSelection() },
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    onShuffleClicked = { viewModel.toggleShuffle() },
                    onRepeatClicked = { viewModel.toggleRepeatMode() }
                )

                // 모드 전환 탭 바
                ExplorationModeTabBar(
                    currentMode = explorationMode,
                    onModeSelected = { viewModel.setExplorationMode(it) }
                )

                if (selectedFolderName == null) {
                    FolderList(folders = folders, onFolderClicked = { viewModel.selectFolder(it) })
                } else {
                    SongList(
                        songs = selectedFolderSongs,
                        currentSong = currentSong,
                        onSongClicked = { song ->
                            viewModel.playSong(song, selectedFolderSongs)
                        }
                    )
                }
            } else {
                // 2. 신규 계층 파일 탐색기 뷰
                HeaderSection(
                    title = explorerDirName ?: "파일 탐색기",
                    showBackButton = explorerPathHistory.isNotEmpty(),
                    onBackClicked = { viewModel.navigateUpExplorer() },
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    onShuffleClicked = { viewModel.toggleShuffle() },
                    onRepeatClicked = { viewModel.toggleRepeatMode() }
                )

                // 모드 전환 탭 바
                ExplorationModeTabBar(
                    currentMode = explorationMode,
                    onModeSelected = { viewModel.setExplorationMode(it) }
                )

                ExplorerList(
                    items = explorerItems,
                    currentSong = currentSong,
                    onDirClicked = { viewModel.enterDirectory(it) },
                    onSongClicked = { song ->
                        // 탐색기 내 현재 디렉토리 내부의 곡들만 추출해 재생 큐 구성
                        val currentDirectorySongs = explorerItems.filter { it.mediaMetadata.isPlayable == true }
                        viewModel.playSong(song, currentDirectorySongs)
                    }
                )
            }
        }

        // 하단 미니 플레이어 바 (곡이 선택되었을 때만 표시)
        if (currentSong != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                MiniPlayerBar(
                    currentSong = currentSong!!,
                    isPlaying = isPlaying,
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    onPlayPauseClicked = { viewModel.playOrPause() },
                    onPreviousClicked = { viewModel.skipToPrevious() },
                    onNextClicked = { viewModel.skipToNext() },
                    onShuffleClicked = { viewModel.toggleShuffle() },
                    onRepeatClicked = { viewModel.toggleRepeatMode() },
                    onBarClicked = { isPlayerDetailOpen = true }
                )
            }
        }

        // 전체화면 플레이어 (바텀 시트 애니메이션 효과처럼 풀화면으로 오버레이)
        AnimatedVisibility(
            visible = isPlayerDetailOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            PlayerDetailScreen(
                viewModel = viewModel,
                onClose = { isPlayerDetailOpen = false }
            )
        }
    }
}

@Composable
fun HeaderSection(
    title: String,
    showBackButton: Boolean,
    onBackClicked: () -> Unit,
    isShuffle: Boolean,
    repeatMode: Int,
    onShuffleClicked: () -> Unit,
    onRepeatClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        // 타이틀 행
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
                IconButton(
                    onClick = onBackClicked,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = NeonCyan)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = title,
                color = BrightWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // 재생 모드 퀵 메뉴 행
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 재생 모드 라벨
            Text(
                text = "재생 모드:",
                color = SoftGrey,
                fontSize = 12.sp
            )

            // 셔플 버튼
            val shuffleTooltipText = if (isShuffle) "랜덤 듣기 (켜짐)" else "순차 재생 (랜덤 듣기 꺼짐)"
            PlaybackTooltip(tooltipText = shuffleTooltipText) {
                PlayModeChip(
                    label = "랜덤",
                    icon = Icons.Filled.Shuffle,
                    isActive = isShuffle,
                    onClick = onShuffleClicked
                )
            }

            // 반복 모드 버튼 (OFF → ALL → ONE 순환)
            val repeatLabel = when (repeatMode) {
                Player.REPEAT_MODE_ALL -> "전체반복"
                Player.REPEAT_MODE_ONE -> "한곡반복"
                else -> "반복없음"
            }
            val repeatIcon = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                else -> Icons.Filled.Repeat
            }
            val repeatTooltipText = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> "반복 듣기 (한 곡 반복)"
                Player.REPEAT_MODE_ALL -> "연속 듣기 (전체 반복)"
                else -> "연속 재생 (반복 없음)"
            }
            PlaybackTooltip(tooltipText = repeatTooltipText) {
                PlayModeChip(
                    label = repeatLabel,
                    icon = repeatIcon,
                    isActive = repeatMode != Player.REPEAT_MODE_OFF,
                    onClick = onRepeatClicked
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun PlayModeChip(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isActive) NeonCyanDim else DarkGrey)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) NeonCyan else SoftGrey,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            color = if (isActive) NeonCyan else SoftGrey,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun FolderList(
    folders: List<MediaItem>,
    onFolderClicked: (MediaItem) -> Unit
) {
    if (folders.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "음악 폴더를 찾을 수 없습니다.\n스토리지 권한 또는 노래 파일을 확인해 주세요.",
                color = SoftGrey,
                fontSize = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(folders) { folder ->
                FolderCard(folder = folder, onClick = { onFolderClicked(folder) })
            }
            // 하단 미니 플레이어 패딩 보장
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun FolderCard(folder: MediaItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkGrey)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.radialGradient(colors = listOf(NeonPurple, DeepBlack))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.mediaMetadata.title.toString(),
                color = BrightWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "재생하려면 터치하세요",
                color = SoftGrey,
                fontSize = 14.sp
            )
        }

        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun SongList(
    songs: List<MediaItem>,
    currentSong: MediaItem?,
    onSongClicked: (MediaItem) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(songs) { song ->
            val isSelected = currentSong?.mediaId == song.mediaId
            SongRow(song = song, isSelected = isSelected, onClick = { onSongClicked(song) })
        }
        // 하단 미니 플레이어 패딩 보장
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
fun SongRow(
    song: MediaItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) NeonCyanDim else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = if (isSelected) NeonCyan else SoftGrey,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.mediaMetadata.title.toString(),
                color = if (isSelected) NeonCyan else BrightWhite,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.mediaMetadata.artist.toString(),
                color = SoftGrey,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MiniPlayerBar(
    currentSong: MediaItem,
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    onPlayPauseClicked: () -> Unit,
    onPreviousClicked: () -> Unit,
    onNextClicked: () -> Unit,
    onShuffleClicked: () -> Unit,
    onRepeatClicked: () -> Unit,
    onBarClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(DarkGrey)
            .clickable(onClick = onBarClicked)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // 상단 행: 곡 정보
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentSong.mediaMetadata.title.toString(),
                    color = BrightWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentSong.mediaMetadata.artist.toString(),
                    color = SoftGrey,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 하단 행: 재생 컨트롤 버튼 전체
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 셔플
            val shuffleTooltipText = if (isShuffle) "랜덤 듣기 (켜짐)" else "순차 재생 (랜덤 듣기 꺼짐)"
            PlaybackTooltip(tooltipText = shuffleTooltipText) {
                IconButton(
                    onClick = { onShuffleClicked() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "셔플",
                        tint = if (isShuffle) NeonCyan else SoftGrey,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 이전 곡
            IconButton(
                onClick = { onPreviousClicked() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "이전 곡",
                    tint = BrightWhite,
                    modifier = Modifier.size(28.dp)
                )
            }

            // 재생/일시정지
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(NeonCyan)
                    .clickable { onPlayPauseClicked() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = DeepBlack,
                    modifier = Modifier.size(24.dp)
                )
            }

            // 다음 곡
            IconButton(
                onClick = { onNextClicked() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "다음 곡",
                    tint = BrightWhite,
                    modifier = Modifier.size(28.dp)
                )
            }

            // 반복 모드
            val repeatIcon = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                else -> Icons.Filled.Repeat
            }
            val repeatTooltipText = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> "반복 듣기 (한 곡 반복)"
                Player.REPEAT_MODE_ALL -> "연속 듣기 (전체 반복)"
                else -> "연속 재생 (반복 없음)"
            }
            PlaybackTooltip(tooltipText = repeatTooltipText) {
                IconButton(
                    onClick = { onRepeatClicked() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "반복",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) NeonCyan else SoftGrey,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerDetailScreen(
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    val currentSong by viewModel.currentMediaItem.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val isShuffle by viewModel.isShuffleModeEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()

    if (currentSong == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 상단 닫기 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = BrightWhite,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // 중앙 앨범아트 카드 (Neon Glow 시각 효과)
        Box(
            modifier = Modifier
                .size(280.dp)
                .shadow(24.dp, RoundedCornerShape(24.dp), ambientColor = NeonPurple, spotColor = NeonCyan)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.sweepGradient(
                        colors = listOf(NeonPurple, NeonCyan, NeonPurple)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkGrey),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(96.dp)
                )
            }
        }

        // 곡 및 가수 이름 정보
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = currentSong!!.mediaMetadata.title.toString(),
                color = BrightWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = currentSong!!.mediaMetadata.artist.toString(),
                color = SoftGrey,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 탐색 진행률 슬라이더
        Column {
            Slider(
                value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                onValueChange = { percent ->
                    val pos = (percent * duration).toLong()
                    viewModel.seekTo(pos)
                },
                colors = SliderDefaults.colors(
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = SoftGrey.copy(alpha = 0.3f),
                    thumbColor = NeonCyan
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatTime(currentPosition), color = SoftGrey, fontSize = 12.sp)
                Text(text = formatTime(duration), color = SoftGrey, fontSize = 12.sp)
            }
        }

        // 메인 재생 제어판
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 셔플 토글 버튼
            val shuffleTooltipText = if (isShuffle) "랜덤 듣기 (켜짐)" else "순차 재생 (랜덤 듣기 꺼짐)"
            PlaybackTooltip(tooltipText = shuffleTooltipText) {
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = null,
                        tint = if (isShuffle) NeonCyan else SoftGrey,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // 이전 곡
            IconButton(onClick = { viewModel.skipToPrevious() }) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = null,
                    tint = BrightWhite,
                    modifier = Modifier.size(40.dp)
                )
            }

            // 재생 / 일시정지 (Neon Cyan Circle Button)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .background(NeonCyan)
                    .clickable { viewModel.playOrPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = DeepBlack,
                    modifier = Modifier.size(36.dp)
                )
            }

            // 다음 곡
            IconButton(onClick = { viewModel.skipToNext() }) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = null,
                    tint = BrightWhite,
                    modifier = Modifier.size(40.dp)
                )
            }

            // 반복 모드 토글 버튼
            val repeatIcon = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                else -> Icons.Filled.Repeat
            }
            val repeatTooltipText = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> "반복 듣기 (한 곡 반복)"
                Player.REPEAT_MODE_ALL -> "연속 듣기 (전체 반복)"
                else -> "연속 재생 (반복 없음)"
            }
            PlaybackTooltip(tooltipText = repeatTooltipText) {
                IconButton(onClick = { viewModel.toggleRepeatMode() }) {
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = null,
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) NeonCyan else SoftGrey,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// 시간 포맷 보조 함수 (밀리초 -> 00:00 포맷)
private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun ExplorationModeTabBar(
    currentMode: Int,
    onModeSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkGrey)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val flatActive = currentMode == MainViewModel.MODE_FLAT
        val explorerActive = currentMode == MainViewModel.MODE_EXPLORER

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (explorerActive) NeonCyanDim else Color.Transparent)
                .clickable { onModeSelected(MainViewModel.MODE_EXPLORER) }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "파일 탐색기",
                color = if (explorerActive) NeonCyan else SoftGrey,
                fontSize = 14.sp,
                fontWeight = if (explorerActive) FontWeight.Bold else FontWeight.Normal
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (flatActive) NeonCyanDim else Color.Transparent)
                .clickable { onModeSelected(MainViewModel.MODE_FLAT) }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "음악 폴더",
                color = if (flatActive) NeonCyan else SoftGrey,
                fontSize = 14.sp,
                fontWeight = if (flatActive) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun ExplorerList(
    items: List<MediaItem>,
    currentSong: MediaItem?,
    onDirClicked: (MediaItem) -> Unit,
    onSongClicked: (MediaItem) -> Unit
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "이 폴더는 비어있습니다.",
                color = SoftGrey,
                fontSize = 16.sp
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items) { item ->
                val isDir = item.mediaMetadata.isPlayable != true
                val isSelected = currentSong?.mediaId == item.mediaId

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) NeonCyanDim else DarkGrey.copy(alpha = 0.6f))
                        .clickable {
                            if (isDir) {
                                onDirClicked(item)
                            } else {
                                onSongClicked(item)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isDir) Icons.Filled.Folder else Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = if (isDir) NeonCyan else (if (isSelected) NeonCyan else SoftGrey),
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.mediaMetadata.title.toString(),
                            color = if (isSelected) NeonCyan else BrightWhite,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!isDir) {
                            Text(
                                text = item.mediaMetadata.artist.toString(),
                                color = SoftGrey,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "폴더 열기",
                                color = SoftGrey.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (isDir) {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = SoftGrey,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            // 미니 플레이어 가림 방지 패딩
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

// 마우스 호버 시 툴팁을 표시해 주는 래퍼 컴포저블
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackTooltip(
    tooltipText: String,
    content: @Composable () -> Unit
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip(
                containerColor = DarkGrey,
                contentColor = BrightWhite
            ) {
                Text(text = tooltipText)
            }
        },
        state = rememberTooltipState(),
        content = content
    )
}

