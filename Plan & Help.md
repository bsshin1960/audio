# Plan & Help - 안드로이드 오토 지원 로컬 오디오 플레이어 (GravityMusic)

본 문서는 펠리세이드 차량의 안드로이드 오토 환경에서 구동되는 로컬 오디오 재생 앱 **GravityMusic**의 개발 계획서, 프로그램 구성 및 테스트(DHU) 도움말을 포함합니다.

---

## 1. 개발 계획서 (Development Plan)

### 1.1 개요
* **목적**: 스마트폰 내부에 저장된 MP3 등 로컬 노래 파일을 폴더별로 순차 및 랜덤(셔플) 재생하며, 차량 안드로이드 오토 화면과 완벽하게 연동되는 전문가 수준의 미디어 플레이어 개발.
* **대상 기기**: 안드로이드 스마트폰 (Android 8.0 Oreo 이상, API 26+) 및 현대 펠리세이드 안드로이드 오토 헤드유닛.
* **주요 타깃 기능**:
  * 스마트폰 저장소 오디오 파일 검색 및 폴더별 구조화.
  * Jetpack Media3 (ExoPlayer + MediaSession) 기반 고품질 백그라운드 재생.
  * 안드로이드 오토 표준 미디어 인터페이스 연동 (폴더 탐색, 셔플/반복 제어, 재생기능).
  * 스마트폰용 모던 & 세련된 Jetpack Compose 기반 GUI (다크 네온 테마).
  * 오디오 포커스 관리 (네비게이션 음성 안내, 전화 수신 시 자동 제어).
  * 재생 위치 및 상태 영속화 (앱 재시작 시 마지막 재생 상태 복원).

### 1.2 마일스톤 및 일정
1. **[Phase 1] 프로젝트 초기화 및 환경 설정**: 표준 Android Gradle 프로젝트 구성, 권한 선언 및 Android Auto 메타데이터 설정.
2. **[Phase 2] 미디어 백엔드 구현 (핵심)**: Jetpack Media3 기반 `MediaLibraryService` 및 `ExoPlayer` 구축. 로컬 미디어 스캐너(폴더 트리 생성) 구현.
3. **[Phase 3] 스마트폰 GUI 개발**: Jetpack Compose를 활용한 미려한 다크 테마 폴더 탐색기 및 플레이어 화면 구축.
4. **[Phase 4] 안드로이드 오토 연동 및 최적화**: 차량 컨트롤러(셔플, 반복, 이전/다음 곡) 매핑 및 재생 동기화.
5. **[Phase 5] DHU 테스트 및 도움말 문서화**: 가상 환경에서의 디버깅 및 사용자 배포 준비.

---

## 2. 프로그램 구성 (System Architecture)

### 2.1 아키텍처 다이어그램
```mermaid
graph TD
    subgraph Mobile Phone (Android App)
        UI[Jetpack Compose GUI] <--> VM[PlayerViewModel]
        Scanner[Local Media Scanner] --> DB[Media Metadata Provider]
        VM <--> MediaController[MediaControllerCompat/Media3]
        
        subgraph Background Service
            MLS[MediaLibraryService] <--> Exo[ExoPlayer / Audio Engine]
            MLS <--> Session[MediaSession / Audio Focus Control]
        end
    end
    
    subgraph Vehicle (Android Auto)
        AA_UI[Android Auto Media UI] <-->|MediaBrowserConnection| MLS
    end
    
    DB --> MLS
    Exo -->|Audio Output| Speaker[Vehicle Speakers / Phone Audio]
```

### 2.2 폴더 및 파일 구성
```text
audio/
├── app/
│   ├── build.gradle.kts       # 앱 모듈 빌드 설정
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml # 권한 및 서비스, Android Auto 메타데이터 선언
│           ├── assets/
│           │   └── automotive_app_desc.xml # 안드로이드 오토 서비스 기능 기술
│           ├── java/com/antigravity/audioplayer/
│           │   ├── MainActivity.kt        # 스마트폰 GUI 진입점 (Compose Navigation)
│           │   ├── service/
│           │   │   ├── AudioPlaybackService.kt  # Media3 MediaLibraryService (핵심 서비스)
│           │   │   └── MediaItemTree.kt         # 폴더/파일 트리 구조 관리 및 탐색 로직
│           │   ├── ui/
│           │   │   ├── theme/                   # 다크 네온 UI 테마 설정
│           │   │   ├── components/              # 플레이어 컨트롤러 및 폴더 뷰 컴포넌트
│           │   │   └── MainViewModel.kt         # 재생 상태 및 미디어 브라우저 상태 관리
│           │   └── utils/
│           │       └── AudioScanner.kt          # MediaStore 기반 로컬 음악 파일 스캐너
│           └── res/                             # 앱 아이콘 및 리소스
└── build.gradle.kts           # 루트 빌드 설정
```

---

## 3. 도움말 및 DHU 테스트 가이드

차량에 직접 연결하기 전, PC에서 가상으로 차량 화면을 띄워 테스트하는 방법입니다.

1. **원클릭 자동 빌드 및 스마트폰 설치**:
   * 스마트폰을 USB로 PC와 연결하고 **USB 디버깅**이 켜져 있는지 확인합니다.
   * PC 터미널(PowerShell)에서 프로젝트 루트 디렉토리로 이동한 뒤 아래 명령어를 단 한 줄 실행합니다:
     ```powershell
     # 무설치 자바 다운로드 및 래퍼 생성, 빌드/설치까지 한 번에 자동 진행
     .\build_app.ps1
     ```

2. **스마트폰 개발자 옵션 활성화**:
   * 휴대폰 [설정] -> [휴대전화 정보] -> [소프트웨어 정보] -> **빌드 번호** 7번 연타하여 개발자 옵션 활성화.
   * [개발자 옵션] -> **USB 디버깅** 활성화.

3. **안드로이드 오토 개발자 모드 활성화**:
   * 스마트폰 [설정] -> [Android Auto] 검색 및 진입.
   * 아래로 스크롤하여 **버전** 정보를 10번 연속 탭하여 "개발자 설정을 허용하시겠습니까?"가 뜨면 [확인] 선택.
   * 우측 상단 점 3개 메뉴 버튼 -> [개발자 설정] 선택.
   * **헤드 단위 서버 시작 (Start Head Unit Server)** 터치하여 실행.

4. **포트 포워딩 (ADB)**:
   * PC 터미널에서 아래 명령어를 입력하여 스마트폰과 PC 간 안드로이드 오토 포트를 포워딩합니다.
     ```bash
     adb forward tcp:5277 tcp:5277
     ```

5. **DHU 실행 및 연동**:
   * Android SDK가 설치된 경로의 `extras\google\auto\` 폴더로 이동하여 `desktop-head-unit.exe`를 실행합니다:
     ```powershell
     cd "$env:LOCALAPPDATA\Android\Sdk\extras\google\auto"
     .\desktop-head-unit.exe
     ```
   * 실행되면 펠리세이드 차량의 안드로이드 오토와 동일한 화면이 PC에 뜨며, 우리가 빌드한 `GravityMusic` 앱이 실행 가능 목록에 표시됩니다.

### 3.2 재생 컨트롤 도움말
* **순서대로 재생**: 셔플(Shuffle) 모드를 비활성화하면, 현재 재생 중인 폴더의 곡들이 정렬 순서(파일명 또는 태그 정보 순)대로 재생됩니다.
* **랜덤 재생**: 셔플(Shuffle) 모드를 활성화하면 폴더 내의 노래들이 무작위로 섞여 재생됩니다. 스마트폰 GUI 및 안드로이드 오토 화면 내 셔플 아이콘으로 쉽게 켜고 끌 수 있습니다.
* **폴더 탐색**: 차량의 터치스크린이나 스마트폰 화면에서 폴더 목록을 누르면 하위 음악 파일만 필터링하여 플레이리스트를 구성합니다.
