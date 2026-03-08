# Formatting

This repository intentionally does **not** enforce formatting through Gradle.

## Recommended approach
Use your IDE formatter (JetBrains/IntelliJ) as the single source of truth.

Suggested settings:
- enable `.editorconfig`
- use consistent imports ordering
- avoid large reformat-only commits mixed with logic changes

## Contribution tip
If you decide to add automated formatting later, prefer one of these opt-in patterns:
- pre-commit hook (formats only on commit)
- CI check (fails if formatting differs)

That keeps formatting consistent without unexpected changes during development.

