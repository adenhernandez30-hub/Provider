# AniLab Batch 10 — Stream Audit Checkpoint
Date: 2026-09-19
Branch: backend-foundation
Status: 🟡 IMPLEMENTED / AWAITING REAL-SITE E2E

## Scope
Audited:
- Otakudesu
- Samehadaku
- Kuramanime
- AnimeDao
- Animeisme

## Findings
- Otakudesu: provider was using a duplicate site-specific resolver before the dedicated OtakudesuServerExtractor. The web source also preferred otakudesu.fit before the supplied live fixture domain otakudesu.blog.
- Samehadaku: adapter/extractor still referenced v2.samehadaku.how while the supplied real-site fixture is samehadaku.li. The SiteSpecificStreamResolver iframe regex also had an over-escaped whitespace pattern.
- Kuramanime: dedicated adapter already had the tokenized player handshake, but search depended on one gallery selector. Native Kuramadrive/Kuramadrive-v2 servers were not explicitly preferred.
- AnimeDao: episode player discovery was limited to known wrapper IDs and #videocontent, so a player refresh moving the media/embed outside that container could produce STREAM EMPTY.
- Animeisme: current NativeHtmlProvider path already reaches STREAM PASS in the supplied E2E; no change made to its working path.

## Implemented
- Align Samehadaku adapter and episode extractor with samehadaku.li.
- Correct Samehadaku iframe extraction regex.
- Prefer otakudesu.blog in the web-source ordering.
- Route Otakudesu HTTP episode playback through the dedicated OtakudesuServerExtractor first.
- Harden Kuramanime search with an anime-link fallback and prefer native Kuramadrive servers.
- Broaden AnimeDao page-level iframe/video/source discovery.

## Validation
Compilation/build and real-site E2E have NOT been re-run after these changes yet.
Do not mark this checkpoint green until the real-site E2E confirms the target stream stages.

## Next
Run:
ANILAB_REAL_E2E=true ANILAB_E2E_QUERY='One Piece' ANILAB_E2E_TIMEOUT_MS=60000 ./gradlew :provider:test --tests com.kakaanime.provider.ProviderRealSiteE2ETest --stacktrace
