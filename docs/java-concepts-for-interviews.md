# Java, OOP and DSA in the actual source

Paths below are relative to `src/main/java/com/motoservice/` unless noted.

| Concept | Source | What to explain |
|---|---|---|
| Classes and objects | entity/Booking.java, entity/Motorcycle.java | Each row is represented by a typed Java object; instances carry their own state |
| Encapsulation | entity/Booking.java | Private fields; getters; `changeStatus` rather than a public field |
| Business abstraction | service/WorkshopService.java | Controllers request operations without implementing lock/capacity rules |
| Interfaces | repository/SlotRepository.java | Spring Data supplies the repository implementation, including a locked query |
| Generics | repository interfaces and WorkshopService.found | `JpaRepository<Booking,Long>` and `Optional<T>` retain type safety |
| Dependency injection | WorkshopService constructor | Spring supplies repositories and Clock; tests substitute them |
| Enums | entity/BookingStatus.java, ServiceType.java | Finite legal domain values rather than scattered free-text strings |
| Records | dto/Requests.java, dto/Views.java | Immutable request/response carriers, separate from persistence entities |
| Collections | WorkshopService list results; ConcurrentRunner test | Lists preserve ordered results; Futures collect worker outcomes |
| Streams | views mapping in WorkshopService | Convert entity lists into response records without mutation |
| Exceptions | exception/BusinessException.java, controller/ApiErrors.java | Business failure rolls back the transaction and maps to an HTTP status |
| Java time | Clock, Instant, LocalDate | Injected time supports reproducible rules; UTC storage and explicit IST dates |
| Transaction proxy | `@Transactional` on WorkshopService | Public calls from controllers start transactions; private promote participates in cancel's existing transaction |
| Concurrency | test/ConcurrencyIT.java, ConcurrentRunner.java | Separate threads start from latches; database locks, not Java monitors, serialize slot writers |
| Queue ordering | repository/WaitlistRepository.java | SQL orders by requestedAt and ID; the composite index supports earliest-entry retrieval |
| State machine | service/BookingRules.java | Two forward service transitions; explicit cancellation guard |
| Persistence | Flyway V1 and JPA entities | SQL constraints/FKs complement service decisions; Hibernate validates rather than creates tables |
| Testing isolation | WorkshopServiceTest, WebRenderingTest | Mockito substitutes dependencies only in unit/MVC tests; this does not demonstrate PostgreSQL correctness |

There is no artificial inheritance hierarchy or generic enterprise service framework. Interface abstraction is useful at the repository boundary; inheritance is used only by the shared database test fixture. Composition through constructor injection keeps business rules readable.

## DSA explanation

FIFO means the earliest eligible request gets the next released space. A timestamp plus unique ID produces a total order, even when two entries have equal timestamps. A database index is used rather than an in-memory queue because records must survive restarts and concurrent application instances must agree on order.

Do not say “I used an O(1) Java queue” or “all booking operations are O(log n).” The index seek is typically O(log W); skipped entries require repeated lookups, and occupancy counts scan matching booking index entries. See architecture.md for the complete comparison and assumptions. Waitlist position is calculated from active entries at or before the request's timestamp/ID, which is a count query, not a stored rank.

## Java 21 scope

The project targets Java 21, uses records/enums/generics/streams and Java's concurrency utilities. It does not need virtual threads, reflection tricks or preview language features. Java 21 is a runtime/toolchain choice; do not claim that records or streams were introduced in Java 21.
