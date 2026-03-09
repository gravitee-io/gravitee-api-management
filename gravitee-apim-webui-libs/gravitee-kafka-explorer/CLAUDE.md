# Kafka Explorer - Conventions

## Architecture

- Standalone lib, no dependency on `@gravitee/ui-particles-angular` or `Constants`
- Smart components (with service) + dumb components (presentation)
- Nx project name: `kafka-explorer` (`npx nx build kafka-explorer`, `npx nx test kafka-explorer`)

## Project structure

```
src/lib/
├── components/              # Shared reusable components (badge, ...)
├── features/
│   ├── brokers/             # Page components at root, dumb in subdirs
│   │   ├── brokers/         #   dumb list component + harness
│   │   └── broker-detail/   #   dumb detail component + harness
│   ├── topics/
│   │   ├── topics/
│   │   └── topic-detail/
│   └── consumer-groups/
│       ├── consumer-groups/
│       └── consumer-group-detail/
├── kafka-explorer/          # Main shell (sidebar + router-outlet)
├── models/
├── pipes/
└── services/
```

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
- Test utilities import from `@gravitee/gravitee-kafka-explorer/testing`
