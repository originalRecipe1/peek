# Peek

Peek is an experimental, FOSS-first Android media viewer for social-media links. This repository currently implements the core local streaming path:

```text
paste / Share / Open With -> local yt-dlp extraction -> structured domain model
                           -> Media3 / Coil -> streamed native viewer
```

The app opens to a URL input screen and does not start extraction until the user submits, shares, or opens a link. It does not download a permanent media file, use an account, contact a project-operated backend, or dynamically update executable code.

## What this experiment validates

- yt-dlp/Python can be packaged and initialized on Android.
- yt-dlp output can be normalized without exposing yt-dlp types to the UI.
- public YouTube, Reddit, X, Instagram, and PeerTube pages have been extracted
  locally and streamed to Media3 without creating a permanent media file.
- destroying the viewer cancels its ViewModel work and terminates the active yt-dlp process; playback is released with the UI lifecycle.
- playback that actually starts is recorded in a private, on-device viewing history.
- images are shown with pinch-to-zoom and pan, audio uses native playback controls, and
  multi-entry results are presented as a swipeable gallery.
- video frames automatically follow the decoded media aspect ratio, so portrait
  media such as Reels uses the available screen height instead of a 16:9 box.
- video can enter an immersive fullscreen mode that survives rotation; Back exits
  fullscreen before leaving the viewer.
- shared text and supported web intents are reduced to a validated public HTTP/HTTPS URL before extraction; HTTP inputs are upgraded to HTTPS before network access.

The playback layer also carries per-format HTTP headers, combines separate video/audio URLs with `MergingMediaSource`, and maps progressive, HLS, and DASH source types. Image requests receive the extractor-provided headers as well. Those cases, plus image, audio, and mixed-gallery normalization, have unit coverage at the extraction boundary. Current live results and the upstream TikTok CDN limitation are recorded in [`docs/experiment-results.md`](docs/experiment-results.md).

This is not the final UI. Quality selection, cookies, and saving remain intentionally deferred until the core viewer has been exercised on devices. Whether a particular image or gallery works still depends on the media entries exposed by that site's current yt-dlp extractor.

## Build

Requirements:

- JDK 21 for the Gradle runtime (the app still targets Java 17 bytecode)
- Android SDK 36
- a 64-bit ARM device or x86_64 emulator running Android 7.0+
- Git, Python 3, Make, and Zip only when building the extractor from source

The repository pins the Gradle daemon to Java 21 in
`gradle/gradle-daemon-jvm.properties`. Gradle 8.14.5 cannot run on Java 25.
In Android Studio, leave **Gradle JDK** set to **GRADLE_LOCAL_JAVA_HOME** and
make sure the resolved JDK is version 21. For command-line builds, install a
discoverable JDK 21 or set `JAVA_HOME` to one before invoking the wrapper.

Build and run tests:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

With an emulator or device selected through `ANDROID_SERIAL`, run the deterministic
Compose tests with `./gradlew connectedDebugAndroidTest`. CI runs the same tests
on an AOSP API 30 Gradle-managed device. Live extraction tests remain manual so
platform rate limits and datacenter blocking cannot make pull requests flaky.

For Android Studio, select the shared **Peek** run configuration, choose one or
more connected devices from the target-device selector, and press **Run**. The
configuration launches the default activity and does not clear app data.

The normal build downloads the official yt-dlp `2026.08.19` zipimport executable
and verifies its pinned SHA-256 before packaging it as an app resource. The app
then uses that bundled copy through youtubedl-android; it does not fetch or update
executable code at runtime.

For an offline/F-Droid-style source build, initialize the pinned submodule and
build the extractor first:

```bash
git submodule update --init
./scripts/build_yt_dlp_from_source.sh
source_file="$PWD/build/yt-dlp-source/yt-dlp"
source_sha="$(sha256sum "$source_file" | awk '{print $1}')"
./gradlew --offline --no-daemon assembleRelease \
  -Ppeek.ytdlp.file="$source_file" \
  -Ppeek.ytdlp.sha256="$source_sha"
```

This path performs no extractor download during Gradle execution. Gradle verifies
the supplied archive's checksum and embedded version before packaging it. The
source-built variant has also completed the YouTube streaming proof of concept
on the emulator.

Before the first extraction in each app process, Peek verifies the app-private
extractor copy against the bundled checksum and atomically refreshes it when it
differs. This makes APK upgrades activate their newly pinned yt-dlp version
without clearing app data or viewing history.

The first extraction can take noticeably longer while the bundled Python runtime initializes. Network behavior is limited to the submitted source platform/CDN; there is no Peek backend.

## Network safety

Peek treats submitted URLs and extractor output as untrusted. Before extraction,
it upgrades HTTP inputs to HTTPS, follows a bounded redirect chain without
reading response bodies, rejects cleartext redirects and extracted media URLs, and
rejects any hop that targets localhost, a literal private address, or a hostname
whose DNS answer contains a non-public address. The same public-only DNS and
redirect policy is shared by Media3 and Coil, including manifest and image
requests. Sensitive and hop-by-hop headers are removed when a request crosses
origins, and extractor-provided connection, forwarding, host, length, and range
headers are ignored.

Extraction is cancellable and limited to 120 seconds. yt-dlp prints only the
metadata and selected-format fields Peek consumes; short metadata is capped at
512 characters, descriptions at 16 KiB, the normalized output at 2 MiB, and
posts at 50 media entries. These controls reduce the attack surface, but they do
not turn arbitrary extraction into a sandbox: yt-dlp and the bundled Python
runtime remain security-sensitive code that must be kept current.

## Update automation

A least-privilege Android CI workflow runs unit tests, lint, and a debug APK
build for every pull request and every push to `main`.

A weekly GitHub Actions workflow checks the latest stable yt-dlp release,
verifies its published checksum, advances the pinned source submodule, tests both
release-asset and source-built packaging, and opens a versioned pull request.
Merging that reviewed pull request creates a GitHub release tag that F-Droid can
monitor. Repository setup and the proposed F-Droid build metadata are documented in
[`docs/automation-and-fdroid.md`](docs/automation-and-fdroid.md).

## Viewing history

Tap **History** on the Home or viewer screen to see prior viewing events. Opening an entry extracts
the original page again so stale stream URLs are never reused. Individual events can
be removed, and **Clear** removes the entire history after confirmation.

History is stored in the app's private SQLite database and is excluded from Android
backup and device transfer. A record contains the original page URL, basic display
metadata, media type/count, duration, and viewing time. Direct CDN URLs, request
headers, cookies, descriptions, thumbnails, and raw extractor output are not stored.

## Manual experiment

1. Install the debug APK and launch Peek.
2. Paste a public URL, or share/open one from another app.
3. Wait for extraction to complete and verify that the native media viewer appears.
4. Confirm playback/seeking for video and audio, zoom/pan for images, swiping and the item indicator for galleries, and the new history event.
5. Repeat with public test cases for YouTube, Reddit, X, Instagram, and TikTok.
6. Capture whether each result is progressive, HLS/DASH, muxed, or split audio/video.
7. Record extraction time, playback errors, and the produced APK size before expanding the UI.

Do not use private links, cookies, or credentials in committed test fixtures. Peek's own success log records only the extractor name and media count. Failures emit a length-limited diagnostic with URLs and common secret fields redacted; direct media URLs, headers, cookies, and raw yt-dlp output are never deliberately logged.

## Architecture

The project intentionally has one Gradle app module. Package boundaries keep the replaceable pieces explicit:

```text
ui -> domain repository -> MediaExtractor -> yt-dlp adapter
 |
 +-> media viewer -> Media3 + OkHttp / Coil
```

`PlaybackSource` carries a URL, request headers, stream type, MIME type, and format ID. `ExtractedMedia.Video` can hold independent video and audio sources, while posts always expose a list of video, image, or audio entries. A separate history repository persists only a safe metadata projection after media is successfully displayed or starts playing. Runtime yt-dlp updating is not called; youtubedl-android `0.18.1` provides the Android/Python integration and yt-dlp `2026.08.19` is pinned separately as the extraction engine.

## Licensing note

The application source is licensed under GPL-3.0-only, matching the yt-dlp Android
integration's copyleft license. The audited runtime dependency families and
bundled native/Python components are recorded in
[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md); release changes must keep that
inventory current.
