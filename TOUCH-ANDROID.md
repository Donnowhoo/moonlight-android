# Moonlight 터치 — 기반 단계

2026-11-10. 공식 b48494cb96bff23d8886c4775cc4f39a1075495d 기반, GPL-3.0.
작업 저장소는 공개 Donnowhoo/moonlight-android, 브랜치는 touch다.
이번 단계는 이름·앱 ID·CI·시험 틀만 변경한다. 손가락/펜/키보드 처리 코드는 변경하지 않는다.

## 앱과 업데이트

- 앱 이름: Moonlight 터치
- 기본 nonRoot ID: com.donnowhoo.moonlight.touch
- 배포 대상: nonRootDebug, 실제 ID com.donnowhoo.moonlight.touch.debug
- namespace는 com.limelight 유지. 공식 앱과 데이터/페어링을 공유하지 않는다.
- 버전: 12.2-touch.<커밋8자리>, versionCode=1000+Actions run number.
  같은 워크플로 재실행은 같은 versionCode를 쓴다. 워크플로를 교체/번호 초기화하면 이 계산부터 검토한다.
- 고정 키와 ID를 유지해야 업데이트 설치 시 데이터가 유지된다. 새 키로 바꾸지 않는다.

## CI와 소스

GitHub-hosted ubuntu-24.04만 사용한다. 이 Sunshine 호스트에는 SDK/NDK/JDK/adb를 설치하지 않는다.
AGP9.4.0, Gradle9.7.1, JDK25, platform37/build-tools37.0.0, NDK29.0.14206865를 사용한다.
SDK 패키지 가용성 및 실제 작업명/경로는 최초 CI에서 확인해야 한다.
JUnit4.13.2의 두 앱 식별 시험으로 공식 앱 덮어쓰기 방지와 버전 표시 계약을 확인한다.
이는 터치/한글의 기능 시험이 아니다. 아직 기능 구현은 없다.

touch 브랜치 push 또는 touch를 선택한 수동 실행만 가능하다. PR/원본 저장소에서는 게시하지 않는다.
액션은 확인한 태그의 커밋 SHA로 고정했다(checkout7.0.1, setup-java6.2.0, setup-android4.0.4).
키 Secret이 없으면 Gradle 이전에 중단한다. 고정 debug 키를 CI에서만 복원하고 작업 종료 시 제거한다.
서명 키/캐시 전체를 아티팩트로 올리지 않는다.

재현하려는 별도 빌드 환경에서는 재귀 서브모듈 checkout 후 다음 작업을 사용한다(호스트 PC에서 실행하지 말 것):

```sh
bash ./gradlew --no-daemon --console=plain \
  -PtouchCommit=<source-commit-first-8> -PtouchBuildNumber=<actions-run-number> \
  testNonRootDebugUnitTest lintNonRootDebug assembleNonRootDebug
```

공개 소스로 다른 사람이 빌드할 때는 자신의 서명 키를 사용한다. David의 개인 키는 재현에 제공하지 않는다.
업데이트 호환 서명과 소스에서의 기능 재현은 별개다.
APK 경로: app/build/outputs/apk/nonRoot/debug/app-nonRoot-debug.apk (최초 CI 전 미검증).
릴리스 파일: Moonlight-Touch-<commit8>.apk 및 .sha256.
각 게시 설명에는 전체 소스 커밋과 서브모듈 해시를 기록한다. touch-latest 태그는 첫 생성 커밋을 유지하며 강제 이동하지 않는다.
현재 APK의 실제 소스는 파일명/버전/릴리스 본문 커밋으로 확인한다. 이후 APK도 커밋별 파일명으로 보관한다.

## David가 직접 할 일

1. fork의 Actions 탭에서 워크플로 허용 여부를 확인한다.
2. 프로젝트 루트 MOBILE-TOUCH-보고1-설계-2026-11-10.md의 서명 안내에 따라 직접 고정 키를 만든다.
   Codex는 키를 만들거나 읽지 않는다. 키는 저장소·OneDrive 밖에 보관한다.
3. GitHub 저장소 Settings → Secrets and variables → Actions에 DEBUG_KEYSTORE_BASE64를 직접 등록한다.
   키 내용이나 base64를 대화에 보내지 말고 등록 완료 여부만 알려준다.
4. 코드 검토 보고 후 commit/push를 별도로 승인한다. 실행 중 추가 push는 하지 않는다.
5. 빌드 성공 후 폰/탭에서 APK를 직접 설치하고 별도로 Sunshine과 페어링한다.
6. T0: 공식 앱과 나란히 설치되는지, 기존 직접 터치가 같은지 확인한다.
   T0-1: 삼성 키보드로 '한글 테스트 123'을 입력한 실제 결과와 기기를 알려준다.

다음 구현 순서: 단축키 바(F3) → 한글 입력(F4) → 트랙패드 드래그/포인터(F1/F2).
탭 S9 실기: 공식 앱에서도 마지막 조합 글자가 다음 입력 전까지 호스트에 보이지 않는다고 David가 확인했다.
F4의 목표는 호스트 입력칸에 조합 중 글자를 즉시 표시하는 것. 확정 글자만 보내는 모드만으로 완료 처리하지 않는다.
원격 글자 교체 방식은 메모장·엑셀·ERP별 삭제/확정/커서 이동 검증 후 적용한다.
이번 앱은 Windows 갤럭시북 Moonlight를 변경하지 않는다.
