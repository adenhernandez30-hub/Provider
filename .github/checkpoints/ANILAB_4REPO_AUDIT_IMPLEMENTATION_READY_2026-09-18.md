# AniLab 4-Repo Audit — Implementation Ready — 2026-09-18

## Status
🟡 AUDIT COMPLETE / IMPLEMENTATION READY

This checkpoint freezes the audit boundary before implementation.

## Repository
- Repo: adenhernandez30-hub/Provider
- Branch: backend-foundation
- HEAD: dc46aebcdfed6f0962619cb9a921cc55e77f54a9
- HEAD: test(provider): skip invalid zero episode in E2E

## Audit sources
Cross-checked against:
1. Dantotsu
2. Saikou
3. AniLili / AnimeTV reference
4. Anivexa-API reference

No implementation changes were made during the audit.

## Existing AniLab foundation — preserve
- ProviderRegistry
- AnimeProvider
- ProviderAnime / ProviderEpisode / ProviderStream
- ProviderEngine
- RaceStreamEngine
- ProviderStreamDeduplicator
- StreamNormalizer
- StreamSelector
- ExtractorRegistry
- StreamResolver
- StreamValidator
- BrowserStreamResolver + Android WebView boundary
- ProviderHealthMonitor
- Backend HTTP API
- Real-site E2E harness
- JVM/Android boundary

## Important source correction
Actual HEAD shows SmartProviderRouter is sequential for search, getAnime, getEpisodes, and getStreams. RaceStreamEngine separately handles concurrent first-usable stream racing. Older checkpoint wording must not override actual source.

## Gaps

### P0
1. CanonicalAnimeIdentity
2. ProviderMapping
3. ProviderMappingResolver
4. Mapping cache
5. Confidence-based matching

Current ProviderPlaybackResolver is title-first: title -> search -> candidates -> provider ID. No AniList mapping implementation was found in the current Provider repo.

### P1
6. Bounded parallel provider availability
7. Inflight request deduplication
8. Adaptive provider ordering using health/latency
9. Direct-stream fast path
10. Bounded stream probing / validation

### P2
11. Episode cache
12. Status-aware TTL
13. Background refresh
14. Provider-aware stream TTL

## Implementation order

### Batch 1 — NEXT
Canonical identity + provider mapping contract + resolver abstraction + unit tests.

### Batch 2
Memory cache + inflight dedup + mapping persistence abstraction.

### Batch 3
Episode/season normalization + confidence matching.

### Batch 4
Bounded parallel availability + failure isolation.

### Batch 5
Adaptive health/latency routing + direct-stream fast path.

### Batch 6
Stream probe optimization + bounded validation.

### Batch 7
Episode cache + background refresh + stream TTL.

### Batch 8
Full E2E + performance comparison + final checkpoint.

## Rules
- AUDIT -> IMPLEMENT -> VALIDATE -> CHECKPOINT.
- No 🟢 until actual build/test succeeds.
- No unnecessary large refactor.
- Do not touch UI/UX.
- Codespaces only when build/test/runtime validation requires it.
- Do not restart earlier migration/audit work.

## Resume point
When opening a new chat:
1. Read this checkpoint.
2. Verify branch and HEAD.
3. Do not restart the project.
4. Continue directly at Batch 1.
5. Batch 1 must be audited/implemented/tested before Batch 2.

## Current state
🟡 No implementation from the 4-repo audit has been committed yet.
🟡 Next work: Batch 1 — Canonical Identity + Provider Mapping foundation.
