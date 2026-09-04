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
4. Updates the pinned engine version and checksum and increments Peek's literal
   `versionCode` and `versionName`.
5. Runs unit tests, Android lint, and an APK build, then verifies the yt-dlp file
   embedded in the APK.
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
include the following update configuration:

```yaml
AutoUpdateMode: Version
UpdateCheckMode: Tags ^v[0-9]+\.[0-9]+\.[0-9]+([-.+][0-9A-Za-z.]+)?$
UpdateCheckData: app/build.gradle.kts|versionCode\s*=\s*(\d+)||v(.*)
CurrentVersion: 0.1.0-experiment.1
CurrentVersionCode: 1
```

F-Droid will then notice the release tags, update its build metadata, and queue a
new build. Publication is asynchronous and remains controlled by F-Droid.

There is one unresolved inclusion task: the current Gradle build downloads the
pinned, checksum-verified yt-dlp zipimport asset while preparing resources.
F-Droid build workers do not permit arbitrary network access during a build.
Before submission, replace that step for the F-Droid build with a source-built
artifact from the matching yt-dlp tag—preferably through a pinned git submodule,
which the weekly updater can advance in the same pull request. Do not claim that
official F-Droid publication is active until that recipe has been accepted in
`fdroiddata`.
