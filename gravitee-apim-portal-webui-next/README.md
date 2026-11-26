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

## Deployment configuration

Portal Next reads `assets/config.json` at startup, then calls `/ui/bootstrap` using
its `baseURL` and optional `environmentId`. The bootstrap response resolves the
Portal API URL, environment and portal ID. A deployment configures the API URL
and, optionally, the environment:

```json
{
  "baseURL": "https://api.example.com/portal",
  "environmentId": "DEFAULT"
}
```

### Portal selection

By default the portal is selected automatically: its ID comes from the bootstrap
response, which selects the environment's default or only portal.

Set `portalId` to select a specific portal of that environment, for local
development or for a portal app reached through an internal name:

```json
{
  "baseURL": "https://api.example.com/portal",
  "environmentId": "DEFAULT",
  "portalId": "e9c87445-920f-4a26-a36d-c6334a05ca37"
}
```

The configured `portalId` takes precedence over the bootstrap's one. Leading and
trailing whitespace is removed. Omit it, set it to `null`, or leave it empty or
blank to use the bootstrap's portal. A non-string value and the dot segments `.`
and `..` are invalid and fail startup before the bootstrap call, so requests
cannot escape the portal path.

The override only chooses the portal. The bootstrap still resolves the Portal API
URL and the environment, and a failed bootstrap still fails startup. The ID grants
no access: the backend refuses a portal that is unknown or belongs to another
environment, and applies its usual visibility and access rules.

One `assets/config.json` serves every host of a deployment: an explicit `portalId`
pins all of them to that portal. Deployments that serve several portals from the
same files, each on its own host, leave it unset.

With the Helm chart, set `portal.portalId`. The chart writes it only to
`config-next.json`, mounted at
`/usr/share/nginx/html/next/browser/assets/config.json`; Classic Portal is
unaffected. When supplying an external volume named `config` through
`portal.extraVolumes`, provide both `config.json` and `config-next.json` yourself:
the chart does not generate or merge those files.

### Request scope

Navigation, homepage content, catalog searches (APIs and API Products), and API
searches use `/environments/{environmentId}/portal/{portalId}/...`. API details,
theme, authentication, applications, subscriptions, and existing category metadata
keep their environment-level paths. The API analytics filter also searches within
the selected portal. Subscription details can still be read when their API has no
documentation link in that portal.

The backend must provide the bootstrap portal ID, portal-scoped navigation,
and portal-scoped API search. The frontend does not fall back to environment-wide
requests if a scoped endpoint fails.
Missing portal context or an unknown portal during startup opens the existing
error page. Correct the configuration and use its retry action to restart.
A known portal with no navigation is valid. After startup, missing documentation
items open the not-found page; other documentation failures open the error page.

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
