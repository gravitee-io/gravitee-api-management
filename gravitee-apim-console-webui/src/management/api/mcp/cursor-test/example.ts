import { readFileSync } from 'fs';

import { OpenAPIToMCPConverter } from './openapi-to-mcp';
import { MCPTool } from './types';

/**
 * Example usage of the OpenAPI to MCP converter
 */
export function convertOpenAPIToMCP(filePath: string): MCPTool[] {
  try {
    // Read the OpenAPI spec file
    const specContent = readFileSync(filePath, 'utf-8');

    // Create a new converter instance
    const converter = new OpenAPIToMCPConverter(specContent);

    // Convert the spec to MCP format
    const mcpTools = converter.convert();

    return mcpTools;
  } catch (error) {
    console.error('Error converting OpenAPI spec to MCP:', error);
    throw error;
  }
}

// Example usage:
// const mcpTools = convertOpenAPIToMCP('./petstore.yaml');
// console.log(JSON.stringify(mcpTools, null, 2)); 