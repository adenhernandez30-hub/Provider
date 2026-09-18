# AniLab Batch 15 — Site Stream Boundary Hardening

Branch: backend-foundation

## Root causes fixed

- SiteSpecificStreamResolver previously returned media URLs discovered from embed pages without always sending those URLs through the shared resolver/validator. This could expose unverified source URLs on Otakudesu/Samehadaku fallback paths.
- AJAX iframe parsing did not consistently consider `data-src`, `embed`, and origin headers.
- AnimeDao player discovery was limited to wrapper IDs and a small set of tags; current code now merges page-level media nodes, data attributes, and inline direct-media candidates while preserving the exact episode URL.

## Validation boundary

Build, unit tests, and live E2E must still be run in an environment with the Gradle/JDK toolchain and network access. No STREAM PASS is claimed by this checkpoint.
