# Third-party notices

This inventory covers the release runtime dependency families currently pulled
by `releaseRuntimeClasspath` and the components bundled by youtubedl-android
0.18.1. Test-only libraries are not shipped in the release APK.

| Component or family | Version used | License |
| --- | --- | --- |
| AndroidX (Activity, AppCompat, Collection, Compose, Core, Lifecycle, Media3, Profile Installer, Saved State, Startup, Tracing and related modules) | resolved by the pinned version catalog / Compose BOM | Apache-2.0 |
| Kotlin standard library, coroutines, and serialization | resolved transitively; Kotlin 2.2.10 toolchain | Apache-2.0 |
| Coil | 3.3.0 | Apache-2.0 |
| OkHttp and Okio | 4.12.0 / transitive | Apache-2.0 |
| Jackson annotations, core, and databind | 2.11.1 (transitive) | Apache-2.0 |
| Apache Commons IO | 2.5 (transitive) | Apache-2.0 |
| JetBrains annotations and JSpecify | transitive | Apache-2.0 |
| youtubedl-android | 0.18.1 | GPL-3.0 |
| yt-dlp | 2026.08.19 | Unlicense |
| CPython runtime embedded by youtubedl-android | 3.12 | Python-2.0 / PSF-2.0 |
| QuickJS runtime embedded by youtubedl-android | bundled native runtime | MIT |
| PyCryptodome embedded in the Python runtime | 3.23.0 | BSD-2-Clause and public-domain portions |
| Mutagen embedded in the Python runtime | bundled Python package | GPL-2.0-or-later |
| OpenSSL | bundled with the Python runtime | Apache-2.0 |
| zlib | bundled with the Python runtime | Zlib |
| libffi | bundled with the Python runtime | MIT |
| bzip2 | bundled with the Python runtime | bzip2-1.0.6 |
| XZ Utils / liblzma | bundled with the Python runtime | 0BSD and public-domain portions |
| SQLite | bundled with the Python runtime | Public Domain |
| ncurses | bundled with the Python runtime | MIT-like ncurses license |
| GNU Readline | bundled with the Python runtime | GPL-3.0-or-later |
| Expat | bundled with the Python runtime | MIT |
| Android C++ shared runtime and Termux Android support libraries | bundled with the Python runtime | Apache-2.0 and respective upstream licenses |

The app itself is GPL-3.0-only; see `LICENSE`. Maven coordinates and resolved
versions can be audited with:

```bash
./gradlew :app:dependencies --configuration releaseRuntimeClasspath
```

Canonical license texts and corresponding source are available from the linked
upstream projects and their source distributions:

- <https://github.com/JunkFood02/youtubedl-android>
- <https://github.com/yt-dlp/yt-dlp>
- <https://github.com/FFmpeg/FFmpeg> (not included by Peek's current dependency set)
- <https://www.python.org/downloads/source/>
- <https://bellard.org/quickjs/>
- <https://github.com/Legrandin/pycryptodome>
- <https://github.com/quodlibet/mutagen>

Before changing runtime dependencies or the Android extraction runtime, update
this file and inspect the resulting release APK again.
