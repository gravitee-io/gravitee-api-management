import { Injectable } from '@angular/core';
import fs from 'fs';
import yaml from 'js-yaml';
import SwaggerParser from '@apidevtools/swagger-parser';

type MCPTool = {
  name: string;
  description: string;
  parameters: Record<string, any>;
};

@Injectable({
  providedIn: 'root'
})
export class ImportToolsFromOpenApiDialogService {

  constructor() { }

  /**
   * Converts an OpenAPI specification file to a list of MCP tools.
   * @param filePath - The path to the OpenAPI specification file.
   * @returns A promise that resolves to an array of MCPTool objects.
   */
  async convertOpenApiToMCP(filePath: string): Promise<MCPTool[]> {
    const fileContents = fs.readFileSync(filePath, 'utf8');
    const parsedYaml = yaml.load(fileContents);

    // Dereference and validate the OpenAPI document
    const api = await SwaggerParser.dereference(parsedYaml as any);

    const tools: MCPTool[] = [];

    if (!api.paths) {
      throw new Error('No paths found in the OpenAPI specification.');
    }

    for (const [path, pathItem] of Object.entries(api.paths)) {
      for (const [method, operation] of Object.entries(pathItem)) {
        const op = operation as any;

        const name = op.operationId || `${method}_${path.replace(/[\/{}]/g, '_')}`;
        const description = op.summary || op.description || `API endpoint for ${method.toUpperCase()} ${path}`;

        // Build parameters schema
        const parameters: any = {
          type: 'object',
          properties: {},
          required: [],
        };

        if (op.parameters) {
          for (const param of op.parameters) {
            parameters.properties[param.name] = {
              type: param.schema?.type || 'string',
              description: param.description || '',
            };
            if (param.required) {
              parameters.required.push(param.name);
            }
          }
        }

        // Handle requestBody (for POST/PUT)
        if (op.requestBody && op.requestBody.content) {
          const content = op.requestBody.content;
          const jsonContent = content['application/json'];
          if (jsonContent && jsonContent.schema) {
            const bodySchema = jsonContent.schema;
            parameters.properties['body'] = bodySchema;
            parameters.required.push('body');
          }
        }

        tools.push({
          name,
          description,
          parameters,
        });
      }
    }

    return tools;
  }
}
