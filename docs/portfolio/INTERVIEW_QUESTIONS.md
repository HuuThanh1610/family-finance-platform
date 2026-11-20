# Interview questions (project-grounded)

Model answers refer to **this repository**. Distinguish implemented vs planned. Follow-ups are deeper probes an interviewer might ask.

---

## Java / Spring Boot

**1. How is dependency injection used in Wallet posting?**  
Constructor injection into `LedgerPostingServiceImpl` / application services; Spring `@Service` + `@Transactional` on posting methods.  
*Follow-up:* Why constructor over field injection?

**2. Where is the transactional boundary for a top-up credit?**  
Wallet consume `PaymentProviderPaid` → post ledger + update balances + insert outbox in one TX (`LedgerPostingServiceImpl`). Payment marks COMPLETED on a later `WalletCreditPosted` consume.

**3. How does JPA locking protect concurrent transfers?**  
Pessimistic locks on balance/account rows during posting so two expenses cannot both pass insufficient-funds checks against the same liability.

**4. How are business errors returned?**  
Service-layer exceptions + global handlers (e.g. wallet `BusinessException` / HTTP status mapping patterns per service).

**5. How is request validation applied?**  
Jakarta Validation on records (`@NotNull`, `@Positive` on `CreatePaymentRequest`, `CreateExpenseRequest`, etc.).

**6. How do configuration profiles affect security/Kafka?**  
`application.yml` defaults `SECURITY_ENABLED`/`KAFKA_ENABLED` false for IDE unit tests; Compose `full` sets them true. Same codebase, different runtime.

---

## Microservices

**7. Why Database-per-Service here?**  
Wallet ledger must not share tables with notifications; Flyway ownership per service; no cross-DB FKs (`AGENTS.md`).

**8. When REST vs Kafka?**  
REST for user commands and sync authz; Kafka for outcomes that other services must react to asynchronously (provider paid, ledger posted, inventory low).

**9. How are service boundaries drawn?**  
Payment owns orders; Wallet owns money; Expense owns expense lifecycle; Analytics owns read models only.

**10. What does the API Gateway do and not do?**  
Routes and optionally validates JWT; does **not** replace per-service Resource Server validation or Family authz.

**11. How does failure isolation work?**  
If Notification is down, inventory can still write outbox; publishing retries. If Family authz is down, money APIs fail closed rather than allow cross-family spend.

---

## Kafka / Distributed systems

**12. Explain Outbox in this project.**  
Business TX inserts `outbox_events`; `OutboxPublisher` publishes after commit and marks PUBLISHED only after Kafka ack (pattern per service).

**13. Explain Inbox.**  
Consumer persists `event_id` UNIQUE before applying effects (Wallet/Analytics/Notification/Budget/Expense variants).

**14. What does at-least-once mean for FakePayment IPN twice?**  
Two IPNs / redeliveries must still produce **one** ledger credit via `pay-credit:{paymentOrderId}`.

**15. Event ID vs business idempotency?**  
Event ID stops identical Kafka redelivery; business key stops logically duplicate operations with different event IDs.

**16. Retry and DLT status?**  
Outbox/inbox retries exist; contracts mention DLT topics — full DLT wiring is **not** claimed complete for every topic.

**17. Saga consistency for expense?**  
Expense stays `PENDING_WALLET` until `WalletDebitPosted`/`ExpensePaymentFailed`; no silent HTTP mark-paid.

**18. Event ordering assumptions?**  
Partition keys (e.g. familyId/aggregate) give per-key order; cross-topic global order is not assumed — handlers are idempotent.

**19. Allowance saga Kafka gap?**  
Budget emits `AllowanceDisbursementRequested`; wallet method exists; **consumer class missing** — interview honesty point.

---

## Database / Financial accounting

**20. What is double-entry here?**  
Each posted journal: sum(DEBIT amounts) = sum(CREDIT amounts) in VND BIGINT.

**21. Why ledger immutability?**  
Posted journals are not edited; `LedgerController` reversal endpoint creates compensating entries.

**22. What is wallet balance projection?**  
`wallet_balances` maintained in the posting TX — never by Analytics or Payment.

**23. Concurrent transfers?**  
Locks + balance checks; loser gets insufficient funds.

**24. Insufficient funds on expense?**  
Permanent `ExpensePaymentFailed`; expense FAILED; Analytics ignores failure for realized expense.

**25. Which UNIQUE constraints matter?**  
Ledger idempotency/reference; inbox event_id; Analytics `ledger_transaction_id`.

**26. How would you reconcile Analytics vs Wallet?**  
Sum journals `PAYMENT_TOPUP` / `EXPENSE_PAYMENT` per family/month vs monthly summary; rebuild via documented ops steps (no silent truncate in app).

---

## Security

**27. Role of Keycloak?**  
OIDC IdP; realm `family-finance`; demo users in import JSON for **local** only.

**28. JWT validation location?**  
Gateway + each service Resource Server (`issuer-uri` / JWK set).

**29. Family-scoped authorization?**  
Membership + permission keys via Family internal API — not only “has JWT”.

**30. Service-to-service auth?**  
`X-Internal-Api-Key` for internal routes (authz, wallet credits).

**31. Tenant isolation?**  
`familyId` on resources; cross-family access denied when authz enabled (security verify script historically).

---

## DevOps / testing / observability

**32. Docker Compose profiles?**  
Default postgres; `full` apps+Kafka+Keycloak; `observability` metrics/traces; `web` Vite.

**33. What does CI prove?**  
Unit tests, frontend build, compose config validity, wallet IT count=14 — **not** production deploy.

**34. Why Postgres for wallet IT?**  
Locking/idempotency semantics differ from H2.

**35. Flyway ownership?**  
Each service migrates only its DB; never write another service’s schema.

**36. Prometheus vs Tempo?**  
Metrics scrape vs distributed traces via OTLP; both optional via observability profile.

**37. Is Kubernetes production-proven here?**  
No — `infra/k8s` starter manifests only.

---

## Deeper cross-cutting

**38. Why not count `PaymentProviderPaid` as income?**  
Provider paid ≠ wallet credit; credit can lag/fail. Package E uses `WalletCreditPosted`.

**39. Why is allowance not household expense?**  
Internal liability transfer; assets stay in the family. Counting it would inflate spend.

**40. How would you finish the allowance Kafka path?**  
Add `AllowanceDisbursementRequestedConsumer` mirroring `ExpensePaymentRequestedConsumer`, calling `disburseAllowance`, with inbox + permanent failure mapping — **not implemented yet**.

**41. What would you change for production payments?**  
Dedicated PSP credentials, webhook authenticity, reconciliation jobs, PCI/scope analysis — far beyond FakePayment/MoMo Sandbox demo.

**42. Modular monolith alternative?**  
Same ledger module + outbox could teach identical money invariants with simpler deploy; microservices emphasize boundaries for portfolio narrative.

**43. How do you avoid Analytics double-count on replay?**  
Inbox PROCESSED skip + `realized_financial_postings` unique on ledger TX id.

**44. What is explicitly out of scope?**  
Regulated banking, live MoMo production, verified K8s production, performance benchmarks, Package-style new features after F.

---

*Total: 44 questions with concise model answers.*
