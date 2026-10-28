# Test matrix

**31 executed unit/MVC cases: 31 passed, 0 failures, 0 errors, 0 skipped.** Database tests are compiled but unverified. One PostgreSQL smoke test was attempted and reported one environment error before any scenario assertions ran. This is not a skipped or passing integration test.

| Suite | Cases | Coverage | Status |
|---|---:|---|---|
| ApiContractTest | 7 | 401/403, CSRF, input 400, missing 404, conflict 409; mocked service | VERIFIED |
| BookingRulesTest | 12 | Allowed transitions, terminal states, cancellation eligibility | VERIFIED |
| ValidationTest | 5 | Required fields, positive IDs, note limits, motorcycle input | VERIFIED |
| WebRenderingTest | 4 | Actual login, customer, advisor, slot Thymeleaf templates; mocked service | VERIFIED |
| WorkshopServiceTest | 3 | Role decisions, duplicate rejection after lock acquisition; Mockito | VERIFIED |

| Database suite | Declared cases | Coverage | Execution |
|---|---:|---|---|
| BookingIT | 14 | Success, capacity, duplicate, ownership, cancellation, promotion, ties, withdrawal, ineligibility, lifecycle, rollback | One smoke method attempted: setup ERROR; all database assertions NOT EXECUTED |
| ConcurrencyIT | 5 | 20/2 and 50/5 contention, duplicate submissions, two cancellations, same-booking cancellation race | NOT EXECUTED |
| SecurityIT | 13 | Actual DB-backed authentication, private records, authorization, CSRF, API validation and creation, templates, form login | NOT EXECUTED |
| EvaluationIT | 2 | Reset fixture and repeated baseline comparisons | NOT EXECUTED |

The 34 declared database cases include expanded parameterized cases and the reset check. This is a source inventory, **not a passing-test count**.

## Integrity rules

No disabled or ignored tests. Database suites fail if Docker is unavailable; they do not silently skip. A dedicated external PostgreSQL override is available. No H2 substitution. No sleeps in concurrent synchronization. Each worker uses a separate proxied service call and transaction. JUnit assertions check exact accepted counts and database state, not just lack of exceptions. Mockito only supports isolated unit/MVC checks; it supplies no concurrency evidence.

`mvn test` runs names ending in Test; `mvn verify` additionally runs names ending in IT with Maven Failsafe. CI runs all suites including evaluation. No tests are intentionally excluded from CI. GitHub Actions itself has not been observed running.
