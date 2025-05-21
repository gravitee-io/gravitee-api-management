import yaml from 'js-yaml';
import SwaggerParser from '@apidevtools/swagger-parser';

type HttpMethod = 'get' | 'post' | 'put' | 'delete' | 'patch' | 'head' | 'options';

interface Parameter {
  name: string;
  in: 'query' | 'path' | 'header' | 'cookie' | 'body';
  required?: boolean;
  description?: string;
  schema?: {
    type?: string;
    description?: string;
  };
}

interface RequestBody {
  content: {
    [contentType: string]: {
      schema: Schema;
    };
  };
}

interface Operation {
  operationId?: string;
  summary?: string;
  description?: string;
  parameters?: Parameter[];
  requestBody?: RequestBody;
}

interface PathItem {
  [method: string]: Operation | undefined;
}

interface Paths {
  [path: string]: PathItem;
}

interface Schema {
  type?: string;
  properties?: {
    [key: string]: Schema;
  };
  required?: string[];
  description?: string;
}

interface OpenApiSpec {
  openapi?: string;
  swagger?: string;
  info: {
    title: string;
    version: string;
  };
  paths: Paths;
  components?: {
    schemas?: {
      [name: string]: Schema;
    };
  };
}

type McpTool = {
  name: string;
  description: string;
  parameters: {
    type: 'object';
    properties: Record<string, Schema>;
    required: string[];
  };
};

type McpToolResult = {
  result: McpTool[];
  errors: string[];
};

function prefixPropertyNames(properties: Record<string, Schema>, prefix: string): Record<string, Schema> {
  const newProps: Record<string, Schema> = {};
  for (const [key, value] of Object.entries(properties)) {
    newProps[`${prefix}${key}`] = value;
  }
  return newProps;
}

function transformSwagger2ToOpenApi3(parsedSpec: OpenApiSpec): void {
  for (const pathItem of Object.values(parsedSpec.paths || {})) {
    for (const operation of Object.values(pathItem || {})) {
      if (!operation?.parameters) continue;

      const bodyParam = operation.parameters.find((p) => p.in === 'body');
      if (bodyParam && 'schema' in bodyParam) {
        operation.requestBody = {
          content: {
            'application/json': {
              schema: (bodyParam as any).schema,
            },
          },
        };
      }
    }
  }
  (parsedSpec as any).openapi = '3.0.0';
  delete (parsedSpec as any).swagger;
}

function determineContentType(operation: Operation): string | null {
  const content = operation.requestBody?.content;
  if (!content) return null;
  const types = Object.keys(content);
  return types.length > 0 ? types[0] : 'application/json';
}

function generateDescription(op: Operation, method: string, path: string): string {
  const descriptionText = op.summary || op.description || `API for ${method.toUpperCase()} ${path}`;
  const transformedPath = path.replace(/{(.*?)}/g, ':$1');
  const contentType = determineContentType(op);

  return JSON.stringify({
    description: descriptionText,
    http: {
      method: method.toUpperCase(),
      path: transformedPath,
      ...(contentType ? { contentType } : {}),
    },
  });
}

function extractParameterSchema(parameters: Parameter[]): McpTool['parameters'] {
  const schema: McpTool['parameters'] = {
    type: 'object',
    properties: {},
    required: [],
  };

  for (const param of parameters) {
    let prefix = '';
    switch (param.in) {
      case 'path':
        prefix = 'p_';
        break;
      case 'query':
        prefix = 'q_';
        break;
      case 'header':
        prefix = 'h_';
        break;
    }

    const name = `${prefix}${param.name}`;
    schema.properties[name] = {
      type: param.schema?.type || 'string',
      description: param.description || `Original header: ${param.name}`,
    };

    if (param.required) {
      schema.required.push(name);
    }
  }

  return schema;
}

function extractBodySchema(requestBody?: RequestBody): Partial<McpTool['parameters']> {
  const jsonSchema = requestBody?.content?.['application/json']?.schema;
  if (!jsonSchema) return {};

  const additionalProps: Record<string, Schema> = {};
  const required: string[] = [];

  if (jsonSchema.type === 'object' && jsonSchema.properties) {
    const newProps = prefixPropertyNames(jsonSchema.properties, 'b_');
    Object.assign(additionalProps, newProps);
    if (jsonSchema.required) {
      for (const r of jsonSchema.required) {
        required.push(`b_${r}`);
      }
    }
  } else {
    additionalProps['b_body'] = jsonSchema;
    required.push('b_body');
  }

  return {
    properties: additionalProps,
    required,
  };
}

async function convertOpenApiToMcpTools(specString: string): Promise<McpToolResult> {
  let parsedSpec: OpenApiSpec;

  try {
    parsedSpec = yaml.load(specString) as OpenApiSpec;
  } catch {
    return { result: [], errors: ['Failed to parse specification'] };
  }

  try {
    await SwaggerParser.validate(parsedSpec as any);
  } catch (e) {
    return { result: [], errors: [(e as Error).message] };
  }

  if (parsedSpec.swagger === '2.0') {
    transformSwagger2ToOpenApi3(parsedSpec);
  }

  let api: OpenApiSpec;
  try {
    api = await SwaggerParser.dereference(parsedSpec as any) as OpenApiSpec;
  } catch (e) {
    return { result: [], errors: ['Failed to dereference OpenAPI spec: ' + (e as Error).message] };
  }

  const tools: McpTool[] = [];
  const errors: string[] = [];
  const usedNames = new Set<string>();

  for (const [path, pathItem] of Object.entries(api.paths || {})) {
    for (const [method, operation] of Object.entries(pathItem || {})) {
      if (!operation) continue;

      const op = operation;
      const baseName = op.operationId || path.replace(/[\/{}]/g, '_');
      const toolName = `${method}_${baseName}`;

      if (usedNames.has(toolName)) {
        errors.push(`Duplicate tool name detected: ${toolName}`);
      } else {
        usedNames.add(toolName);
      }

      const description = generateDescription(op, method, path);

      // Parameter schema from query, path, header
      const paramSchema = extractParameterSchema(op.parameters || []);

      // Schema from request body
      const bodySchema = extractBodySchema(op.requestBody);

      // Combine
      const mergedParameters: McpTool['parameters'] = {
        type: 'object',
        properties: {
          ...paramSchema.properties,
          ...bodySchema.properties,
        },
        required: [...paramSchema.required, ...(bodySchema.required || [])],
      };

      tools.push({
        name: toolName,
        description,
        parameters: mergedParameters,
      });
    }
  }

  return { result: tools, errors };
}

export { convertOpenApiToMcpTools, McpTool, McpToolResult };
