# ${api.name}

Welcome to the documentation for **${api.name}**.

<#if api.description?? && api.description?has_content>
${api.description}

</#if>
## API information

- Version: `${api.version!""}`
- Type: `${api.type!""}`
- Visibility: `${api.visibility!""}`
- Identifier: `${api.id}`

## Install this MCP server

<gmd-install-mcp name="${api.name}" transport="http" url="<#if api.entrypoints?? && (api.entrypoints?size > 0)>${api.entrypoints[0]}</#if><#if api.mcp?? && api.mcp.mcpPath??>${api.mcp.mcpPath}</#if>" />
