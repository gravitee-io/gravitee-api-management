# Kafka Explorer - Conventions

## Architecture

- Standalone lib, no dependency on `@gravitee/ui-particles-angular` or `Constants`
- Smart components (with service) + dumb components (presentation)

## Components

- Standalone components only
- Inputs via signals (`input()`, `input.required()`)
- Internal state via `signal()`
- Selector prefix: `gke-`

## Styling

- Layout: BEM with component name as block

## Tests

- Use **test harnesses** (CDK `ComponentHarness`)
- Each component has its own `.harness.ts`
