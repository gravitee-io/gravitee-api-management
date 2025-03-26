export class MCP {
  enabled: boolean;
  tools: MCPTool[];
}

export class MCPTool {
  name?: string;
  description?: string;
  inputSchema?: MCPToolInputSchema;
}

export class MCPToolInputSchema {
  type: string;
  properties?: MCPToolProperties;
  required?: string[];
}

export class MCPToolProperties {
  [property: string]: unknown;
}
