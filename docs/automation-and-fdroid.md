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

### Optional signed GitHub APKs

F-Droid builds and signs its own APK, so signing secrets are not required for the
F-Droid tag flow. To also attach an installable upstream APK and SHA-256 file to
each GitHub release, configure all four Actions secrets:

- `ANDROID_SIGNING_KEYSTORE_BASE64`: base64-encoded release keystore
- `ANDROID_SIGNING_KEY_ALIAS`: key alias
- `ANDROID_SIGNING_STORE_PASSWORD`: keystore password
- `ANDROID_SIGNING_KEY_PASSWORD`: key password

The workflow aligns the unsigned release APK and signs/verifies it with Android
Build Tools 34.0.0. If none of these secrets exists, it creates a source-only
GitHub release. A partial secret configuration fails closed. Keep the original
keystore and credentials backed up securely outside GitHub; losing them prevents
seamless upgrades of upstream-signed APKs. For a PKCS#12 keystore, use the same
value for the store and key password secrets; JKS keystores can use distinct
passwords.

## F-Droid auto-update configuration

Official F-Droid metadata does not live in this repository. Once Peek has been
accepted, the authoritative `metadata/org.peek.app.yml` in `fdroiddata` should
include the following build block for the current submission release:

```yaml
AntiFeatures:
  NonFreeNet:
    en-US: Connects to third-party social platforms and media CDNs
Categories:
  - Internet
License: GPL-3.0-only
SourceCode: https://github.com/originalRecipe1/peek
IssueTracker: https://github.com/originalRecipe1/peek/issues

AutoName: Peek

RepoType: git
Repo: https://github.com/originalRecipe1/peek.git

Builds:
  - versionName: 0.1.0-experiment.3
    versionCode: 3
    commit: ed1bab12da2265f3b7031975a01014ff0bfce758
    submodules: true
    sudo:
      - apt-get update
      - apt-get install -y make zip
    gradle:
      - yes
    output: app/build/outputs/apk/release/app-release-unsigned.apk
    build:
      - ./scripts/build_yt_dlp_from_source.sh
      - echo "peek.ytdlp.file=$PWD/build/yt-dlp-source/yt-dlp" >> gradle.properties
      - echo "peek.ytdlp.sha256=$(sha256sum build/yt-dlp-source/yt-dlp | awk '{print
        $1}')" >> gradle.properties

AutoUpdateMode: Version
UpdateCheckMode: Tags ^v[0-9]+\.[0-9]+\.[0-9]+([-.+][0-9A-Za-z.]+)?$
UpdateCheckData: app/build.gradle.kts|versionCode\s*=\s*(\d+)||v(.*)
CurrentVersion: 0.1.0-experiment.3
CurrentVersionCode: 3
```

This recipe was normalized and linted successfully in the current official
`fdroiddata` configuration with the current `fdroidserver` container on
2026-09-04. Its full source scan reported zero problems, and its offline build
produced the expected unsigned APK with an embedded yt-dlp hash matching the
source-built file. A second build starting with an empty Gradle cache confirmed
that the declared repositories resolve the complete dependency graph; Android
SDK 36 was mounted because the standalone image only bundled an older platform.
The public `v0.1.0-experiment.3` tag resolves to the exact commit pinned above.
Repeat these checks if the recipe changes before submission.

Store text, the app icon, and phone screenshots are maintained in the upstream
`fastlane/metadata/android/en-US` directory. F-Droid imports those assets from
the tagged app source rather than from `fdroiddata`.

F-Droid will then notice the release tags, update its build metadata, and queue a
new build. Publication is asynchronous and remains controlled by F-Droid.

The source-build script constructs yt-dlp from the pinned git submodule before
calling Gradle. The local-file Gradle path never downloads the release asset and
rejects checksum or version mismatches. The youtubedl-android runtime is resolved
from Maven Central, a trusted Maven repository; it contains the native Python and
QuickJS runtimes documented in `THIRD_PARTY_NOTICES.md`.

The repository is public, the first reviewed release is tagged, and this block
has been validated with the current `fdroidserver`. The remaining initial step is
to submit it to `fdroiddata`. Do not claim that official F-Droid publication is
active until that merge request has been accepted. GitHub Actions cannot publish
directly into the official repository; F-Droid detects tags and controls its own
build and signing queue.
