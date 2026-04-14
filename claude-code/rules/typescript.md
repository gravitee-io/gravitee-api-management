# TypeScript Style & Structure

## Core Principles

- **Safety**: Prefer strict typing. Avoid `any`; use `unknown` if necessary.
- **Immutability**: Prefer `readonly` properties and immutable data structures where possible.
- **Functional**: Prefer pure functions and composition over class inheritance.

## Syntax Preferences

- **Types vs Interface**: Prefer `type` for data structures and unions. Use `interface` only for object shapes that need declaration merging (rare) or specific library requirements.
- **Functions**: Use arrow functions (`const myFn = () => {}`) for all function definitions.
- **Exports**: Use named exports. Avoid default exports to ensure consistent naming on import.
- **Async**: Always use `async/await` over raw promises.

## Structure

- **Directories**: Group by feature, not technical type. (e.g., `feature-A/components`, `feature-A/hooks` rather than `components/feature-A`).
- **Barrels**: Use `index.ts` files sparingly to expose public API of a module.

## Documentation

- **JSDoc for Exports**: All exported functions, types, and classes MUST have JSDoc comments (for intellisense and API clarity).
- **Sparse Inline Comments**: Outside of JSDoc, be very conservative with comments. Only add inline comments for:
    - High cyclomatic complexity blocks (deep if/elif chains)
    - Non-obvious async/await patterns where it's unclear what external call we're awaiting
    - Genuinely sophisticated logic that can't be "read" by another engineer
- **Avoid Changelog Comments**: Don't add comments just to call out recent changes--that's what git history is for.
- **Namespace Pattern**: For related types, use TypeScript `namespace` to group them (e.g., `ArazzoV1_0.Document`).

## Preferred Libraries

- **Validation**: `zod` is the standard for schema validation.
- **Testing**: `vitest` for unit/integration tests.
- **State**: React Context or `zustand` for simple state; `tanstack-query` for server state.

## LLM Integration Patterns

When building LLM-powered TypeScript code:

- **Schema-First**: Define Zod schemas for all LLM outputs, then derive types with `z.infer<>`.
- **Structured Output**: Always request `json_schema` response format from OpenAI/Azure.
- **LangChain**: Preferred framework for agent construction (ChatPromptTemplate, structured output).
