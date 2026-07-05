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

4. **DHU 자동 연결 및 실행**:
   * PC 터미널에서 아래 배치 파일을 실행하면 포트 포워딩, 헤드유닛 서버 활성화 및 DHU 실행이 자동으로 진행됩니다.
     ```powershell
     # 다중 디바이스 충돌이 방지된 연결 전용 배치 파일 실행
     .\start_dhu.bat
     ```
   * 실행되면 펠리세이드 차량의 안드로이드 오토와 동일한 화면이 PC에 뜨며, 빌드된 플레이어 앱이 목록에 표시됩니다.

### 3.2 재생 컨트롤 도움말
* **순서대로 재생**: 셔플(Shuffle) 모드를 비활성화하면, 현재 재생 중인 폴더의 곡들이 정렬 순서(파일명 또는 태그 정보 순)대로 재생됩니다.
* **랜덤 재생**: 셔플(Shuffle) 모드를 활성화하면 폴더 내의 노래들이 무작위로 섞여 재생됩니다. 스마트폰 GUI 및 안드로이드 오토 화면 내 셔플 아이콘으로 쉽게 켜고 끌 수 있습니다.
* **폴더 탐색**: 차량의 터치스크린이나 스마트폰 화면에서 폴더 목록을 누르면 하위 음악 파일만 필터링하여 플레이리스트를 구성합니다.

---

## 4. 실차 네비게이션 연결 실패 시 조치 사항

PC DHU 에뮬레이터에서는 앱이 정상적으로 나타나지만, **실제 펠리세이드 차량 네비게이션 화면에 앱 아이콘이 나타나지 않는 경우** 아래의 조치 사항을 스마트폰에서 수행해야 합니다.

### 4.1 '출처를 알 수 없는 앱 (Unknown sources)' 활성화
실차 안드로이드 오토는 구글 플레이스토어를 통하지 않고 개발용으로 수동 설치된 디버그 APK를 보안상 숨기도록 되어 있습니다.

1. 스마트폰 **[설정]** -> **[Android Auto]** 검색 및 진입.
2. 맨 아래로 스크롤하여 **[버전 (Version)]** 정보 영역을 **10번 연속 탭**합니다.
3. *"개발자 설정을 허용하시겠습니까?"* 팝업창에서 **[확인]**을 누릅니다.
4. 우측 상단 **점 3개 메뉴** -> **[개발자 설정]** 선택.
5. 아래로 스크롤하여 **[출처를 알 수 없는 앱 (Unknown sources)]** 항목을 찾아 **체크(활성화)**합니다.

### 4.2 '앱 목록 맞춤설정' 체크 확인
1. 스마트폰 **[Android Auto 설정]** 화면으로 이동합니다.
2. 일반 탭의 **[앱 목록 맞춤설정 (Customize launcher)]** 메뉴를 선택합니다.
3. 목록에서 **BsshinMusic** (또는 GravityMusic) 앱이 정상적으로 **체크(선택)**되어 있는지 확인합니다. 꺼져 있는 경우 체크해 줍니다.

### 4.3 안드로이드 오토 앱 캐시 삭제 및 기기 재시작
설정을 변경한 후 연결 정보를 강제로 동기화하기 위한 절차입니다.

1. 스마트폰 **[설정]** -> **[애플리케이션]** -> **[Android Auto]** 선택.
2. **[저장공간]** -> **[캐시 삭제]** 실행.
3. 스마트폰을 **재시작(재부팅)**합니다.
4. 차량 정품 USB 포트에 다시 케이블을 꽂아 안드로이드 오토를 재연결합니다.

### 4.4 배터리 제한 없음 설정 (백그라운드 서비스 보호)
스마트폰이 절전 기능으로 인해 백그라운드 미디어 재생 서비스(`MediaLibraryService`)를 강제 종료하지 않도록 해줍니다.

1. 스마트폰 **[설정]** -> **[애플리케이션]** -> **BsshinMusic** (또는 GravityMusic) 선택.
2. **[배터리]** 메뉴 진입.
3. **[제한 없음 (Unrestricted)]**으로 설정을 변경해 줍니다.
