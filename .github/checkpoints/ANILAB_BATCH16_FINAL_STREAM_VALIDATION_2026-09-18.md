# AniLab Batch 16 — Final Stream Validation Boundary

Branch: backend-foundation

## Root cause

`StreamResolver.resolveWithBrowser()` previously returned typed browser-discovered URLs without passing them through `StreamValidator`. This made the browser fallback the only path where a non-playable page or stale URL could escape the shared final validation contract.

## Fix

Browser/WebView results are now treated as candidates and passed through the same bounded `StreamValidator` path as extractor results. This preserves the Android browser boundary while ensuring the final stream URL and media type are verified consistently.

## Validation boundary

No LIVE E2E result is claimed by this checkpoint. Build, unit tests, integration tests, and real-site E2E require the Gradle/JDK and network runtime.
