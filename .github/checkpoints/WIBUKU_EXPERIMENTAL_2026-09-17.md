# Wibuku Experimental Checkpoint — 2026-09-17

## Status
- 🟢 Wibuku API client boundary added.
- 🟢 Wibuku provider adapter added as an isolated experimental provider.
- 🟢 Episode ID resolver boundary is injectable.
- 🟢 Wibuku provider unit tests pass locally with `./gradlew :provider:testDebugUnitTest`.
- 🟡 Wibuku live API remains AUTH-blocked; no credential bypass implemented.
- 🟡 Wibuku is not registered in `ProviderFactory`.

## Scope
The experimental branch keeps Wibuku isolated from the production provider registry. Catalog/episode ID resolution and stream-link resolution remain injectable because the currently verified API surface only exposes episode metadata/stream sources.

## Validation
Local Codespaces unit-test task completed successfully after making `ANDROID_HOME=/home/codespace/Android/sdk` persistent in `~/.bashrc`.

## Next
Audit/validate the Wibuku test seam and, only if needed, extend the isolated adapter. Do not promote Wibuku to the production registry until live API authentication and stream resolution are independently verified.
