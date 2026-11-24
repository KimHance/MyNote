---
id: network
name: Network
aliases: [네트워크, GitHub API, Contents API, 원격]
uses: [data, domain]
units:
  - {id: network, name: Network module root, aliases: [OkHttp 설정, Retrofit, NetworkModule, 타임아웃], code: [src/]}
  - {id: contents, name: GitHub contents adapter, aliases: [노트 받기, index 받기, 동기화 실패 종류, SyncFailure], code: [src/main/java/com/hancekim/mynote/core/network/api/GithubContentsApi.kt, src/main/java/com/hancekim/mynote/core/network/datasource/GithubManifestAdapter.kt, src/main/java/com/hancekim/mynote/core/network/failure/, src/test/java/com/hancekim/mynote/core/network/datasource/]}
  - {id: probe, name: Connection check, aliases: [연결 확인, 저장소 확인, 토큰 확인, probe], code: [src/main/java/com/hancekim/mynote/core/network/api/ConnectProbeApi.kt, src/main/java/com/hancekim/mynote/core/network/datasource/GithubConnectProbe.kt, src/main/java/com/hancekim/mynote/core/network/model/, src/test/java/com/hancekim/mynote/core/network/probe/]}
  - {id: token, name: Applied token, aliases: [토큰 적용, 인증 헤더, AuthInterceptor, GithubTokenHolder], code: [src/main/java/com/hancekim/mynote/core/network/config/, src/main/java/com/hancekim/mynote/core/network/interceptor/]}
  - {id: status, name: Network status, aliases: [네트워크 상태, 오프라인 판정, 와이파이], code: [src/main/java/com/hancekim/mynote/core/network/status/, src/test/java/com/hancekim/mynote/core/network/status/]}
---
## Overview
Network talks to the GitHub REST API, read-only: it implements the GitHub note-source adapter of `data` (list file and note originals), the connection check behind the connect flow, the in-memory applied token with its auth header, and a snapshot of the device's network state.
Every type it hands out belongs to `data` or `domain`; Retrofit and OkHttp types stay inside. It does not compare revisions, reconcile, parse note bodies, store tokens or show anything; it never writes to GitHub.

## Structure
- `contents` is the sync path: `data:sync` calls the adapter (bound into the adapter set by kind GitHub); requests go through the main client, which carries `token`'s auth header.
- `probe` is the connect path: domain connect use cases call it with an explicit candidate token on a separate client without the auth header.
- `token` is filled by the app's token sync from the stored token and read by the auth header on every request; its applied stream tells the notes screen when to sync again.
- `status` answers "offline?" for the connect check before any request.
- `network` builds both clients, the Retrofit instances and the Hilt bindings, injecting the shared `Json` from `data`.
```mermaid
flowchart LR
  sync[data sync] --> contents --> main[main client]
  domain[connect use cases] --> probe --> bare[probe client]
  domain --> status
  appsync[app token sync] --> token
  main --> token
```
