# AniLab Provider Monitoring Boundary — 2026-09-17

## Status
🟡 Checkpoint / audit boundary. Build validation after the latest duplicate-engine cleanup is still pending.

## Repository
- Repo: `adenhernandez30-hub/Provider`
- Working branch: `backend-foundation`
- Focus: standalone Provider/backend architecture

## What was audited
- Provider registry/factory and JVM module structure.
- Provider orchestration now has health/cooldown behavior in `provider-core`'s `SmartProviderRouter`.
- Router tracks failures per provider + operation and temporarily excludes repeatedly failing providers.
- Stream/episode/anime requests use first-success racing among eligible providers.
- Search remains fan-out aggregation.
- `ProviderEngine` delegates to the router and normalizes/deduplicates streams.

## Important cleanup
There were duplicate `ProviderEngine` and `SmartProviderRouter` implementations in the `provider` module while the same orchestration classes also existed in `provider-core`.

The duplicate copies in `provider` were removed so orchestration has one source of truth in `provider-core`; `provider` remains the adapter/provider implementation module and backend continues to depend on it.

## Validation state
- Earlier provider-core + backend JVM build was green before this latest duplicate cleanup.
- The latest cleanup has **not** been rebuilt yet.
- Therefore do **not** mark this checkpoint green until `./gradlew :provider-core:build :backend:build` succeeds after the cleanup.

## Next action
Open Codespaces on `backend-foundation` and run the build validation. If green, continue wiring/validating provider monitoring and backend health visibility.

## Rules retained
- Audit before implementation.
- No unnecessary large refactor.
- Codespaces only for build/test/runtime validation; keep closed otherwise.
- Do not claim 🟢 without actual successful validation.
