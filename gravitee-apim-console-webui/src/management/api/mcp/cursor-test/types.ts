import { OpenAPIV2, OpenAPIV3 } from 'openapi-types';

export interface Schema {
  type?: string;
  properties?: {
    [key: string]: Schema;
  };
  required?: string[];
  description?: string;
  format?: string;
  items?: Schema;
  enum?: any[];
}

export interface MCPParameter {
  name: string;
  type: string;
  description?: string;
  required: boolean;
  default?: any;
  enum?: any[];
  schema?: any;
}

export interface MCPRequestBody {
  required: boolean;
  content: {
    [mediaType: string]: {
      schema: any;
    };
  };
}

export interface MCPEndpoint {
  path: string;
  method: string;
  operationId?: string;
  summary?: string;
  description?: string;
  parameters: MCPParameter[];
  requestBody?: MCPRequestBody;
  responses: {
    [statusCode: string]: {
      description: string;
      content?: {
        [mediaType: string]: {
          schema: any;
        };
      };
    };
  };
  security?: any[];
}

export interface MCPTool {
  name: string;
  description: string;
  parameters: {
    type: 'object';
    properties: Record<string, Schema>;
    required: string[];
  };
}

export type OpenAPISpec = OpenAPIV2.Document | OpenAPIV3.Document; 