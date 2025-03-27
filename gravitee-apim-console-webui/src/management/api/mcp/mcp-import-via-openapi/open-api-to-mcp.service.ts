import { Injectable } from '@angular/core';
import {OpenAPIV3} from "openapi-types";
import {MCPTool} from "../../../../entities/management-api-v2";
import * as yaml from "js-yaml";

@Injectable({
  providedIn: 'root'
})
export class OpenAPIToMCPService {
  private yamlSchema = yaml.DEFAULT_SCHEMA.extend([]);

  public parseOpenAPISpecToTools(openAPIString: string): MCPTool[] {
    if (!openAPIString) {
      return [];
    }
    const loaded = yaml.load(openAPIString, {schema: this.yamlSchema});
    const spec = loaded as OpenAPIV3.Document;
    const tools: MCPTool[] = [];
    // Convert each OpenAPI path to an MCP tool
    for (const [path, pathItem] of Object.entries(spec.paths)) {
      if (!pathItem) continue;

      for (const [method, operation] of Object.entries(pathItem)) {
        if (method === "parameters" || method === "servers" || !operation) continue;

        const op = operation as OpenAPIV3.OperationObject;
        // Create a clean tool ID by removing the leading slash and replacing special chars
        const cleanPath = path.replace(/^\//, "");
        const toolId = `${method.toUpperCase()}-${cleanPath}`.replace(
          /[^a-zA-Z0-9-]/g,
          "-",
        );
        console.error(`Registering tool: ${toolId}`); // Debug logging
        const tool: MCPTool = {
          name:
            op.operationId || `${method.toUpperCase()}_${path}`,
          description:
            op.description ||
            `Make a ${method.toUpperCase()} request to ${path}`,
          inputSchema: {
            type: "object",
            properties: {},
            // Add any additional properties from OpenAPI spec
          },
        };

        // Store the mapping between name and ID for reverse lookup
        // console.error(`Registering tool: ${toolId} (${tool.name})`);

        // Add parameters from operation
        if (op.parameters) {
          for (const param of op.parameters) {
            if ("name" in param && "in" in param) {
              const paramSchema = param.schema as OpenAPIV3.SchemaObject;
              tool.inputSchema.properties[param.name] = {
                type: paramSchema.type || "string",
                description: param.description || `${param.name} parameter`,
              };
              if (param.required) {
                tool.inputSchema.required = tool.inputSchema.required || [];
                tool.inputSchema.required.push(param.name);
              }
            }
          }
        }
        tools.push(tool);
      }
    }
    return tools;
  }
}
