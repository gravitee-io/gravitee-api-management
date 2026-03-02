---
description: Angular, TypeScript, and Frontend design system rules
trigger: always_on
---

# Frontend Rules

## Tech Stack

| Component       | Version/Tool           |
| --------------- | ---------------------- |
| Node.js         | 20.19.0 (see `.nvmrc`) |
| Package Manager | yarn 4.1.1             |
| Framework       | Angular 19.x           |
| UI Library      | Angular Material 19.x  |
| Testing         | Jest                   |
| Linting         | ESLint + Prettier      |

## Projects

| Project     | Directory                         | Port       |
| ----------- | --------------------------------- | ---------- |
| Console UI  | `gravitee-apim-console-webui`     | 4200 (dev) |
| Portal UI   | `gravitee-apim-portal-webui`      | 4100 (dev) |
| Portal Next | `gravitee-apim-portal-webui-next` | -          |

## Commands

```bash
# Always use nvm first
nvm use

# Install dependencies
yarn

# Development server
yarn serve

# Lint
yarn lint
yarn lint:fix

# Test
yarn test
yarn test:auto  # watch mode

# Build
yarn build
yarn build:prod
```

## Gravitee UI Libraries

Custom UI components from Gravitee:

- `@gravitee/ui-components` - Web components
- `@gravitee/ui-particles-angular` - Angular components
- `@gravitee/ui-policy-studio-angular` - Policy studio
- `@gravitee/ui-analytics` - Analytics components

## Code Style

- ESLint config extends `eslint-config-prettier`
- Prettier for formatting: `yarn prettier:fix`
- License headers required: `yarn lint:license:fix`

## Storybook

Console UI has Storybook for component development:

```bash
cd gravitee-apim-console-webui
yarn storybook
```

## Environment

Console UI connects to REST API at `http://localhost:8083/management/`.

For remote backends:

```bash
BACKEND_ENV=apim-master-api.team-apim.gravitee.dev yarn serve
```
