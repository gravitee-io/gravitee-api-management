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
import { convertOpenApiToMcpTools } from './open-api-to-mcp-tools.util';

describe('convertOpenApiToMCP', () => {
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
      operationId: createUser
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

    const cases: Array<[string, string]> = [
      ['OpenAPI YAML', yamlSpec],
      ['OpenAPI JSON', jsonSpec],
      ['Swagger 2.0', swaggerSpec],
    ];

    it.each(cases)('converts %s with proper prefixes and names', async (_label, input) => {
      const { result, errors } = await convertOpenApiToMcpTools(input);

      expect(errors).toHaveLength(0);
      expect(result).toHaveLength(2);

      const [getTool, postTool] = result;

      // Check the GET method (getUser)
      expect(getTool.name).toBe('get_getUser');
      expect(getTool.inputSchema.properties).toHaveProperty('p_id');
      expect(getTool.inputSchema.properties).toHaveProperty('q_verbose');
      expect(getTool.inputSchema.properties).toHaveProperty('h_X-Custom-Header'); // Check if header is serialized
      expect(getTool.inputSchema.properties['h_X-Custom-Header'].description).toBe('A custom header for the request'); // Check description

      // Check the POST method (create user)
      expect(postTool.name).toMatch(/^post_/);
      expect(postTool.inputSchema.properties).toHaveProperty('b_username');
      expect(postTool.inputSchema.properties).toHaveProperty('b_email');
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
  /user/{userId}:
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
          path: '/user/:userId',
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
    ])('serializes description field for %s', async (_label, spec, expected) => {
      const { result, errors } = await convertOpenApiToMcpTools(spec);
      expect(errors).toHaveLength(0);
      expect(result.length).toBeGreaterThan(0);

      const tool = result[0];
      expect(() => JSON.parse(tool.description)).not.toThrow();

      const desc = JSON.parse(tool.description);
      expect(desc).toHaveProperty('description', expected.summary);
      expect(desc).toHaveProperty('http.method', expected.method);
      expect(desc).toHaveProperty('http.path', expected.path);

      if (expected.contentType) {
        expect(desc).toHaveProperty('http.contentType', expected.contentType);
      } else {
        expect(desc).not.toHaveProperty('http.contentType');
      }
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
      expect(errors[0]).toEqual('Failed to parse specification');
      expect(result).toHaveLength(0);
    });

    it('handles invalid OpenAPI spec gracefully', async () => {
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
      expect(errors[0]).toMatch(/Missing required property: version/);
      expect(result).toHaveLength(0);
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
      expect(errors[0]).toMatch(/Duplicate tool name detected: get_duplicateTool/);
      expect(result).toHaveLength(2);
    });
  });
});
