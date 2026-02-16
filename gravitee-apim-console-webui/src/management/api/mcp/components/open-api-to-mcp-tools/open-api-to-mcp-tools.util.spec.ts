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
import { dereference, validate } from '@scalar/openapi-parser';

import { convertOpenApiToMcpTools } from './open-api-to-mcp-tools.util';

import { MCPToolGatewayMapping } from '../../../../../entities/entrypoint/mcp';

// Cast the imported functions as jest.Mock to access mock methods
const mockValidate = validate as jest.Mock;
const mockDereference = dereference as jest.Mock;

describe('convertOpenApiToMCP', () => {
  beforeEach(() => {
    // Reset all mocks before each test
    jest.clearAllMocks();

    // Set up default mock implementations
    mockValidate.mockResolvedValue(true);
    mockDereference.mockImplementation(async spec => ({ schema: spec }));
  });

  describe('Success cases', () => {
    const yamlSpec = `
openapi: 3.0.0
info:
  title: Sample API
  version: 1.0.0
paths:
  /user/{id}:
    server:
      - url: https://api.example.com
    get:
      operationId: getUser
      summary: Get user by ID
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
        - name: verbose
          in: query
          schema:
            type: boolean
        - name: X-Custom-Header
          in: header
          schema:
            type: string
          description: A custom header for the request
      responses:
        '200':
          description: Successful response
          content:
            application/json:
              schema:
                type: object
                properties:
                  id:
                    type: string
                  username:
                    type: string
                  email:
                    type: string
  /user:
    post:
      summary: Create a new user
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                username:
                  type: string
                email:
                  type: string
              required: [username, email]
      responses:
        '201':
          description: User created successfully
`;

    const jsonSpec = JSON.stringify({
      openapi: '3.0.0',
      info: { title: 'Sample API', version: '1.0.0' },
      paths: {
        '/user/{id}': {
          get: {
            operationId: 'getUser',
            summary: 'Get user by ID',
            parameters: [
              {
                name: 'id',
                in: 'path',
                required: true,
                schema: { type: 'string' },
              },
              {
                name: 'verbose',
                in: 'query',
                schema: { type: 'boolean' },
              },
              {
                name: 'X-Custom-Header',
                in: 'header',
                schema: { type: 'string' },
                description: 'A custom header for the request',
              },
            ],
            responses: {
              '200': {
                description: 'Successful response',
                content: {
                  'application/json': {
                    schema: {
                      type: 'object',
                      properties: {
                        id: { type: 'string' },
                        username: { type: 'string' },
                        email: { type: 'string' },
                      },
                    },
                  },
                },
              },
            },
          },
        },
        '/user': {
          post: {
            summary: 'Create a new user',
            requestBody: {
              content: {
                'application/json': {
                  schema: {
                    type: 'object',
                    properties: {
                      username: { type: 'string' },
                      email: { type: 'string' },
                    },
                    required: ['username', 'email'],
                  },
                },
              },
            },
            responses: {
              '201': {
                description: 'User created successfully',
              },
            },
          },
        },
      },
    });

    const swaggerSpec = `
swagger: '2.0'
info:
  title: Swagger Sample API
  version: 1.0.0
paths:
  /user/{id}:
    get:
      operationId: getUser
      summary: Get user by ID
      parameters:
        - name: id
          in: path
          required: true
          type: string
        - name: verbose
          in: query
          type: boolean
        - name: X-Custom-Header
          in: header
          type: string
          description: A custom header for the request
      responses:
        200:
          description: Successful response
          schema:
            type: object
            properties:
              id:
                type: string
              username:
                type: string
              email:
                type: string
  /user:
    post:
      summary: Create a new user
      parameters:
        - name: body
          in: body
          required: true
          schema:
            type: object
            properties:
              username:
                type: string
              email:
                type: string
            required: [username, email]
      responses:
        201:
          description: User created successfully
`;

    const openapi31Spec = `
openapi: 3.1.0
info:
  title: OpenAPI 3.1 Sample API
  version: 1.0.0
paths:
  /user/{id}:
    get:
      operationId: getUser
      summary: Get user by ID
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
        - name: verbose
          in: query
          schema:
            type: boolean
        - name: X-Custom-Header
          in: header
          schema:
            type: string
          description: A custom header for the request
      responses:
        '200':
          description: Successful response
          content:
            application/json:
              schema:
                type: object
                properties:
                  id:
                    type: string
                  username:
                    type: string
                  email:
                    type: string
  /user:
    post:
      summary: Create a new user
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                username:
                  type: string
                email:
                  type: string
              required: [username, email]
      responses:
        '201':
          description: User created successfully
`;

    const cases: Array<[string, string]> = [
      ['OpenAPI YAML', yamlSpec],
      ['OpenAPI JSON', jsonSpec],
      ['Swagger 2.0', swaggerSpec],
      ['OpenAPI 3.1', openapi31Spec],
    ];

    it.each(cases)('converts %s with proper prefixes and names', async (_label, input) => {
      const { result, errors } = await convertOpenApiToMcpTools(input);

      expect(errors).toHaveLength(0);
      expect(result).toHaveLength(2);

      const [getTool, postTool] = result;

      // Tool Definition

      // Check the GET method (getUser)
      expect(getTool.toolDefinition.name).toBe('getUser');
      const getToolProperties = getTool.toolDefinition.inputSchema['properties'];
      expect(getToolProperties).toHaveProperty('id');
      expect(getToolProperties).toHaveProperty('verbose');
      expect(getToolProperties).toHaveProperty('X-Custom-Header'); // Check if header is serialized
      expect(getToolProperties['X-Custom-Header'].description).toBe('A custom header for the request'); // Check description

      // Check the POST method (create user)
      expect(postTool.toolDefinition.name).toEqual('post_user');

      const postToolProperties = postTool.toolDefinition.inputSchema['properties'];
      expect(postToolProperties).toHaveProperty('bodySchema');
      expect(postToolProperties['bodySchema'].properties).toHaveProperty('username');
      expect(postToolProperties['bodySchema'].properties).toHaveProperty('email');

      // Verify that the mock functions were called
      expect(mockValidate).toHaveBeenCalled();
      expect(mockDereference).toHaveBeenCalled();
    });

    it.each([
      [
        'OpenAPI 3.0 with request body',
        `
openapi: 3.0.0
info:
  title: Test API
  version: 1.0.0
paths:
  /user:
    post:
      summary: Create user
      requestBody:
        content:
          application/json:
            schema:
              type: object
              required: [name]
              properties:
                name:
                  type: string
      responses:
        '201':
          description: Created
`,
        {
          method: 'POST',
          path: '/user',
          contentType: 'application/json',
          summary: 'Create user',
        },
      ],
      [
        'OpenAPI 3.0 with no request body',
        `
openapi: 3.0.0
info:
  title: Test API
  version: 1.0.0
paths:
  /status:
    get:
      summary: Get status
      responses:
        '200':
          description: OK
`,
        {
          method: 'GET',
          path: '/status',
          contentType: null,
          summary: 'Get status',
        },
      ],
      [
        'Swagger 2.0 with body parameter',
        `
swagger: '2.0'
info:
  title: Swagger Test
  version: 1.0.0
paths:
  /upload/{fileId}:
    put:
      summary: Upload file
      parameters:
        - name: fileId
          in: path
          required: true
          type: string
        - name: body
          in: body
          required: true
          schema:
            type: object
            properties:
              fileName:
                type: string
            required: [fileName]
      responses:
        204:
          description: No content
`,
        {
          method: 'PUT',
          path: '/upload/:fileId',
          pathParams: ['fileId'],
          contentType: 'application/json',
          summary: 'Upload file',
        },
      ],
      [
        'Swagger 2.0 with no body parameter',
        `
swagger: '2.0'
info:
  title: Swagger Test
  version: 1.0.0
paths:
  /ping:
    get:
      summary: Ping service
      parameters: []
      responses:
        200:
          description: Pong
`,
        {
          method: 'GET',
          path: '/ping',
          contentType: null,
          summary: 'Ping service',
        },
      ],
      [
        'OpenAPI 3.1 with null type',
        `
openapi: 3.1.0
info:
  title: Test API
  version: 1.0.0
paths:
  /upload/{fileId}:
    put:
      summary: Upload file
      parameters:
        - name: fileId
          in: path
          required: true
          schema:
            type: string
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                fileName:
                  type: [string, "null"]
              required: [fileName]
      responses:
        204:
          description: No content
`,
        {
          method: 'PUT',
          path: '/upload/:fileId',
          pathParams: ['fileId'],
          contentType: 'application/json',
          summary: 'Upload file',
        },
      ],
      [
        'OpenAPI 3.1 with multiple content types',
        `
openapi: 3.1.0
info:
  title: Test API
  version: 1.0.0
paths:
  /upload:
    post:
      summary: Upload data
      requestBody:
        content:
          'application/json':
            schema:
              type: object
              properties:
                data:
                  type: string
          'application/xml':
            schema:
              type: object
              properties:
                data:
                  type: string
      responses:
        200:
          description: Success
`,
        {
          method: 'POST',
          path: '/upload',
          contentType: 'application/json',
          summary: 'Upload data',
        },
      ],
      [
        'OpenAPI 3.1 with webhooks',
        `
openapi: 3.1.0
info:
  title: Test API
  version: 1.0.0
webhooks:
  newPet:
    post:
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                id:
                  type: integer
                name:
                  type: string
      responses:
        '200':
          description: Success
paths:
  /pets:
    get:
      summary: List pets
      responses:
        '200':
          description: Success
`,
        {
          method: 'GET',
          path: '/pets',
          contentType: null,
          summary: 'List pets',
        },
      ],
      [
        'OpenAPI 3.0 with query, header parameters',
        `
openapi: 3.0.0
info:
  title: Test API
  version: 1.0.0
paths:
  /user/{userId}:
    get:
      summary: Get user
      parameters:
        - name: userId
          in: path
          required: true
          schema:
            type: string
        - name: verbose
          in: query
          schema:
            type: boolean
        - name: X-Custom-Header
          in: header
          schema:
            type: string
          description: A custom header for the request
      responses:
        '200':
          description: Successful response
`,
        {
          method: 'GET',
          path: '/user/:userId',
          pathParams: ['userId'],
          summary: 'Create user',
          queryParams: ['verbose'],
          headers: ['X-Custom-Header'],
        },
      ],
    ])('creates gateway mapping for %s', async (_label, spec, expected: MCPToolGatewayMapping['http']) => {
      const { result, errors } = await convertOpenApiToMcpTools(spec);
      expect(errors).toHaveLength(0);
      expect(result.length).toBeGreaterThan(0);

      const toolGatewayMapping = result[0].gatewayMapping;

      expect(toolGatewayMapping.http.method).toEqual(expected.method);
      expect(toolGatewayMapping.http.path).toEqual(expected.path);
      expect(toolGatewayMapping.http.pathParams).toEqual(expected.pathParams);
      expect(toolGatewayMapping.http.queryParams).toEqual(expected.queryParams);
      expect(toolGatewayMapping.http.headers).toEqual(expected.headers);

      if (expected.contentType) {
        expect(toolGatewayMapping.http.contentType).toEqual(expected.contentType);
      } else {
        expect(toolGatewayMapping.http).not.toHaveProperty('contentType');
      }

      // Verify that the mock functions were called
      expect(mockValidate).toHaveBeenCalled();
      expect(mockDereference).toHaveBeenCalled();
    });
  });

  describe('outputSchema extraction', () => {
    it('extracts schema from 200 response with application/json content', async () => {
      const spec = JSON.stringify({
        openapi: '3.0.0',
        info: { title: 'Test API', version: '1.0.0' },
        paths: {
          '/user': {
            get: {
              operationId: 'getUser',
              summary: 'Get user',
              responses: {
                '200': {
                  description: 'Successful response',
                  content: {
                    'application/json': {
                      schema: {
                        type: 'object',
                        properties: {
                          id: { type: 'string' },
                          name: { type: 'string' },
                        },
                      },
                    },
                  },
                },
              },
            },
          },
        },
      });

      const { result, errors } = await convertOpenApiToMcpTools(spec);

      expect(errors).toHaveLength(0);
      expect(result).toHaveLength(1);
      expect(result[0].toolDefinition.outputSchema).toEqual({
        type: 'object',
        properties: {
          bodySchema: {
            type: 'object',
            properties: {
              id: { type: 'string' },
              name: { type: 'string' },
            },
          },
        },
        required: [],
      });
    });

    it('extracts schema from 201 response when 200 has no application/json body', async () => {
      const spec = JSON.stringify({
        openapi: '3.0.0',
        info: { title: 'Test API', version: '1.0.0' },
        paths: {
          '/user': {
            post: {
              operationId: 'createUser',
              summary: 'Create user',
              responses: {
                '200': {
                  description: 'No JSON response',
                  content: {
                    'text/plain': {
                      schema: { type: 'string' },
                    },
                  },
                },
                '201': {
                  description: 'User created',
                  content: {
                    'application/json': {
                      schema: {
                        type: 'object',
                        properties: {
                          id: { type: 'string' },
                        },
                      },
                    },
                  },
                },
              },
            },
          },
        },
      });

      const { result, errors } = await convertOpenApiToMcpTools(spec);

      expect(errors).toHaveLength(0);
      expect(result).toHaveLength(1);
      expect(result[0].toolDefinition.outputSchema).toEqual({
        type: 'object',
        properties: {
          bodySchema: {
            type: 'object',
            properties: {
              id: { type: 'string' },
            },
          },
        },
        required: [],
      });
    });

    it('picks first 2xx response with application/json body (200, 201, 202)', async () => {
      const spec = JSON.stringify({
        openapi: '3.0.0',
        info: { title: 'Test API', version: '1.0.0' },
        paths: {
          '/async': {
            post: {
              operationId: 'asyncOp',
              summary: 'Async operation',
              responses: {
                '202': {
                  description: 'Accepted',
                  content: {
                    'application/json': {
                      schema: {
                        type: 'object',
                        properties: {
                          taskId: { type: 'string' },
                        },
                      },
                    },
                  },
                },
              },
            },
          },
        },
      });

      const { result, errors } = await convertOpenApiToMcpTools(spec);

      expect(errors).toHaveLength(0);
      expect(result).toHaveLength(1);
      expect(result[0].toolDefinition.outputSchema).toEqual({
        type: 'object',
        properties: {
          bodySchema: {
            type: 'object',
            properties: {
              taskId: { type: 'string' },
            },
          },
        },
        required: [],
      });
    });

    it('returns no outputSchema when no application/json response body exists', async () => {
      const spec = JSON.stringify({
        openapi: '3.0.0',
        info: { title: 'Test API', version: '1.0.0' },
        paths: {
          '/delete': {
            delete: {
              operationId: 'deleteItem',
              summary: 'Delete item',
              responses: {
                '204': {
                  description: 'No content',
                },
              },
            },
          },
        },
      });

      const { result, errors } = await convertOpenApiToMcpTools(spec);

      expect(errors).toHaveLength(0);
      expect(result).toHaveLength(1);
      expect(result[0].toolDefinition.outputSchema).toBeUndefined();
    });

    it('ignores non-JSON content types (application/xml, text/plain)', async () => {
      const spec = JSON.stringify({
        openapi: '3.0.0',
        info: { title: 'Test API', version: '1.0.0' },
        paths: {
          '/xml': {
            get: {
              operationId: 'getXml',
              summary: 'Get XML',
              responses: {
                '200': {
                  description: 'XML response',
                  content: {
                    'application/xml': {
                      schema: { type: 'string' },
                    },
                    'text/plain': {
                      schema: { type: 'string' },
                    },
                  },
                },
              },
            },
          },
        },
      });

      const { result, errors } = await convertOpenApiToMcpTools(spec);

      expect(errors).toHaveLength(0);
      expect(result).toHaveLength(1);
      expect(result[0].toolDefinition.outputSchema).toBeUndefined();
    });

    it('extracts response headers and includes them in outputSchema', async () => {
      const spec = JSON.stringify({
        openapi: '3.0.0',
        info: { title: 'Test API', version: '1.0.0' },
        paths: {
          '/user': {
            get: {
              operationId: 'getUser',
              summary: 'Get user',
              responses: {
                '200': {
                  description: 'Successful response',
                  headers: {
                    'X-Rate-Limit': {
                      description: 'Rate limit remaining',
                      schema: { type: 'integer' },
                    },
                  },
                  content: {
                    'application/json': {
                      schema: { type: 'object', properties: { id: { type: 'string' } } },
                    },
                  },
                },
              },
            },
          },
        },
      });

      const { result, errors } = await convertOpenApiToMcpTools(spec);

      expect(errors).toHaveLength(0);
      expect(result[0].toolDefinition.outputSchema).toEqual({
        type: 'object',
        properties: {
          'X-Rate-Limit': { type: 'integer', description: 'Rate limit remaining' },
          bodySchema: { type: 'object', properties: { id: { type: 'string' } } },
        },
        required: [],
      });
    });

    it('handles response with only headers (no body)', async () => {
      const spec = JSON.stringify({
        openapi: '3.0.0',
        info: { title: 'Test API', version: '1.0.0' },
        paths: {
          '/resource': {
            delete: {
              operationId: 'deleteResource',
              summary: 'Delete resource',
              responses: {
                '200': {
                  description: 'Deleted',
                  headers: {
                    'X-Deleted-At': {
                      description: 'Deletion timestamp',
                      schema: { type: 'string', format: 'date-time' },
                    },
                  },
                },
              },
            },
          },
        },
      });

      const { result, errors } = await convertOpenApiToMcpTools(spec);

      expect(errors).toHaveLength(0);
      expect(result[0].toolDefinition.outputSchema).toEqual({
        type: 'object',
        properties: {
          'X-Deleted-At': { type: 'string', format: 'date-time', description: 'Deletion timestamp' },
        },
        required: [],
      });
    });

    it('extracts multiple response headers', async () => {
      const spec = JSON.stringify({
        openapi: '3.0.0',
        info: { title: 'Test API', version: '1.0.0' },
        paths: {
          '/items': {
            get: {
              operationId: 'listItems',
              summary: 'List items',
              responses: {
                '200': {
                  description: 'Successful response',
                  headers: {
                    'X-Total-Count': {
                      description: 'Total number of items',
                      schema: { type: 'integer' },
                    },
                    'X-Page-Size': {
                      description: 'Items per page',
                      schema: { type: 'integer' },
                    },
                  },
                  content: {
                    'application/json': {
                      schema: { type: 'array', items: { type: 'object' } },
                    },
                  },
                },
              },
            },
          },
        },
      });

      const { result, errors } = await convertOpenApiToMcpTools(spec);

      expect(errors).toHaveLength(0);
      expect(result[0].toolDefinition.outputSchema).toEqual({
        type: 'object',
        properties: {
          'X-Total-Count': { type: 'integer', description: 'Total number of items' },
          'X-Page-Size': { type: 'integer', description: 'Items per page' },
          bodySchema: { type: 'array', items: { type: 'object' } },
        },
        required: [],
      });
    });

    it('does not include responseHeaders in gateway mapping when no headers defined', async () => {
      const spec = JSON.stringify({
        openapi: '3.0.0',
        info: { title: 'Test API', version: '1.0.0' },
        paths: {
          '/user': {
            get: {
              operationId: 'getUser',
              summary: 'Get user',
              responses: {
                '200': {
                  description: 'Successful response',
                  content: {
                    'application/json': {
                      schema: { type: 'object', properties: { id: { type: 'string' } } },
                    },
                  },
                },
              },
            },
          },
        },
      });

      const { errors } = await convertOpenApiToMcpTools(spec);
      expect(errors).toHaveLength(0);
    });
  });

  describe('annotations extraction', () => {
    it('extracts all annotation fields from x-mcp extensions', async () => {
      const spec = JSON.stringify({
        openapi: '3.0.0',
        info: { title: 'Test API', version: '1.0.0' },
        paths: {
          '/user': {
            get: {
              operationId: 'getUser',
              summary: 'Get user',
              'x-mcp-title': 'Retrieve User Information',
              'x-mcp-readOnlyHint': true,
              'x-mcp-destructiveHint': false,
              'x-mcp-idempotentHint': true,
              'x-mcp-openWorldHint': false,
              responses: {
                '200': { description: 'OK' },
              },
            },
          },
        },
      });

      const { result, errors } = await convertOpenApiToMcpTools(spec);

      expect(errors).toHaveLength(0);
      expect(result).toHaveLength(1);
      expect(result[0].toolDefinition.annotations).toEqual({
        title: 'Retrieve User Information',
        readOnlyHint: true,
        destructiveHint: false,
        idempotentHint: true,
        openWorldHint: false,
      });
    });

    it('handles partial annotations (only some fields present)', async () => {
      const spec = JSON.stringify({
        openapi: '3.0.0',
        info: { title: 'Test API', version: '1.0.0' },
        paths: {
          '/user': {
            delete: {
              operationId: 'deleteUser',
              summary: 'Delete user',
              'x-mcp-destructiveHint': true,
              'x-mcp-openWorldHint': true,
              responses: {
                '204': { description: 'No content' },
              },
            },
          },
        },
      });

      const { result, errors } = await convertOpenApiToMcpTools(spec);

      expect(errors).toHaveLength(0);
      expect(result).toHaveLength(1);
      expect(result[0].toolDefinition.annotations).toEqual({
        destructiveHint: true,
        openWorldHint: true,
      });
    });

    it('handles no annotations gracefully', async () => {
      const spec = JSON.stringify({
        openapi: '3.0.0',
        info: { title: 'Test API', version: '1.0.0' },
        paths: {
          '/user': {
            get: {
              operationId: 'getUser',
              summary: 'Get user',
              responses: {
                '200': { description: 'OK' },
              },
            },
          },
        },
      });

      const { result, errors } = await convertOpenApiToMcpTools(spec);

      expect(errors).toHaveLength(0);
      expect(result).toHaveLength(1);
      expect(result[0].toolDefinition.annotations).toBeUndefined();
    });

    it('validates boolean types for hint fields (ignores non-boolean values)', async () => {
      const spec = JSON.stringify({
        openapi: '3.0.0',
        info: { title: 'Test API', version: '1.0.0' },
        paths: {
          '/user': {
            get: {
              operationId: 'getUser',
              summary: 'Get user',
              'x-mcp-title': 'Valid Title',
              'x-mcp-readOnlyHint': 'true', // String, not boolean - should be ignored
              'x-mcp-destructiveHint': 1, // Number, not boolean - should be ignored
              'x-mcp-idempotentHint': true, // Valid boolean
              responses: {
                '200': { description: 'OK' },
              },
            },
          },
        },
      });

      const { result, errors } = await convertOpenApiToMcpTools(spec);

      expect(errors).toHaveLength(0);
      expect(result).toHaveLength(1);
      expect(result[0].toolDefinition.annotations).toEqual({
        title: 'Valid Title',
        idempotentHint: true,
      });
    });

    it('ignores title if not a string', async () => {
      const spec = JSON.stringify({
        openapi: '3.0.0',
        info: { title: 'Test API', version: '1.0.0' },
        paths: {
          '/user': {
            get: {
              operationId: 'getUser',
              summary: 'Get user',
              'x-mcp-title': 123, // Number, not string - should be ignored
              'x-mcp-readOnlyHint': true,
              responses: {
                '200': { description: 'OK' },
              },
            },
          },
        },
      });

      const { result, errors } = await convertOpenApiToMcpTools(spec);

      expect(errors).toHaveLength(0);
      expect(result).toHaveLength(1);
      expect(result[0].toolDefinition.annotations).toEqual({
        readOnlyHint: true,
      });
    });
  });

  describe('Error cases', () => {
    it.each([
      [
        'YAML is not parseable',
        `
      openapi: 3.0.0
      info:
        title: Sample API
        version
      paths:
  `,
      ],
      [
        'JSON is not parseable',
        `
      {
        "openapi": "3.0.0",
        "info": {
          "title": "Sample API",
          "version":
        },
        "paths": {}
      `,
      ],
    ])('throws an error when %s', async (_label, invalidSpec) => {
      const { result, errors } = await convertOpenApiToMcpTools(invalidSpec);

      expect(errors).not.toHaveLength(0);
      expect(errors[0]).toEqual({ key: 'invalidFormat', message: 'Failed to parse specification' });
      expect(result).toHaveLength(0);
    });

    it('handles invalid OpenAPI spec gracefully', async () => {
      // Override validate mock to simulate validation failure
      mockValidate.mockRejectedValueOnce(new Error('Invalid OpenAPI spec'));

      const invalidSpec = JSON.stringify({
        openapi: '3.0.0',
        info: { title: 'Sample API' }, // Missing version
        paths: {
          '/user': {
            post: {
              summary: 'Create a new user',
              requestBody: {
                content: {
                  'application/json': {
                    schema: {
                      type: 'object',
                      properties: {
                        username: { type: 'string' },
                        email: { type: 'string' },
                      },
                    },
                  },
                },
              },
              responses: {
                '200': {
                  description: 'Successful response',
                  content: {
                    'application/json': {
                      schema: {
                        type: 'object',
                        properties: {
                          id: { type: 'string' },
                          username: { type: 'string' },
                          email: { type: 'string' },
                        },
                      },
                    },
                  },
                },
              },
            },
          },
        },
      });

      const { result, errors } = await convertOpenApiToMcpTools(invalidSpec);

      expect(errors).not.toHaveLength(0);
      expect(errors[0]).toEqual({ key: 'invalidSpec', message: 'Invalid OpenAPI spec' });
      expect(result).toHaveLength(0);

      // Verify that validate was called but dereference was not
      expect(mockValidate).toHaveBeenCalled();
      expect(mockDereference).not.toHaveBeenCalled();
    });

    it('handles duplicated tool name gracefully', async () => {
      const invalidSpec = JSON.stringify({
        openapi: '3.0.0',
        info: { title: 'Sample API', version: '1.0.0' },
        paths: {
          '/duplicateTool': {
            get: {
              operationId: 'duplicateTool',
              summary: 'Duplicate tool test',
              parameters: [
                {
                  name: 'param',
                  in: 'query',
                  schema: { type: 'string' },
                },
              ],
              responses: {
                '200': {
                  description: 'Successful response',
                  content: {
                    'application/json': {
                      schema: {
                        type: 'object',
                        properties: {
                          id: { type: 'string' },
                          username: { type: 'string' },
                          email: { type: 'string' },
                        },
                      },
                    },
                  },
                },
              },
            },
          },
          '/duplicateToolAgain': {
            get: {
              operationId: 'duplicateTool', // Intentional duplicate name
              summary: 'Duplicate tool test',
              parameters: [
                {
                  name: 'param',
                  in: 'query',
                  schema: { type: 'string' },
                },
              ],
              responses: {
                '200': {
                  description: 'Successful response',
                  content: {
                    'application/json': {
                      schema: {
                        type: 'object',
                        properties: {
                          id: { type: 'string' },
                          username: { type: 'string' },
                          email: { type: 'string' },
                        },
                      },
                    },
                  },
                },
              },
            },
          },
        },
      });

      const { result, errors } = await convertOpenApiToMcpTools(invalidSpec);

      expect(errors).not.toHaveLength(0);
      expect(errors[0]).toEqual({ key: 'duplicateName', message: 'Duplicate tool name detected: duplicateTool' });
      expect(result).toHaveLength(2);

      // Verify that the mock functions were called
      expect(mockValidate).toHaveBeenCalled();
      expect(mockDereference).toHaveBeenCalled();
    });
  });
});
