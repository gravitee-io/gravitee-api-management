import { OpenAPIV2, OpenAPIV3 } from 'openapi-types';
import { load } from 'js-yaml';

import { MCPTool, MCPEndpoint, MCPParameter, MCPRequestBody, OpenAPISpec, Schema } from './types';

export class OpenAPIToMCPConverter {
  private spec: OpenAPISpec;
  private isOpenAPIV3: boolean;

  constructor(specContent: string) {
    try {
      // Try parsing as JSON first
      this.spec = JSON.parse(specContent);
    } catch {
      // If JSON parsing fails, try YAML
      this.spec = load(specContent) as OpenAPISpec;
    }
    
    this.isOpenAPIV3 = this.detectOpenAPIVersion();
  }

  private detectOpenAPIVersion(): boolean {
    const version = (this.spec as any).openapi || (this.spec as any).swagger;
    return version && version.startsWith('3');
  }

  private isReferenceObject(obj: any): obj is OpenAPIV3.ReferenceObject {
    return obj && '$ref' in obj;
  }

  private prefixPropertyNames(properties: Record<string, Schema>, prefix: string): Record<string, Schema> {
    const newProps: Record<string, Schema> = {};
    for (const [key, value] of Object.entries(properties)) {
      newProps[`${prefix}${key}`] = value;
    }
    return newProps;
  }

  private generateDescription(operation: OpenAPIV2.OperationObject | OpenAPIV3.OperationObject, method: string, path: string): string {
    const descriptionText = operation.summary || operation.description || `API for ${method.toUpperCase()} ${path}`;
    const transformedPath = path.replace(/{(.*?)}/g, ':$1');
    const contentType = this.determineContentType(operation);

    return JSON.stringify({
      description: descriptionText,
      http: {
        method: method.toUpperCase(),
        path: transformedPath,
        ...(contentType ? { contentType } : {}),
      },
    });
  }

  private determineContentType(operation: OpenAPIV2.OperationObject | OpenAPIV3.OperationObject): string | null {
    if (this.isOpenAPIV3) {
      const v3Op = operation as OpenAPIV3.OperationObject;
      const content = v3Op.requestBody && !this.isReferenceObject(v3Op.requestBody) 
        ? v3Op.requestBody.content 
        : null;
      if (!content) return null;
      const types = Object.keys(content);
      return types.length > 0 ? types[0] : 'application/json';
    } else {
      const v2Op = operation as OpenAPIV2.OperationObject;
      const bodyParam = v2Op.parameters?.find(p => (p as OpenAPIV2.Parameter).in === 'body');
      return bodyParam ? 'application/json' : null;
    }
  }

  private extractParameterSchema(parameters: (OpenAPIV2.Parameter | OpenAPIV3.ParameterObject)[]): MCPTool['parameters'] {
    const schema: MCPTool['parameters'] = {
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
      if (this.isOpenAPIV3) {
        const v3Param = param as OpenAPIV3.ParameterObject;
        if (v3Param.schema && !this.isReferenceObject(v3Param.schema)) {
          schema.properties[name] = {
            type: v3Param.schema.type || 'string',
            description: v3Param.description || `Original parameter: ${param.name}`,
          };
        }
      } else {
        const v2Param = param as OpenAPIV2.Parameter;
        schema.properties[name] = {
          type: (v2Param as any).type || 'string',
          description: v2Param.description || `Original parameter: ${param.name}`,
        };
      }

      if (param.required) {
        schema.required.push(name);
      }
    }

    return schema;
  }

  private extractBodySchema(
    operation: OpenAPIV2.OperationObject | OpenAPIV3.OperationObject
  ): Partial<MCPTool['parameters']> {
    let jsonSchema: Schema | undefined;

    if (this.isOpenAPIV3) {
      const v3Op = operation as OpenAPIV3.OperationObject;
      if (v3Op.requestBody && !this.isReferenceObject(v3Op.requestBody)) {
        jsonSchema = v3Op.requestBody.content?.['application/json']?.schema as Schema;
      }
    } else {
      const v2Op = operation as OpenAPIV2.OperationObject;
      const bodyParam = v2Op.parameters?.find(p => (p as OpenAPIV2.Parameter).in === 'body') as OpenAPIV2.Parameter;
      if (bodyParam?.schema) {
        jsonSchema = bodyParam.schema as Schema;
      }
    }

    if (!jsonSchema) return {};

    const additionalProps: Record<string, Schema> = {};
    const required: string[] = [];

    if (jsonSchema.type === 'object' && jsonSchema.properties) {
      const newProps = this.prefixPropertyNames(jsonSchema.properties, 'b_');
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

  private convertParameter(param: OpenAPIV2.Parameter | OpenAPIV3.ParameterObject): MCPParameter {
    const base = {
      name: param.name,
      required: param.required || false,
      description: param.description,
    };

    if (this.isOpenAPIV3) {
      const v3Param = param as OpenAPIV3.ParameterObject;
      if (v3Param.schema && !this.isReferenceObject(v3Param.schema)) {
        return {
          ...base,
          type: v3Param.schema.type || 'string',
          schema: v3Param.schema,
          enum: v3Param.schema.enum,
        };
      }
    } else {
      const v2Param = param as OpenAPIV2.Parameter;
      return {
        ...base,
        type: (v2Param as any).type || 'string',
        enum: (v2Param as any).enum,
        schema: (v2Param as any).schema,
      };
    }

    return {
      ...base,
      type: 'string',
    };
  }

  private convertRequestBody(
    body: OpenAPIV3.RequestBodyObject | OpenAPIV2.Parameter
  ): MCPRequestBody | undefined {
    if (this.isOpenAPIV3) {
      const v3Body = body as OpenAPIV3.RequestBodyObject;
      const content: MCPRequestBody['content'] = {};
      
      Object.entries(v3Body.content || {}).forEach(([mediaType, mediaTypeObj]) => {
        content[mediaType] = {
          schema: mediaTypeObj.schema || {},
        };
      });

      return {
        required: v3Body.required || false,
        content,
      };
    } else {
      const v2Body = body as OpenAPIV2.Parameter;
      if (v2Body.in === 'body') {
        return {
          required: v2Body.required || false,
          content: {
            'application/json': {
              schema: v2Body.schema || {},
            },
          },
        };
      }
    }
    return undefined;
  }

  private convertPath(
    path: string,
    pathItem: OpenAPIV2.PathItemObject | OpenAPIV3.PathItemObject
  ): MCPEndpoint[] {
    const endpoints: MCPEndpoint[] = [];
    const methods = ['get', 'post', 'put', 'delete', 'patch', 'options', 'head'] as const;

    for (const method of methods) {
      const operation = pathItem[method];
      if (!operation) continue;

      const parameters: MCPParameter[] = [];
      
      // Handle path parameters
      if (pathItem.parameters) {
        parameters.push(
          ...(pathItem.parameters as any[]).map(p => this.convertParameter(p))
        );
      }

      // Handle operation parameters
      if (operation.parameters) {
        parameters.push(
          ...(operation.parameters as any[]).map(p => this.convertParameter(p))
        );
      }

      let requestBody: MCPRequestBody | undefined;
      if (this.isOpenAPIV3) {
        const v3Op = operation as OpenAPIV3.OperationObject;
        if (v3Op.requestBody) {
          requestBody = this.convertRequestBody(
            this.isReferenceObject(v3Op.requestBody)
              ? { content: {} } as OpenAPIV3.RequestBodyObject
              : v3Op.requestBody
          );
        }
      } else {
        const bodyParam = (operation.parameters as OpenAPIV2.Parameter[])?.find(
          p => p.in === 'body'
        );
        if (bodyParam) {
          requestBody = this.convertRequestBody(bodyParam);
        }
      }

      const convertedResponses = this.isOpenAPIV3
        ? this.convertV3Responses(operation.responses as OpenAPIV3.ResponsesObject)
        : this.convertV2Responses(operation.responses as OpenAPIV2.ResponsesObject);

      const endpoint: MCPEndpoint = {
        path,
        method,
        operationId: operation.operationId,
        summary: operation.summary,
        description: operation.description,
        parameters,
        requestBody,
        responses: convertedResponses,
        security: operation.security,
      };

      endpoints.push(endpoint);
    }

    return endpoints;
  }

  private convertV3Responses(
    responses: OpenAPIV3.ResponsesObject
  ): MCPEndpoint['responses'] {
    const convertedResponses: MCPEndpoint['responses'] = {};
    
    for (const [statusCode, response] of Object.entries(responses)) {
      if (this.isReferenceObject(response)) {
        convertedResponses[statusCode] = {
          description: 'Reference response',
        };
        continue;
      }

      const content: { [key: string]: { schema: any } } = {};
      if (response.content) {
        Object.entries(response.content).forEach(([mediaType, mediaTypeObj]) => {
          content[mediaType] = {
            schema: mediaTypeObj.schema || {},
          };
        });
      }

      convertedResponses[statusCode] = {
        description: response.description,
        content: Object.keys(content).length > 0 ? content : undefined,
      };
    }

    return convertedResponses;
  }

  private convertV2Responses(
    responses: OpenAPIV2.ResponsesObject
  ): MCPEndpoint['responses'] {
    const convertedResponses: MCPEndpoint['responses'] = {};
    
    for (const [statusCode, response] of Object.entries(responses)) {
      if (this.isReferenceObject(response)) {
        convertedResponses[statusCode] = {
          description: 'Reference response',
        };
        continue;
      }

      convertedResponses[statusCode] = {
        description: response.description,
        content: response.schema
          ? {
              'application/json': {
                schema: response.schema,
              },
            }
          : undefined,
      };
    }

    return convertedResponses;
  }

  public convert(): MCPTool[] {
    const tools: MCPTool[] = [];
    const paths = this.isOpenAPIV3
      ? (this.spec as OpenAPIV3.Document).paths
      : (this.spec as OpenAPIV2.Document).paths;

    for (const [path, pathItem] of Object.entries(paths)) {
      const methods = ['get', 'post', 'put', 'delete', 'patch', 'options', 'head'] as const;

      for (const method of methods) {
        const operation = pathItem[method];
        if (!operation) continue;

        const baseName = operation.operationId || path.replace(/[\/{}]/g, '_');
        const toolName = `${method}_${baseName}`;
        const description = this.generateDescription(operation, method, path);

        // Parameter schema from query, path, header
        const paramSchema = this.extractParameterSchema(operation.parameters || []);

        // Schema from request body
        const bodySchema = this.extractBodySchema(operation);

        // Combine schemas
        const mergedParameters: MCPTool['parameters'] = {
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

    return tools;
  }
} 