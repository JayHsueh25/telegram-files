# Parallel Refactor Status

## Baseline

- API test command: `cd api && .\gradlew.bat test`
- Web check command: `cd web && npm run check`
- API baseline result: Lane G updated `api/build.gradle` to pass `--enable-preview` to Java compilation and the test JVM. Re-running `cd api && .\gradlew.bat test` now passes `compileJava` and `compileTestJava`, then fails in the test phase with 36 tests completed, 7 failed, 1 skipped. Remaining failures are not preview compilation errors: `AutoRecordsHolderTest` hits `ExceptionInInitializerError`, `DataVerticleMigrationTest` and `DataVerticleTest` cannot initialize `telegram.files.Config`, `FileDownloadStatusConcurrentTest` cannot initialize `telegram.files.DataVerticle`, and `MessyUtilsTest` cannot find `small_test_file.txt`.
- Web baseline result: after installing locked dependencies with `npm ci`, `cd web && npm run check` exits 0 with 3 pre-existing warnings in `debug-telegram-method.tsx`, `use-toast.ts`, and `use-websocket.tsx`.
- Integration check after merging lanes G/D/E: `cd web && npm run check` still exits 0 with the same 3 warnings. `cd api && .\gradlew.bat test` passes `compileJava` and `compileTestJava`, then fails in the test phase with the same 36 tests completed, 7 failed, 1 skipped pattern recorded above.
- Lane B integration check after merging the query model and SQL builder: `cd api && .\gradlew.bat test --tests telegram.files.repository.query.FileQuerySqlBuilderTest --tests telegram.files.repository.query.FileQueryFilterTest` exits 0. Gradle reports only the existing deprecation warning.
- Batch 2 integration check after merging Lane A route support, Lane D realtime status, and Lane E file filters: `cd api && .\gradlew.bat test --tests telegram.files.http.RouteSupportTest --tests telegram.files.repository.query.FileQuerySqlBuilderTest --tests telegram.files.repository.query.FileQueryFilterTest` exits 0. `cd web && npm run check` exits 0 with the same 3 pre-existing warnings in `debug-telegram-method.tsx`, `use-toast.ts`, and `use-websocket.tsx`.
- Lane A SettingsRoutes integration check after merging Task 1B: `cd api && .\gradlew.bat test --tests telegram.files.http.SettingsRoutesTest --tests telegram.files.http.RouteSupportTest` exits 0. Gradle reports only the existing deprecation and Java preview warnings.

## Active Lanes

| Lane | Owner | Branch/Worktree | Files Owned | Status | Notes |
| --- | --- | --- | --- | --- | --- |
| A | reviewed and partially merged | `.worktrees/lane-a-route-support`, `.worktrees/lane-a-settings-routes` | `api/src/main/java/telegram/files/HttpVerticle.java`, `api/src/main/java/telegram/files/http/**` | in_progress | Task 1A route support helpers and Task 1B `SettingsRoutes` merged; `RouteSupportTest` and `SettingsRoutesTest` pass |
| B | reviewed and merged | `.worktrees/lane-b-sql-builder` | `api/src/main/java/telegram/files/repository/impl/FileRepositoryImpl.java`, `api/src/main/java/telegram/files/repository/query/**` | completed | Task 2A query model and Task 2B SQL builder merged; `FileQuerySqlBuilderTest` and `FileQueryFilterTest` pass |
| C | unassigned | `.worktrees/parallel-system-refactor` | `api/src/main/java/telegram/files/TelegramVerticle.java`, `api/src/main/java/telegram/files/telegram/**` | blocked | Starts after Lane A/B |
| D | reviewed and merged | `.worktrees/lane-d-realtime-status` | `web/src/hooks/use-files.ts`, `web/src/hooks/files/**` | completed | Task 3A query key and Task 3B realtime status merged; `npm run check` passes with existing warnings |
| E | reviewed and merged | `.worktrees/lane-e-file-filters` | large frontend components | completed | Task 4A automation form and Task 4B file filters merged; `npm run check` passes with existing warnings |
| F | unassigned | `.worktrees/parallel-system-refactor` | API contract files | blocked | Starts after Lane A/B/D |
| G | reviewed and merged | `.worktrees/lane-g-build-baseline` | tests and docs | completed | Merged via `merge: lane g build baseline`; API still has test runtime failures unrelated to preview compilation |

## Shared Change Requests

| Time | Requester | File | Reason | Decision |
| --- | --- | --- | --- | --- |
