# Gamma Module Loading Architecture

## Overview

Gamma modules are micro-frontends loaded at runtime via [Module Federation](https://module-federation.io/). The host application (Gamma Control Plane WebUI) discovers available modules from the backend, dynamically registers them as Module Federation remotes, and renders them as lazy-loaded React components within protected routes.

Key design decisions:

- **Dynamic remote registration** -- remotes are not known at build time; they are registered at runtime after fetching the module list from the API.
- **Manifest-driven discovery** -- each module's `mf-manifest.json` is served by the backend as a static asset, so the host never needs to know the module's internal chunk layout.
- **Lazy loading** -- modules are fetched only when the user navigates to their route, keeping the initial bundle small.

## Bootstrap sequence

Before any module can be loaded, the host app resolves its runtime configuration:

1. **Fetch `/constants.json`** -- returns `{ gammaBaseURL }`, the management API root (e.g. `http://localhost:8083/gamma`).
2. **Fetch `{gammaBaseURL}/ui/bootstrap`** -- returns `{ gammaBaseURL, managementBaseURL, organizationId }` with the resolved Gamma URL, management URL and the current organization.

The resulting `BootstrapConfig` (`managementBaseURL`, `organizationId`, `gammaBaseURL`) is provided to the React tree via `BootstrapContext`.

**Source:** `src/bootstrap/bootstrap.service.ts`

## Module discovery

The `useGammaModules` hook fetches the module list and registers remotes.

### 1. Fetch the module list

```
GET {gammaBaseURL}/organizations/{organizationId}/modules
```

Returns `GammaModuleResponse[]`:

```ts
interface GammaModuleResponse {
    id: string;
    name: string;
    version: string;
    mfManifest: {
        name: string;
        exposes?: Array<{ name: string; [key: string]: unknown }>;
    };
}
```

### 2. Parse modules

`parseModule()` extracts the fields needed for Module Federation:

- **`remoteName`** -- `mfManifest.name` (the federated module name, e.g. `"gamma_module_foo"`).
- **`exposedModule`** -- the first entry in `mfManifest.exposes[].name`, with the leading `./` stripped. Falls back to `"Module"` if no exposes are declared.

### 3. Register remotes

Each parsed module is registered with `@module-federation/runtime`:

```ts
registerRemotes(
    parsed.map(m => ({
        name: m.remoteName,
        entry: `${gammaBaseURL}/organizations/${organizationId}/modules/${m.id}/assets/mf-manifest.json`,
    })),
    { force: true },
);
```

The `entry` points to the module's `mf-manifest.json` served by the backend. `force: true` allows re-registration if the hook re-runs.

**Source:** `src/gamma-modules/use-gamma-modules.ts`, `src/gamma-modules/gamma-module.ts`

## Module loading

`RemoteModuleRoute` renders a single remote module as a React component.

### Lazy component creation

`getOrCreateLazyModule(remoteName, exposedModule)` wraps `loadRemote()` from `@module-federation/runtime` inside `React.lazy()`:

```ts
React.lazy(async () => {
    const mod = await loadRemote<{ default: React.ComponentType }>(`${remoteName}/${exposedModule}`);
    if (!mod) throw new Error(`Failed to load remote module: ${remoteName}/${exposedModule}`);
    return mod;
});
```

Results are cached in a `Map<string, LazyExoticComponent>` keyed by `"remoteName/exposedModule"`, so each remote is only wrapped once.

When `loadRemote()` is called, the Module Federation runtime:

1. Fetches `mf-manifest.json` from the registered entry URL.
2. Resolves the exposed module's chunk URLs from the manifest.
3. Loads the JavaScript chunks and returns the module's default export.

**Source:** `src/gamma-modules/remote-module-loader.tsx`

## Routing

Dynamic routes are generated in `App` from the discovered modules:

```tsx
{
    modules.map(m => <Route key={m.id} path={`/${m.id}/*`} element={<RemoteModuleRoute module={m} />} />);
}
```

Each module gets a route at `/{moduleId}/*`:

- The `/*` wildcard allows the remote module to define its own sub-routes.
- Routes are nested inside a `<ProtectedRoute />` layout route (authentication guard).
- The entire `<Routes>` tree is wrapped in `<React.Suspense fallback={<p>Loading...</p>}>`, which shows the fallback while any lazy module is being fetched.

The home page (`/`) lists all discovered modules as navigation links.

**Source:** `src/app/app.tsx`

## Shared dependencies

`module-federation.config.ts` configures which libraries are shared between the host and remotes:

| Library            | Singleton | Strict version |
| ------------------ | --------- | -------------- |
| `react`            | Yes       | Yes            |
| `react-dom`        | Yes       | Yes            |
| `react-router-dom` | Yes       | Yes            |

All other libraries are **not shared** (`shared` callback returns `false`). This ensures the host and all remotes use the exact same instance of React and the router, avoiding context mismatches.

**Source:** `module-federation.config.ts`

## Request flow

```
Browser                          Host App                         Backend (Gamma API)
  |                                 |                                     |
  |--- GET /constants.json -------->|                                     |
  |<-- { baseURL } ----------------|                                     |
  |                                 |                                     |
  |--- GET {baseURL}/v2/ui/bootstrap ---------------------------------->|
  |<-- { baseURL, organizationId } ------------------------------------|
  |                                 |                                     |
  |            derive gammaBaseURL = baseURL.replace(/management, /gamma) |
  |                                 |                                     |
  |--- GET {gammaBaseURL}/organizations/{orgId}/modules --------------->|
  |<-- GammaModuleResponse[] -------------------------------------=-----|
  |                                 |                                     |
  |     parseModule() for each      |                                     |
  |     registerRemotes()           |                                     |
  |     render routes               |                                     |
  |                                 |                                     |
  | ---- user navigates to /{moduleId} ---                                |
  |                                 |                                     |
  |--- GET {gammaBaseURL}/.../modules/{id}/assets/mf-manifest.json ---->|
  |<-- mf-manifest.json ------------------------------------------------|
  |                                 |                                     |
  |--- GET {chunk URLs from manifest} --------------------------------->|
  |<-- JS chunks -------------------------------------------------------|
  |                                 |                                     |
  |     React.lazy resolves         |                                     |
  |     <RemoteModuleRoute> renders |                                     |
```

## Key files

| File                                         | Role                                                         |
| -------------------------------------------- | ------------------------------------------------------------ |
| `src/bootstrap/bootstrap.service.ts`         | Fetches runtime config, derives `gammaBaseURL`               |
| `src/bootstrap/bootstrap-context.tsx`        | React context providing `BootstrapConfig` to the tree        |
| `src/gamma-modules/gamma-module.ts`          | `GammaModuleResponse` / `GammaModule` types, `parseModule()` |
| `src/gamma-modules/use-gamma-modules.ts`     | `useGammaModules` hook -- fetches modules, registers remotes |
| `src/gamma-modules/remote-module-loader.tsx` | `getOrCreateLazyModule()`, `RemoteModuleRoute` component     |
| `src/app/app.tsx`                            | Root component with dynamic route generation                 |
| `module-federation.config.ts`                | Shared dependency configuration                              |
