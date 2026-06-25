package com.antigravity.audioplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 네온 스타일 다크 테마 컬러 정의
val DeepBlack = Color(0xFF0D0E15)
val DarkGrey = Color(0xFF161824)
val NeonPurple = Color(0xFFBD00FF)
val NeonCyan = Color(0xFF00F0FF)
val NeonCyanDim = Color(0x3300F0FF)
val BrightWhite = Color(0xFFF5F6FA)
val SoftGrey = Color(0xFF8E92A8)

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = NeonPurple,
    background = DeepBlack,
    surface = DarkGrey,
    onPrimary = DeepBlack,
    onSecondary = BrightWhite,
    onBackground = BrightWhite,
    onSurface = BrightWhite
)

// 라이트 모드도 유사하게 다크 스펙트럼의 밝은 네온 조합으로 정의하여 세련됨 유지
private val LightColorScheme = lightColorScheme(
    primary = NeonPurple,
    secondary = NeonCyan,
    background = Color(0xFFFAF9FC),
    surface = Color(0xFFF0EDF5),
    onPrimary = BrightWhite,
    onSecondary = DeepBlack,
    onBackground = DeepBlack,
    onSurface = DeepBlack
)

@Composable
fun GravityMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
