# Raw verification evidence

- build-package.log: final successful Java 21 Maven package execution.
- unit/: Surefire summaries for 31 passing cases across five classes.
- postgres-attempt.log: attempted PostgreSQL smoke test; context startup failed because Docker is absent.
- com.motoservice.BookingIT.txt and failsafe-summary.xml: one setup error, not a successful database test.
- initial-unit-attempt.txt: initial Mockito self-attachment failure, fixed with the subclass mock maker without disabling assertions.

Logs contain workspace paths and fictional test identities only. Repeated runs are not added together to inflate counts. No measured evaluation CSV exists because experiments did not execute.
