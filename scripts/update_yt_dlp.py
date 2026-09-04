#!/usr/bin/env python3
"""Update Peek's pinned yt-dlp release and advance the app release version."""

from __future__ import annotations

import argparse
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
VERSION_FILE = ROOT / "gradle" / "libs.versions.toml"
BUILD_FILE = ROOT / "app" / "build.gradle.kts"
README_FILE = ROOT / "README.md"
YT_DLP_VERSION = re.compile(r"^20\d{2}\.\d{2}\.\d{2}(?:\.\d+)?$")
SHA256 = re.compile(r"^[0-9a-f]{64}$")


def replace_once(path: Path, pattern: str, replacement: str) -> None:
    original = path.read_text(encoding="utf-8")
    updated, count = re.subn(pattern, replacement, original, count=1, flags=re.MULTILINE)
    if count != 1:
        raise RuntimeError(f"Expected exactly one match in {path.relative_to(ROOT)}")
    path.write_text(updated, encoding="utf-8")


def version_key(version: str) -> tuple[int, int, int, int]:
    parts = [int(part) for part in version.split(".")]
    return parts[0], parts[1], parts[2], parts[3] if len(parts) == 4 else 0


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("version", help="Official yt-dlp release tag")
    parser.add_argument("sha256", help="SHA-256 of the official yt-dlp zipimport asset")
    args = parser.parse_args()

    new_version = args.version.strip()
    new_hash = args.sha256.strip().lower()
    if not YT_DLP_VERSION.fullmatch(new_version):
        raise SystemExit(f"Refusing unexpected yt-dlp version: {new_version!r}")
    if not SHA256.fullmatch(new_hash):
        raise SystemExit("Refusing invalid SHA-256")

    versions = VERSION_FILE.read_text(encoding="utf-8")
    current_match = re.search(r'^ytDlpEngine = "([^"]+)"$', versions, re.MULTILINE)
    if current_match is None:
        raise RuntimeError("Could not find ytDlpEngine in the version catalog")
    current_version = current_match.group(1)

    build = BUILD_FILE.read_text(encoding="utf-8")
    hash_match = re.search(r'^val ytDlpReleaseSha256 = "([0-9a-f]{64})"$', build, re.MULTILINE)
    if hash_match is None:
        raise RuntimeError("Could not find ytDlpReleaseSha256 in app/build.gradle.kts")
    current_hash = hash_match.group(1)

    if new_version == current_version:
        if new_hash != current_hash:
            raise SystemExit("The immutable release tag now has a different checksum; refusing it")
        print(f"yt-dlp {current_version} is already pinned")
        return
    if version_key(new_version) <= version_key(current_version):
        raise SystemExit(f"Refusing yt-dlp downgrade from {current_version} to {new_version}")

    code_match = re.search(r"^\s*versionCode = (\d+)$", build, re.MULTILINE)
    name_match = re.search(r'^\s*versionName = "([^"]+)"$', build, re.MULTILINE)
    if code_match is None or name_match is None:
        raise RuntimeError("Could not find literal Android app version fields")
    current_code = int(code_match.group(1))
    current_name = name_match.group(1)
    expected_suffix = f".{current_code}"
    if not current_name.endswith(expected_suffix):
        raise RuntimeError(
            f"Expected versionName {current_name!r} to end with versionCode {current_code}",
        )
    next_code = current_code + 1
    next_name = current_name.removesuffix(expected_suffix) + f".{next_code}"

    replace_once(
        VERSION_FILE,
        r'^ytDlpEngine = "[^"]+"$',
        f'ytDlpEngine = "{new_version}"',
    )
    replace_once(
        BUILD_FILE,
        r'^val ytDlpReleaseSha256 = "[0-9a-f]{64}"$',
        f'val ytDlpReleaseSha256 = "{new_hash}"',
    )
    replace_once(BUILD_FILE, r"^(\s*)versionCode = \d+$", rf"\g<1>versionCode = {next_code}")
    replace_once(
        BUILD_FILE,
        r'^(\s*)versionName = "[^"]+"$',
        rf'\g<1>versionName = "{next_name}"',
    )

    readme = README_FILE.read_text(encoding="utf-8")
    if current_version not in readme:
        raise RuntimeError("README does not mention the currently pinned yt-dlp version")
    README_FILE.write_text(readme.replace(current_version, new_version), encoding="utf-8")

    print(
        f"Updated yt-dlp {current_version} -> {new_version}; "
        f"app {current_name} ({current_code}) -> {next_name} ({next_code})",
    )


if __name__ == "__main__":
    main()
