package com.antigravity.audioplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.antigravity.audioplayer.ui.MainViewModel
import com.antigravity.audioplayer.ui.screens.PlayerScreen
import com.antigravity.audioplayer.ui.theme.*

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        setContent {
            GravityMusicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 기기 오디오 및 비디오 권한 획득 처리
                    var hasPermission by remember { mutableStateOf(checkMediaPermissions()) }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissionsMap ->
                        val allGranted = permissionsMap.values.all { it }
                        hasPermission = allGranted
                        if (allGranted) {
                            viewModel.initMediaController(applicationContext)
                        }
                    }

                    LaunchedEffect(hasPermission) {
                        if (hasPermission) {
                            viewModel.initMediaController(applicationContext)
                        }
                    }

                    if (hasPermission) {
                        PlayerScreen(viewModel = viewModel)
                    } else {
                        PermissionRequiredScreen(
                            onRequestPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.READ_MEDIA_AUDIO,
                                            Manifest.permission.READ_MEDIA_VIDEO
                                        )
                                    )
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkMediaPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
            val videoGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            audioGranted && videoGranted
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }
}

@Composable
fun PermissionRequiredScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepBlack, DarkGrey)
                )
            )
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "오디오 접근 권한 필요",
            color = BrightWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "내 스마트폰에 저장된 노래 폴더를 가져오고 재생하기 위해서는 저장소 읽기 권한이 필요합니다.",
            color = SoftGrey,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonCyan,
                contentColor = DeepBlack
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(text = "권한 허용하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
