# Family Finance Platform — Agent Instructions

Monorepo for family finances: internal wallets (double-entry), budgets/allowances, expenses, household ops, MoMo sandbox payments. Not a regulated bank/e-wallet.

**Layout:** `services/` (profile, family, wallet, payment, budget, expense, household, notification, analytics + `api-gateway`), `frontend/web`, `infra/`, `contracts/`, `docs/`, `scripts/`.

**Stack:** Java 21 · Spring Boot 3.3.5 · PostgreSQL per service · Flyway · Keycloak OAuth2 Resource Server · Kafka outbox/inbox · React/TypeScript/Vite · Docker Compose · GitHub Actions · Prometheus/Grafana/OTel/Tempo.

Canonical docs: `docs/architecture/overview.md`, `docs/database/wallet-ledger-accounting.md`, `docs/events/KAFKA_CONTRACTS.md`, `docs/workflows/PAYMENT_TOPUP_SAGA.md`, `docs/implementation/FINAL_IMPLEMENTATION_REPORT.md`. Prefer verified source/config/tests when docs disagree (some overview pages still mark JWT/Kafka/MoMo as “planned” while runtime implementations exist).

## Architecture invariants

- Database-per-service: no cross-DB FKs or writing another service’s tables.
- Sync = REST; async = domain events. Wallet Service alone posts ledger and owns balance projections.
- Double-entry: every posted journal has Σ DEBIT = Σ CREDIT. Never mutate `wallet_balances` outside wallet posting; never rewrite posted journals (use reversals).
- Family-scoped authz (membership/permissions) at the owning service—not only FE or gateway.
- Identity: Keycloak JWT `sub` maps to profile UUID when provisioned; do not invent a second identity model.
- A workflow is not complete until the authoritative downstream effect succeeds (e.g. provider paid → wallet credit).

## Coding behavior

- Search existing packages before adding classes, APIs, DTOs, clients, or abstractions.
- Reuse service patterns: constructor injection, thin controllers, service-layer rules, DTO boundaries, existing exception handlers.
- No speculative refactors, drive-by cleanups, new microservices, or shared frameworks without an explicit ask.
- Preserve API/contracts unless a breaking change is required—and call it out first.
- Keep docs in sync when public API or architecture changes.
- Smallest necessary diff; remove dead code/imports you introduce; avoid duplicate utilities and one-off abstractions.

## Efficiency

- Targeted `rg`/reads/diffs for the touched service and direct deps only.
- Do not dump the repo or rerun unrelated suites; run the smallest relevant tests first.
- Reports: changed files, decisions, exact test counts/failures/skips, remaining risks—no long narration or secret values.

## Git

- Never develop on `main`. Feature branch per change; Conventional Commits; push feature branch only; PR → `main`.
- CI must PASS before merge; preserve history (no force-push, amend of published commits, destructive reset/clean).
- Do not claim PR create/merge without confirmation; do not include unrelated files.

## Security & data

- Never commit credentials, real MoMo secrets, tokens, or production config. Treat `.env` as private; never log secrets.
- Do not weaken JWT, family isolation, or internal API auth.
- No destructive DB/Docker volume cleanup without explicit permission.

## Before changing code

1. Locate the owning service and existing tests for the behavior.
2. Confirm no duplicate API/client/publisher/migration already covers the need.
3. Prefer extending current classes over new layers.
4. Run the smallest relevant Maven module or `frontend/web` lint/build.
5. If touching money movement, re-read wallet ledger docs and posting implementation.

## Tool-specific rules

- Cursor: path-scoped `.cursor/rules/*.mdc` (`alwaysApply: false`).
- Claude Code: `CLAUDE.md` imports this file; path-scoped `.claude/rules/*.md`.
- Maintenance: `docs/engineering/AI_AGENT_RULES.md`. Do not add legacy `.cursorrules` or duplicate root instruction dumps.
