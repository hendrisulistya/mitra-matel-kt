# /Users/yosemite/code/JoSoft/mitra-matel/TODO.md

Mitra Matel — Auth Lifecycle Production Readiness

High Priority

- Resume Grace Check

  - Add lifecycle observer to refresh inside grace: if `sessionManager.isInGracePeriod()` and credentials exist → silent re-login; if `sessionManager.isTokenExpired()` → clear session and navigate to `welcome`.
  - Where: `app/src/main/kotlin/app/mitra/matel/navigation/App.kt` (augment lifecycle), or `MainActivity.onResume` hook.
  - References: `app/src/main/kotlin/app/mitra/matel/navigation/App.kt:90–111`, `app/src/main/kotlin/app/mitra/matel/MainActivity.kt:47–54`
  - Acceptance: Returning from background within grace triggers refresh without waiting for a 401.

- Scheduling Deduplication

  - Track `lastScheduledGraceEpoch` and only schedule when it differs from `getGraceStartEpoch()`. Reset on `saveToken`.
  - Where: `app/src/main/kotlin/app/mitra/matel/utils/SessionManager.kt`
  - Acceptance: Exactly one scheduled job per token; subsequent `AuthState.Success` events don’t create duplicates.

- Connectivity-Aware Refresh

  - Scheduled refresh: if offline at grace start, retry with backoff (e.g., 3 tries, 5s intervals) until either success or expiry.
  - Where: scheduled block in `App.kt`.
  - References: `app/src/main/kotlin/app/mitra/matel/navigation/App.kt:90–111`, `app/src/main/kotlin/app/mitra/matel/network/NetworkDebugHelper.kt`
  - Acceptance: Avoid immediate failure when offline; attempts respect grace window.

- Immediate Grace Refresh on Resume
  - On resume: if already in grace and not yet refreshed, run silent re-login immediately (with network check).
  - Where: `MainActivity.onResume` or `App.kt` lifecycle observer.
  - References: `app/src/main/kotlin/app/mitra/matel/MainActivity.kt:47–54`
  - Acceptance: No missed refresh due to canceled scheduled jobs during background.

Medium Priority

- gRPC Alignment

  - Ensure gRPC paths mirror HTTP refresh behavior:
    - Use `sessionManager.saveToken(newToken)` so `exp`/grace caches update.
    - Optionally trigger proactive refresh before long-running gRPC calls if in grace.
  - References: `app/src/main/kotlin/app/mitra/matel/network/GrpcService.kt:486–547`
  - Acceptance: Consistent token lifecycle across HTTP and gRPC.

- Observability

  - Structured logs for token lifecycle:
    - Log `exp`, `graceStart`, scheduled time, refresh outcomes (success/fail, reason).
    - Tag: `AuthLifecycle`.
  - Where: `SessionManager.saveToken`, `App.kt` scheduling, HTTP/gRPC refresh paths.
  - Acceptance: Logs make timing and outcomes traceable without PII.

- UX Cues

  - Optional banner in Dashboard during grace if last refresh failed, offering “Re-login” action.
  - Where: `DashboardScreen` state integration.
  - References: `app/src/main/kotlin/app/mitra/matel/navigation/App.kt:225–246`
  - Acceptance: Users can recover quickly when proactive refresh fails.

- Refresh Endpoint (Backend-Dependent)
  - If backend provides a refresh endpoint, implement refresh using a refresh token instead of credentials.
  - Where: new method in `ApiService`, `AuthViewModel`.
  - Acceptance: No credential usage for token renewal; smaller payload vs re-login.

Security & Resilience

- Token Expiration Handling

  - If refresh fails and token passes expiry, clear session and route to `welcome`.
  - References: `app/src/main/kotlin/app/mitra/matel/navigation/App.kt:121–142`, `app/src/main/kotlin/app/mitra/matel/network/HttpClient.kt:132–160`
  - Acceptance: No lingering invalid sessions.

- Certificate Pinning Audit
  - Review pin management and rotation strategy for production domains.
  - References: `app/src/main/kotlin/app/mitra/matel/network/HttpClient.kt:15–41`, `app/src/main/kotlin/app/mitra/matel/network/SecurityConfig.kt:114–128`
  - Acceptance: Pins are current; fallback behavior documented.

Testing Matrix

- Scenarios
  - Login success: schedule at `graceStart`, refresh triggers as scheduled.
  - Resume within grace: immediate refresh.
  - Offline at grace: backoff retries; expiry → session cleared.
  - 401 on request: reactive refresh path still works.
  - Conflict (409/403): session cleared; navigation to `welcome`.
  - Multiple `AuthState.Success`: no duplicate schedules.
- Tools
  - Use adjustable token lifetimes on dev server and network toggling.

Code References

- Schedule on login success: `app/src/main/kotlin/app/mitra/matel/navigation/App.kt:90–111`
- Session caches and helpers: `app/src/main/kotlin/app/mitra/matel/utils/SessionManager.kt:161–199` + added grace helpers
- HTTP refresh on 401: `app/src/main/kotlin/app/mitra/matel/network/HttpClient.kt:132–160`, `180–262`
- gRPC refresh: `app/src/main/kotlin/app/mitra/matel/network/GrpcService.kt:486–547`
- Resume hook: `app/src/main/kotlin/app/mitra/matel/MainActivity.kt:47–54`
