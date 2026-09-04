#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd -- "$script_dir/.." && pwd)"
source_dir="$repo_root/third_party/yt-dlp"
build_dir="$repo_root/build/yt-dlp-source"
output_file="$build_dir/yt-dlp"

if [[ ! -f "$source_dir/yt_dlp/version.py" ]] || ! git -C "$source_dir" rev-parse --verify HEAD >/dev/null 2>&1; then
  echo "Initialize the pinned yt-dlp source with: git submodule update --init" >&2
  exit 1
fi

pinned_version="$(sed -nE 's/^ytDlpEngine = "([^"]+)"$/\1/p' "$repo_root/gradle/libs.versions.toml")"
source_version="$(sed -nE "s/^__version__ = '([^']+)'$/\1/p" "$source_dir/yt_dlp/version.py")"
if [[ -z "$pinned_version" ]] || [[ "$source_version" != "$pinned_version" ]]; then
  echo "Pinned yt-dlp version $pinned_version does not match submodule version $source_version" >&2
  exit 1
fi

case "$build_dir" in
  "$repo_root"/build/yt-dlp-source) ;;
  *) echo "Refusing unsafe build directory: $build_dir" >&2; exit 1 ;;
esac
rm -rf -- "$build_dir"
mkdir -p "$build_dir"
git -C "$source_dir" archive --format=tar HEAD | tar -xf - -C "$build_dir"
make -C "$build_dir" lazy-extractors yt-dlp

actual_version="$("$output_file" --ignore-config --version)"
if [[ "$actual_version" != "$pinned_version" ]]; then
  echo "Built yt-dlp version $actual_version does not match $pinned_version" >&2
  exit 1
fi

printf 'Built yt-dlp %s from source\n' "$actual_version"
sha256sum "$output_file"
