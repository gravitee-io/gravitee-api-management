import { OpenAPIToMCPConverter } from './openapi-to-mcp';

describe('OpenAPIToMCPConverter', () => {
  let converter: OpenAPIToMCPConverter;

  const openApiV3Spec = {
    openapi: '3.0.0',
    info: {
      title: 'Test API',
      version: '1.0.0',
      description: 'Test API Description',
    },
    servers: [
      {
        url: 'https://api.example.com/v1',
      },
    ],
    paths: {
      '/pets': {
        get: {
          operationId: 'listPets',
          summary: 'List all pets',
          description: 'Returns all pets from the system',
          parameters: [
            {
              name: 'limit',
              in: 'query',
              description: 'Maximum number of items to return',
              required: false,
              schema: {
                type: 'integer',
                format: 'int32',
              },
            },
          ],
          responses: {
            '200': {
              description: 'A paged array of pets',
              content: {
                'application/json': {
                  schema: {
                    type: 'array',
                    items: {
                      type: 'object',
                      properties: {
                        id: {
                          type: 'integer',
                        },
                        name: {
                          type: 'string',
                        },
                      },
                    },
                  },
                },
              },
            },
          },
        },
        post: {
          operationId: 'createPet',
          summary: 'Create a pet',
          requestBody: {
            required: true,
            content: {
              'application/json': {
                schema: {
                  type: 'object',
                  required: ['name'],
                  properties: {
                    name: {
                      type: 'string',
                    },
                  },
                },
              },
            },
          },
          responses: {
            '201': {
              description: 'Pet created',
            },
          },
        },
      },
    },
  };

  const openApiV2Spec = {
    swagger: '2.0',
    info: {
      title: 'Test API',
      version: '1.0.0',
      description: 'Test API Description',
    },
    host: 'api.example.com',
    basePath: '/v1',
    schemes: ['https'],
    paths: {
      '/pets': {
        get: {
          operationId: 'listPets',
          summary: 'List all pets',
          description: 'Returns all pets from the system',
          parameters: [
            {
              name: 'limit',
              in: 'query',
              description: 'Maximum number of items to return',
              required: false,
              type: 'integer',
              format: 'int32',
            },
          ],
          responses: {
            '200': {
              description: 'A paged array of pets',
              schema: {
                type: 'array',
                items: {
                  type: 'object',
                  properties: {
                    id: {
                      type: 'integer',
                    },
                    name: {
                      type: 'string',
                    },
                  },
                },
              },
            },
          },
        },
        post: {
          operationId: 'createPet',
          summary: 'Create a pet',
          parameters: [
            {
              name: 'pet',
              in: 'body',
              required: true,
              schema: {
                type: 'object',
                required: ['name'],
                properties: {
                  name: {
                    type: 'string',
                  },
                },
              },
            },
          ],
          responses: {
            '201': {
              description: 'Pet created',
            },
          },
        },
      },
    },
  };

  describe('OpenAPI v3', () => {
    beforeEach(() => {
      converter = new OpenAPIToMCPConverter(JSON.stringify(openApiV3Spec));
    });

    it('should convert endpoints to MCP tools', () => {
      const result = converter.convert();
      expect(result).toHaveLength(2);

      const listPetsTool = result.find(t => t.name === 'get_listPets');
      expect(listPetsTool).toBeDefined();
      expect(listPetsTool?.parameters.type).toBe('object');
      expect(listPetsTool?.parameters.properties.q_limit).toEqual({
        type: 'integer',
        description: expect.stringContaining('limit'),
      });

      const createPetTool = result.find(t => t.name === 'post_createPet');
      expect(createPetTool).toBeDefined();
      expect(createPetTool?.parameters.type).toBe('object');
      expect(createPetTool?.parameters.properties.b_name).toEqual({
        type: 'string',
      });
      expect(createPetTool?.parameters.required).toContain('b_name');
    });

    it('should generate proper descriptions with HTTP metadata', () => {
      const result = converter.convert();
      const listPetsTool = result.find(t => t.name === 'get_listPets');
      const description = JSON.parse(listPetsTool!.description);

      expect(description).toEqual({
        description: 'List all pets',
        http: {
          method: 'GET',
          path: '/pets',
        },
      });

      const createPetTool = result.find(t => t.name === 'post_createPet');
      const createDescription = JSON.parse(createPetTool!.description);

      expect(createDescription).toEqual({
        description: 'Create a pet',
        http: {
          method: 'POST',
          path: '/pets',
          contentType: 'application/json',
        },
      });
    });
  });

  describe('OpenAPI v2', () => {
    beforeEach(() => {
      converter = new OpenAPIToMCPConverter(JSON.stringify(openApiV2Spec));
    });

    it('should convert endpoints to MCP tools', () => {
      const result = converter.convert();
      expect(result).toHaveLength(2);

      const listPetsTool = result.find(t => t.name === 'get_listPets');
      expect(listPetsTool).toBeDefined();
      expect(listPetsTool?.parameters.type).toBe('object');
      expect(listPetsTool?.parameters.properties.q_limit).toEqual({
        type: 'integer',
        description: expect.stringContaining('limit'),
      });

      const createPetTool = result.find(t => t.name === 'post_createPet');
      expect(createPetTool).toBeDefined();
      expect(createPetTool?.parameters.type).toBe('object');
      expect(createPetTool?.parameters.properties.b_name).toEqual({
        type: 'string',
      });
      expect(createPetTool?.parameters.required).toContain('b_name');
    });

    it('should generate proper descriptions with HTTP metadata', () => {
      const result = converter.convert();
      const listPetsTool = result.find(t => t.name === 'get_listPets');
      const description = JSON.parse(listPetsTool!.description);

      expect(description).toEqual({
        description: 'List all pets',
        http: {
          method: 'GET',
          path: '/pets',
        },
      });

      const createPetTool = result.find(t => t.name === 'post_createPet');
      const createDescription = JSON.parse(createPetTool!.description);

      expect(createDescription).toEqual({
        description: 'Create a pet',
        http: {
          method: 'POST',
          path: '/pets',
          contentType: 'application/json',
        },
      });
    });
  });
}); 