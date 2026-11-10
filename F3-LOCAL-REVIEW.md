# Android F3 + 손가락 업무 제스처 — 로컬 검토 기록

2026-11-10. 기반 APK 41db538d는 David가 탭 S9에 설치하여 페어링·직접 터치 정상 확인.
이번 변경은 아직 APK로 빌드하거나 실행하지 않았다. commit/push/CI/릴리스도 실행하지 않았다.

## 구현 범위

- F3-a/b/c: 기본 켜짐, 비포커스 48dp 바. 손잡이 이동·접힘·비율 좌표 저장, 창/키보드 크기에 따라 화면 안으로 재배치.
- 편집·탐색·수정키·한글·시스템·클립보드 그룹 및 불투명도 30–100% 설정, 영어/한국어 안내.
- 키보드, 복사/붙여넣기/잘라내기/실행취소/다시실행/전체선택/저장, Esc/Tab/Enter/Delete/F2/방향키 반복,
  한/영·한자, Alt+Tab(마지막 누름 2초 후 Alt 해제), Win, 우클릭.
- Ctrl/Shift/Alt: 1회 적용/잠금/해제. 상태는 바에 표시하며 접을 때 바의 잠금을 해제한다.
  실제 키 Down/Up을 전송하고 Game.modifierFlags에는 합치지 않는다.
  물리 키는 장치별·좌우 VK별 소유권을 별도로 추적한다. 기존 preIME 처리의 반환값을 유지하여 이중 전달 경로를 만들지 않았다.
- 1회용 수정키는 실제 키 Up/마우스 Up 뒤 해제한다. 기존 짧은 탭의 100ms 지연 Up도 이 지점을 사용한다.
  드래그는 손을 떼는 즉시 Up을 보내며 지연하지 않는다. 모두 해제는 보조 수단이지 필수 조작이 아니다.
- 클립보드는 버튼을 누를 때만 읽는다. 최대 2000 코드 포인트, 유효 BMP만 허용, NUL/비BMP/고립 surrogate/불필요 제어문자는 전체 요청 거절.
  글자 경계에서 32바이트 이하로 나누고 10ms 간격. Enter/Tab은 명시적 키로 보내며 둘 다 확인 대화상자를 거친다.
  진행/취소 버튼 제공. 새 화면 터치·펜·마우스·키 입력, 포커스 상실·종료는 미전송 부분을 취소한다. 이미 보낸 글자는 되돌리지 않는다.

## 추가 승인된 제스처

`checkbox_touch_work_gestures`는 기본 켜짐이고 바와 독립적으로 끌 수 있다. 직접 터치/트랙패드 양쪽에 적용한다.

| 손가락 동작 | 전송 |
|---|---|
| 두 손가락이 같은 세로 방향으로 움직임 | 고해상도 휠, 소수 단위 누적. 손가락 하나라도 떼면 중단 |
| 한 손가락 고정 + 다른 손가락 이동 | 왼쪽 버튼 Down + 이동. 어느 손가락이든 떼면 즉시 Up |
| 한 손가락 고정 + 다른 손가락 두 번 탭 | 우클릭 정확히 1회. 왼쪽 클릭 없음. 남은 고정 손가락은 모두 뗄 때까지 무시 |
| 세 손가락 이동 | 휠(가운데) 버튼 Down + 중심점 이동. 첫 손가락을 떼면 즉시 Up |
| 짧고 정지된 세 손가락 탭 | 기존 키보드 열기. 가운데 클릭은 보내지 않음 |

처음 한 손가락은 이동/두 번째 접촉/Up까지 보류하므로 스크롤 전에 왼쪽 클릭이 새지 않는다.
한 손가락을 오래 고정해도 650ms에 자동 우클릭하지 않는다. 직접 터치와 트랙패드 모두 동일하다.
따라서 **업무 제스처 ON에서는 기존 한 손가락 오래 누르기 우클릭/트랙패드 드래그를 대신**하며,
고정 손가락+이동/두 번 탭을 사용한다. OFF에서는 원래 650ms 경로·기존 세 손가락 동작을 보존한다.
이미 한 손가락 이동이 확정된 뒤 다른 손가락을 추가하면 기존 단일 동작을 안전하게 종료하고 새 후보로 전환한다.
펜/지우개는 새 제스처 판정에 넣지 않는다. 네이티브 펜 전송·압력·호버 구현은 변경하지 않는다.

판정값: 단일 손가락 이동 8dp, 고정 손가락 허용 6dp, 드래그 시작 12dp,
두 손가락 스크롤은 각각 세로 8dp 이상/동일 방향, 고정 드래그 판정은 두 번째 접촉 후 80ms 대기.
모호한 움직임 28dp 초과는 전체 접촉이 끝날 때까지 무시. 확정 후 scroll↔drag로 바뀌지 않는다.
우클릭 탭 각각 220ms 이내, 탭 사이 300ms 이내/24dp 이내, 고정 손가락 이동 6dp 이내.
세 손가락 중간 드래그는 중심점 12dp, 키보드 탭은 300ms 이내/각 손가락 8dp 이내.
휠은 48dp 이동당 120단위이며 소수 잔량을 보존한다. 앱 자체가 부드러운 휠을 지원하는지는 실기 확인이 필요하다.

## 정리와 OFF 경로

- 포커스/캡처 상실, PiP, 정지 전에 제스처와 터치 예약을 취소하고 원격 Down을 해제한다.
  포커스 경계에서는 물리 Up 유실에 대비하여 추적한 물리 Down도 해제하고 비운다.
- 바 단축키 종료만으로 물리 키를 해제하지 않는다. 제스처 전환은 터치 소유만 종료하여 고정 수정키를 유지한다.
- `grabbedInput=false` 또는 연결/포커스 비활성 상태에서 새 전송을 막는다.
- 상위 `checkbox_touch_enhancements` OFF 또는 바와 업무 제스처 둘 다 OFF면 새 컨트롤러를 만들지 않는다.
  원래 터치 수학·타이밍·preIME·펜 경로가 남는다. 바만 OFF이면 별도로 켠 업무 제스처는 계속 적용된다.
- F1/F2 드래그 잠금·포인터 가속·가장자리 자동 이동과 F4 IME 조합은 구현하지 않았다.

## 검증과 남은 확인

순수 Java JUnit 파일: ShortcutInputTest, TouchMouseStateTest, ClipboardTextTest, ClipboardTyperTest,
FingerGesturePolicyTest. CI 검증 스크립트는 각 파일의 모든 @Test 메서드가 실제 실행·통과했는지 확인하도록 갱신했다.
로컬 javac/JUnit/Gradle/앱 실행은 하지 않았다. 소스·XML·워크플로 정적 검토만 수행한다.

다음 승인된 CI에서 컴파일·lint·JUnit을 실행해야 한다. 그 후 탭 S9에서 바 이동/스크롤/접힘/IME 겹침,
손가락 제스처·PDF·엑셀 드래그·중간 버튼, 지연 탭 빠른 재클릭, Shift/Ctrl 선택,
물리 좌우 수정키 동시 사용/포커스 재진입, S펜, PiP, 화면 회전/분할창을 확인해야 한다.
ERP 유니코드·한/영/한자 반응, Android 키보드의 한글 실시간 조합은 이번 변경으로 검증되지 않았다.
연결이 이미 끊어진 뒤에는 Up 도착을 보장할 수 없으며 호스트 세션 종료 정리에 의존한다.

정적 확인 결과: 변경 XML 3개 파싱/중복 이름 검사, 새 문자열 참조 검사,
워크플로 YAML 및 포함 Python 문법 검사, 새 Java 파일 괄호 검사, git diff --check 통과.
이는 Java 컴파일·타입 검사나 Android 실행 결과가 아니다. 새 JUnit 메서드는 5개 클래스에 총 36개이며 미실행이다.

## 변경 파일

아래 경로는 모두 이 Android 저장소 기준이다.

- `app/src/main/java/com/limelight/Game.java`
- `app/src/main/java/com/limelight/binding/input/`: 새 `ShortcutInput.java`, `ShortcutController.java`,
  `TouchMouseState.java`, `ClipboardText.java`, `ClipboardTyper.java`, `FingerGesturePolicy.java`, `FingerGestureController.java`
- 같은 경로의 `touch/AbsoluteTouchContext.java`, `touch/RelativeTouchContext.java`, `touch/TouchContext.java`
- `app/src/main/java/com/limelight/ui/ShortcutBar.java`(신규), `StreamView.java`
- `app/src/main/java/com/limelight/preferences/PreferenceConfiguration.java`, `StreamSettings.java`
- `app/src/main/res/xml/preferences.xml`, `values/strings.xml`, `values-ko/strings.xml`
- `app/src/test/java/com/limelight/binding/input/`: 새 `ShortcutInputTest.java`, `TouchMouseStateTest.java`,
  `ClipboardTextTest.java`, `ClipboardTyperTest.java`, `FingerGesturePolicyTest.java`
- `.github/workflows/touch-android.yml`, `.github/scripts/verify_touch_build.py`
- `README.md`, `TOUCH-ANDROID.md`, 이 기록 `F3-LOCAL-REVIEW.md`

루트 인수인계 문서와 Sunshine/moonlight-qt는 수정하지 않았다.
