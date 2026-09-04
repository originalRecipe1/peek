# Extraction and streaming experiment results

These are manual compatibility observations, not deterministic tests. URLs and services can change independently of the app.

## Environment

- Date: 2026-09-03
- Device: Pixel 7, Android 17 / API 37, arm64-v8a
- Extractor integration: `youtubedl-android` 0.18.1, bundled engine only
- Player: AndroidX Media3 1.11.0 with OkHttp data sources

## YouTube

Source: Blender Foundation's public Big Buck Bunny upload (`aqz-KE-bpKQ`).

- Local extractor initialization: succeeded.
- Extraction: succeeded in approximately six seconds on a warm installation.
- Normalization: one video result, represented as Media3 source(s).
- Playback request: failed with HTTP 403 from the media CDN.
- Permanent media file: none created by Peek.

This means the initial experiment has not yet validated end-to-end YouTube playback with the bundled extractor. Likely investigation areas are the age/capabilities of the yt-dlp build, YouTube player-client selection and token requirements, and whether all extractor-requested request context is represented by HTTP headers alone. Runtime extractor updating remains intentionally disabled pending a dedicated F-Droid/security decision.

## PeerTube

Source: Blender Foundation's public Big Buck Bunny video on `video.blender.org`.

- Local extractor initialization: succeeded.
- Extraction: succeeded in approximately 2.8 seconds on a warm installation.
- Playback: succeeded; Media3 initialized hardware video and audio decoders and rendered the stream with audio.
- Seeking/native controls: present in Media3's `PlayerView`.
- Permanent media file: none created by Peek. App-private files contained only the extraction runtime, its preference file, and profile-installer metadata.

This validates the experiment's core local-extract-to-stream path for one public social-media source. It does not establish compatibility with the five target platforms.

## Size observations

- Universal two-ABI debug APK: 49,067,428 bytes (46.8 MiB).
- Universal two-ABI unsigned release APK: 44,245,288 bytes (42.2 MiB).
- Estimated compressed payload with only arm64-v8a: approximately 28.3 MiB.
- Extracted app-private runtime on the arm64 device: approximately 50 MiB.

These are whole-app prototype sizes, not an exact dependency delta. A controlled build without the extractor and ABI-specific release artifacts should be added before making a final packaging decision.
