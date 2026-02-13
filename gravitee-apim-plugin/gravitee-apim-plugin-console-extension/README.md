# Console Extensions

Console extensions extend the Gravitee Console with self-contained features, bundled as standard Gravitee plugin ZIPs. Drop the ZIP into the plugins folder, start the platform, and the feature appears — no rebuild, no redeployment of the console itself.

Extensions come in two flavors:

- **UI extensions** include both a backend (Java/JAX-RS) and a frontend (any JS framework). They ship a `manifest.json` that declares custom elements, placement, and menu entries. The console loads the entrypoint script and mounts the components.
- **Backend-only extensions** provide a servlet without any UI assets. They implement `ConsoleExtensionServletFactory` to register a servlet at a dedicated path. The console ignores them (no manifest to load).

## Why Console Extensions?

Parts of the Gravitee Console have always been tightly coupled to the main application: alerts, audit logs, analytics dashboards. Any change requires rebuilding and redeploying the whole console. Console extensions decouple these features so they can be developed, versioned, and shipped independently — just like policies or resources already are for the gateway.

For example, the alerts system could have been provided as a console extension: a backend that manages alert rules and evaluates conditions, paired with a UI that renders the configuration screens and alert history. Instead of living inside the monorepo, it would ship as a single ZIP.

## What Console Extensions Can Do

Three proof-of-concept extensions validate the breadth of the approach. Together, they cover every capability the framework was designed to support:

| Capability | Validated by |
|---|---|
| Overlay widgets that float above the page | Chat |
| Full-page views indistinguishable from built-in pages | OTel |
| Sidebar menu integration with icons | OTel |
| Backend-only extensions (no UI) | MCP |
| Cross-extension collaboration | Chat + MCP |
| In-extension navigation with browser back/forward | OTel |
| Event-driven console refresh | Chat |
| Framework independence (Angular 19, 21, or none) | Chat, OTel |
| CSS theming via custom properties across Shadow DOM | OTel |
| Third-party library bundling with classloader isolation | Chat (LangChain4J), MCP (MCP SDK) |
| Dynamic protocol support (MCP over StreamableHTTP) | MCP |
| Zero-rebuild tool evolution from OpenAPI specs | MCP |
| CSRF token reuse from console session | Chat |
| Role-based permission enforcement | Chat |

### The Three Experimentations

- **[Chat](https://github.com/gravitee-io/gravitee-apim-plugin-console-chat)** — An AI chatbot overlay in the bottom-right corner. Users ask the assistant to create, deploy, and start APIs through natural language. The assistant confirms the plan, executes actions via MCP tools, and the console refreshes automatically through `gio:extension-event`. Uses Angular 19, LangChain4J, and Azure OpenAI.

- **[OpenTelemetry](https://github.com/gravitee-io/gravitee-apim-console-extension-otel)** — A distributed trace viewer that appears as a sidebar menu entry. Renders trace waterfalls with span details, in-extension navigation, and browser back/forward — all visually indistinguishable from a built-in console page. Uses Angular 21 with no backend (frontend-only, mock data).

- **[MCP](https://github.com/gravitee-io/gravitee-apim-console-extension-mcp)** — A backend-only extension that exposes an MCP server at `/ai`. AI assistants (Claude, ChatGPT, custom agents) call tools that are derived automatically from APIM's OpenAPI specifications — when APIM adds a new endpoint with `x-mcp: enabled`, it becomes available to AI assistants without rebuilding the MCP extension.

### Possibilities

Console extensions are not limited to these three experiments. The framework supports a wide range of future extensions:

- **Dashboard widgets** — placement `top`/`bottom`/`left`/`right` for real-time metric panels alongside existing pages
- **API documentation editor** — center placement with in-extension routing for a rich Markdown/OpenAPI editor
- **Approval workflows** — overlay for pending deployment approvals with notification badges
- **Cost analytics** — center placement with charts, powered by a backend aggregating gateway metrics
- **Connector marketplace** — center placement browsing and installing community connectors
- **Custom alerts UI** — overlay or center, replacing the built-in alerts with a richer experience

## Architecture

### UI Extensions

A UI console extension ZIP contains:

```
gravitee-console-extension-example-1.0.0.zip
├── gravitee-console-extension-example-1.0.0.jar   # Backend (plugin.properties + JAX-RS resource)
└── ui/
    ├── manifest.json          # Declares components, placement, menu entries
    ├── example-extension.js   # Compiled frontend (IIFE-wrapped)
    └── ...                    # Additional assets (SVGs, fonts, etc.)
```

### Backend-Only Extensions

A backend-only console extension ZIP contains:

```
gravitee-console-extension-mcp-1.0.0.zip
├── gravitee-console-extension-mcp-1.0.0.jar   # Backend (plugin.properties + servlet factory)
└── lib/
    └── *.jar                                   # Runtime dependencies
```

There is no `ui/` directory and no `manifest.json`. The main class implements `ConsoleExtensionServletFactory`:

```java
public class McpConsoleExtensionServletFactory implements ConsoleExtensionServletFactory {

    @Override
    public HttpServlet createServlet(ApplicationContext parentContext) {
        // Build and return an HttpServlet
    }

    @Override
    public String getServletPath() {
        return "/ai";
    }

    @Override
    public Class<?>[] getSpringConfigClasses() {
        return new Class<?>[] { McpServerConfiguration.class };
    }
}
```

The handler detects that the main class implements `ConsoleExtensionServletFactory` and registers a servlet factory instead of a JAX-RS resource. At startup, `JettyEmbeddedContainer` mounts each servlet factory at its declared path.

### Discovery and Endpoints

When the REST API starts, `ConsoleExtensionHandler` discovers each extension, registers its backend resource class (or servlet factory), and makes its UI assets available through a dedicated endpoint:

```
GET  /v2/extensions/                                        → list extensions + manifests
GET  /v2/extensions/{id}/assets/{path}                      → serve UI assets
*    /v2/extensions/{id}/{path}                              → extension backend (no env context)
*    /v2/extensions/environments/{envId}/{id}/{path}         → extension backend (with env context + permissions)
```

The console fetches the extension list at startup, filters out extensions without a manifest, loads each entrypoint script, and mounts the declared custom elements into the appropriate placement slots.

## Manifest

Each UI extension declares its components in `ui/manifest.json`:

```json
{
  "entrypoint": "chat-extension.js",
  "components": [
    {
      "tagName": "gio-chat-plugin",
      "placement": "overlay"
    }
  ]
}
```

### Placement

The console layout has several slots where extension components can be mounted:

| Placement | Description | Example |
|-----------|-------------|---------|
| `overlay` | Floating layer on top of the page | Chat widget (bottom-right FAB + panel) |
| `center`  | Main content area, replacing the default view | OTel trace viewer (full-page experience) |
| `top`     | Above the main content | — |
| `bottom`  | Below the main content | — |
| `left`    | Left of the main content | — |
| `right`   | Right of the main content | — |

### Menu Items

A component can declare a `label` and `icon` to appear as a sidebar menu item:

```json
{
  "tagName": "gio-otel-plugin",
  "placement": "center",
  "label": "OpenTelemetry",
  "icon": "gio:bar-chart-2"
}
```

This adds a clickable entry in the sidebar that activates the extension view. Exact ordering within the menu is not yet configurable.

## Framework Independence

Extensions register [Web Components](https://developer.mozilla.org/en-US/docs/Web/API/Web_components) (custom elements), so any JavaScript framework — or none — can be used:

- **Chat** uses Angular 19
- **OTel** uses Angular 21
- Vanilla JS or React would work just as well

There is no version coupling between the console application and its extensions. The console only interacts with the standard `customElements.define()` API.

## Styles and Theming

The console exposes CSS custom properties that extensions can consume:

```css
:host {
    font-family: var(--gio-font-body, 'Manrope', sans-serif);
    color: var(--gio-color-text, #1e1b1b);
}

.primary-button {
    background: var(--gio-color-primary, #da3b00);
}
```

Available variables include `--gio-color-primary`, `--gio-color-surface`, `--gio-color-background`, `--gio-color-text`, `--gio-color-border`, `--gio-color-error`, `--gio-font-body`, among others. Because these are CSS custom properties, they cross Shadow DOM boundaries — extensions pick up theme changes without rebuilding.

## Asset Loading

Extensions can bundle static assets (SVGs, images, fonts) alongside their JavaScript entrypoint. Assets are served from the extension's dedicated endpoint and accessed at runtime through a per-extension base URL provided by the console loader:

```typescript
// Provided as a global map before any extension script loads
window.__GRAVITEE_CONSOLE_EXTENSION_ASSETS__ = {
  'chat': '/management/v2/extensions/chat/assets',
  'otel': '/management/v2/extensions/otel/assets'
};
```

Extensions read their entry from this map (typically injected via a DI token) and reference assets with relative paths:

```typescript
const iconUrl = `${this.assetBase}/bot-icon.svg`;
```

This works transparently behind reverse proxies because the base URL is derived from the bootstrap endpoint, which respects `X-Forwarded-*` headers.

## In-Plugin Navigation

Extensions can implement internal navigation (e.g., from a trace list to a span detail view). The OTel extension demonstrates this with Angular Router:

- Defines routes (`traces`, `span/:traceId/:spanId`)
- Provides a custom `ExtensionLocationStrategy` to keep routing scoped within the extension
- Browser back/forward buttons work as expected

Navigation must remain progressive — it happens within the extension's custom element, not by taking over the console's router.

## Communication with the Console

Extensions communicate with the console through a simple event bus built on `window` custom events:

```typescript
// Extension dispatches an event
window.dispatchEvent(new CustomEvent('gio:extension-event', {
    detail: { type: 'API', action: 'created', resourceId: 'api-123' }
}));
```

```typescript
// Console subscribes and reacts
consoleExtensionEventsService.on('API').subscribe(event => {
    if (event.action === 'created') {
        // refresh API list
    }
});
```

This provides loose coupling: extensions don't import console code, and the console doesn't import extension code. The event contract is the only shared interface.

## Security and Permissions

Console extensions reuse the v2 management API permission mechanism. The `ConsoleExtensionApplication` registers the same `SecurityContextFilter`, `PermissionsFilter`, and `GraviteeContextResponseFilter` used by the management API, so extensions can annotate their JAX-RS resource methods with `@Permissions` to enforce role-based access control.

### Environment-Scoped Permissions

Permissions with ENVIRONMENT scope (e.g., `ENVIRONMENT_API`) require the environment ID to be present in the request path. The `GraviteeContextFilter` (a Spring Security filter) extracts the environment ID by parsing the path for `/environments/{envId}` segments.

To use environment-scoped permissions, extensions must be called through the environment-scoped path:

```
*    /v2/extensions/environments/{envId}/{id}/{path}
```

The non-environment path (`/v2/extensions/{id}/{path}`) remains available but does not support environment-scoped permission checks.

### Frontend Context

The console exposes the current organization and environment IDs via a window global that extensions can read:

```typescript
window.__GRAVITEE_CONSOLE_CONTEXT__
// { orgId: "org-id", envId: "env-id" }
```

This global is updated each time the user switches environments.

### Usage

Add a dependency on `gravitee-apim-rest-api-rest` (provided scope) and annotate methods:

```java
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;

@POST
@Path("/completions")
@Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.CREATE }) })
public Response complete(String body) { ... }
```

The `PermissionsFilter` reads the annotation, resolves the permission scope (ENVIRONMENT, API, etc.), and delegates to `PermissionService.hasPermission()`. Unauthenticated or unauthorized requests receive a **403 Forbidden** response.

Methods without a `@Permissions` annotation remain open to any authenticated user (the `SecurityContextFilter` still runs).

## Building a Console Extension

### UI Extension — Project Structure

```
gravitee-console-extension-example/
├── pom.xml
├── src/main/
│   ├── assembly/plugin-assembly.xml
│   ├── java/.../ExampleConsoleExtension.java    # JAX-RS resource
│   └── resources/
│       ├── plugin.properties
│       └── ui/manifest.json
└── ui-src/                                      # Frontend sources
    ├── package.json
    ├── angular.json                             # (if using Angular)
    └── src/
        └── main.ts                              # Registers custom element
```

### Backend-Only Extension — Project Structure

```
gravitee-console-extension-mcp/
├── pom.xml
├── src/main/
│   ├── assembly/plugin-assembly.xml
│   └── java/.../McpConsoleExtensionServletFactory.java   # Implements ConsoleExtensionServletFactory
│   └── resources/
│       └── plugin.properties
```

### Plugin Properties

```properties
id=example
name=Example
type=console-extension
version=${project.version}
description=An example console extension
class=io.gravitee.console.extension.example.ExampleConsoleExtension
```

### Build

Everything is orchestrated by Maven. For UI extensions, the POM uses `exec-maven-plugin` to run `npm install` and `npm run build` during the `generate-resources` phase, then `maven-assembly-plugin` packages the ZIP:

```bash
# Full build (backend + frontend + ZIP)
mvn clean install

# Skip tests for faster iteration
mvn clean install -DskipTests -Dskip.validation=true
```

During development, the frontend can be built independently:

```bash
cd ui-src
npm install
npm run build
```

The `postbuild` npm script wraps the compiled JavaScript in an IIFE (to prevent global scope pollution) and copies it along with any static assets to `target/classes/ui/`, where the assembly descriptor picks them up.

### Deployment

Copy the extension ZIP to the REST API's plugins directory and restart:

```bash
cp target/gravitee-console-extension-example-*.zip \
   /path/to/gravitee-apim-rest-api-standalone-distribution/plugins/
```
