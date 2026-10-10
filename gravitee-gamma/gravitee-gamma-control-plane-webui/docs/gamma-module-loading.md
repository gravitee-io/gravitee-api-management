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

`registerModuleRemotes()` registers each parsed module with `@module-federation/runtime`:

```ts
registerRemotes(
    modules.map(m => ({
        name: m.remoteName,
        entry: DEV_MODULE_ENTRIES[m.id] ?? `${gammaBaseURL}/organizations/${organizationId}/modules/${m.id}/assets/mf-manifest.json`,
    })),
    { force: true },
);
```

The `entry` points to the module's `mf-manifest.json` served by the backend (or a local dev server URL when using `DEV_MODULE_ENTRIES`). `force: true` allows re-registration if the hook re-runs, and lets a retry register a module again (see [When loading fails](#when-loading-fails)).

**Source:** `src/features/modules/modules.remotes.ts`, `src/features/modules/hooks/useGammaModules.ts`, `src/features/modules/modules.types.ts`

## Module loading

`RemoteModuleRoute` renders a single remote module as a React component.

### Lazy component creation

`getOrCreateLazyModule(module)` wraps `loadRemoteModule()` inside `React.lazy()`. `loadRemoteModule()` calls `loadRemote()` from `@module-federation/runtime` and tries again while it fails (see [When loading fails](#when-loading-fails)):

```ts
React.lazy(() => loadRemoteModule(module, onStatus));
```

Results are cached in a `Map<string, LazyExoticComponent>` keyed by `"remoteName/exposedModule"`, so each remote is only wrapped once. A lazy component whose load failed is dropped from the cache, so opening the module again loads it again.

When `loadRemote()` is called, the Module Federation runtime:

1. Fetches `mf-manifest.json` from the registered entry URL.
2. Resolves the exposed module's chunk URLs from the manifest.
3. Loads the JavaScript chunks and returns the module's default export.

**Source:** `src/features/modules/components/RemoteModuleRoute.tsx`, `src/features/modules/modules.remotes.ts`

## When loading fails

During a rolling restart of the Management API, old and new pods answer together, so a module's manifest and the files it lists can come from two builds: a file the manifest names may not exist on the pod that answers (`Loading chunk N failed`, `remoteEntryExports is undefined`, `RUNTIME-008`).

### Retries

`loadRemoteModule()` tries again after 2, 5 and 10 seconds, then every 15 seconds: 15 attempts over about 3 minutes, longer than a restart. The 15 s cap opens the module soon after the platform is back. Before each new attempt, it registers the module again under its manifest URL plus a `?retry=<timestamp>` query, so the runtime fetches a fresh manifest: Module Federation 0.18 keeps the manifest it read for a URL, even through a forced registration. The module's files do not move, as their URLs are resolved from the manifest URL without the query.

Each failed attempt logs a `[Modules]` warning with the error, a retry that succeeds logs an info, and giving up logs an error.

### When the user is told

Most failures heal within one or two retries, so the console says nothing at first: a message shown for two seconds reads as an error, and the module replacing it reads as a reload. A failed attempt reports the load as delayed only when the next attempt is at least 10 seconds away, or when 10 seconds have passed since the load started. With the delays above, that is the third failed attempt, about 7 seconds in. From then on the next attempt always waits at least 10 seconds, so the message stays readable and never flashes. Then each attempt is reported (`attempting`), and so is the success (`ready`).

### What the user sees

`RemoteModuleRoute` keeps the shell and replaces only the module area:

| Moment                                                     | Shown                                                                                                                                                        |
| ---------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| First attempts, until the load is delayed                  | `ContentSkeleton`, as for any load                                                                                                                           |
| The load is delayed                                        | `ModuleUpdating`: "Agent Management isn't ready yet", saying the console keeps retrying and will open the module there, with an outline **Retry now** button |
| An attempt is running after the delay                      | The same message; the button reads **Retrying…** and does nothing                                                                                            |
| An attempt succeeds                                        | The module, in place of the message                                                                                                                          |
| Last attempt failed, or the module crashed while rendering | `ModuleUnavailable`: "Agent Management isn't available right now", with a primary **Reload page** button                                                     |

The app is named with `getModuleLabel()`. A crash while rendering is not retried. The messages never show the technical error, which stays in the browser console. The error boundary is keyed by module, and opening the module again after a failure starts a new load.

Each message's title is the page's `<h1>`, in place of the heading the module would have rendered. The final message tints its icon red, the color Graphene keeps for errors, while the waiting message stays neutral.

**Retry now** wakes the pending wait (`retryModuleNow()`) and runs the next attempt at once, with a fresh manifest. Its outline style is deliberate: the console keeps retrying by itself, so waiting is the expected path. The primary style is kept for the final message, where reloading is the only way forward; the change of style tells the user it is now up to them.

A status region that stays mounted announces the delay and the success to screen readers: they often skip a region that appears already filled. When the module replaces the message and the focus was on its button, the focus moves to that region instead of falling back to the top of the page.

### When the console cannot start

If the console's own code fails to load (`import('./bootstrap')` in `main.ts`) or `runApplicationBootstrap()` rejects, `showStartupError()` replaces the page with a plain DOM message, "The console couldn't start", and a **Reload page** button, and logs the error with a `[Startup]` prefix. It does not use React, which may be what failed to load. It needs no file of its own either, as any other file can fail during the same rolling update: its red alert icon is drawn inline, it shows no logo (Graphene's `Logo` is an image file), and inline styles keep it centered and readable if the console's stylesheet failed too.

**Source:** `src/features/modules/modules.remotes.ts`, `src/features/modules/components/RemoteModuleRoute.tsx`, `src/features/modules/components/ModuleUpdating.tsx`, `src/features/modules/components/ModuleUnavailable.tsx`, `src/startup-error.ts`

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
- `ShellLayout` provides the sidebar with an app switcher (to navigate between modules), a content header with breadcrumbs, and layout slots via `@gravitee/graphene-core`'s `LayoutSlotsProvider`.
- `RouteLayout` provides breadcrumb and navigation slots for host-level pages.
- Each module route renders its module inside its own `Suspense` and error boundary, so loading or failing never replaces the shell (see [When loading fails](#when-loading-fails)).

The home page (`/`) lists all discovered modules as navigation links.

**Source:** `src/app/AppRoutes.tsx`, `src/shared/components/ShellLayout.tsx`

## Shared dependencies

`module-federation.config.ts` configures which libraries are shared between the host and remotes:

| Library                   | Singleton | Strict version |
| ------------------------- | --------- | -------------- |
| `react`                   | Yes       | Yes            |
| `react-dom`               | Yes       | Yes            |
| `react-router-dom`        | Yes       | Yes            |
| `zustand`                 | Yes       | Yes            |
| `@gravitee/graphene-core` | Yes       | Yes            |

All other libraries are **not shared** (`shared` callback returns `false`). This ensures the host and all remotes use the exact same instance of React, the router, the state manager, and the design system, avoiding context mismatches.

**Source:** `module-federation.config.ts`

## Development mode

The `DEV_MODULE_ENTRIES` environment variable allows overriding module manifest URLs to point to local dev servers. This is useful when developing a module locally alongside the host.

Format: comma-separated `id=url` pairs. The `id` must match each module’s plugin id (the `id` field in that module’s `plugin.properties`, and the same value returned by the modules API), not the Nx project name—for example the APIM module uses plugin id `apim` while its Nx project is `gravitee-gamma-module-apim`.

```bash
DEV_MODULE_ENTRIES="apim=http://localhost:3001/mf-manifest.json"
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

| File                                                    | Role                                                                             |
| ------------------------------------------------------- | -------------------------------------------------------------------------------- |
| `src/shared/config/bootstrap.store.ts`                  | Zustand store for runtime config (`BootstrapConfig`)                             |
| `src/features/modules/modules.types.ts`                 | `GammaModuleResponse` / `GammaModule` types, `parseModule()`                     |
| `src/features/modules/hooks/useGammaModules.ts`         | `useGammaModules` hook -- fetches modules, registers remotes                     |
| `src/features/modules/modules.remotes.ts`               | `registerModuleRemotes()`, `loadRemoteModule()` with retries, `retryModuleNow()` |
| `src/features/modules/components/RemoteModuleRoute.tsx` | `getOrCreateLazyModule()`, `RemoteModuleRoute` component                         |
| `src/features/modules/components/ModuleUpdating.tsx`    | Message once a module's load is delayed                                          |
| `src/features/modules/components/ModuleUnavailable.tsx` | Message when a module cannot be shown                                            |
| `src/startup-error.ts`                                  | `showStartupError()`, the page shown when the console cannot start               |
| `src/app/AppRoutes.tsx`                                 | Root routes with dynamic module route generation                                 |
| `src/shared/components/ShellLayout.tsx`                 | App shell with sidebar, app switcher, and layout slots                           |
| `src/bootstrap.tsx`                                     | App initialization, store setup, React root                                      |
| `module-federation.config.ts`                           | Shared dependency configuration                                                  |
