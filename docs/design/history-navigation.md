# Home and History navigation

The interaction follows Android's distinction between in-content paging and
system edge navigation:

- [Pager in Compose](https://developer.android.com/develop/ui/compose/layouts/pager)
  provides drag-following pages, snapping, animated programmatic navigation,
  and page-offset-driven visual effects.
- [Predictive Back progress](https://developer.android.com/develop/ui/compose/system/predictive-back-progress)
  provides gesture progress, completion, and cancellation for custom navigation.
- [Gesture navigation compatibility](https://developer.android.com/develop/ui/views/touch-and-input/gestures/gesturenav)
  describes the system's use of screen edges. This implementation does not
  request gesture-exclusion areas or replace the system Back gesture on Home.

History is the page to the left of Home. An in-content right swipe opens it;
a left swipe returns. Pager position drives a subtle scale, rounded-corner,
and shadow treatment, so the pages move during the drag. The existing History
and Back buttons animate between the same pages.

From History, predictive Back previews the return destination and follows the
invoked system edge. Cancellation restores History. Navigation state changes
after a page settles, rather than halfway through a drag or back preview.

Home remains composed to retain typed input and scroll position. The viewer
is removed when fully offscreen to release playback. Opening History from the
viewer returns to that viewer; root paging into History is disabled on the
viewer so image, gallery, and player gestures retain their existing behavior.

The transition uses the app's current Material colors and Compose animation
primitives. It adds no dependencies and uses the existing AndroidX versions.

Validation includes integration tests for both swipe directions, short and
diagonal swipes, movement before release, retained input, button navigation,
predictive Back completion/cancellation, and preview direction from both edges.
