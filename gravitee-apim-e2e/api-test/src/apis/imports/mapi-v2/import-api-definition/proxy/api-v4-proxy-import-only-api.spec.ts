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
import { test, describe, expect, afterAll } from '@jest/globals';
import { APIsApi, ApiV4, HttpListener, PlanMode, PlanSecurityType } from '@gravitee/management-v2-webclient-sdk/src/lib';
import { forManagementV2AsApiUser } from '@gravitee/utils/configuration';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { created, fail, noContent, succeed } from '@lib/jest-utils';
import { MAPIV2PlansFaker } from '@gravitee/fixtures/management/MAPIV2PlansFaker';

const envId = 'DEFAULT';

const v2ApisResourceAsApiPublisher = new APIsApi(forManagementV2AsApiUser());

describe('API - V4 - Proxy - Import - Gravitee Definition - Only API -', () => {
  test('should fail because of plan mode is invalid', async () => {
    await fail(
      v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
        envId,
        exportApiV4: MAPIV2ApisFaker.apiImportV4({
          plans: [
            MAPIV2PlansFaker.planV4({
              security: { type: PlanSecurityType.API_KEY },
              mode: 'dummy' as PlanMode,
            }),
          ],
        }),
      }),
      400,
      "Cannot construct instance of `io.gravitee.rest.api.management.v2.rest.model.PlanMode`, problem: Unexpected value 'dummy'",
    );
  });

  describe('Create v4 API from import -', () => {
    describe('Create v4 API without ID', () => {
      let importedApi;

      test('should import v4 API', async () => {
        importedApi = await created(
          v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
            envId,
            exportApiV4: MAPIV2ApisFaker.apiImportV4(),
          }),
        );
      });

      test('should get created v4 API with generated ID', async () => {
        const apiV4 = await succeed(
          v2ApisResourceAsApiPublisher.getApiRaw({
            envId,
            apiId: importedApi.id,
          }),
        );
        expect(apiV4).toBeTruthy();
        expect(apiV4.id).toStrictEqual(importedApi.id);
      });

      afterAll(async () => {
        await noContent(
          v2ApisResourceAsApiPublisher.deleteApiRaw({
            envId,
            apiId: importedApi.id,
          }),
        );
      });
    });
    describe('Create v4 API with flows and resources', () => {
      let importedApi;
      const apiToImport = MAPIV2ApisFaker.apiImportV4({
        api: MAPIV2ApisFaker.apiV4Proxy({
          flows: [
            {
              enabled: true,
              name: 'Orinoco flow',
              selectors: [
                {
                  type: 'HTTP',
                  path: '/products',
                  pathOperator: 'EQUALS',
                  methods: [],
                },
              ],
              request: [
                {
                  name: 'Transform headers',
                  enabled: true,
                  description: 'Transform request headers',
                  policy: 'transform-headers',
                  condition: undefined,
                  messageCondition: undefined,
                  configuration: {
                    scope: 'REQUEST',
                  },
                },
              ],
              response: [],
              publish: [],
              entrypointConnect: [],
              interact: [],
              subscribe: [],
              tags: ['test-tag'],
            },
          ],
          services: {
            dynamicProperty: {
              enabled: false,
              configuration: undefined,
              overrideConfiguration: false,
              type: 'http-dynamic-properties',
            },
          },
          properties: [
            {
              key: 'prop-key',
              value: 'prop-value',
              encrypted: false,
              dynamic: false,
            },
          ],
          resources: [
            {
              enabled: true,
              name: 'Cache',
              type: 'cache',
              configuration: {},
            },
          ],
        }),
      });

      test('should import v4 API', async () => {
        importedApi = await created(
          v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
            envId,
            exportApiV4: apiToImport,
          }),
        );
      });

      test('should get created v4 API with flows, resources and services', async () => {
        const apiV4: ApiV4 = await succeed(
          v2ApisResourceAsApiPublisher.getApiRaw({
            envId,
            apiId: importedApi.id,
          }),
        );
        expect(apiV4).toBeTruthy();
        expect(apiV4.id).toStrictEqual(importedApi.id);
        expect(apiV4.flows).toHaveLength(1);
        apiToImport.api.flows[0].id = apiV4.flows[0].id;
        expect(apiV4.flows).toEqual(apiToImport.api.flows);
        expect(apiV4.tags).toEqual(apiToImport.api.tags);
        expect(apiV4.services).toEqual(apiToImport.api.services);
        expect(apiV4.properties).toEqual(apiToImport.api.properties);

        const apiToImportWithValidConfig = [
          {
            ...apiToImport.api.resources[0],
            configuration: {
              maxEntriesLocalHeap: 1000,
              timeToIdleSeconds: 0,
              timeToLiveSeconds: 0,
            },
          },
        ];

        expect(apiV4.resources).toEqual(apiToImportWithValidConfig);
      });

      afterAll(async () => {
        await noContent(
          v2ApisResourceAsApiPublisher.deleteApiRaw({
            envId,
            apiId: importedApi.id,
          }),
        );
      });
    });
    describe('Try to import API with an already existing context path', () => {
      let importedApi: ApiV4;

      test('should create a first v4 API', async () => {
        importedApi = await created(
          v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
            envId,
            exportApiV4: MAPIV2ApisFaker.apiImportV4(),
          }),
        );
      });
      test('should get created v4 API with generated ID', async () => {
        const apiV4 = await succeed(
          v2ApisResourceAsApiPublisher.getApiRaw({
            envId,
            apiId: importedApi.id,
          }),
        );
        expect(apiV4).toBeTruthy();
        expect(apiV4.id).toStrictEqual(importedApi.id);
      });
      test('should fail to import another v4 API with same context path', async () => {
        await fail(
          v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
            envId,
            exportApiV4: MAPIV2ApisFaker.apiImportV4({
              api: MAPIV2ApisFaker.apiV4Proxy({
                listeners: importedApi.listeners,
              }),
            }),
          }),
          400,
          `Cannot import API with invalid paths (Path [${(importedApi.listeners[0] as HttpListener).paths[0].path}] already exists)`,
        );
      });

      afterAll(async () => {
        await noContent(
          v2ApisResourceAsApiPublisher.deleteApiRaw({
            envId,
            apiId: importedApi.id,
          }),
        );
      });
    });
  });
  describe('API definition imported twice', () => {
    let importedApi: ApiV4;
    const apiToImport = MAPIV2ApisFaker.apiImportV4();

    test('should create a first v4 API', async () => {
      importedApi = await created(
        v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
          envId,
          exportApiV4: apiToImport,
        }),
      );
    });
    test('should get created v4 API with generated ID', async () => {
      const apiV4 = await succeed(
        v2ApisResourceAsApiPublisher.getApiRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
      expect(apiV4).toBeTruthy();
      expect(apiV4.id).toStrictEqual(importedApi.id);
    });
    test('should fail to import another v4 API with same context path', async () => {
      await fail(
        v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
          envId,
          exportApiV4: apiToImport,
        }),
        400,
        `Cannot import API with invalid paths (Path [${(importedApi.listeners[0] as HttpListener).paths[0].path}] already exists)`,
      );
    });

    afterAll(async () => {
      await noContent(
        v2ApisResourceAsApiPublisher.deleteApiRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
    });
  });
});
