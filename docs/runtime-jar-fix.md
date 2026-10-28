# Runtime JAR repair (2026-10-28)

Observed on Windows: `Error: Invalid or corrupt jarfile app.jar` after a successful `mvn verify` and Docker cached app image build.

The original multi-stage Dockerfile ran a **second** `mvn -DskipTests package` inside an independently cached Docker build layer. The updated Dockerfile copies **the same Maven-produced executable JAR from `target/` that was generated during the successful `mvn clean verify` run**. The Windows launcher verifies that the JAR is a ZIP archive with a manifest, Spring Boot launcher, application classes and dependencies, then force-recreates the app container.

This removes the suspect cached-JAR path. It does not claim a successful Windows run until the HTTP smoke test actually finishes.

Do not manually delete Docker volumes. Existing demo data is retained.
