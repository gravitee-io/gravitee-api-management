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
import { created, noContent, succeed } from '@lib/jest-utils';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { MAPIV2PlansFaker } from '@gravitee/fixtures/management/MAPIV2PlansFaker';
import {
  APIEventsApi,
  APIPlansApi,
  APIsApi,
  ApiV4,
  ListenerType,
  PlanSecurityType,
  PlanV4,
  UpdateApi,
} from '@gravitee/management-v2-webclient-sdk/src/lib';
import { forManagementV2AsApiUser } from '@gravitee/utils/configuration';
import { afterAll, beforeAll, beforeEach, expect, test } from '@jest/globals';
import faker from '@faker-js/faker';

describe('API - V4 - Rollback', () => {
  describe('Rollback API to eventId', () => {
    const envId = 'DEFAULT';
    const v2ApisResourceAsApiPublisher = new APIsApi(forManagementV2AsApiUser());
    const v2EventsResourceAsApiPublisher = new APIEventsApi(forManagementV2AsApiUser());
    const v2APlansResourceAsApiPublisher = new APIPlansApi(forManagementV2AsApiUser());

    let importedApi: ApiV4;
    let initialPlan: PlanV4;

    beforeEach(async () => {
      importedApi = await created(
        v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
          envId,
          exportApiV4: MAPIV2ApisFaker.apiImportV4({
            plans: [
              MAPIV2PlansFaker.planV4({
                security: { type: PlanSecurityType.API_KEY },
              }),
            ],
            api: MAPIV2ApisFaker.apiV4(),
          }),
        }),
      );
      initialPlan = (
        await succeed(
          v2APlansResourceAsApiPublisher.listApiPlansRaw({
            envId,
            apiId: importedApi.id,
          }),
        )
      ).data[0];
      await noContent(
        v2ApisResourceAsApiPublisher.startApiRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
    });

    afterEach(async () => {
      if (initialPlan) {
        await succeed(
          v2APlansResourceAsApiPublisher.closeApiPlanRaw({
            envId,
            apiId: importedApi.id,
            planId: initialPlan.id,
          }),
        );
      }

      if (importedApi) {
        await noContent(
          v2ApisResourceAsApiPublisher.stopApiRaw({
            envId,
            apiId: importedApi.id,
          }),
        );
        await succeed(
          v2ApisResourceAsApiPublisher.updateApiRaw({
            envId,
            apiId: importedApi.id,
            updateApi: { ...importedApi, lifecycleState: 'DEPRECATED' } as UpdateApi,
          }),
        );

        await noContent(
          v2ApisResourceAsApiPublisher.deleteApiRaw({
            envId,
            apiId: importedApi.id,
          }),
        );
      }
    });

    test('should rollback "update on api, flow, heathCheck"', async () => {
      // Given
      // Update the API
      const updatedApi = await succeed(
        v2ApisResourceAsApiPublisher.updateApiRaw({
          envId,
          apiId: importedApi.id,
          updateApi: {
            ...importedApi,
            definitionVersion: 'V4',
            type: 'PROXY',
            name: 'updated-api-name-' + importedApi.name,
            apiVersion: 'updated-api-version-' + importedApi.apiVersion,
            description: 'updated-api-description',
            lifecycleState: 'PUBLISHED',
            visibility: 'PUBLIC',
            labels: ['foo', 'bar'],
            flowExecution: {
              mode: 'BEST_MATCH',
              matchRequired: true,
            },
            flows: [
              MAPIV2ApisFaker.newFlow({
                name: 'updated-flow',
              }),
            ],
            endpointGroups: [
              {
                name: 'updated-endpoint-group',
                type: 'http-proxy',
                endpoints: [
                  {
                    name: 'updated-endpoint-endpoint',
                    type: 'http-proxy',
                  },
                ],
                services: {
                  healthCheck: {
                    enabled: false,
                    type: 'http-health-check',
                    configuration: {
                      schedule: '*/60 * * * * *',
                      assertion: '{#response.status == 200}',
                      failureThreshold: 2,
                      headers: [],
                      method: 'GET',
                      overrideEndpointPath: true,
                      successThreshold: 2,
                      target: '/updated-health-check',
                    },
                  },
                },
              },
            ],
            listeners: [
              {
                type: ListenerType.HTTP,
                paths: [
                  {
                    path: `/updated-entrypoint-${faker.helpers.slugify(faker.lorem.words(3))}`,
                  },
                ],
                entrypoints: [
                  {
                    type: 'http-proxy',
                  },
                ],
              },
            ],
          },
        }),
      );

      // Deploy
      await succeed(
        v2ApisResourceAsApiPublisher.createApiDeploymentRaw({
          envId,
          apiId: importedApi.id,
        }),
        202,
      );

      // When
      // Rollback to the first event without updating changes
      const events = await succeed(
        v2EventsResourceAsApiPublisher.getApiEventsRaw({
          envId,
          apiId: importedApi.id,
          types: ['PUBLISH_API'],
        }),
      );
      expect(events.data).toHaveLength(2);
      await noContent(
        v2ApisResourceAsApiPublisher.rollbackApiRaw({
          envId,
          apiId: importedApi.id,
          apiRollback: {
            eventId: events.data[1].id,
          },
        }),
      );

      // Then
      const api = await succeed(
        v2ApisResourceAsApiPublisher.getApiRaw({
          envId,
          apiId: importedApi.id,
        }),
      );

      // Rollbacked API values
      expect(api.name).toEqual(importedApi.name);
      expect(api.apiVersion).toEqual(importedApi.apiVersion);
      expect(api.description).toEqual(importedApi.description);

      // Non rollbacked API values
      expect(api.visibility).toEqual(updatedApi.visibility);
    });

    test('should rollback" a new plan created"', async () => {
      // Given

      // Create a plan
      const createdPlan = await created(
        v2APlansResourceAsApiPublisher.createApiPlanRaw({
          envId,
          apiId: importedApi.id,
          createPlan: MAPIV2PlansFaker.newPlanV4({
            name: 'new-plan-name',
            security: { type: PlanSecurityType.API_KEY },
          }),
        }),
      );
      // And publish it
      await succeed(
        v2APlansResourceAsApiPublisher.publishApiPlanRaw({
          envId,
          apiId: importedApi.id,
          planId: createdPlan.id,
        }),
      );

      // Deploy
      await succeed(
        v2ApisResourceAsApiPublisher.createApiDeploymentRaw({
          envId,
          apiId: importedApi.id,
        }),
        202,
      );

      // When
      // Rollback to the first event
      const events = await succeed(
        v2EventsResourceAsApiPublisher.getApiEventsRaw({
          envId,
          apiId: importedApi.id,
          types: ['PUBLISH_API'],
        }),
      );
      expect(events.data).toHaveLength(2);
      await noContent(
        v2ApisResourceAsApiPublisher.rollbackApiRaw({
          envId,
          apiId: importedApi.id,
          apiRollback: {
            eventId: events.data[1].id,
          },
        }),
      );

      // Then
      const api = await succeed(
        v2ApisResourceAsApiPublisher.getApiRaw({
          envId,
          apiId: importedApi.id,
        }),
      );

      const plans = await succeed(
        v2APlansResourceAsApiPublisher.listApiPlansRaw({
          envId,
          apiId: importedApi.id,
          statuses: ['CLOSED', 'DEPRECATED', 'PUBLISHED', 'STAGING'],
        }),
      );
      expect(plans.pagination.totalCount).toEqual(2);
      expect(
        plans.data.map((plan) => {
          return { id: plan.id, status: plan.status };
        }),
      ).toEqual(
        expect.arrayContaining([
          { id: initialPlan.id, status: 'PUBLISHED' },
          { id: createdPlan.id, status: 'CLOSED' },
        ]),
      );
    });

    test('should rollback "closing a Plan and adding a new one"', async () => {
      // Given

      // Create a plan
      const createdPlan = await created(
        v2APlansResourceAsApiPublisher.createApiPlanRaw({
          envId,
          apiId: importedApi.id,
          createPlan: MAPIV2PlansFaker.newPlanV4({
            security: { type: PlanSecurityType.API_KEY },
          }),
        }),
      );
      // And publish it
      await succeed(
        v2APlansResourceAsApiPublisher.publishApiPlanRaw({
          envId,
          apiId: importedApi.id,
          planId: createdPlan.id,
        }),
      );

      // Close the initial plan
      await succeed(
        v2APlansResourceAsApiPublisher.closeApiPlanRaw({
          envId,
          apiId: importedApi.id,
          planId: initialPlan.id,
        }),
      );

      // Deploy
      await succeed(
        v2ApisResourceAsApiPublisher.createApiDeploymentRaw({
          envId,
          apiId: importedApi.id,
        }),
        202,
      );

      // When
      // Rollback to the first event
      const events = await succeed(
        v2EventsResourceAsApiPublisher.getApiEventsRaw({
          envId,
          apiId: importedApi.id,
          types: ['PUBLISH_API'],
        }),
      );
      expect(events.data).toHaveLength(2);
      await noContent(
        v2ApisResourceAsApiPublisher.rollbackApiRaw({
          envId,
          apiId: importedApi.id,
          apiRollback: {
            eventId: events.data[1].id,
          },
        }),
      );

      // Then
      const api = await succeed(
        v2ApisResourceAsApiPublisher.getApiRaw({
          envId,
          apiId: importedApi.id,
        }),
      );

      const plans = await succeed(
        v2APlansResourceAsApiPublisher.listApiPlansRaw({
          envId,
          apiId: importedApi.id,
          statuses: ['CLOSED', 'DEPRECATED', 'PUBLISHED', 'STAGING'],
        }),
      );
      expect(plans.pagination.totalCount).toEqual(2);
      expect(
        plans.data.map((plan) => {
          return { id: plan.id, status: plan.status };
        }),
      ).toEqual(
        expect.arrayContaining([
          { id: initialPlan.id, status: 'PUBLISHED' },
          { id: createdPlan.id, status: 'CLOSED' },
        ]),
      );
    });

    test('should rollback "update on a plan"', async () => {
      // Given

      // Update the plan
      const updatedPlan = await succeed(
        v2APlansResourceAsApiPublisher.updateApiPlanRaw({
          envId,
          apiId: importedApi.id,
          planId: initialPlan.id,
          updatePlan: MAPIV2PlansFaker.newPlanV4({
            name: 'updated-plan-name',
            description: 'updated-plan-description',
            security: {
              type: PlanSecurityType.API_KEY,
              configuration: {
                propagateApiKey: true,
              },
            },
          }),
        }),
      );

      // Deploy
      await succeed(
        v2ApisResourceAsApiPublisher.createApiDeploymentRaw({
          envId,
          apiId: importedApi.id,
        }),
        202,
      );

      // When
      // Rollback to the first event
      const events = await succeed(
        v2EventsResourceAsApiPublisher.getApiEventsRaw({
          envId,
          apiId: importedApi.id,
          types: ['PUBLISH_API'],
        }),
      );
      expect(events.data).toHaveLength(2);
      await noContent(
        v2ApisResourceAsApiPublisher.rollbackApiRaw({
          envId,
          apiId: importedApi.id,
          apiRollback: {
            eventId: events.data[1].id,
          },
        }),
      );

      // Then
      const api = await succeed(
        v2ApisResourceAsApiPublisher.getApiRaw({
          envId,
          apiId: importedApi.id,
        }),
      );

      const plans = await succeed(
        v2APlansResourceAsApiPublisher.listApiPlansRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
      // Rollbacked plan values
      expect(plans.data[0].name).toEqual(initialPlan.name);
      expect(plans.data[0].security.configuration).toEqual(initialPlan.security.configuration);

      // Non rollbacked plan values
      expect(plans.data[0].description).toEqual(updatedPlan.description);
    });
  });
});
