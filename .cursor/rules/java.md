# Java Style & Structure (Gravitee Standard)

## Core Principles

- **Version**: Target Java 21+.
- **Build System**: Maven is the standard build tool.

## Styling

- **Format**: Follow standard Java conventions (Google Java Style).
- **Lombok**: Use Lombok for boilerplate reduction (`@Data`, `@Builder`, `@Slf4j`).
- **Var**: Use `var` for local variable type inference where the type is obvious.

## Structure

- **Architecture**: Follow Hexagonal or Clean Architecture principles where applicable.
- **Testing**:
  - **Testcontainers**: Use for integration testing with real dependencies.
  - **JUnit 5 / AssertJ**: Use for fluent assertions.

## Dependencies

- **Gravitee BOM**: Align with `gravitee-apim-bom` for version management.
