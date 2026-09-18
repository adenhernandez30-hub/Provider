# AniLab Batch 14 — Gateway and Direct Candidate Boundary

Branch: backend-foundation

## Changes

- Restored the BrowserStreamResolver dependency at the RemoteSourceProviderV2 boundary and routed all gateway candidates through StreamResolver and StreamValidator.
- Ensured ProviderFactory passes the browser resolver to every remote provider.
- Expanded DirectStreamFastPath to recognize MKV and WebM candidates while keeping validation authoritative.

## Validation

No build or live E2E result is recorded here. Gateway schemas, Otakudesu/Samehadaku AJAX flows, and AnimeDao player pages still require live E2E verification.
