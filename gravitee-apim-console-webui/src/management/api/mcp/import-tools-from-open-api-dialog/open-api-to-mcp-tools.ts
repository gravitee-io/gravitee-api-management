import yaml from 'js-yaml';
import SwaggerParser from '@apidevtools/swagger-parser';

type McpTool = {
  name: string;
  description: string;
  parameters: Record<string, any>;
};

type McpToolResult = {
  result: McpTool[];
  errors: string[];
};

function prefixPropertyNames(
  properties: Record<string, any>,
  prefix: string
): Record<string, any> {
  const newProps: Record<string, any> = {};
  for (const [key, value] of Object.entries(properties)) {
    newProps[`${prefix}${key}`] = value;
  }
  return newProps;
}

async function convertOpenApiToMCP(specString: string): Promise<McpToolResult> {
  let parsedSpec: any;

  try {
    parsedSpec = yaml.load(specString); // Works for YAML and JSON
  } catch (e) {
    return {
      result: [],
      errors: ['Failed to parse specification'],
    };
  }

  try {
    await SwaggerParser.validate(parsedSpec);
  } catch (e) {
    return {
      result: [],
      errors: ['Failed to validate OpenAPI spec: ' + (e as Error).message],
    };
  }

  let api: any;
  try {
    api = await SwaggerParser.dereference(parsedSpec);
  } catch (e) {
    return {
      result: [],
      errors: ['Failed to dereference OpenAPI spec: ' + (e as Error).message],
    };
  }

  const tools: McpTool[] = [];
  const errors: string[] = [];
  const usedNames = new Set<string>();

  for (const [path, pathItem] of Object.entries(api.paths)) {
    for (const [method, operation] of Object.entries(pathItem)) {
      const op = operation as any;
      const baseName = op.operationId || path.replace(/[\/{}]/g, '_');
      const toolName = `${method}_${baseName}`;

      if (usedNames.has(toolName)) {
        errors.push(`Duplicate tool name detected: ${toolName}`);
      } else {
        usedNames.add(toolName);
      }

      const description =
        op.summary || op.description || `API for ${method.toUpperCase()} ${path}`;

      const parametersSchema: any = {
        type: 'object',
        properties: {},
        required: [],
      };

      if (op.parameters) {
        for (const param of op.parameters) {
          const prefix =
            param.in === 'path'
              ? 'p_'
              : param.in === 'query'
                ? 'q_'
                : '';

          const name = `${prefix}${param.name}`;
          parametersSchema.properties[name] = {
            type: param.schema?.type || 'string',
            description: param.description || '',
          };

          if (param.required) {
            parametersSchema.required.push(name);
          }
        }
      }

      if (op.requestBody?.content?.['application/json']?.schema) {
        const bodySchema = op.requestBody.content['application/json'].schema;

        if (bodySchema.type === 'object' && bodySchema.properties) {
          const newProps = prefixPropertyNames(bodySchema.properties, 'b_');
          parametersSchema.properties = {
            ...parametersSchema.properties,
            ...newProps,
          };

          if (bodySchema.required) {
            for (const req of bodySchema.required) {
              parametersSchema.required.push(`b_${req}`);
            }
          }
        } else {
          parametersSchema.properties['b_body'] = bodySchema;
          parametersSchema.required.push('b_body');
        }
      }

      tools.push({
        name: toolName,
        description,
        parameters: parametersSchema,
      });
    }
  }

  return { result: tools, errors };
}

export { convertOpenApiToMCP, McpTool, McpToolResult };
