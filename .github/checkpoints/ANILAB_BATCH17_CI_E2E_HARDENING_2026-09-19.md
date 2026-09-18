# AniLab Batch 17 — CI/E2E Hardening
Date: 2026-09-19
Branch: backend-foundation
Status: 🟡 IMPLEMENTED / AWAITING REAL-SITE E2E

## Audit basis
- Batch 10/11 checkpoints record the target-site failures that still require live verification: Otakudesu STREAM ERROR, Samehadaku EPISODES EMPTY, Kuramanime SEARCH EMPTY, AnimeDao STREAM EMPTY; Animeisme previously reached STREAM PASS.
- Batches 13–16 are present as code/checkpoints but explicitly unverified at runtime.
- The current E2E test only reported SEARCH/ANIME/EPISODES/STREAM and could pass when any single provider produced a stream, masking required-provider failures.
- The current E2E workflow did not pin checkout to backend-foundation and did not preserve the matrix report as an artifact.

## Implemented
- E2E matrix now reports SEARCH, DETAIL, EPISODES, and final STREAM per provider.
- Required stream providers default to Otakudesu, Samehadaku, Kuramanime, AnimeDao, and Animeisme; this can be overridden with ANILAB_E2E_REQUIRED_PROVIDERS.
- The test writes build/reports/provider-e2e-matrix.md and fails when a required provider does not reach a usable typed HTTP(S) stream.
- The workflow explicitly checks out backend-foundation and uploads the matrix report as a 7-day artifact even when the test fails.

## Validation boundary
No BUILD, UNIT, INTEGRATION, or LIVE E2E success is claimed by this checkpoint. The implementation must be validated by GitHub Actions or Codespaces with live network access.

## Next
1. Run Provider Validation on backend-foundation.
2. Run Provider Real-Site E2E on backend-foundation.
3. Inspect the matrix artifact and fix only the providers/stages that actually fail.
4. Update this checkpoint to 🟢 only after the real validation succeeds.