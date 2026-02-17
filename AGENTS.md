# Repository Guidelines

## Project Structure & Module Organization
This is a Maven-based Spring Boot (Java 21) application.

- `src/main/java/com/pehlione/web`: application source code (`WebApplication` entry point).
- `src/main/resources`: runtime config (`application.properties`), Flyway path (`db/migration`), static assets (`static`), and templates (`templates`).
- `src/test/java/com/pehlione/web`: JUnit/Spring Boot tests and Testcontainers config.
- Root docs: `START.md` and `PORT_KILL.md` for local run/troubleshooting.

Keep new code under the existing `com.pehlione.web` package tree unless a clear module split is introduced.

## Build, Test, and Development Commands
Use Maven Wrapper when possible:

- `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`: run locally with `dev` profile.
- `./mvnw test`: run unit/integration tests.
- `./mvnw -DskipTests package`: build the jar quickly.
- `./mvnw clean package`: full clean build.
- `java -jar target/web-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev`: run packaged artifact.

## Coding Style & Naming Conventions
- Follow standard Java/Spring conventions: classes `PascalCase`, methods/fields `camelCase`, constants `UPPER_SNAKE_CASE`.
- Use package names in lowercase (`com.pehlione.web...`).
- Match existing indentation style (tabs in current Java/XML files).
- Keep configuration keys in dotted lowercase (for example: `spring.session.jdbc.initialize-schema`).
- Prefer small focused classes and constructor-based dependency injection.

## Testing Guidelines
- Frameworks: JUnit 5, Spring Boot Test, Testcontainers (MySQL).
- Test classes should follow `*Tests.java` naming (example: `WebApplicationTests`).
- Add or update tests for behavior changes, especially DB/session/security paths.
- If tests depend on Testcontainers, ensure Docker/Testcontainers runtime is available before running `./mvnw test`.

## Commit & Pull Request Guidelines
Git history currently uses short messages (for example: `doku`, `implement basis code`, `ok`) without strict formatting. Prefer clearer imperative messages:

- Good: `fix(session): initialize JDBC session schema`
- Avoid: `ok`, `update`

For PRs, include:
- concise summary of what changed and why,
- how it was tested (commands run),
- DB/config impacts (ports, MySQL/Flyway/session),
- screenshots/log snippets only when behavior is UI/ops-visible.

## Security & Configuration Tips
- Do not commit real secrets. Local defaults in `application.properties` should be overridden via environment variables in shared environments.
- Default app port is `8083`; check and free the port before startup if needed.
