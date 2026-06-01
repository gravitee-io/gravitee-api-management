# ${api.name}

Connect AI assistants and agents to **${api.name}** through the Model Context Protocol (MCP). This server is published via the Gravitee API gateway, giving you secure, managed access to tools and resources.

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

## Install in your AI client

Add this MCP server to Cursor, VS Code, or Claude Desktop with one click. The configuration uses the gateway endpoint for this API.

<gmd-install-mcp name="${api.name}" transport="http" url="<#if api.entrypoints?? && (api.entrypoints?size > 0)>${api.entrypoints[0]}</#if><#if api.mcp?? && api.mcp.mcpPath??>${api.mcp.mcpPath}</#if>" />

## What you can do

<gmd-grid columns="3">
    <gmd-card class="overview-card">
        <gmd-card-title>Tools and resources</gmd-card-title>
        <gmd-md>
            Discover MCP tools, prompts, and resources exposed by this server once connected.
        </gmd-md>
    </gmd-card>
    <gmd-card class="overview-card">
        <gmd-card-title>Secure access</gmd-card-title>
        <gmd-md>
            Traffic is routed through the Gravitee gateway with the same policies, authentication, and rate limits as your other APIs.
        </gmd-md>
    </gmd-card>
    <gmd-card class="overview-card">
        <gmd-card-title>Subscribe</gmd-card-title>
        <gmd-md>
            Request a plan to obtain credentials before connecting. Manage access from your application dashboard.
        </gmd-md>
    </gmd-card>
</gmd-grid>

## Customize this page

Describe the tools this server exposes, expected use cases, and any setup steps your consumers should follow.

- List available MCP tools and when to use them
- Document authentication or environment requirements
- **[Secure MCP proxy with Gravitee APIM](https://documentation.gravitee.io/apim/ai-agent-management/secure-mcp-proxy-with-oauth2)**

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
