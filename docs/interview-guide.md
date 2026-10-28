# Interview guide

Use the real source and verification report. Never describe an unexecuted scenario as a successful result.

## 1. What real-world problem does this solve?

A small workshop has a limited number of appointment allocations. Two customers can both see a free space and book simultaneously if the application only checks availability without coordinating writes. Cancellations can also leave unused capacity while other customers wait. MotoService Lite implements a transactional admission rule and automatic FIFO promotion. It is a synthetic student prototype; I do not claim real workshop adoption or financial impact.

## 2. Why Java?

Java gives me explicit types, enums for the booking lifecycle, records for API contracts, classes with encapsulated state and mature database libraries. This project extends my TypeScript/Python experience by showing constructor injection, JPA entities, transaction boundaries, exception rollback and JUnit tests in Java. I chose Java 21 as the requested LTS toolchain, without adding language features merely for decoration.

## 3. Why Spring Boot?

Spring Boot supplies a coherent application runtime for Spring MVC, validation, JPA, Security and Thymeleaf. I can focus on booking rules rather than manually wiring a server, database pool and authentication filters. Auto-configuration does not replace understanding: I explicitly configure the data source, security chain, CSRF handling, migration strategy and transaction boundaries.

## 4. What is the architecture?

It is one monolithic application with controllers, a business service, repositories and PostgreSQL. Controllers accept validated DTO records and return JSON or Thymeleaf views. WorkshopService owns authorization and capacity decisions. Repositories persist entities. Flyway creates the schema and Hibernate validates it. There are no microservices, message brokers or separate frontend applications because the selected problem does not require them.

## 5. How does booking work?

The authenticated customer submits a motorcycle, slot and service type. The service locks the slot row, checks that the motorcycle belongs to the customer, verifies future time and duplicate rules, then counts non-cancelled bookings. If space exists it inserts a confirmed booking and an event in one transaction. Otherwise it returns a conflict with a waitlist option. The confirmation reference is derived from the persisted booking ID.

## 6. How do transactions prevent double booking?

A transaction alone is not enough: two read-committed transactions could both observe the same free space. My service first obtains a pessimistic write lock on the slot row. Every application mutation follows that protocol. The next writer waits, then checks occupancy after the earlier commit. Cancellation and promotion also happen under that lock in one transaction, so an exception restores the previous state. Database integration execution remains pending here.

## 7. What is a race condition?

A race occurs when correctness depends on how concurrent operations interleave. Imagine capacity two and occupancy one. Two callers both read one, both decide there is space, and both insert; occupancy becomes three. The isolated naive evaluation harness demonstrates that risk when executed. The real service serializes decisions for each slot using a database lock, which works across application threads and processes.

## 8. What is pessimistic locking?

Pessimistic locking reserves the right to change a database row before making the decision. SlotRepository uses JPA PESSIMISTIC_WRITE, which PostgreSQL implements with row locking. Other writers following the same protocol wait. The tradeoff is contention for popular slots. The lock is released on commit or rollback, and there is a bounded lock timeout. Different slots can proceed independently.

## 9. How does FIFO promotion work?

Waiting entries are persisted with request time and ID. After cancellation, the service selects the earliest WAITING entry using an ordered indexed query. It rechecks ownership, customer role, future time and duplicates. An ineligible entry is marked and skipped; a valid one gets a confirmed booking and is marked PROMOTED with the booking ID. Both changes commit atomically. FIFO is transparent and easy to test, not universally optimal.

## 10. Which OOP principles are used?

Entities encapsulate private state, and lifecycle changes go through named methods. Controllers depend on a service abstraction instead of embedding SQL rules. Repositories are interfaces implemented by Spring Data. Constructor dependency injection composes the service from collaborators and supports test substitution. I avoided unnecessary inheritance and patterns: the business model is small, so direct composition is easier to explain and maintain.

## 11. Which DSA concepts are used?

The main algorithmic concept is FIFO ordering with deterministic tie-breaking. The real queue lives in PostgreSQL and is accessed through a composite B-tree index; I do not claim an in-memory Java Queue. Java Lists represent ordered results, and Futures plus CountDownLatch coordinate concurrent tests. State transitions are a small finite-state machine. The meaningful skill is choosing data structures that match persistence and concurrency requirements.

## 12. What is the time complexity?

For W queue entries, indexed head retrieval is typically a logarithmic seek plus fetching a row. Skipping K invalid candidates repeats this work. However, the full operation also counts B bookings for the slot and performs eligibility queries and writes. Therefore I do not label the whole service O(log W) or O(1). The exact query plan depends on data distribution and indexes; the documentation states the assumptions.

## 13. How are the REST APIs designed?

Endpoints use nouns such as motorcycles, slots, bookings and waitlist, with explicit cancellation and withdrawal actions. Successful creation returns 201; invalid requests use 400, unauthenticated requests 401, forbidden actions 403, missing records 404 and business conflicts 409. Request and response records are separate from JPA entities. Bean Validation checks fields, while the service checks ownership, time, state and capacity.

## 14. What does JPA/Hibernate do?

JPA defines Java persistence APIs and annotations; Hibernate implements them. It maps entity fields to PostgreSQL columns, tracks managed changes and issues SQL. Spring Data generates repository implementations. I still define indexes, constraints and migrations in SQL. I disable open-in-view to avoid accidental lazy database work in templates, use explicit DTO mapping, and rely on Flyway rather than automatic schema creation.

## 15. How does PostgreSQL store the data?

The schema separates users, motorcycles, centers, slots, bookings, waiting entries and booking events. Primary keys identify rows; foreign keys enforce relationships. Partial unique indexes prevent two non-cancelled bookings or two active waitlist entries for the same motorcycle and slot. An ordered composite index supports FIFO. Timestamps use timestamptz. Total capacity is a cross-row rule enforced by the service lock protocol, not an ordinary CHECK constraint.

## 16. Which tests did you write?

The source includes rule and validation tests, Mockito service tests, actual Thymeleaf rendering through MockMvc, and PostgreSQL integration tests for booking, privacy, rollback, FIFO and concurrency. Testcontainers is the default database provider. Concurrency tests use bounded latches and futures, including 20 requests for capacity two and 50 for capacity five. I distinguish locally executed unit/MVC results from unexecuted database scenarios in the verification report.

## 17. What did the research papers contribute?

The automotive scheduling study motivates explicit resource constraints. The nonsequential appointment paper shows that demand uncertainty can justify policies beyond simple FIFO. The uncertainty paper highlights processing time, punctuality and no-show limitations. I reviewed accessible abstracts and metadata, not all full papers. Their contribution is problem framing and honest scope boundaries; I did not implement their optimization methods or claim to outperform them.

## 18. What experiment did you conduct?

I implemented two reproducible experiments: naive versus locked concurrent admission, and cancellation without reassignment versus FIFO promotion. They use fresh synthetic data, repeated workloads and CSV outputs. In this environment PostgreSQL execution was unavailable, so I have not conducted the database experiment successfully or accepted either hypothesis. After running it locally, I can discuss the actual rows and environment rather than expected assertions.

## 19. What are the limitations?

An appointment consumes one fixed capacity unit; the system does not model repair duration, technicians, no-shows or overlapping appointments. Demo slots are seeded for fourteen days, without a slot administration UI. Popular slots serialize writes. Read views favor simplicity over pagination/query efficiency. Public demo credentials are local-only. Database and end-to-end validation must still be executed; the delivered report makes that explicit.

## 20. How would you improve it in a company?

First I would run the database and browser tests in the target environment, measure lock contention and examine query plans. Then I would add requirements justified by real workflows: appointment idempotency, pagination, supported dependency upgrades, secure account management and operational monitoring. Duration and technician constraints need a separately designed scheduling model. I would preserve the documented lock order and expand experiments before making performance or business claims.

## Five-minute walkthrough

**0:00–0:40 — Problem.** Explain finite service capacity and cancelled appointments with waiting demand. State the one-unit-per-appointment assumption and independent student scope.

**0:40–1:20 — User journey.** Customer logs in, registers a motorcycle, selects a slot and gets a confirmation or joins the FIFO waitlist. Customer sees status and history; advisor advances service.

**1:20–2:10 — Java architecture.** Show DTO records, an entity, the constructor-injected WorkshopService and a repository interface. Point to Flyway constraints and BCrypt/CSRF.

**2:10–3:15 — Core correctness.** Show SlotRepository.lockById, then book and cancel. Explain the read-then-insert race, why a shared slot lock protects the count, and why cancellation plus promotion must share a transaction. Explain scalar ID lookup before loading a mutable booking.

**3:15–4:00 — Queue and DSA.** Show timestamp/ID ordering, eligibility rechecks and promotion linkage. Explain the persisted queue/index and the cost of repeated count queries. State that FIFO is a fair, explainable policy rather than an optimal scheduler.

**4:00–4:40 — Evidence.** Show the exact test reports. Explain the executed unit/MVC tests and the database harness that still needs execution. Describe both synthetic baselines without quoting unobserved outcomes.

**4:40–5:00 — Limits.** Fixed capacity, local demo authentication, no real integration or production claims. Finish with the next engineering step: execute the PostgreSQL suites and review actual evidence.
