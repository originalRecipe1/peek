# Privacy

Peek has no project-operated backend, analytics, advertising SDK, telemetry, or
mandatory account. URL extraction runs on the device.

Opening media still sends requests from the device to the submitted social
platform, its redirect targets, CDNs, and other hosts required by that platform.
Those services can observe the device's IP address, request metadata, and any
cookies the operating environment supplies. Peek does not make this access
anonymous.

Viewing history is opt-in through use of the viewer and is stored only in Peek's
private on-device SQLite database. It contains the original page URL, basic
display metadata, media type/count, duration, and viewing time. It excludes
direct CDN URLs, request headers, cookies, descriptions, thumbnails, and raw
extractor output. History is excluded from Android backup and device transfer
and can be deleted per item or cleared in full.

Peek reads the clipboard only after the user presses the Paste button. Release
logging redacts URLs and common secret fields and does not deliberately log
cookies, authorization headers, direct media URLs, or raw extractor output.

If optional cookie import is added later, this document and the in-app disclosure
must be updated before release.
