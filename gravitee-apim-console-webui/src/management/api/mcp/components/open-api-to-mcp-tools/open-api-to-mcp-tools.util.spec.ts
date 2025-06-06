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
    mockDereference.mockImplementation(async (spec) => ({ schema: spec }));
  });

  describe('Success cases', () => {
    const yamlSpec = `
openapi: 3.0.0
info:
  title: Sample API
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
