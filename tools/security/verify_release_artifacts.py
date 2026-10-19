#!/usr/bin/env python3
"""Fail CI if a release build accidentally includes Bonds wiring/SDK collection.

Validates the *merged* AndroidManifest (not just src/release) and the
generated release BuildConfig; the source manifest alone is insufficient.
No credentials, network calls or real user information are needed.
"""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"
ROOT = Path("app/build")


def fail(reason):
    raise SystemExit("RELEASE PRIVACY CHECK FAILED: " + reason)


def manifests(variant):
    found = []
    for p in ROOT.rglob("AndroidManifest.xml"):
        parts = p.parts
        if variant in parts and (
            "merged_manifest" in parts or "merged_manifests" in parts
        ):
            found.append(p)
    if not found:
        fail(f"no merged AndroidManifest.xml found for {variant}; run Gradle first")
    return found


def metadata(application):
    return {
        item.get(ANDROID + "name"): item.get(ANDROID + "value")
        for item in application.findall("meta-data")
    }


def inspect_release():
    for path in manifests("release"):
        root = ET.parse(path).getroot()
        app = root.find("application")
        if app is None:
            fail(f"missing application in {path}")
        metas = metadata(app)
        if metas.get("firebase_analytics_collection_deactivated") != "true":
            fail(f"Firebase Analytics is not deactivated in {path}")
        if metas.get("firebase_messaging_auto_init_enabled") != "false":
            fail(f"FCM auto-init is not disabled in {path}")
        for svc in app.findall("service"):
            name = svc.get(ANDROID + "name", "")
            if name.endswith(".FcmService"):
                fail(f"unreleased partner FCM service remains in {path}: {name}")
        for data in root.iter("data"):
            scheme = data.get(ANDROID + "scheme")
            host = data.get(ANDROID + "host")
            prefix = data.get(ANDROID + "pathPrefix", "")
            if (scheme == "lumi" and host == "pair") or (
                host == "lumi.app" and prefix.startswith("/pair/")
            ):
                fail(f"Bonds pairing deep link remains in release manifest {path}")
        print(f"PASS release privacy: {path}")


def inspect_debug():
    for path in manifests("debug"):
        root = ET.parse(path).getroot()
        if not any(
            data.get(ANDROID + "scheme") == "lumi"
            and data.get(ANDROID + "host") == "pair"
            for data in root.iter("data")
        ):
            fail(f"debug lost intended Bonds pairing URI: {path}")
        print(f"PASS debug development deep links: {path}")


def inspect_build_config():
    candidates = list(ROOT.glob("generated/source/buildConfig/release/**/BuildConfig.java"))
    if not candidates:
        fail("no generated BuildConfig.java for release variant")
    for path in candidates:
        text = path.read_text(encoding="utf-8")
        if not re.search(r"\bENABLE_BONDS\s*=\s*false\s*;", text):
            fail(f"ENABLE_BONDS is not false in release BuildConfig: {path}")
        print(f"PASS release Bonds flag: {path}")


if __name__ == "__main__":
    inspect_release()
    inspect_debug()
    inspect_build_config()
    print("RELEASE PRIVACY CHECK PASSED")
