# Parallel Refactor Status

## Baseline

- API test command: `cd api && .\gradlew.bat test`
- Web check command: `cd web && npm run check`
- API baseline result: failed before refactor. `compileJava` reports unnamed variable `_` is a Java preview feature and the Gradle build does not pass `--enable-preview`.
- Web baseline result: failed before refactor. `eslint` was not found because `node_modules` was not installed in the fresh worktree.

## Active Lanes

| Lane | Owner | Branch/Worktree | Files Owned | Status | Notes |
| --- | --- | --- | --- | --- | --- |
| A | unassigned | `.worktrees/parallel-system-refactor` | `api/src/main/java/telegram/files/HttpVerticle.java`, `api/src/main/java/telegram/files/http/**` | waiting | Backend routes |
| B | unassigned | `.worktrees/parallel-system-refactor` | `api/src/main/java/telegram/files/repository/impl/FileRepositoryImpl.java`, `api/src/main/java/telegram/files/repository/query/**` | waiting | File query |
| C | unassigned | `.worktrees/parallel-system-refactor` | `api/src/main/java/telegram/files/TelegramVerticle.java`, `api/src/main/java/telegram/files/telegram/**` | blocked | Starts after Lane A/B |
| D | unassigned | `.worktrees/parallel-system-refactor` | `web/src/hooks/use-files.ts`, `web/src/hooks/files/**` | waiting | File data flow |
| E | unassigned | `.worktrees/parallel-system-refactor` | large frontend components | waiting | Component split |
| F | unassigned | `.worktrees/parallel-system-refactor` | API contract files | blocked | Starts after Lane A/B/D |
| G | coordinator | `.worktrees/parallel-system-refactor` | tests and docs | in_progress | Quality gate |

## Shared Change Requests

| Time | Requester | File | Reason | Decision |
| --- | --- | --- | --- | --- |
