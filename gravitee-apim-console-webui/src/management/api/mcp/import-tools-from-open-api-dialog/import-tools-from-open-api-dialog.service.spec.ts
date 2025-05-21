import {TestBed} from '@angular/core/testing';

import {ImportToolsFromOpenApiDialogService} from './import-tools-from-open-api-dialog.service';
import fs from 'fs';
import yaml from 'js-yaml';

// Mock fs.readFileSync
jest.mock('fs');
const mockFs = fs as jest.Mocked<typeof fs>;

describe('ImportToolsFromOpenApiDialogService', () => {
  let service: ImportToolsFromOpenApiDialogService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ImportToolsFromOpenApiDialogService);
  });

  describe('convertOpenApiToMCP', () => {
    describe('yaml', () => {
      it('converts simple OpenAPI spec to MCP tools', async () => {
        const openApiYaml = `
openapi: 3.0.0
info:
  title: Sample API
  version: 1.0.0
paths:
  /user:
    post:
      operationId: createUser
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

        mockFs.readFileSync.mockReturnValue(openApiYaml);

        const tools = await service.convertOpenApiToMCP('dummy.yaml');

        expect(tools).toHaveLength(1);

        expect(tools[0]).toEqual({
          name: 'createUser',
          description: 'Create a new user',
          parameters: {
            type: 'object',
            properties: {
              body: {
                type: 'object',
                properties: {
                  username: { type: 'string' },
                  email: { type: 'string' },
                },
                required: ['username', 'email'],
              },
            },
            required: ['body'],
          },
        });
      });

      it('handles missing operationId gracefully', async () => {
        const yamlString = `
openapi: 3.0.0
info:
  title: API
  version: 1.0.0
paths:
  /ping:
    get:
      summary: Health check
      parameters:
        - name: verbose
          in: query
          required: false
          schema:
            type: boolean
`;

        mockFs.readFileSync.mockReturnValue(yamlString);

        const tools = await service.convertOpenApiToMCP('dummy.yaml');

        expect(tools[0].name).toMatch(/get__ping/);
        expect(tools[0].description).toBe('Health check');
        expect(tools[0].parameters).toHaveProperty('properties.verbose');
      });
    })

  });
});
