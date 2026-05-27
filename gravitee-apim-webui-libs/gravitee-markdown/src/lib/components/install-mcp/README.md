# Install MCP Component

`gmd-install-mcp` renders one-click installer actions and copyable configuration snippets for supported MCP clients.

## Supported clients

- Cursor
- VS Code
- Claude Desktop

## Usage

### Explicit configuration

```html
<gmd-install-mcp name="weather" url="https://api.example.com/mcp" clients="cursor,vscode,claude-desktop" />
```

### Local stdio server

```html
<gmd-install-mcp name="weather" transport="stdio" command="npx" args='["-y","@acme/weather-mcp"]' clients="cursor,vscode,claude-desktop" />
```

### Missing configuration

If the required inputs are missing, the component renders a placeholder instead of installer actions.

## Inputs

| Input       | Description                                               |
| ----------- | --------------------------------------------------------- |
| `name`      | MCP server name used in generated client configurations   |
| `transport` | MCP transport: `http`, `sse`, or `stdio`                  |
| `url`       | Remote MCP endpoint URL for `http` and `sse` transports   |
| `headers`   | JSON object of headers for remote transports              |
| `command`   | Executable used to start a stdio MCP server               |
| `args`      | JSON array of stdio command arguments                     |
| `env`       | JSON object of environment variables for stdio transports |
| `clients`   | Comma-separated list of installer ids to display          |

## Theming

Use the `@gmd.install-mcp-overrides()` mixin from the library SCSS public API to customize the component tokens.
