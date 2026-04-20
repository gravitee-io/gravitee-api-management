# Gravitee Gamma — APIM module

HTTP API Management module for [Gravitee Gamma](../gravitee-gamma-plugin/gravitee-gamma-plugin-module/README.md): backend (Jersey) and React micro-frontend (Module Federation remote).

## Local development

Commands run from the **repository root** (Nx workspace).

1. Run the Management API on port 8083 with **`gamma.enabled: true`** in `gravitee.yml` (or `GAMMA_ENABLED=true`). See the **Gamma Console** section in [`CONTRIBUTING.adoc`](../../CONTRIBUTING.adoc#dev-guide-gamma-console).
2. Start this module’s UI dev server (port **3001**, hot reload):

    ```bash
    yarn nx serve gravitee-gamma-module-apim
    ```

3. Start the Gamma Console with a manifest override. The key must be the **plugin id** `apim` (see `src/main/resources/plugin.properties`), not the Nx project name:

    ```bash
    DEV_MODULE_ENTRIES="apim=http://localhost:3001/mf-manifest.json" yarn gamma-console:serve
    ```

For host architecture, module discovery, and routing (`/apim/*`), see [docs/gamma-module-loading.md](../gravitee-gamma-control-plane-webui/docs/gamma-module-loading.md).
