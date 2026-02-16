# Gravitee APIM Portal Next

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) and is now part of an **Nx workspace** at the repository root.

## Architecture

This project is part of an **Nx workspace** at the repository root.

- **Intelligent caching**: Nx caches build and test results
- **Affected commands**: Only test/build what changed
- **Dependency graph**: Visualize project dependencies with `yarn graph`
- **Unified configuration**: All dependencies managed at the root

Learn more about Nx at https://nx.dev[nx.dev]

## Prerequisites

- Install [nvm](https://github.com/nvm-sh/nvm)
- Use with `nvm use` or install with `nvm install` the version of Node.js declared in `.nvmrc` (at repository root)
- Install dependencies from the **repository root**:

```bash
# Go to repository root
yarn install
```

## Development

All commands must be run from the **repository root** (not from this directory).

### Development server

Run `yarn portal-next:serve` for a dev server. Navigate to `http://localhost:4101/`. The application will automatically reload if you change any of the source files.

Alternative backends:

- `yarn portal-next:serve:apim-master` - Connect to apim-master backend

### Code scaffolding

From the repository root:

```bash
yarn nx generate component component-name --project=portal-next
```

You can also use `nx generate directive|pipe|service|class|guard|interface|enum|module --project=portal-next`.

## Build

From the repository root:

- `yarn portal-next:build` - Build the project in development mode
- `yarn portal-next:build:prod` - Build the project in production mode

The build artifacts will be stored in `gravitee-apim-portal-webui-next/dist/`.

## Testing & Quality

From the repository root:

- `yarn portal-next:test` or `nx test portal-next` - Run unit tests via [Jest](https://jestjs.io/)
- `yarn portal-next:lint` or `nx lint portal-next` - Run ESLint
- `yarn portal-next:storybook` - Start Storybook on port 6006

## Shared Libraries

This project uses shared libraries located in `gravitee-apim-webui-libs/`:

- `@gravitee/gravitee-markdown` (moved from `projects/gravitee-markdown`)
- `@gravitee/gravitee-dashboard` (moved from `projects/gravitee-dashboard`)

### Building libraries

The libraries are automatically built by Nx when needed. To build them manually from the repository root:

```bash
nx build gravitee-markdown
nx build gravitee-dashboard
```

## Translations

You can add your own translations to this project.

### Manually add your translations

1. Run `yarn nx extract-i18n portal-next` to extract the source language file.
2. Rename the translation file in `src/locale` to add the locale: `messages.xlf --> messages.{locale}.xlf`
3. Complete the file with the desired translations.
4. In the `project.json` file, add the new locale to the `build` target configurations.
5. Build all translations with the appropriate configuration.
6. The translations will be available via `<host>/en-US/` and for each locale specified, example: `<host>/fr/`.

Find out more about [@angular/localize](https://angular.io/guide/i18n-common-translation-files).

## Further help

To get more help on Nx use `nx help` or check out the [Nx Documentation](https://nx.dev).

For Angular CLI help use `ng help` or go check out the [Angular CLI Overview and Command Reference](https://angular.io/cli) page.
