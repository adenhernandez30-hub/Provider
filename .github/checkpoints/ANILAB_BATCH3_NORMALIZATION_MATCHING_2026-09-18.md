# AniLab Batch 3 — Episode Normalization + Confidence Matching — 2026-09-18

## Status
🟢 BATCH 3 VALIDATED

## Repository
- Repo: adenhernandez30-hub/Provider
- Branch: backend-foundation
- Validated commit: 9471def6e9fb238b2a4c691428e7da24e762f00c
- Validation: `./gradlew :provider-core:test`
- Result: BUILD SUCCESSFUL

## Implemented
- AnimeMatchScorer for canonical-to-provider title matching.
- Alternative-title and provider alias matching.
- Year and season confidence signals.
- ProviderEpisodeNormalizer for positive episode filtering, season filtering, duplicate collapse, metadata merge, and deterministic ordering.
- Unit tests covering confidence and episode normalization.

## Design boundary
- Scoring is a matching signal, not provider routing priority.
- Provider-specific IDs remain unchanged.
- Existing SeasonIdentityParser remains intact.
- ProviderPlaybackResolver integration is deferred until the next integration step to avoid an unnecessary refactor before validation.

## Next
Batch 4 — bounded parallel provider availability + failure isolation.
