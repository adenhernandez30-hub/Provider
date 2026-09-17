# AniLab Provider Backend

Backend foundation separated from the Android/provider library.

## Purpose

Expose the existing provider engine through a standalone HTTP layer without changing provider behavior.

Planned endpoints:

- `GET /health`
- `GET /providers`
- `GET /search`
- `GET /anime/{id}`
- `GET /anime/{id}/episodes`
- `GET /episode/{id}/streams`

Provider adapters remain the source of streaming/provider logic. The backend layer should depend on provider contracts rather than duplicating extraction logic.

## Current state

Foundation branch only. No backend runtime is wired yet. Build/test validation is required before this is considered ready.
