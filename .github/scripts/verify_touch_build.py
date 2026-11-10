"""CI-only checks for the separately installed touch APK; never reads signing keys."""
import os
import re
import sys
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path


def main():
    apk, badging_path = map(Path, sys.argv[1:])
    badging = badging_path.read_text(encoding="utf-8")
    sha = os.environ["GITHUB_SHA"][:8]
    expected_code = str(1000 + int(os.environ["GITHUB_RUN_NUMBER"]))
    package_line = next(line for line in badging.splitlines() if line.startswith("package: "))
    fields = dict(re.findall(r"(\w+)='([^']*)'", package_line))
    if (fields.get("name") != "com.donnowhoo.moonlight.touch.debug"
            or fields.get("versionName") != f"12.2-touch.{sha}"
            or fields.get("versionCode") != expected_code):
        raise SystemExit("APK identity/version does not match this source build")
    with zipfile.ZipFile(apk) as archive:
        if not any(name.startswith("lib/arm64-v8a/") and name.endswith(".so")
                   for name in archive.namelist()):
            raise SystemExit("APK has no ARM64 native library")

    reports = list(Path("app/build/test-results/testNonRootDebugUnitTest").glob("TEST-*.xml"))
    if not reports:
        raise SystemExit("Unit test XML reports are missing")
    required = {
        ("com.limelight.TouchBuildIdentityTest", "touchDebugDoesNotReplaceOfficialApp"),
        ("com.limelight.TouchBuildIdentityTest", "touchVersionIdentifiesSource"),
    }
    # Require every declared F3/gesture test, not just the presence of a green report.
    suites = ("ShortcutInputTest", "ClipboardTextTest", "ClipboardTyperTest",
              "TouchMouseStateTest", "FingerGesturePolicyTest")
    for suite in suites:
        source = Path("app/src/test/java/com/limelight/binding/input") / f"{suite}.java"
        names = re.findall(r"@Test\s+public\s+void\s+(\w+)\s*\(", source.read_text(encoding="utf-8"))
        if not names:
            raise SystemExit(f"Required test suite has no test methods: {suite}")
        required.update((f"com.limelight.binding.input.{suite}", name) for name in names)
    passed = 0
    for report in reports:
        for case in ET.parse(report).getroot().iter("testcase"):
            if any(case.find(tag) is not None for tag in ("failure", "error", "skipped")):
                raise SystemExit(f"Test did not pass: {case.get('classname')}.{case.get('name')}")
            name = case.get("name", "")
            print(f"PASS {case.get('classname')}.{name}")
            passed += 1
            required.discard((case.get("classname"), name))
    if required:
        raise SystemExit(f"Required tests did not execute: {sorted(required)}")
    print(f"Verified {passed} tests and APK {fields['name']} {fields['versionName']}")


if __name__ == "__main__":
    main()
