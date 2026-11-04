# Gravitee Gamma — Platform module

Platform module for [Gravitee Gamma](../gravitee-gamma-plugin/gravitee-gamma-plugin-module/README.md): backend (Jersey) and React micro-frontend (Module Federation remote).

## Local development

Commands run from the **repository root** (Nx workspace).

1. Run the Management API on port 8083 with **`gamma.enabled: true`** in `gravitee.yml` (or `GAMMA_ENABLED=true`). See the **Gamma Console** section in [`CONTRIBUTING.adoc`](../../CONTRIBUTING.adoc#dev-guide-gamma-console).
2. Start this module’s UI dev server (port **3002**, hot reload):

    ```bash
    yarn nx serve gravitee-gamma-module-platform
    ```

3. Start the Gamma Console with a manifest override. The key must be the **plugin id** `platform` (see `src/main/resources/plugin.properties`), not the Nx project name:

    ```bash
    DEV_MODULE_ENTRIES="platform=http://localhost:3002/mf-manifest.json" yarn gamma-console:serve
    ```

For host architecture, module discovery, and routing (`/platform/*`), see [docs/gamma-module-loading.md](../gravitee-gamma-control-plane-webui/docs/gamma-module-loading.md).

## Metrics configuration (Environment › System & Security)

`environment/metrics-configuration` mounts the `AdditionalMetricsCatalog` of `@gravitee/gamma-lib-observability` on the Gamma API of the current environment (`{gammaBaseURL}/organizations/{orgId}/environments/{envId}/observability/metric-definitions`), with the console HTTP options from `@gravitee/gamma-ui-shared/api` and the base URL from `@gravitee/gamma-ui-shared/observability`. The catalog saves per sheet: the page has no Save bar.

| Right of the reader                                                                                       | What the page shows                                                         |
| --------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------- |
| `environment-settings-r`                                                                                  | The nav item and the table, read-only                                       |
| `environment-settings-u`                                                                                  | Declare a metric, edit and delete a record, toggle Exposed                  |
| A traffic reader (`ENVIRONMENT_PLATFORM[READ]`, or API analytics / log read on an API of the environment) | Discovered rows and "Last seen" too                                         |
| An environment-settings admin without a traffic right                                                     | Built-in and declared rows only, no "Last seen": the server leaves them out |

The library and `@gravitee/graphene-charts` are bundled with this module, not shared through Module Federation: `@tanstack/react-query` is shared by no module, and a library copy served by another module (the APIM module's, once its Observability pages were opened) runs on that module's react-query and does not see this module's `QueryClientProvider`. Share them again once `@tanstack/react-query` is a shared singleton in the shell and in every module that mounts the library (AIM included): [OBS-191](https://gravitee.atlassian.net/browse/OBS-191).

The catalog's `docsUrl` is omitted on purpose: no documentation page exists for additional metrics yet, so its empty state shows no "Learn more" link.
