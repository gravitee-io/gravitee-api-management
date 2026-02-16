/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { OpenAPIV3, OpenAPIV3_1 } from 'openapi-types';
import * as yaml from 'js-yaml';
import { dereference, validate } from '@scalar/openapi-parser';
import { isEmpty, snakeCase } from 'lodash';

import { MCPTool, MCPToolAnnotations, MCPToolDefinition, MCPToolGatewayMapping } from '../../../../../entities/entrypoint/mcp';

type OpenAPIObject = OpenAPIV3.Document | OpenAPIV3_1.Document;
type OperationObject = OpenAPIV3.OperationObject | OpenAPIV3_1.OperationObject;
type ParameterObject = OpenAPIV3.ParameterObject | OpenAPIV3_1.ParameterObject;
type RequestBodyObject = OpenAPIV3.RequestBodyObject | OpenAPIV3_1.RequestBodyObject;
type SchemaObject = OpenAPIV3.SchemaObject | OpenAPIV3_1.SchemaObject;
type ReferenceObject = OpenAPIV3.ReferenceObject | OpenAPIV3_1.ReferenceObject;
type ResponsesObject = OpenAPIV3.ResponsesObject | OpenAPIV3_1.ResponsesObject;
type ResponseObject = OpenAPIV3.ResponseObject | OpenAPIV3_1.ResponseObject;
type HeaderObject = OpenAPIV3.HeaderObject | OpenAPIV3_1.HeaderObject;

type JsonSchema = {
  type: 'object';
  properties: Record<string, SchemaObject>;
  required: string[];
};

type ParameterSchema = {
  pathParams: Record<string, SchemaObject>;
  queryParams: Record<string, SchemaObject>;
  headers: Record<string, SchemaObject>;
  required: string[];
};

type ResponseSchema = {
  bodySchema: SchemaObject | null;
  headers: Record<string, SchemaObject>;
};

type ErrorObject = {
  key: string;
  message: string;
};

export type OpenApiToMcpToolsResult = {
  result: MCPTool[];
  errors: ErrorObject[];
};

function transformSwagger2ToOpenApi3(parsedSpec: any): void {
  for (const pathItem of Object.values(parsedSpec.paths || {})) {
    for (const operation of Object.values(pathItem || {})) {
      if (!operation?.parameters) continue;

      const bodyParam = operation.parameters.find(p => p.in === 'body');
      if (bodyParam && 'schema' in bodyParam) {
        operation.requestBody = {
          content: {
            'application/json': {
              schema: bodyParam.schema,
            },
          },
        };
      }
    }
  }
  parsedSpec.openapi = '3.0.0';
  delete parsedSpec.swagger;
}

function determineContentType(operation: OperationObject): string | null {
  const requestBody = operation.requestBody as RequestBodyObject;
  const content = requestBody?.content;
  if (!content) return null;
  const types = Object.keys(content);
  return types.length > 0 ? types[0] : 'application/json';
}

function generateGatewayMapping(op: OperationObject, method: string, path: string, paramSchema: ParameterSchema): MCPToolGatewayMapping {
  const transformedPath = path.replace(/{([a-zA-Z0-9_-]+)}/g, ':$1');
  const contentType = determineContentType(op);

  return {
    http: {
      method: method.toUpperCase(),
      path: transformedPath,
      ...(!isEmpty(paramSchema.pathParams) ? { pathParams: Object.keys(paramSchema.pathParams) } : {}),
      ...(contentType ? { contentType } : {}),
      ...(!isEmpty(paramSchema.queryParams) ? { queryParams: Object.keys(paramSchema.queryParams) } : {}),
      ...(!isEmpty(paramSchema.headers) ? { headers: Object.keys(paramSchema.headers) } : {}),
    },
  };
}

function extractParameterSchema(parameters: (ParameterObject | ReferenceObject)[]): ParameterSchema {
  const pathParams: ParameterSchema['pathParams'] = {};
  const queryParams: ParameterSchema['queryParams'] = {};
  const headers: ParameterSchema['headers'] = {};
  const required: ParameterSchema['required'] = [];

  for (const param of parameters as ParameterObject[]) {
    const name = `${param.name}`;
    if (param.required) {
      required.push(name);
    }

    let property: SchemaObject;
    if (param.schema) {
      property = { ...(param.schema as SchemaObject), description: param.description };
    } else {
      property = { description: param.description };
    }

    switch (param.in) {
      case 'path':
        pathParams[name] = property;
        break;
      case 'query':
        queryParams[name] = property;
        break;
      case 'header':
        headers[name] = property;
        break;
      default:
        // Ignore other parameter types for now
        break;
    }
  }

  return {
    pathParams,
    queryParams,
    headers,
    required,
  };
}

function extractRequestBodySchema(requestBody?: RequestBodyObject | ReferenceObject): SchemaObject | null {
  if (!requestBody) return null;

  const typedRequestBody = requestBody as RequestBodyObject;
  const jsonSchema = typedRequestBody.content?.['application/json']?.schema as SchemaObject;
  if (!jsonSchema) return null;

  return jsonSchema;
}

function extractResponseSchema(responses?: ResponsesObject): ResponseSchema {
  const result: ResponseSchema = {
    bodySchema: null,
    headers: {},
  };

  if (!responses) return result;

  // Check 2xx status codes in order of preference (most common success codes with body)
  const successStatusCodes = ['200', '201', '202'];

  for (const statusCode of successStatusCodes) {
    const response = responses[statusCode] as ResponseObject | undefined;
    if (!response) continue;

    // Extract body schema
    const jsonSchema = response.content?.['application/json']?.schema as SchemaObject | undefined;
    if (jsonSchema && !result.bodySchema) {
      result.bodySchema = jsonSchema;
    }

    // Extract response headers
    if (response.headers) {
      for (const [headerName, headerDef] of Object.entries(response.headers)) {
        const header = headerDef as HeaderObject;
        if (header.schema && !result.headers[headerName]) {
          result.headers[headerName] = {
            ...(header.schema as SchemaObject),
            description: header.description,
          };
        }
      }
    }

    // If we found a body schema, use this response
    if (result.bodySchema) break;
  }

  return result;
}

function extractAnnotations(operation: OperationObject): MCPToolAnnotations | null {
  const op = operation as Record<string, unknown>;
  const annotations: MCPToolAnnotations = {};

  const annotationMapping: Partial<Record<keyof MCPToolAnnotations, { key: string; type: 'string' | 'boolean' }>> = {
    title: { key: 'x-mcp-title', type: 'string' },
    readOnlyHint: { key: 'x-mcp-readOnlyHint', type: 'boolean' },
    destructiveHint: { key: 'x-mcp-destructiveHint', type: 'boolean' },
    idempotentHint: { key: 'x-mcp-idempotentHint', type: 'boolean' },
    openWorldHint: { key: 'x-mcp-openWorldHint', type: 'boolean' },
  };

  for (const [prop, { key, type }] of Object.entries(annotationMapping)) {
    if (typeof op[key] === type) {
      (annotations as any)[prop] = op[key];
    }
  }

  return Object.keys(annotations).length > 0 ? annotations : null;
}

async function convertOpenApiToMcpTools(specString: string): Promise<OpenApiToMcpToolsResult> {
  let parsedSpec: OpenAPIObject;

  try {
    parsedSpec = yaml.load(specString) as OpenAPIObject;
  } catch {
    return { result: [], errors: [{ key: 'invalidFormat', message: 'Failed to parse specification' }] };
  }

  try {
    await validate(parsedSpec);
  } catch (e) {
    return { result: [], errors: [{ key: 'invalidSpec', message: (e as Error).message }] };
  }

  if (!parsedSpec) {
    return { result: [], errors: [] };
  }

  if ((parsedSpec as any).swagger === '2.0') {
    transformSwagger2ToOpenApi3(parsedSpec);
  }

  let api: OpenAPIObject;
  try {
    const { schema } = await dereference(parsedSpec);
    api = schema as OpenAPIObject;
  } catch (e) {
    return { result: [], errors: [{ key: 'invalidRefs', message: 'Failed to dereference OpenAPI spec: ' + (e as Error).message }] };
  }

  const tools: MCPTool[] = [];
  const errors: ErrorObject[] = [];
  const usedNames = new Set<string>();

  for (const [path, pathItem] of Object.entries(api.paths || {})) {
    for (const [method, operation] of Object.entries(pathItem || {})) {
      // if method is not a valid HTTP method, skip it
      if (!['get', 'post', 'put', 'delete', 'patch', 'options', 'head'].includes(method)) {
        continue;
      }

      const op = operation as OperationObject;
      if (!op) continue;

      const toolName = op.operationId || snakeCase(`${method}_${path}`);
      if (usedNames.has(toolName)) {
        errors.push({ key: 'duplicateName', message: `Duplicate tool name detected: ${toolName}` });
      } else {
        usedNames.add(toolName);
      }

      const description = op.summary || op.description || `API for ${method.toUpperCase()} ${path}`;
      const paramSchema = extractParameterSchema(op.parameters || []);
      const bodySchema = extractRequestBodySchema(op.requestBody);
      const responseSchema = extractResponseSchema(op.responses);
      const annotations = extractAnnotations(op);

      const inputSchema: JsonSchema = {
        type: 'object',
        properties: {
          ...paramSchema.pathParams,
          ...paramSchema.queryParams,
          ...paramSchema.headers,
          ...(bodySchema ? { bodySchema } : {}),
        },
        required: paramSchema.required,
      };

      // Build merged outputSchema (similar to inputSchema)
      let outputSchema: JsonSchema | null = null;
      if (responseSchema.bodySchema || Object.keys(responseSchema.headers).length > 0) {
        outputSchema = {
          type: 'object',
          properties: {
            ...responseSchema.headers,
            ...(responseSchema.bodySchema ? { bodySchema: responseSchema.bodySchema } : {}),
          },
          required: [],
        };
      }

      const toolDefinition: MCPToolDefinition = {
        name: toolName,
        description,
        inputSchema: inputSchema,
        ...(outputSchema ? { outputSchema } : {}),
        ...(annotations ? { annotations } : {}),
      };

      const gatewayMapping: MCPToolGatewayMapping = generateGatewayMapping(op, method, path, paramSchema);

      tools.push({
        toolDefinition,
        gatewayMapping,
      });
    }
  }

  return { result: tools, errors };
}

export { convertOpenApiToMcpTools };
