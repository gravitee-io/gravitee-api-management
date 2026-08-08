---
name: Kafka Explorer Library Conventions
layer: local
dirs: [gravitee-apim-webui-libs/gravitee-kafka-explorer]
description: kafka-explorer library conventions - standalone-library boundaries, Nx commands, project structure, the gke- prefix, BEM, and per-component harnesses
---

# Kafka Explorer Library Conventions

- **Standalone library:** no dependency on `@gravitee/ui-particles-angular` or `Constants` — the shared rule's reuse-Gio-patterns guidance does not apply in this library.
- **Nx project name `kafka-explorer`:** `npx nx build kafka-explorer`, `npx nx test kafka-explorer`.
- **Selector prefix:** `gke-`. **Styling:** BEM with the component name as block.
- **Structure under `src/lib/`:** `components/` (shared reusable components), `features/<area>/` with the page component at the feature root and dumb components in subdirectories (e.g. `brokers/brokers/`, `brokers/broker-detail/`), `kafka-explorer/` (main shell: sidebar + router-outlet), `models/`, `pipes/`, `services/`.
- **Tests:** every component has its own `.harness.ts` (CDK `ComponentHarness`); test utilities import from `@gravitee/gravitee-kafka-explorer/testing`.
