export class MCP {
  enabled: boolean;
  tools: MCPTool[];
}

export class MCPTool {
  name?: string;
  description?: string;
  type: string;
  properties?: MCPToolProperty[];
  required?: string[];
}

export class MCPToolProperty {
  type: string;
  items?: Record<string, unknown>;
}
