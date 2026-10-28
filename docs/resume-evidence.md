# Resume evidence

## What is substantiated now

- Java 21 and Spring Boot 3.5.16 production/test source compiles; executable JAR packaging succeeded.
- 31 distinct JUnit cases passed: 20 rule/validation/service-unit cases, 4 Thymeleaf rendering cases and 7 API/security MVC cases.
- PostgreSQL schema, JPA locking/promotion implementation, Testcontainers suites, Docker configuration and GitHub Actions workflow exist in source.
- No database correctness, throughput, cancellation-recovery percentage, hosted CI success, deployment, customers or financial impact has been verified.

## Conservative ATS bullets

These bullets describe implemented source and the limited executed verification. They are not a claim that full project acceptance has passed.

- Developed MotoService Lite with Java 21, Spring Boot, object-oriented design and PostgreSQL, implementing 13 REST endpoints for motorcycle service booking, persisted FIFO waitlists and role-based service history.
- Verified booking rules, input validation, HTTP security contracts and Thymeleaf rendering through 31 passing JUnit/Mockito/MockMvc tests, documenting separate Testcontainers integration and concurrency scenarios for PostgreSQL validation.

Use these only after reviewing and understanding the code. Do not add “prevented all overbooking,” a capacity/concurrency success count, recovery improvement, production scalability or GitHub Actions success until the corresponding evidence is executed. The endpoint count is a static code inventory, not an assertion that all endpoints passed end-to-end tests.

## After PostgreSQL verification

Read the actual `target/failsafe-reports` and `target/evaluation/*.csv`. A future revised bullet may quantify observed workloads, final occupancy or synthetic slot recovery, with environment and scope. Never substitute expected test assertions for observed measurements. Final database-result-based XYZ bullets are deferred until that verification exists.
