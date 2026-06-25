# BsshinMusic 개발 로그

> **프로젝트**: Android Auto 음악 플레이어 앱  
> **패키지명**: `com.antigravity.audioplayer`  
> **GitHub**: https://github.com/bsshin1960/audio  
> **최종 업데이트**: 2026-06-26

---

## 📱 앱 개요

삼성 갤럭시 스마트폰과 현대 팰리세이드 차량의 **안드로이드 오토**를 연동하는 로컬 음악 플레이어 앱입니다.  
스마트폰에 저장된 음악 파일을 폴더별로 분류하여 차량 디스플레이(또는 DHU 에뮬레이터)에서 재생할 수 있습니다.

---

## 🏗️ 기술 스택

| 항목 | 내용 |
|------|------|
| 언어 | Kotlin |
| 최소 SDK | API 26 (Android 8.0) |
| 타겟 SDK | API 34 (Android 14) |
| UI 프레임워크 | Jetpack Compose |
| 미디어 | AndroidX Media3 (ExoPlayer + MediaLibraryService) |
| 아이콘 | Adaptive Icon (API 26+) |
| 빌드 | Gradle Kotlin DSL |
| JDK | JDK 17 (프로젝트 내장 `.jdk17`) |

---

## 📂 프로젝트 구조

```
app/src/main/
├── AndroidManifest.xml
├── java/com/antigravity/audioplayer/
│   ├── MainActivity.kt                    # 앱 진입점 (Compose UI 호스트)
│   ├── service/
│   │   ├── AudioPlaybackService.kt        # MediaLibraryService (Android Auto 연동 핵심)
│   │   └── MediaItemTree.kt              # 음악 라이브러리 트리 구조 관리
│   ├── ui/
│   │   ├── screens/PlayerScreen.kt       # 메인 플레이어 화면 (Compose)
│   │   ├── viewmodel/PlayerViewModel.kt  # UI 상태 관리
│   │   └── theme/                        # 앱 테마, 색상, 타이포그래피
│   └── utils/
│       └── AudioScanner.kt               # MediaStore 기반 음악 파일 스캔
└── res/
    ├── drawable/
    │   ├── ic_launcher.png               # 레거시 아이콘 (API 25 이하)
    │   ├── ic_launcher_foreground.png    # Adaptive Icon 전경 (흰 높은음자리표 + 빨간배경)
    │   └── ic_launcher_background.xml   # Adaptive Icon 배경 (#CC2020)
    ├── mipmap-anydpi-v26/
    │   └── ic_launcher.xml              # Adaptive Icon 정의 (API 26+)
    └── values/
        └── strings.xml                  # 앱 이름: BsshinMusic
```

---

## 🔑 핵심 구현 내용

### 1. Android Auto 미디어 서비스 (`AudioPlaybackService.kt`)

- `MediaLibraryService` 기반으로 Android Auto와 연동
- `CustomSessionCallback`이 안드로이드 오토의 브라우징 요청 처리:
  - `onGetLibraryRoot`: 루트 미디어 항목 반환
  - `onGetChildren`: 폴더/곡 목록 반환 (DHU 연결 시 빈 트리 자동 재초기화 포함)
  - `onAddMediaItems`: 폴더 선택 시 해당 폴더 내 전체 곡을 플레이리스트에 추가
- ExoPlayer 오디오 포커스 자동 관리 (전화 수신, 네비게이션 음성 감쇠)
- 이어폰 분리 시 자동 일시정지

### 2. 음악 파일 스캐너 (`AudioScanner.kt`)

- `MediaStore.Audio.Media` ContentProvider로 기기의 음악 파일 전체 스캔
- `IS_MUSIC != 0` 필터로 실제 음악 파일만 선별
- 파일 경로에서 폴더명을 추출하여 폴더별 분류
- `LocalSong` → `MediaItem` 변환 지원

### 3. 미디어 트리 구조 (`MediaItemTree.kt`)

```
[ROOT]
└── 음악 폴더 (카테고리)
    ├── 폴더A
    │   ├── 곡1
    │   └── 곡2
    └── 폴더B
        └── 곡3
```

### 4. 앱 아이콘 (Adaptive Icon)

- **방식**: Android 8.0+ Adaptive Icon 표준 적용
- **배경 레이어**: 순수 빨간색 (`#CC2020`) XML drawable
- **전경 레이어**: 흰색 높은음자리표 PNG (캔버스의 약 45% 크기, 충분한 여백)
- **효과**: 삼성 One UI 런처가 자동으로 squircle 마스크 적용 → 흰 테두리 없음
- **AndroidManifest**: `@mipmap/ic_launcher` 참조

---

## 🔐 필요 권한

```xml
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

> ⚠️ 앱 삭제 후 재설치 시 `READ_MEDIA_AUDIO` 권한을 수동으로 허용해야 합니다.  
> 스마트폰 설정 → 앱 → BsshinMusic → 권한 → 음악 파일 → 허용

---

## 🖥️ 개발 환경 설정

### DHU (Desktop Head Unit) 실행 방법

```powershell
# 1. 포트 포워딩
& "C:\Users\SBS\AppData\Local\Android\Sdk\platform-tools\adb.exe" forward tcp:5277 tcp:5277

# 2. DHU 실행
cd "C:\Users\SBS\AppData\Local\Android\Sdk\extras\google\auto"
.\desktop-head-unit.exe
```

### 앱 빌드 및 설치

```powershell
# 프로젝트 루트: c:\Temp\Antigrvity\audio\
$env:JAVA_HOME = "C:\Temp\Antigrvity\audio\.jdk17\jdk-17.0.11+9"
& .\gradlew clean installDebug
```

---

## 🐛 트러블슈팅 히스토리

### 문제 1: 아이콘 흰 테두리 문제
- **원인**: AI 생성 PNG의 안티앨리어싱 → 런처가 흰 컨테이너에 다시 삽입
- **해결**: Adaptive Icon으로 전환. 배경/전경 분리 → 시스템이 직접 마스크 적용

### 문제 2: DHU에서 음악 폴더를 찾을 수 없음
- **원인**: DHU가 서비스에 연결할 때 `MediaItemTree`가 빈 상태인 경우 발생
- **해결**: `onGetChildren` 호출 시 루트가 비어있으면 자동 재초기화

### 문제 3: 앱 재설치 후 음악 스캔 실패
- **원인**: 앱 삭제 시 런타임 권한(`READ_MEDIA_AUDIO`)이 초기화됨
- **해결**: 앱 실행 후 스마트폰에서 수동 권한 허용

### 문제 4: 앱 이름 "GravityMusic"으로 표시
- **원인**: `PlayerScreen.kt`와 `MediaItemTree.kt`에 하드코딩된 문자열
- **해결**: 모든 위치에서 "BsshinMusic"으로 변경

---

## 🚀 향후 개선 사항

- [ ] 앨범 아트 표시
- [ ] 재생 목록 저장
- [ ] 검색 기능
- [ ] 셔플/반복 재생 UI 개선
- [ ] 차량 내 음성 명령(Voice Action) 지원
- [ ] Release 빌드 후 Play Store 배포
