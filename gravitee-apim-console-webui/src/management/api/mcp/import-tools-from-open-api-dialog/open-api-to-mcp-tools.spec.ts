import { convertOpenApiToMCP } from './open-api-to-mcp-tools';

describe('convertOpenApiToMCP (parameterized for YAML and JSON)', () => {
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
          ],
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
        },
      },
    },
  });

  const cases: Array<[string, string]> = [
    ['YAML spec', yamlSpec],
    ['JSON spec', jsonSpec],
  ];

  it.each(cases)('converts %s with proper prefixes and names', async (_label, input) => {
    const { result, errors } = await convertOpenApiToMCP(input);

    expect(errors).toHaveLength(0);
    expect(result).toHaveLength(2);

    const [getTool, postTool] = result;

    expect(getTool.name).toBe('get_getUser');
    expect(getTool.parameters.properties).toHaveProperty('p_id');
    expect(getTool.parameters.properties).toHaveProperty('q_verbose');

    expect(postTool.name).toMatch(/^post_/);
    expect(postTool.parameters.properties).toHaveProperty('b_username');
    expect(postTool.parameters.properties).toHaveProperty('b_email');
  });


  it.each([
    ['YAML is not parseable', `
      openapi: 3.0.0
      info:
        title: Sample API
        version
      paths:
  `],
    ['JSON is not parseable', `
      {
        "openapi": "3.0.0",
        "info": {
          "title": "Sample API",
          "version":
        },
        "paths": {}
      `],
  ])('throws an error when %s', async (_label, invalidSpec) => {
    const {result, errors} = await convertOpenApiToMCP(invalidSpec);

    expect(errors).not.toHaveLength(0);
    expect(errors[0]).toEqual('Failed to parse specification');
    expect(result).toHaveLength(0);
  });

  it.each([
    ['OpenAPI spec is invalid', JSON.stringify({
      openapi: '3.0.0',
      info: {title: 'Sample API'}, // Missing version
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
                      username: {type: 'string'},
                      email: {type: 'string'},
                    },
                  },
                },
              },
            },
          },
        },
      },
    }), 'Missing required property: version'],
    ['duplicate tool names are generated', JSON.stringify({
      openapi: '3.0.0',
      info: {title: 'Sample API', version: '1.0.0'},
      paths: {
        '/duplicateTool': {
          get: {
            operationId: 'duplicateTool',
            summary: 'Duplicate tool test',
            parameters: [
              {
                name: 'param',
                in: 'query',
                schema: {type: 'string'},
              },
            ],
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
                schema: {type: 'string'},
              },
            ],
          },
        },
      },
    }), 'Duplicate tool name detected: get_duplicateTool'],
  ])('throws an error when %s', async (_label, invalidSpec, expectedError) => {
    const {result, errors} = await convertOpenApiToMCP(invalidSpec);

    expect(errors).not.toHaveLength(0);
    expect(errors[0]).toMatch(expectedError);
    expect(result).toHaveLength(0);
  });
});
