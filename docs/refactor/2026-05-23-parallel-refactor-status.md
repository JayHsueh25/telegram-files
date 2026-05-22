# Parallel Refactor Status

## Baseline

- API test command: `cd api && .\gradlew.bat test`
- Web check command: `cd web && npm run check`
- API baseline result: Lane G updated `api/build.gradle` to pass `--enable-preview` to Java compilation and the test JVM. Re-running `cd api && .\gradlew.bat test` now passes `compileJava` and `compileTestJava`, then fails in the test phase with 36 tests completed, 7 failed, 1 skipped. Remaining failures are not preview compilation errors: `AutoRecordsHolderTest` hits `ExceptionInInitializerError`, `DataVerticleMigrationTest` and `DataVerticleTest` cannot initialize `telegram.files.Config`, `FileDownloadStatusConcurrentTest` cannot initialize `telegram.files.DataVerticle`, and `MessyUtilsTest` cannot find `small_test_file.txt`.
- Web baseline result: after installing locked dependencies with `npm ci`, `cd web && npm run check` exits 0 with 3 pre-existing warnings in `debug-telegram-method.tsx`, `use-toast.ts`, and `use-websocket.tsx`.
- Integration check after merging lanes G/D/E: `cd web && npm run check` still exits 0 with the same 3 warnings. `cd api && .\gradlew.bat test` passes `compileJava` and `compileTestJava`, then fails in the test phase with the same 36 tests completed, 7 failed, 1 skipped pattern recorded above.

## Active Lanes

| Lane | Owner | Branch/Worktree | Files Owned | Status | Notes |
| --- | --- | --- | --- | --- | --- |
| A | unassigned | `.worktrees/parallel-system-refactor` | `api/src/main/java/telegram/files/HttpVerticle.java`, `api/src/main/java/telegram/files/http/**` | waiting | Backend routes |
| B | reviewed and partially merged | `.worktrees/lane-b-file-query` | `api/src/main/java/telegram/files/repository/impl/FileRepositoryImpl.java`, `api/src/main/java/telegram/files/repository/query/**` | in_progress | Task 2A query model merged via `merge: lane b file query model`; `FileQueryFilterTest` passes |
| C | unassigned | `.worktrees/parallel-system-refactor` | `api/src/main/java/telegram/files/TelegramVerticle.java`, `api/src/main/java/telegram/files/telegram/**` | blocked | Starts after Lane A/B |
| D | reviewed and merged | `.worktrees/lane-d-file-hooks` | `web/src/hooks/use-files.ts`, `web/src/hooks/files/**` | completed | Merged via `merge: lane d file query hook` |
| E | reviewed and merged | `.worktrees/lane-e-components` | large frontend components | completed | Merged via `merge: lane e automation form split` |
| F | unassigned | `.worktrees/parallel-system-refactor` | API contract files | blocked | Starts after Lane A/B/D |
| G | reviewed and merged | `.worktrees/lane-g-build-baseline` | tests and docs | completed | Merged via `merge: lane g build baseline`; API still has test runtime failures unrelated to preview compilation |

## Shared Change Requests

| Time | Requester | File | Reason | Decision |
| --- | --- | --- | --- | --- |
