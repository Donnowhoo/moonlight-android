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
    required = {"touchDebugDoesNotReplaceOfficialApp", "touchVersionIdentifiesSource"}
    passed = 0
    for report in reports:
        for case in ET.parse(report).getroot().iter("testcase"):
            if any(case.find(tag) is not None for tag in ("failure", "error", "skipped")):
                raise SystemExit(f"Test did not pass: {case.get('classname')}.{case.get('name')}")
            name = case.get("name", "")
            print(f"PASS {case.get('classname')}.{name}")
            passed += 1
            if case.get("classname") == "com.limelight.TouchBuildIdentityTest":
                required.discard(name)
    if required:
        raise SystemExit("Build identity tests did not execute")
    print(f"Verified {passed} tests and APK {fields['name']} {fields['versionName']}")


if __name__ == "__main__":
    main()
