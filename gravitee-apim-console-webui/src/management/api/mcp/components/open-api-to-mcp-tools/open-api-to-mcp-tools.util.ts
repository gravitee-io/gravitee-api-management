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
import { OpenAPIV3 } from 'openapi-types';
import * as yaml from 'js-yaml';
import SwaggerParser from '@apidevtools/swagger-parser';

type OpenAPIObject = OpenAPIV3.Document;
type OperationObject = OpenAPIV3.OperationObject;
type ParameterObject = OpenAPIV3.ParameterObject;
type RequestBodyObject = OpenAPIV3.RequestBodyObject;
type SchemaObject = OpenAPIV3.SchemaObject;
type ReferenceObject = OpenAPIV3.ReferenceObject;

type JsonSchema = {
  type: 'object';
  properties: Record<string, SchemaObject>;
  required: string[];
};

// TODO: Rename so that it's different from centralized entity
interface McpTool {
  name: string;
  description?: string;
  inputSchema: JsonSchema;
}

type McpToolResult = {
  result: McpTool[];
  errors: string[];
};

function prefixPropertyNames(properties: Record<string, SchemaObject>, prefix: string): Record<string, SchemaObject> {
  const newProps: Record<string, SchemaObject> = {};
  for (const [key, value] of Object.entries(properties)) {
    newProps[`${prefix}${key}`] = value as SchemaObject;
  }
  return newProps;
}

function transformSwagger2ToOpenApi3(parsedSpec: any): void {
  for (const pathItem of Object.values(parsedSpec.paths || {})) {
    for (const operation of Object.values(pathItem || {})) {
      if (!operation?.parameters) continue;

      const bodyParam = operation.parameters.find((p) => p.in === 'body');
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

function generateDescription(op: OperationObject, method: string, path: string): string {
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

function extractParameterSchema(parameters: (ParameterObject | ReferenceObject)[]): JsonSchema {
  const schema: JsonSchema = {
    type: 'object',
    properties: {},
    required: [],
  };

  for (const param of parameters as ParameterObject[]) {
    // if (!param.schema) continue;

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
    if (param.schema) {
      schema.properties[name] = { ...(param.schema as SchemaObject), description: param.description };
    } else {
      schema.properties[name] = { description: param.description };
    }

    if (param.required) {
      schema.required.push(name);
    }
  }

  return schema;
}

function extractBodySchema(requestBody?: RequestBodyObject | ReferenceObject): Partial<JsonSchema> {
  if (!requestBody) return {};

  const typedRequestBody = requestBody as RequestBodyObject;
  const jsonSchema = typedRequestBody.content?.['application/json']?.schema as SchemaObject;
  if (!jsonSchema) return {};

  const additionalProps: Record<string, SchemaObject> = {};
  const required: string[] = [];

  if (jsonSchema.type === 'object' && jsonSchema.properties) {
    const newProps = prefixPropertyNames(jsonSchema.properties as Record<string, SchemaObject>, 'b_');
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
  let parsedSpec: OpenAPIObject;

  try {
    parsedSpec = yaml.load(specString) as OpenAPIObject;
  } catch {
    return { result: [], errors: ['Failed to parse specification'] };
  }

  try {
    await SwaggerParser.validate(parsedSpec as any);
  } catch (e) {
    return { result: [], errors: [(e as Error).message] };
  }

  if ((parsedSpec as any).swagger === '2.0') {
    transformSwagger2ToOpenApi3(parsedSpec);
  }

  let api: OpenAPIObject;
  try {
    api = (await SwaggerParser.dereference(parsedSpec)) as OpenAPIObject;
  } catch (e) {
    return { result: [], errors: ['Failed to dereference OpenAPI spec: ' + (e as Error).message] };
  }

  const tools: McpTool[] = [];
  const errors: string[] = [];
  const usedNames = new Set<string>();

  for (const [path, pathItem] of Object.entries(api.paths || {})) {
    for (const [method, operation] of Object.entries(pathItem || {})) {
      const op = operation as OperationObject;
      if (!op) continue;

      const baseName = op.operationId || path.replace(/[/{}]/g, '_');
      const toolName = `${method}_${baseName}`;

      if (usedNames.has(toolName)) {
        errors.push(`Duplicate tool name detected: ${toolName}`);
      } else {
        usedNames.add(toolName);
      }

      const description = generateDescription(op, method, path);
      const paramSchema = extractParameterSchema(op.parameters || []);
      const bodySchema = extractBodySchema(op.requestBody);

      const mergedParameters: JsonSchema = {
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
        inputSchema: mergedParameters,
      });
    }
  }

  return { result: tools, errors };
}

export { convertOpenApiToMcpTools };
