# Development

Build, test, and understand Unfurlit. For using the app, see the [user guide](usage.md).

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

For Android Studio, select the shared **Unfurlit** run configuration, choose one or
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
  -Punfurlit.ytdlp.file="$source_file" \
  -Punfurlit.ytdlp.sha256="$source_sha"
```

This path performs no extractor download during Gradle execution. Gradle verifies
the supplied archive's checksum and embedded version before packaging it. The
source-built variant has also completed the YouTube streaming proof of concept
on the emulator.

Before the first extraction in each app process, Unfurlit verifies the app-private
extractor copy against the bundled checksum and atomically refreshes it when it
differs. This makes APK upgrades activate their newly pinned yt-dlp version
without clearing app data or viewing history.

The first extraction can take noticeably longer while the bundled Python runtime initializes. Network behavior is limited to the submitted source platform/CDN; there is no Unfurlit backend.

## Tests and manual checks

The build command above runs Android unit tests and lint. Release-version tests
run separately with `python3 -m unittest discover -s scripts/tests`.

1. Install the debug APK and launch Unfurlit.
2. Paste a public URL, or share/open one from another app.
3. Wait for extraction to complete and verify that the native media viewer appears.
4. Confirm playback/seeking for video and audio, zoom/pan for images, swiping and the item indicator for galleries, and the new history event.
5. Repeat with public test cases for YouTube, Reddit, X, Instagram, and TikTok.
6. Capture whether each result is progressive, HLS/DASH, muxed, or split audio/video.
7. Record extraction time, playback errors, and the produced APK size when investigating compatibility changes.

Do not use private links, cookies, or credentials in committed test fixtures. Unfurlit's own success log records only the extractor name and media count. Failures emit a length-limited diagnostic with URLs and common secret fields redacted; direct media URLs, headers, cookies, and raw yt-dlp output are never deliberately logged.

## Architecture

The project intentionally has one Gradle app module. Package boundaries keep the replaceable pieces explicit:

```text
ui -> domain repository -> MediaExtractor -> yt-dlp adapter
 |
 +-> media viewer -> Media3 + OkHttp / Coil
```

`PlaybackSource` carries a URL, request headers, stream type, MIME type, format ID, and temporary scoped playback cookies. `ExtractedMedia.Video` can hold independent video and audio sources, while posts always expose a list of video, image, or audio entries. A separate history repository persists only a safe metadata projection after media is successfully displayed or starts playing. Runtime yt-dlp updating is not called; youtubedl-android `0.18.1` provides the Android/Python integration and yt-dlp `2026.08.19` is pinned separately as the extraction engine.

The app exports yt-dlp's scoped cookies separately from its HTTP headers and
uses an in-memory cookie jar per video/audio source. Cookie domain, path, secure
flags, and expiry are checked for each request, including redirects. History
stores display metadata and small thumbnail copies, never playback credentials.

## Network safety

Unfurlit treats submitted URLs and extractor output as untrusted. Before extraction,
it upgrades HTTP inputs to HTTPS, follows a bounded redirect chain without
reading response bodies, rejects cleartext redirects and extracted media URLs, and
rejects any hop that targets localhost, a literal private address, or a hostname
whose DNS answer contains a non-public address. The same public-only DNS and
redirect policy is shared by Media3 and Coil, including manifest and image
requests. Sensitive and hop-by-hop headers are removed when a request crosses
origins, and extractor-provided connection, forwarding, host, length, and range
headers are ignored.

Extraction is cancellable and limited to 120 seconds. yt-dlp prints only the
metadata and selected-format fields Unfurlit consumes; short metadata is capped at
512 characters, descriptions at 16 KiB, the normalized output at 2 MiB, and
posts at 50 media entries. These controls reduce the attack surface, but they do
not turn arbitrary extraction into a sandbox: yt-dlp and the bundled Python
runtime remain security-sensitive code that must be kept current.

## Release and licensing

Extractor updates are shipped through reviewed app releases. See
[release automation and F-Droid](automation-and-fdroid.md) for version numbering,
signing, source builds, and publishing.

The application is licensed under [GPL-3.0-only](../LICENSE). Keep the
[third-party notices](../THIRD_PARTY_NOTICES.md) current when changing dependencies.

[All documentation](README.md)
