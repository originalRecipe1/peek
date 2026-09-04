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

TikTok remains an upstream-sensitive limitation. yt-dlp has an open TikTok
site/impersonation regression reported on 2026-09-01, including failures on a
newer nightly than Peek currently bundles. Its documented mobile-API fallback
requires a genuine app install ID and is not an appropriate anonymous default
for Peek. Re-test after an upstream fix is released; do not paper over this by
shipping a third-party proxy or fabricated device credentials.

- [Current yt-dlp TikTok regression](https://github.com/yt-dlp/yt-dlp/issues/17604)
- [yt-dlp TikTok extractor arguments](https://github.com/yt-dlp/yt-dlp#tiktok)

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

- Universal two-ABI debug APK: 49,067,428 bytes (46.8 MiB).
- Universal two-ABI unsigned release APK: 44,245,288 bytes (42.2 MiB).
- Estimated compressed payload with only arm64-v8a: approximately 28.3 MiB.
- Extracted app-private runtime on the arm64 device: approximately 50 MiB.

These are whole-app prototype sizes, not an exact dependency delta. A controlled
build without the extractor and ABI-specific release artifacts should be added
before making a final packaging decision.
