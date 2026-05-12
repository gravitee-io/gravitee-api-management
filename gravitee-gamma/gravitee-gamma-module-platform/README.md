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
