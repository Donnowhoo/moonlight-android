# Moonlight 터치 — F3 및 손가락 업무 제스처

2026-11-10. 공식 b48494cb96bff23d8886c4775cc4f39a1075495d 기반, GPL-3.0.
작업 저장소는 공개 Donnowhoo/moonlight-android, 브랜치는 touch다.
기반 APK 41db538d는 탭 S9에서 설치·페어링·직접 터치 정상 확인을 받았다.
현재 로컬 변경은 F3 단축키 바와 추가 승인된 손가락 업무 제스처다. 상세 사용법·임계값·검증 한계는 [F3-LOCAL-REVIEW.md](F3-LOCAL-REVIEW.md)를 따른다.
F4 한글 실시간 조합과 F1/F2 드래그 잠금·가속은 포함하지 않는다. 로컬 변경의 빌드·실기 시험은 아직 하지 않았다.

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
JUnit4.13.2의 앱 식별 시험과 F3/제스처 순수 Java 시험을 CI에서 실행한다.
검증 스크립트는 새 테스트 다섯 클래스의 모든 시험 메서드가 실행·통과했는지 검사한다.
이 호스트에서는 JUnit·javac·Gradle·앱을 실행하지 않는다.

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

## 다음 확인

현재 로컬 수정은 commit/push/CI 실행 승인을 포함하지 않는다. 서명 키와 ID는 기반 단계 값을 유지한다.
다음 승인된 빌드가 성공한 뒤 탭에 업데이트 설치하고 F3-LOCAL-REVIEW.md의 실기 항목을 확인한다.
손가락을 떼면 드래그/스크롤은 즉시 끝나며 모두 해제 버튼을 누를 필요가 없다.
모두 해제는 보조 수단이고 짧은 클릭의 100ms 인식 시간은 드래그 해제와 별개다.
한글 실시간 조합은 다음 F4 작업에서 다룬다. 이번 앱은 Windows Moonlight/Sunshine을 변경하지 않는다.
