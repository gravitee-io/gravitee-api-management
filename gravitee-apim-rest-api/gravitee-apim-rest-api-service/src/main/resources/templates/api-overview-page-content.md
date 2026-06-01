# ${api.name}

Discover **${api.name}** through the Gravitee Developer Portal. Subscribe to a plan, explore interactive documentation, and connect your applications through a secure API gateway.

<#if api.description?? && api.description?has_content>
${api.description}

</#if>
<gmd-card class="overview-info">
    <gmd-card-title>API information</gmd-card-title>
    <gmd-md>
        - **Version:** `${api.version!""}`
        - **Visibility:** `${api.visibility!""}`
<#if api.primaryOwner?? && api.primaryOwner.displayName?? && api.primaryOwner.displayName?has_content>
        - **Owner:** ${api.primaryOwner.displayName}
</#if>
<#if api.deployedAt??>
        - **Last deployed:** ${api.deployedAt?string("yyyy-MM-dd")}
</#if>
    </gmd-md>
</gmd-card>

## Get started

<gmd-grid columns="3">
    <gmd-card class="overview-card">
        <gmd-card-title>Subscribe</gmd-card-title>
        <gmd-md>
            Request access to a plan that fits your integration. Manage API keys and credentials from your application dashboard.
        </gmd-md>
    </gmd-card>
    <gmd-card class="overview-card">
        <gmd-card-title>Explore documentation</gmd-card-title>
        <gmd-md>
            Browse endpoints, request schemas, and response examples. Try requests directly from the portal when available.
        </gmd-md>
    </gmd-card>
    <gmd-card class="overview-card">
        <gmd-card-title>Integrate</gmd-card-title>
        <gmd-md>
            Call the gateway endpoint with your preferred SDK or HTTP client. Check the subscription page for authentication details.
        </gmd-md>
    </gmd-card>
</gmd-grid>

## Customize this page

Use this overview to introduce your API, highlight key use cases, and guide consumers to the resources they need most.

- Add a **Quick start** section with example requests
- Link to related guides or changelogs
- **[Discover the Gravitee Developer Portal](https://documentation.gravitee.io/apim/developer-portal/new-developer-portal)**

<style>
  .overview-info {
    --gmd-card-container-color: color-mix(in srgb, var(--gio-app-primary-main-color, #32329f) 8%, transparent);
    --gmd-card-text-color: inherit;
    --gmd-card-outline-color: color-mix(in srgb, var(--gio-app-primary-main-color, #32329f) 24%, transparent);
    --gmd-card-outline-width: 1px;
    --gmd-card-container-shape: 12px;
    margin-bottom: 1.5rem;
  }

  .overview-info gmd-card-title {
    color: var(--gio-app-primary-main-color, #32329f);
  }

  .overview-card {
    --gmd-card-container-color: var(--mat-sys-surface-container-lowest, #ffffff);
    --gmd-card-text-color: inherit;
    --gmd-card-outline-color: color-mix(in srgb, var(--gio-app-primary-main-color, #32329f) 12%, var(--mat-sys-outline-variant, #e2e8f0));
    --gmd-card-outline-width: 1px;
    --gmd-card-container-shape: 10px;
  }

  .overview-card gmd-card-title {
    color: var(--gio-app-primary-main-color, #32329f);
  }
</style>
