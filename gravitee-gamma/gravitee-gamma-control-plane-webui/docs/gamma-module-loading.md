# Gamma Module Loading Architecture

## Overview

Gamma modules are micro-frontends loaded at runtime via [Module Federation](https://module-federation.io/). The host application (Gamma Control Plane WebUI) discovers available modules from the backend, dynamically registers them as Module Federation remotes, and renders them as lazy-loaded React components within protected routes.

Key design decisions:

- **Dynamic remote registration** -- remotes are not known at build time; they are registered at runtime after fetching the module list from the API.
- **Manifest-driven discovery** -- each module's `mf-manifest.json` is served by the backend as a static asset, so the host never needs to know the module's internal chunk layout.
- **Lazy loading** -- modules are fetched only when the user navigates to their route, keeping the initial bundle small.

## Bootstrap sequence

Before any module can be loaded, the host app resolves its runtime configuration:

1. **Fetch `/constants.json`** -- returns `{ gammaBaseURL }`, the Gamma API root (e.g. `http://localhost:8083/gamma`).
2. **Fetch `{gammaBaseURL}/ui/bootstrap`** -- returns `{ gammaBaseURL, managementBaseURL, organizationId }` with the resolved Gamma URL, management URL and the current organization.
3. **Initialize authentication** -- the auth store is initialized so the user session is ready before rendering.

The resulting `BootstrapConfig` (`managementBaseURL`, `organizationId`, `gammaBaseURL`) is stored in a Zustand store accessible throughout the app via `useBootstrapStore`.

**Source:** `src/shared/config/bootstrap.store.ts`, `src/bootstrap.tsx`

## Module discovery

The `useGammaModules` hook fetches the module list and registers remotes. It depends on the bootstrap config and the authenticated user -- modules are only fetched once the user is logged in.

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
        entry:
            DEV_MODULE_ENTRIES[m.id] ??
            `${gammaBaseURL}/organizations/${organizationId}/modules/${m.id}/assets/mf-manifest.json`,
    })),
    { force: true },
);
```

The `entry` points to the module's `mf-manifest.json` served by the backend (or a local dev server URL when using `DEV_MODULE_ENTRIES`). `force: true` allows re-registration if the hook re-runs.

**Source:** `src/features/modules/hooks/useGammaModules.ts`, `src/features/modules/modules.types.ts`

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

**Source:** `src/features/modules/components/RemoteModuleRoute.tsx`

## Routing

Dynamic routes are generated in `AppRoutes` from the discovered modules:

```tsx
<Routes>
    <Route element={<PublicOnlyRoute />}>
        <Route path="/login" element={<LoginPage />} />
    </Route>
    <Route element={<ProtectedRoute />}>
        <Route element={<ShellLayout modules={modules} />}>
            <Route element={<RouteLayout />}>
                <Route path="/" element={<HomePage />} />
                <Route path="/about" element={<AboutPage />} />
            </Route>
            {modules.map(m => (
                <Route key={m.id} path={`/${m.id}/*`} element={<RemoteModuleRoute module={m} />} />
            ))}
        </Route>
    </Route>
</Routes>
```

Each module gets a route at `/{moduleId}/*`:

- The `/*` wildcard allows the remote module to define its own sub-routes.
- Routes are nested inside `<ProtectedRoute />` (authentication guard) and `<ShellLayout />` (app shell).
- `ShellLayout` provides the sidebar with an app switcher (to navigate between modules), a content header with breadcrumbs, and layout slots via `@gravitee/graphene`'s `LayoutSlotsProvider`.
- `RouteLayout` provides breadcrumb and navigation slots for host-level pages.
- The entire tree is wrapped in `<React.Suspense fallback={<div>Loading...</div>}>` at the root, which shows the fallback while any lazy module is being fetched.

The home page (`/`) lists all discovered modules as navigation links.

**Source:** `src/app/AppRoutes.tsx`, `src/shared/components/ShellLayout.tsx`

## Shared dependencies

`module-federation.config.ts` configures which libraries are shared between the host and remotes:

| Library            | Singleton | Strict version |
| ------------------ | --------- | -------------- |
| `react`            | Yes       | Yes            |
| `react-dom`        | Yes       | Yes            |
| `react-router-dom` | Yes       | Yes            |
| `zustand`          | Yes       | Yes            |
| `@gravitee/graphene` | Yes     | Yes            |

All other libraries are **not shared** (`shared` callback returns `false`). This ensures the host and all remotes use the exact same instance of React, the router, the state manager, and the design system, avoiding context mismatches.

**Source:** `module-federation.config.ts`

## Development mode

The `DEV_MODULE_ENTRIES` environment variable allows overriding module manifest URLs to point to local dev servers. This is useful when developing a module locally alongside the host.

Format: comma-separated `id=url` pairs:

```bash
DEV_MODULE_ENTRIES="gamma-module-apim=http://localhost:3001/mf-manifest.json"
```

When set, the hook uses the provided URL instead of the backend-served manifest for matching module IDs.

**Source:** `src/features/modules/hooks/useGammaModules.ts`

## Request flow

```
Browser                          Host App                         Backend (Gamma API)
  |                                 |                                     |
  |--- GET /constants.json -------->|                                     |
  |<-- { gammaBaseURL } ------------|                                     |
  |                                 |                                     |
  |--- GET {gammaBaseURL}/ui/bootstrap --------------------------------->|
  |<-- { gammaBaseURL, managementBaseURL, organizationId } --------------|
  |                                 |                                     |
  |     initialize auth store       |                                     |
  |     render AppRoutes            |                                     |
  |                                 |                                     |
  |--- GET {gammaBaseURL}/organizations/{orgId}/modules --------------->|
  |<-- GammaModuleResponse[] -------------------------------------------|
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

| File                                                    | Role                                                           |
| ------------------------------------------------------- | -------------------------------------------------------------- |
| `src/shared/config/bootstrap.store.ts`                  | Zustand store for runtime config (`BootstrapConfig`)           |
| `src/features/modules/modules.types.ts`                 | `GammaModuleResponse` / `GammaModule` types, `parseModule()`  |
| `src/features/modules/hooks/useGammaModules.ts`         | `useGammaModules` hook -- fetches modules, registers remotes  |
| `src/features/modules/components/RemoteModuleRoute.tsx` | `getOrCreateLazyModule()`, `RemoteModuleRoute` component      |
| `src/app/AppRoutes.tsx`                                 | Root routes with dynamic module route generation               |
| `src/shared/components/ShellLayout.tsx`                 | App shell with sidebar, app switcher, and layout slots         |
| `src/bootstrap.tsx`                                     | App initialization, store setup, React root                    |
| `module-federation.config.ts`                           | Shared dependency configuration                                |
