# Unfurlit 1.0.0 F-Droid validation

Validated locally on September 12, 2026, against the published release.

| Item | Value |
| --- | --- |
| Application ID | `io.github.originalrecipe1.unfurlit` |
| Version / code | `1.0.0` / `7` |
| Tag | `v1.0.0` |
| Commit | `80f79dd1328775d57c19c99d6592f54e552e8440` |
| Published APK SHA-256 | `224dc5d0db6737dd7add62343fbc08bd1b112fc73b21535c12abbd04b5a942ed` |
| Signing certificate SHA-256 | `3528e91676bde711bf40c70bce363f7c76a554ae9de91f221635b6e975400a3c` |

## Results

- Metadata lint passed; `rewritemeta --list` reported no formatting changes.
- The source scan within `fdroid build` passed without scanner exceptions.
- The source build succeeded from the pinned commit, including the yt-dlp submodule.
- F-Droid successfully copied and verified the published APK's signatures against
  the rebuilt APK and accepted the allowlisted signing certificate.

The final build output included:

```text
...successfully verified
compared built binary to supplied reference binary successfully
supplied reference binary has allowed signer 3528e91676bde711bf40c70bce363f7c76a554ae9de91f221635b6e975400a3c
success: io.github.originalrecipe1.unfurlit
1 build succeeded
```

## Environment and reproduction

Used fdroidserver 2.4.5, Python 3.12.11 with standard zlib 1.3.1, JDK 21,
Android SDK 36, Gradle 8.14.5, and apksigner from Android Build Tools 36.0.0.
This was a local build with an existing Gradle dependency cache, not a run on
F-Droid's dedicated build server. The recipe's `sudo` package-install commands
were skipped locally; `make` and `zip` were already installed.

With F-Droid configured for those tools and the
[candidate](io.github.originalrecipe1.unfurlit.yml) copied into `metadata/`:

```sh
fdroid lint io.github.originalrecipe1.unfurlit
fdroid rewritemeta --list io.github.originalrecipe1.unfurlit
fdroid build --stop io.github.originalrecipe1.unfurlit:7
```

The initial local Python 3.14 environment used zlib-ng and failed signature
copying with `Unsupported compresslevel`. Repeating the complete build with
standard zlib passed without changing app source or the metadata recipe.
Use standard zlib if reproducing this signature-compression issue.

GitLab pipelines and official F-Droid review remain separate checks.

## Suggested MR update

Title: **New app: Unfurlit**

```text
The app was renamed from Peek to Unfurlit before its initial F-Droid publication.
The application ID is now io.github.originalrecipe1.unfurlit, replacing org.peek.app.
The source repository is https://github.com/originalRecipe1/unfurlit.

This update replaces the old metadata file and targets the published Unfurlit
1.0.0 release (versionCode 7), commit 80f79dd1328775d57c19c99d6592f54e552e8440.
The signing certificate is unchanged. Store text, icon, screenshots, and release
notes are included in the tagged source's Fastlane metadata.

Local fdroidserver 2.4.5 validation passed metadata lint, source scanning, the
source build, and signature verification against the published APK, including
the allowed signing-certificate check. The new GitLab pipelines still need to
pass before the combined build/pipeline checklist item is marked complete.
```
