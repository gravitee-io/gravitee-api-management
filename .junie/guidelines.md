# Java Development Guidelines

## General Principles
- **Senior-Level Code**: Write clean, maintainable, and self-documenting code. Avoid over-engineering but ensure robust error handling and scalability.
- **TDD (Test-Driven Development)**: Always prefer a TDD approach. Write a failing test first, then implement the minimum code required to pass the test, and finally refactor.
- **Immutability**: Favor immutability. Use `final` keywords where appropriate and prefer data structures that discourage in-place modification.

## Java Coding Standards
- **Modern Java**: Use Java 21 features.
- **Records**: Use `record` for DTOs (unless generated), domain events, and any pure data carriers. Do not use Lombok `@Data` or `@Value` when a record suffices.
- **Jakarta EE**: Use `jakarta.*` imports (not `javax.*`).
- **Lombok**: Use Lombok sparingly, primarily for `@Slf4j` or complex builders that records cannot handle.
- **Naming**: Use descriptive names. Avoid abbreviations. Classes should be nouns; methods should be verbs.

## Mapping
- **MapStruct**: Use MapStruct for all object-to-object mappings (e.g., Entity to DTO).
- **Configuration**: Use `componentModel = "jakarta"` in `@Mapper` definitions.
- **No Manual Mapping**: Avoid manual "converter" classes or boilerplate mapping logic in services.

## Persistence
- **Spring Data MONGO**: Follow Spring Data patterns for repository interfaces.
- **Queries**: Use method derivation for simple queries and `@Query` or `MongoTemplate` for complex logic.

## Formatting and Style
- **Prettier**: The project uses Prettier for formatting. Ensure all generated or modified code adheres to the rules defined in `.prettierrc`.
- **Consistency**: Always inspect existing files in the module you are working on to match the local style (e.g., package structure, naming conventions).

## Testing
- **JUnit 5**: Use JUnit 5 for unit and integration tests.
- **Mockito**: Use Mockito for mocking dependencies.
- **Naming**: Test methods must follow **snake_case** and clearly state the expected behavior (e.g., `should_return_api_when_id_exists`).
- **Assertions**: Use AssertJ for fluent assertions.

## Workflow
1.  **Analyze**: Understand the requirements and the existing codebase.
2.  **Test**: Write a unit test in the appropriate `src/test/java` directory.
3.  **Implement**: Implement the logic in `src/main/java`.
4.  **Format**: Ensure code is formatted correctly (matches Prettier/EditorConfig).