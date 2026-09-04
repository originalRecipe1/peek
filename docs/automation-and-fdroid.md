# Release automation and F-Droid

Peek separates extractor updates from runtime behavior. The app never downloads
new executable code. Instead, GitHub Actions checks for a new stable yt-dlp
release every Monday at 04:23 UTC.

## Weekly yt-dlp updates

`.github/workflows/update-yt-dlp.yml` performs the following steps:

1. Reads the latest stable release from the official `yt-dlp/yt-dlp` repository.
2. Downloads `yt-dlp` and `SHA2-256SUMS` from that release.
3. Rejects unexpected version formats, checksum mismatches, changed immutable
   releases, and version downgrades.
4. Updates the pinned engine version and checksum, advances the yt-dlp source
   submodule to the same release, and increments Peek's literal `versionCode`
   and `versionName`.
5. Runs unit tests, Android lint, and APK builds, then verifies both the official
   release asset and the locally source-built yt-dlp file embedded in debug APKs.
6. Opens a pull request for review. It never merges the update itself.

The workflow can also be run manually from the Actions tab. For its pull-request
step to work, enable **Settings → Actions → General → Workflow permissions →
Allow GitHub Actions to create and approve pull requests**. The workflow itself
still requests only `contents: write` and `pull-requests: write`.

After an automation pull request is merged, `release-tag.yml` rebuilds the merged
commit and creates a GitHub release and `v<versionName>` tag. It can also be run
manually to tag a normal app release. A human merge is deliberately the gate
between an upstream extractor release and an app/F-Droid release.

## F-Droid auto-update configuration

Official F-Droid metadata does not live in this repository. Once Peek has been
accepted, the authoritative `metadata/org.peek.app.yml` in `fdroiddata` should
include a build block based on the following template. Replace the commit marker
with the full commit hash for the release tag during the initial submission:

```yaml
Categories:
  - Internet
License: GPL-3.0-only
AntiFeatures:
  NonFreeNet:
    en-US: Connects to third-party social platforms and media CDNs
SourceCode: https://github.com/originalRecipe1/peek
IssueTracker: https://github.com/originalRecipe1/peek/issues

RepoType: git
Repo: https://github.com/originalRecipe1/peek.git

Builds:
  - versionName: 0.1.0-experiment.1
    versionCode: 1
    commit: REPLACE_WITH_FULL_RELEASE_COMMIT
    submodules: true
    build: |-
      ./scripts/build_yt_dlp_from_source.sh
      ytdlp_file="$PWD/build/yt-dlp-source/yt-dlp"
      ytdlp_sha="$(sha256sum "$ytdlp_file" | awk '{print $1}')"
      ./gradlew --offline --no-daemon assembleRelease \
        -Ppeek.ytdlp.file="$ytdlp_file" \
        -Ppeek.ytdlp.sha256="$ytdlp_sha"
    output: app/build/outputs/apk/release/app-release-unsigned.apk

AutoUpdateMode: Version
UpdateCheckMode: Tags ^v[0-9]+\.[0-9]+\.[0-9]+([-.+][0-9A-Za-z.]+)?$
UpdateCheckData: app/build.gradle.kts|versionCode\s*=\s*(\d+)||v(.*)
CurrentVersion: 0.1.0-experiment.1
CurrentVersionCode: 1
```

F-Droid will then notice the release tags, update its build metadata, and queue a
new build. Publication is asynchronous and remains controlled by F-Droid.

The source-build script constructs yt-dlp from the pinned git submodule before
calling Gradle. The local-file Gradle path never downloads the release asset and
rejects checksum or version mismatches. The youtubedl-android runtime is resolved
from Maven Central, a trusted Maven repository; it contains the native Python and
QuickJS runtimes documented in `THIRD_PARTY_NOTICES.md`.

Before the initial submission, make the GitHub repository public, create the
first reviewed release tag, validate this block with the current `fdroidserver`,
and submit it to `fdroiddata`. Do not claim that official F-Droid publication is
active until that merge request has been accepted. GitHub Actions cannot publish
directly into the official repository; F-Droid detects tags and controls its own
build and signing queue.
