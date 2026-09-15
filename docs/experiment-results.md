# Extraction and streaming experiment results

These are manual compatibility observations, not deterministic tests. URLs,
extractors, CDNs, and platform access policies can change independently of the
app.

## Current environment

- Date: 2026-09-04
- Device: Android emulator, Android 16 / API 36, x86_64
- Extractor integration: `youtubedl-android` 0.18.1 with bundled yt-dlp
  `2026.08.19`
- Player: AndroidX Media3 1.11.0 with OkHttp data sources

## Target-platform retest

| Platform | Public test case | Extraction | Native playback | Observation |
| --- | --- | --- | --- | --- |
| YouTube | Blender Foundation's Big Buck Bunny upload (`aqz-KE-bpKQ`) | Pass | Pass | Media3 initialized video and audio decoders; screenshots taken three seconds apart contained different rendered frames. |
| Reddit | yt-dlp's public `r/videos` video fixture (`6rrwyj`) | Pass | Pass | Media3 initialized video and audio decoders and displayed the expected post metadata. |
| X / Twitter | yt-dlp's public `x.com/historyinmemes` fixture (`1790637656616943991`) | Pass | Pass | The direct video played to completion with native controls. An older Star Wars fixture was rejected because its linked Amplify asset no longer exists; that is a dead fixture rather than an X extraction failure. |
| Instagram | Public Reel `Dc0QpRNB-C2` supplied during testing | Pass | Pass | The Reel rendered and played without cookies. This does not imply that login-gated, private, age-restricted, or region-restricted posts will work anonymously. |
| TikTok | yt-dlp's public `patroxofficial` fixture (`6742501081818877190`) | Pass | Fail | Metadata and one progressive video were produced, but the freshly signed TikTok CDN URL returned HTTP 403. The same URL failed immediately on the host with every yt-dlp-provided header, so this is not caused by Peek dropping request headers. |

No permanent user-visible media file was created in any of these tests.

## TikTok follow-up — 2026-09-12

The September 4 TikTok failure above is superseded by this investigation. The
app exported `http_headers` but omitted yt-dlp's separate `cookies` field.
yt-dlp deliberately removes Cookie from the header map and exports scoped
cookies separately. Replaying only its HTTP headers therefore did not reproduce
yt-dlp's complete playback request.

With public video `7659097456294006029` (the supplied `vm.tiktok.com` share link):

| Experiment | Result |
| --- | --- |
| Default extraction | Metadata and 11 video formats returned. |
| Every exported video URL with only HTTP headers | HTTP 403; the separate music track loaded. |
| Firefox / empty user-agent workarounds on upstream fixtures | Video URLs still returned 403. |
| Chrome 146 impersonation with curl_cffi 0.16.0 | Did not fix video requests without cookies. |
| Normal HTTP client, same URL, extraction-session cookies | HTTP 206 with MP4 bytes. |
| Same request with cookies removed | HTTP 403. |
| Python-serialized quotes sent literally in tt_chain_token | HTTP 403; decoding the value restored HTTP 206. |

The app now exports and parses scoped cookies, decodes Python's quoted values,
and provides a separate in-memory cookie jar to each video/audio playback
source. Domain, path, HTTPS-only flags, and expiry are checked on every request,
including redirects and manifest subrequests. Cookies are not copied into
History, saved to disk by the app, or mixed between separate media sources.
No extractor upgrade, browser-impersonation dependency, imported login cookies,
or proxy service was needed for this fix.

Native playback of the supplied share link was confirmed on the Pixel 7 and
API 36 emulator. The original `6742501081818877190` fixture also played on
the emulator. Confirmation used the history event emitted only after the
player enters its playing state. All 44 unit tests and 17 UI tests passed;
Android lint and the source-built release APK passed.

The upstream issue is still relevant to failures while fetching TikTok pages,
but it was not sufficient evidence that this app's playback failure was solely
upstream. This fix does not promise access to private or restricted posts.

- [yt-dlp issue #17604](https://github.com/yt-dlp/yt-dlp/issues/17604)
- [yt-dlp's scoped cookie export](https://github.com/yt-dlp/yt-dlp/blob/2026.08.19/yt_dlp/YoutubeDL.py#L2688)

## Earlier prototype observation

On 2026-09-03, a Pixel 7 running Android 17 / API 37 extracted the same YouTube
test video but received HTTP 403 from the media CDN during playback. That result
was from the earlier bundled extractor and is superseded by the successful
2026-09-04 retest above. It demonstrates why the extractor version must remain
visible and independently updatable through reviewed app releases.

The same physical-device run successfully extracted and played the Blender
Foundation's Big Buck Bunny video from `video.blender.org` through PeerTube.
Media3 initialized hardware video and audio decoders, seeking worked, and Peek
created no permanent media file.

## Size observations

### Python runtime trimming — 2026-09-15

The build now removes `usr/lib/quickjs/libquickjs.a` and seven CPython test
extension modules from the dependency's nested runtime ZIPs. In the ARM64 APK,
this removes 6,800,030 bytes of expanded runtime files: 41,680,927 bytes becomes
34,880,897 bytes. The release APK decreased from 20,079,981 to 18,282,669 bytes.
The app's QuickJS executable, Python interpreter, required modules, and yt-dlp
engine remain bundled.

All retained runtime file contents, compressed sizes, and Unix modes were
compared against the original Maven AAR for every bundled ABI. Archive unit
tests cover retained compressed bytes, permissions, symlinks, timestamps,
repeatability, and rejection of an unexpected upstream layout. The existing
installer's bytecode confirms that an archive size change replaces the old
Python directory, without touching the history database.

Release builds, release unit tests, lint, and the x86_64 CI APK build passed.
After reconnecting the Pixel 7, a fresh, separately identified release build
with shrinking and the trimmed ARM64 runtime successfully extracted YouTube
fixture `aqz-KE-bpKQ`, initialized video and audio decoders, and recorded the
history event emitted when playback starts. Two targeted instrumented tests
also passed: starting the pinned engine with `--version` without network
access, and simulating an old archive-size marker to verify runtime replacement
removes an obsolete build file, preserves a database entry, and leaves the
engine runnable. These automated checks used the debug test variant; the live
playback check used the optimized release. Temporary test packages were removed
and the user's installed app was left unchanged. Other sites were not retested.

The x86_64 debug variant was also tested on the Android 16 / API 36
`unfurlit-review` emulator. All 19 instrumented tests passed, including both
Python runtime checks. A fresh test installation extracted the same YouTube
fixture, initialized AV1 video and Opus audio decoders, and displayed its visit
in history, confirming the player entered its playing state. The test APK
contained only x86_64 native libraries. This complements the optimized ARM64
release check above; emulator debug tests do not exercise R8. The emulator's
SwiftShader renderer crashed before app launch on this host; starting with
`-gpu host -feature -Vulkan -no-snapshot` booted successfully and supported the
full test run. The temporary app was removed after testing.

### Release optimization — 2026-09-14

With the same ARM64 runtime and pinned yt-dlp engine, enabling R8 and optimized
resource shrinking reduced the unsigned release APK from 29,972,943 to
20,079,981 bytes (33.0%). Uncompressed DEX code fell from 30,492,864 to
3,995,044 bytes. The final APK contains only ARM64 native libraries, and the
bundled yt-dlp SHA-256 still matches the pinned value. This does not shrink
the Python runtime unpacked into app-private storage.

A fresh install of a separately identified, debug-key-signed release build on
the Pixel 7 exposed a reflective ZIP extra-field constructor removed by R8.
The targeted keep rule in `app/proguard-rules.pro` fixed first-use Python
initialization. After clearing only the test copy's data, YouTube's
`aqz-KE-bpKQ` fixture extracted successfully, initialized video and audio
decoders, and produced the history event emitted when playback starts. The
temporary test app was removed afterward. All 44 release unit tests and release
lint passed. Other platforms were not retested for this packaging change.

### Earlier prototype

- Universal two-ABI debug APK: 49,067,428 bytes (46.8 MiB).
- Universal two-ABI unsigned release APK: 44,245,288 bytes (42.2 MiB).
- Estimated compressed payload with only arm64-v8a: approximately 28.3 MiB.
- Extracted app-private runtime on the arm64 device: approximately 50 MiB.

These are whole-app prototype sizes, not an exact dependency delta. A controlled
build without the extractor and ABI-specific release artifacts should be added
before making a final packaging decision.
