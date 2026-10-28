# Business requirements

## People and problem

A customer owns a motorcycle and seeks a workshop service appointment. A service advisor runs the workshop booking list and records service progress. Limited capacity creates full slots; simultaneous bookings can oversubscribe a naive implementation; cancellations can leave unused allocations despite waiting demand.

MotoService Lite targets transparent admission and automatic reassignment. Business benefit is the ability to use released capacity, not a quantified claim of revenue, reduced repair time or customer satisfaction. All centers and accounts are fictional.

## Functional requirements and acceptance

| ID | User story | Acceptance criterion | Implementation |
|---|---|---|---|
| FR1 | As a customer, I want a garage | Seeded login; valid registration, model, optional note; only my motorcycles listed | register, motorcycles |
| FR2 | As a customer, I want available slots | Persisted center, date/time, capacity and availability; date filter | slots, slot |
| FR3 | As a customer, I want a reservation | Ownership/future/duplicate checks; confirmation reference; 409 if full | book |
| FR4 | As a customer, I want a place in line | Full slot only; one active request per motorcycle/slot; timestamp and ID ordering | join, withdraw |
| FR5 | As a waiting customer, I want a released appointment | Eligible FIFO head promoted atomically on cancellation; no excess occupancy | cancel, promote |
| FR6 | As an advisor, I want controlled service progress | Only permitted forward transitions; immutable event history retained | status |
| FR7 | As a customer, I want service history | Confirmed, in-service, completed, cancelled bookings plus waiting/promotion history | dashboard, bookings |
| FR8 | As a customer, I want private records | Cross-owner writes rejected, reads scoped to authenticated customer | service authorization |

## Nonfunctional requirements

Java 21; stable 3.5.x Spring Boot; one Maven monolith; PostgreSQL schema migrations; explicit DTO validation; BCrypt; CSRF on state-changing browser and API requests; readable responsive UI; repeatable JUnit/MockMvc/PostgreSQL tests. No in-memory-only concurrency mechanism. Errors use 400/401/403/404/409 as appropriate. Unexpected server errors must not expose database details.

Correctness is measured by final SQL state, expected HTTP responses, persisted event history, rollback assertions, and deterministic latch-based concurrent tests. Tests fail on unexpected exceptions and timeouts; no arbitrary sleep synchronization. H1/H2 decisions require executed database evidence.

## Scope boundary

Each appointment consumes one unit; slots do not model duration, technicians or overlapping appointments at different times/centers. No new-calendar administration UI: demo slots are generated once for the next 14 days. No rescheduling workflow. Customers can withdraw a waiting entry, then choose another slot.

No payments, notifications, mapping, parts inventory, payroll, external identity, AI, optimization, microservices, mobile app or external motorcycle platform integration. These do not help demonstrate the selected capacity and recovery problem and would weaken a five-minute explanation.

## Risks and limitations

Direct SQL can bypass service locks; test harness must never point at a real database. Database test mode accepts only a database named motoservice_test. Demo users are intentionally public local credentials; use local loopback only. Seed data loads only into an empty user table; it does not continuously regenerate slots. This prototype is not internet-ready. Future deployment needs HTTPS, secret management, rate limits, operational monitoring and a currently supported dependency review. Fixed capacity cannot represent uncertain repair duration. Fair request order is not guaranteed optimal scheduling.
