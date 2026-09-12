# Unfurlit identity

Unfurlit is the selected name. The mark depicts a folded panel opening into a
media window, with a play-shaped cutout. It is original vector artwork.

## Color and geometry

The app continues to use `dynamicLightColorScheme` / `dynamicDarkColorScheme`
on Android 12+ and follows the system's light/dark setting. UI elements use
Material color roles; the wordmark's symbol is tinted with `primary`. The
existing fallback palette remains available for older Android versions.

The launcher provides separate adaptive foreground and monochrome vectors.
Its two-panel silhouette and negative-space cutout survive monochrome theming.
The artwork is scaled into the adaptive icon's central safe region. The blue
store/non-themed launcher palette is a fallback, not an override of the user's
personalized in-app or themed launcher colors.

The source store artwork is [`unfurlit-icon.svg`](unfurlit-icon.svg). Export it
without raster redrawing:

```sh
inkscape docs/design/unfurlit-icon.svg --export-type=png \
  --export-filename=fastlane/metadata/android/en-US/images/icon.png \
  --export-width=512 --export-height=512
```

Keep the Android foreground, monochrome, legacy launcher, and in-app vectors
consistent with that source when changing the silhouette.

## Compatibility

The Android application ID, internal package names, private history storage,
extractor build properties, signing identity, and GitHub repository address
remain stable. Existing media features and navigation controls are retained. Home also supports
animated Home/History paging and predictive Back. See
[`history-navigation.md`](history-navigation.md) for the interaction design. Version
code 6 makes the rebrand an upgrade to the existing v5 release.

## Name research

On September 12, 2026, exact-name general web searches and searches scoped to
Google Play, Apple's App Store, F-Droid, GitLab, and Codeberg found no app named
Unfurlit. GitHub's repository-name search returned zero results. These are
preliminary discovery results and do not establish exclusive rights or prove
the absence of unindexed uses.

The earlier [`rebrand-concepts.html`](rebrand-concepts.html) remains an
exploration board, not a specification of app behavior or a release screenshot.

## Validation on September 12, 2026

- 39 unit tests and 13 UI instrumentation tests passed; UI tests ran on an
  Android 16 / API 36 emulator. They cover full navigation, short/diagonal
  swipes, bidirectional paging, movement before release, retained typed input,
  button navigation, predictive Back cancellation/completion, and both edges.
- Debug APK, test APK, Android lint, and the offline source-extractor release
  build passed. Lint reports dependency-update notices; the monochrome icon
  warning has been resolved.
- The release APK reports `Unfurlit`, version code 6, application ID
  `org.peek.app`. Its embedded extractor matches the locally source-built hash.
- Light and dark screens were inspected, and changing the emulator's
  personalized system palette changed the app's accent colors.
- A live YouTube Big Buck Bunny link played and created a history entry.
  The four store screenshots show the actual app: light home, playback,
  history, and dark home. The existing film attribution is retained.
- The prepared F-Droid recipe passed `fdroid lint` and `fdroid rewritemeta`
  using official F-Droid category/anti-feature definitions. Full F-Droid source
  scanning and signed-binary reproducibility verification await the new release.

The rebrand is prepared for review. No v6 release has been published and the
live F-Droid submission still references v5.
