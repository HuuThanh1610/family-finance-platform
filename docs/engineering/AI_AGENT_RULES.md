# AI Agent Rules — Maintenance Guide

Token-efficient instructions for **Cursor** and **Claude Code** on this repository. Rules guide agent behavior; they do **not** replace automated tests, CI, JWT/family authz, or Flyway discipline.

## Entry points

| File | Role | Load |
|------|------|------|
| `AGENTS.md` | Shared global project invariants, coding/git/security defaults | Always (both tools via import) |
| `CLAUDE.md` | Compatibility shim: `@AGENTS.md` only | Claude Code entry |
| `.cursor/rules/*.mdc` | Cursor scoped rules | When globs match (not always-on) |
| `.claude/rules/*.md` | Claude Code scoped rules | When `paths` match |

Do not create `.cursorrules` or duplicate root instruction files.

## Cursor vs Claude pairing

| Topic | Cursor | Claude | Scope |
|-------|--------|--------|-------|
| Backend Java | `.cursor/rules/backend-java.mdc` | `.claude/rules/backend-java.md` | `services/**/*.java`, `pom.xml` |
| Security | `.cursor/rules/security-auth.mdc` | `.claude/rules/security-auth.md` | SecurityConfig, JWT, FamilyAuthorization*, CurrentUserResolver, security tests |
| Wallet Ledger | `.cursor/rules/wallet-ledger.mdc` | `.claude/rules/wallet-ledger.md` | `services/wallet-service/**` |
| Messaging | `.cursor/rules/kafka-outbox-saga.mdc` | `.claude/rules/kafka-outbox-saga.md` | messaging/, Outbox/Inbox, event docs |
| Database | `.cursor/rules/database-flyway.mdc` | `.claude/rules/database-flyway.md` | Flyway SQL + service `application.yml` |
| Frontend | `.cursor/rules/frontend-web.mdc` | `.claude/rules/frontend-web.md` | `frontend/web/**` |
| Testing | `.cursor/rules/testing-quality.mdc` | `.claude/rules/testing-quality.md` | `src/test`, smoke/verify scripts |
| Infrastructure | `.cursor/rules/infrastructure-ci.mdc` | `.claude/rules/infrastructure-ci.md` | Compose, `infra/`, Actions, scripts |

Semantic requirements must stay aligned. Only frontmatter differs:

- Cursor: `description`, `globs`, `alwaysApply: false`
- Claude: `paths: [...]`

## Update workflow

1. Change the invariant once in the relevant scoped pair (both Cursor + Claude files).
2. Put cross-cutting items only in `AGENTS.md` (keep ~60–100 lines).
3. Link to canonical docs instead of pasting them (`docs/database/wallet-ledger-accounting.md`, `docs/events/KAFKA_CONTRACTS.md`, …).
4. Prefer verified source over outdated architecture prose (e.g. overview still saying JWT “planned”).
5. After architecture changes: re-check globs/`paths` still match real directories; remove dead rules.

## Avoid redundancy

- No second global always-apply rule file.
- No repeating AGENTS.md inside scoped rules.
- No generic Java/React style already enforced by compiler/linter.
- No Skills/Subagents/Hooks/MCP configs as part of the rules set.

## Audit checklist

- [ ] Each `.mdc` has `alwaysApply: false` and useful `description` + `globs`
- [ ] Each `.claude/rules/*.md` has `paths` (no unscoped global Claude rules)
- [ ] Paired files exist 1:1 with matching names
- [ ] No business source changed when only updating AI rules
- [ ] No secrets in rule text

## Doc discrepancy note (2026-11-19)

`docs/architecture/overview.md` still describes JWT/Kafka/MoMo/UI mock as partially planned; runtime code, compose full profile, and `docs/implementation/FINAL_IMPLEMENTATION_REPORT.md` show JWT Resource Server, Kafka outbox/inbox, FakePayment, and real API default (`VITE_USE_MOCK=false`). Agent rules follow the verified implementation.
