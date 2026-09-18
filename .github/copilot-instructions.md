# AniLab Provider — Copilot Repository Instructions

## IDENTITAS PROJECT

Project: AniLab / KakaAnime Provider
Repository utama: `adenhernandez30-hub/Provider`
Working baseline: `backend-foundation`

Repo ini adalah Provider production source. Kerjakan perubahan dengan prinsip audit-first dan jangan menganggap compile sukses sebagai bukti provider sudah bekerja.

Peran:
- Denia = Provider / Streaming / Extractor / E2E
- Yangyang = UI/UX
- AniLab = integration / finishing / release

## ATURAN KERJA WAJIB

Urutan kerja selalu:

1. CEK REPO
2. CEK BRANCH + COMMIT TERBARU
3. CEK CHECKPOINT / HISTORY
4. AUDIT CODE + WORKFLOW + TEST
5. PETAKAN ROOT CAUSE
6. REFERENSI jika diperlukan
7. IMPLEMENTASI
8. VALIDASI
9. CHECKPOINT

Jangan langsung patch sebelum audit.
Jangan refactor besar jika fix lokal sudah cukup.
Jangan menghapus, disable, skip, atau memalsukan test agar PASS.
Jangan rewrite history atau force-push tanpa instruksi eksplisit.

## TARGET PIPELINE

Audit setiap provider melalui:

`SEARCH -> DETAIL -> EPISODE -> SOURCE -> RESOLVER -> EXTRACTOR -> STREAM`

Masalah satu tahap tidak boleh ditutupi dengan patch di tahap lain.

Buat matrix:

| Provider | Search | Detail | Episode | Source | Extractor | Stream | Tests |
|---|---|---|---|---|---|---|---|

Status:
- PASS = benar-benar tervalidasi
- FAIL = gagal
- PARTIAL = sebagian jalan
- UNKNOWN = belum diuji

BUILD PASS bukan STREAM PASS.

## AUDIT SEMUA PROVIDER

Jangan hanya memperbaiki provider yang disebut user.

Cari seluruh provider yang ada. Untuk tiap provider:
- implementasi
- interface/contract
- caller
- test
- log/error
- network flow
- parser
- resolver
- extractor

Bandingkan provider yang bekerja dengan provider yang gagal dan cari root cause sebelum memilih fix.

Jika beberapa provider rusak karena masalah shared, perbaiki akar masalah shared daripada membuat patch duplikat.

## SEARCH

Audit:
- endpoint/domain
- HTTP method
- query parameter
- headers
- response status
- HTML/JSON structure
- selector
- pagination
- parser
- normalization
- empty-result handling

Search harus menghasilkan data nyata. Jangan membuat dummy result agar test hijau.

## DETAIL

Audit:
- URL
- slug/id
- title
- poster
- synopsis
- metadata
- status
- genre
- episode information

Mapping harus konsisten dengan model dan provider contract.

## EPISODE

Audit:
- episode URL
- number/title
- ordering
- pagination
- special/OVA bila didukung
- duplicate handling

Episode harus berasal dari data provider nyata.

## SOURCE / SERVER

Audit setiap source/server:
- URL source
- response
- redirect
- required headers
- referer/origin bila memang diperlukan secara normal
- content type
- resolver chain
- extractor yang dipanggil

Jangan menganggap halaman source sebagai direct media URL.
Jangan membuat dummy, placeholder, atau temporary stream URL.

## EXTRACTOR / STREAM

Target akhir adalah stream nyata yang dapat dikonsumsi oleh contract Provider.

Audit:
- resolver chain
- redirect chain
- response body
- JSON/API payload
- media URL
- HLS/DASH/direct file
- required non-sensitive headers
- referer/origin bila diperlukan secara normal
- URL expiry
- MIME/content type
- quality
- subtitle bila tersedia
- error handling

Dilarang:
- dummy URL
- localhost URL
- placeholder URL
- mematikan extractor
- swallow exception lalu return success palsu
- mengubah FAIL menjadi PASS tanpa bukti

Jangan membypass DRM, authentication, paywall, CAPTCHA, atau access-control/security mechanism. Jika akses membutuhkan mekanisme resmi, dokumentasikan sebagai limitation.

## DOMAIN CHANGE

Jika domain berubah:
1. cari semua hardcoded domain
2. cek base URL/config
3. cek redirect
4. update titik konfigurasi yang benar
5. audit selector/parser lagi
6. jalankan test terkait

Jangan search-and-replace membabi buta.

## HTTP / NETWORKING

Pertahankan pola networking yang ada kecuali audit membuktikan perlu perubahan.

Perhatikan:
- timeout
- redirect
- User-Agent
- Accept
- Referer
- Origin
- cookies/session bila memang bagian normal flow
- content type
- encoding
- response status

Jangan commit credential, token, cookie pribadi, atau secret. Jangan log secret.

## TESTING

Bedakan secara eksplisit:
- BUILD PASS
- UNIT TEST PASS
- INTEGRATION PASS
- LIVE E2E PASS

Live E2E idealnya membuktikan:

`SEARCH -> DETAIL -> EPISODE -> SOURCE -> EXTRACT -> STREAM`

Jika search jalan tetapi stream gagal, provider bukan PASS.

Jika live site down, CAPTCHA/rate-limit muncul, atau environment bermasalah:
- dokumentasikan
- jangan palsukan PASS
- jangan mengubah production logic hanya agar test palsu hijau

## DEBUGGING

Saat test gagal:
1. baca error pertama yang relevan
2. trace stack
3. tentukan pipeline stage
4. reproduksi dengan test terkecil
5. audit implementation
6. buat fix minimal
7. test ulang
8. cek regression provider lain

Jangan menumpuk patch tanpa mengetahui penyebab.

## CODE QUALITY

Pertahankan:
- existing architecture
- provider contract
- naming convention
- coroutine/threading model
- error handling
- dependency boundaries

Hindari:
- duplicate logic
- global mutable state tanpa alasan
- blocking network call di context yang salah
- swallowing exceptions
- giant utility class
- unnecessary abstraction/dependency

## GIT & CHECKPOINT

Setiap batch harus mudah direview.

Contoh commit:
- `fix(provider): repair search parser`
- `fix(provider): repair episode extraction`
- `fix(extractor): resolve stream source`
- `test(provider): add live regression coverage`

Sebelum checkpoint:
- cek diff
- cek affected files
- cek tests
- cek build
- cek git status

Jangan menyatakan checkpoint hijau sebelum validasi relevan benar-benar sukses.

## OUTPUT SETIAP BATCH

### AUDIT
- provider diperiksa
- stage diperiksa
- root cause

### IMPLEMENTASI
- file diubah
- perubahan utama
- alasan

### VALIDASI
- compile: PASS/FAIL
- unit: PASS/FAIL/NOT RUN
- integration: PASS/FAIL/NOT RUN
- live E2E: PASS/FAIL/NOT RUN

### STATUS PROVIDER

| Provider | Search | Detail | Episode | Source | Extractor | Stream | Overall |
|---|---|---|---|---|---|---|---|

### CHECKPOINT
- commit SHA jika dibuat
- perubahan belum tervalidasi
- blocker

## PRIORITAS

1. shared infrastructure yang rusak
2. search gagal
3. detail/episode gagal
4. source/resolver gagal
5. extractor/stream gagal
6. regression tests
7. cleanup/refactor kecil

## ATURAN PALING PENTING

**AUDIT DULU. ROOT CAUSE DULU. FIX MINIMAL. TEST NYATA. JANGAN PALSUIN PASS.**

Target akhir bukan sekadar `./gradlew build` berhasil.

Target akhir:

`SEARCH -> DETAIL -> EPISODE -> SOURCE -> EXTRACTOR -> STREAM`

dengan data nyata dan hasil yang benar-benar sesuai contract Provider.

Jika suatu stage belum terbukti, gunakan UNKNOWN, FAIL, atau PARTIAL sesuai kondisi sebenarnya.
