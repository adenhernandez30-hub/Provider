# AniLab Batch 13 — Native and Gateway Stream Boundary

Branch: backend-foundation

## Root causes fixed

- NativeHtmlProvider stopped at the first non-empty episode selector, which could discard partial episode lists, and passed the catalog root as the resolver referer instead of the exact episode page.
- RemoteSourceProviderV2 now routes gateway candidates through StreamResolver; live verification is still required to confirm every gateway schema and host.
- Kuramanime now preserves canonical episode URLs and broadens player discovery.

## Validation boundary

This checkpoint records code changes only. Build, unit tests, and real-site E2E must be run in an environment with the Gradle/JDK toolchain and live network access. No provider is marked STREAM PASS from this checkpoint alone.
