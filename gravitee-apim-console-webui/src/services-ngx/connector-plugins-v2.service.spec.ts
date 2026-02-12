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
import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { GioJsonSchema } from '@gravitee/ui-particles-angular';

import { ConnectorPluginsV2Service } from './connector-plugins-v2.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeConnectorPlugin } from '../entities/management-api-v2';

describe('Installation Plugins Service', () => {
  let httpTestingController: HttpTestingController;
  let service: ConnectorPluginsV2Service;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<ConnectorPluginsV2Service>(ConnectorPluginsV2Service);
  });

  describe('Endpoints', () => {
    describe('listEndpointPlugins', () => {
      it('should call the API', done => {
        const fakeConnectors = [fakeConnectorPlugin()];

        service.listEndpointPlugins().subscribe(connectors => {
          expect(connectors).toMatchObject(fakeConnectors);
          done();
        });

        httpTestingController
          .expectOne({
            url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/endpoints`,
            method: 'GET',
          })
          .flush(fakeConnectors);
      });
    });

    describe('listEndpointPluginsByApiType', () => {
      it('should call the API', done => {
        const response = [
          fakeConnectorPlugin({ name: 'z-plugin' }),
          fakeConnectorPlugin({ name: 'a-plugin' }),
          fakeConnectorPlugin({ supportedApiType: 'MESSAGE' }),
        ];
        const result = [fakeConnectorPlugin({ name: 'a-plugin' }), fakeConnectorPlugin({ name: 'z-plugin' })];

        service.listEndpointPluginsByApiType('PROXY').subscribe(connectors => {
          expect(connectors).toStrictEqual(result);
          done();
        });

        httpTestingController
          .expectOne({
            url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/endpoints`,
            method: 'GET',
          })
          .flush(response);
      });
    });

    describe('v4Get', () => {
      it('should call the API', done => {
        const fakeConnectors = fakeConnectorPlugin();

        service.getEndpointPlugin('endpointId').subscribe(connectors => {
          expect(connectors).toEqual(fakeConnectors);
          done();
        });

        httpTestingController
          .expectOne({
            url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/endpoints/endpointId`,
            method: 'GET',
          })
          .flush(fakeConnectors);
      });
    });
  });

  describe('Entrypoints', () => {
    describe('listSyncEntrypointPlugins', () => {
      it('should call the API', done => {
        const fakeConnectors = [fakeConnectorPlugin({ supportedApiType: 'PROXY' }), fakeConnectorPlugin({ supportedApiType: 'MESSAGE' })];

        service.listSyncEntrypointPlugins().subscribe(connectors => {
          expect(connectors).toMatchObject([fakeConnectors[0]]);
          done();
        });

        httpTestingController
          .expectOne({
            url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/entrypoints`,
            method: 'GET',
          })
          .flush(fakeConnectors);
      });
    });

    describe('listAsyncEntrypointPlugins', () => {
      it('should call the API', done => {
        const fakeConnectors = [fakeConnectorPlugin({ supportedApiType: 'PROXY' }), fakeConnectorPlugin({ supportedApiType: 'MESSAGE' })];

        service.listAsyncEntrypointPlugins().subscribe(connectors => {
          expect(connectors).toMatchObject([fakeConnectors[1]]);
          done();
        });

        httpTestingController
          .expectOne({
            url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/entrypoints`,
            method: 'GET',
          })
          .flush(fakeConnectors);
      });
    });

    describe('getSubscriptionSchema', () => {
      it('should call the API', done => {
        const expectedSchema: GioJsonSchema = {
          $schema: 'http://json-schema.org/draft-07/schema#',
          type: 'object',
          properties: {
            foo: {
              type: 'string',
            },
          },
        };

        service.getEntrypointPluginSubscriptionSchema('entrypoint-id').subscribe(schema => {
          expect(schema).toMatchObject(expectedSchema);
          done();
        });

        httpTestingController
          .expectOne({
            url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/entrypoints/entrypoint-id/subscription-schema`,
            method: 'GET',
          })
          .flush(expectedSchema);
      });
    });

    describe('getEntrypointPluginSchema', () => {
      const entrypoint = fakeConnectorPlugin();
      it('should call the API', done => {
        service.getEntrypointPlugin('entrypoint-id').subscribe(schema => {
          expect(schema).toMatchObject(entrypoint);
          done();
        });

        httpTestingController
          .expectOne({
            url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/entrypoints/entrypoint-id`,
            method: 'GET',
          })
          .flush(entrypoint);
      });
    });

    describe('listAIEntrypointPlugins', () => {
      it('should filter entrypoints by LLM_PROXY, MCP_PROXY, and A2A_PROXY supportedApiType', done => {
        const fakeConnectors = [
          fakeConnectorPlugin({ id: 'llm-proxy', supportedApiType: 'LLM_PROXY' }),
          fakeConnectorPlugin({ id: 'mcp-proxy', supportedApiType: 'MCP_PROXY' }),
          fakeConnectorPlugin({ id: 'a2a-proxy', supportedApiType: 'A2A_PROXY' }),
          fakeConnectorPlugin({ id: 'http-proxy', supportedApiType: 'PROXY' }),
          fakeConnectorPlugin({ id: 'webhook', supportedApiType: 'MESSAGE' }),
        ];

        service.listAIEntrypointPlugins().subscribe(connectors => {
          expect(connectors).toHaveLength(3);
          expect(connectors).toMatchObject([
            fakeConnectors[0], // LLM_PROXY
            fakeConnectors[1], // MCP_PROXY
            fakeConnectors[2], // A2A_PROXY
          ]);
          done();
        });

        httpTestingController
          .expectOne({
            url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/entrypoints`,
            method: 'GET',
          })
          .flush(fakeConnectors);
      });
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
