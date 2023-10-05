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
import {
  ApiLogsResponse,
  APIsApi,
  APISubscriptionsApi,
  HttpListener,
  PlanSecurityType,
} from '@gravitee/management-v2-webclient-sdk/src/lib';
import { forManagementAsAppUser, forManagementV2AsApiUser } from '@gravitee/utils/configuration';
import { afterAll, beforeEach, describe, expect, test } from '@jest/globals';
import { created, noContent, succeed } from '@lib/jest-utils';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { AnalyticsLogsApi, APIPlansApi } from '../../../../../../../lib/management-v2-webclient-sdk/src/lib';
import { MAPIV2PlansFaker } from '@gravitee/fixtures/management/MAPIV2PlansFaker';
import { fetchGatewaySuccess, fetchRestApiSuccess } from '@gravitee/utils/apim-http';
import { ApplicationsApi } from '../../../../../../../lib/management-webclient-sdk/src/lib/apis/ApplicationsApi';
import { ApplicationsFaker } from '@gravitee/fixtures/management/ApplicationsFaker';

const envId = 'DEFAULT';
const orgId = 'DEFAULT';
const v2ApisResourceAsApiPublisher = new APIsApi(forManagementV2AsApiUser());
const v2ApiLogsResourceAsApiPublisher = new AnalyticsLogsApi(forManagementV2AsApiUser());
const applicationManagementApiAsAppUser = new ApplicationsApi(forManagementAsAppUser());
const v2ApiSubscriptionApiAsApiUser = new APISubscriptionsApi(forManagementV2AsApiUser());
const v2ApiPlanApiAsApiUser = new APIPlansApi(forManagementV2AsApiUser());

describe('API - V4 - Connection Logs', () => {
  describe('WITH V4 API with full logging enabled and API_KEY plan', () => {
    let testContext: Record<string, any>;

    beforeEach(() => {
      testContext = {};
    });

    test('should call the gateway with api keys and get the logs', async () => {
      // init test data
      await createApiWithPlan();
      await createApplicationsAndSubscribeToPlan();

      // fetch gateway with application 1
      await fetchGatewayWithApiKey(testContext.application1ApiKey.key);
      await fetchGatewayWithApiKey(testContext.application1ApiKey.key);
      await fetchGatewayWithApiKey(testContext.application1ApiKey.key);

      // fetch gateway with application 2
      await fetchGatewayWithApiKey(testContext.application2ApiKey.key);
      await fetchGatewayWithApiKey(testContext.application2ApiKey.key);

      // check connection logs
      await expectConnectionLogsForApplications(3, [
        { id: testContext.createdApplication1.id, name: testContext.createdApplication1.name },
      ]);
      await expectConnectionLogsForApplications(2, [
        { id: testContext.createdApplication2.id, name: testContext.createdApplication2.name },
      ]);
      await expectConnectionLogsForApplications(5, [
        { id: testContext.createdApplication1.id, name: testContext.createdApplication1.name },
        { id: testContext.createdApplication2.id, name: testContext.createdApplication2.name },
      ]);
    });

    const createApiWithPlan = async () => {
      // import API
      const importedApi = await created(
        v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
          envId,
          exportApiV4: MAPIV2ApisFaker.apiImportV4({
            plans: [MAPIV2PlansFaker.planV4({ security: { type: PlanSecurityType.API_KEY }, validation: 'AUTO' })],
            api: MAPIV2ApisFaker.apiV4Message({
              analytics: {
                enabled: true,
                logging: {
                  content: {
                    headers: true,
                    messageHeaders: true,
                    messagePayload: true,
                    messageMetadata: true,
                  },
                  phase: {
                    request: true,
                    response: true,
                  },
                  mode: {
                    entrypoint: true,
                    endpoint: true,
                  },
                },
                sampling: {
                  type: 'COUNT',
                  value: '10',
                },
              },
            }),
          }),
        }),
      );
      expect(importedApi).toBeTruthy();
      testContext.apiPath = (importedApi.listeners[0] as HttpListener).paths[0].path;

      // start API
      await noContent(
        v2ApisResourceAsApiPublisher.startApiRaw({
          envId,
          apiId: importedApi.id,
        }),
      );

      // get the api key plan
      const plans = await succeed(
        v2ApiPlanApiAsApiUser.listApiPlansRaw({
          apiId: importedApi.id,
          envId,
        }),
      );
      expect(plans.pagination.totalCount).toStrictEqual(1);
      expect(plans.data[0].security.type).toStrictEqual('API_KEY');
      expect(plans.data[0].validation).toStrictEqual('AUTO');
      testContext.apiKeyPlanId = plans.data[0].id;

      testContext.importedApi = importedApi;
    };

    const createApplicationsAndSubscribeToPlan = async () => {
      // create applications for subscription
      testContext.createdApplication1 = await created(
        applicationManagementApiAsAppUser.createApplicationRaw({
          orgId,
          envId,
          newApplicationEntity: ApplicationsFaker.newApplication(),
        }),
      );

      // Subscribe to API_KEY plan and get the key
      testContext.application1Subscription = await created(
        v2ApiSubscriptionApiAsApiUser.createApiSubscriptionRaw({
          apiId: testContext.importedApi.id,
          envId,
          createSubscription: {
            applicationId: testContext.createdApplication1.id,
            planId: testContext.apiKeyPlanId,
          },
        }),
      );

      testContext.application1ApiKey = await succeed(
        v2ApiSubscriptionApiAsApiUser.getApiSubscriptionApiKeysRaw({
          envId,
          apiId: testContext.importedApi.id,
          subscriptionId: testContext.application1Subscription.id,
        }),
      ).then((response) => response.data[0]);

      testContext.createdApplication2 = await created(
        applicationManagementApiAsAppUser.createApplicationRaw({
          orgId,
          envId,
          newApplicationEntity: ApplicationsFaker.newApplication(),
        }),
      );

      testContext.application2Subscription = await created(
        v2ApiSubscriptionApiAsApiUser.createApiSubscriptionRaw({
          apiId: testContext.importedApi.id,
          envId,
          createSubscription: {
            applicationId: testContext.createdApplication2.id,
            planId: testContext.apiKeyPlanId,
          },
        }),
      );

      testContext.application2ApiKey = await succeed(
        v2ApiSubscriptionApiAsApiUser.getApiSubscriptionApiKeysRaw({
          envId,
          apiId: testContext.importedApi.id,
          subscriptionId: testContext.application2Subscription.id,
        }),
      ).then((response) => response.data[0]);
    };

    const fetchGatewayWithApiKey = async (apiKey: string) => {
      await fetchGatewaySuccess({ contextPath: testContext.apiPath, headers: { 'X-Gravitee-Api-Key': apiKey } })
        .then((res) => res.json())
        .then((json) => {
          expect(json.items).toHaveLength(40);
          expect(json.items[0].id).toEqual('0');
          expect(json.items[0].content).toEqual('Mock message');
          expect(json.items[0].headers).toEqual({
            'X-Header': ['header-value'],
          });
          expect(json.items[0].metadata).toEqual({
            Metadata: 'metadata-value',
          });
        });
    };

    const expectConnectionLogsForApplications = async (expectedLogsCount: number, applications: { id: string; name: string }[]) => {
      let apiLogsResponse = await fetchRestApiSuccess<ApiLogsResponse>({
        restApiHttpCall: () =>
          v2ApiLogsResourceAsApiPublisher.getApiLogsRaw({
            envId,
            apiId: testContext.importedApi.id,
            applicationIds: applications.map((app) => app.id),
          }),
        maxRetries: 10,
        expectedResponseValidator: async (response) => {
          const body = response.value;
          return body.data.length === expectedLogsCount;
        },
      });

      expect(apiLogsResponse.data).toHaveLength(expectedLogsCount);
      testContext.apiKeyPlanId = apiLogsResponse.data[0].plan.id;

      for (let connectionLog of apiLogsResponse.data) {
        expect(connectionLog.plan.security.type).toEqual('API_KEY');
        expect(applications.map((app) => app.id)).toContain(connectionLog.application.id);
        expect(applications.map((app) => app.name)).toContain(connectionLog.application.name);
        expect(connectionLog.status).toEqual(200);
        expect(connectionLog.method).toEqual('GET');
      }
    };

    afterAll(async () => {
      const closeSubscriptionPromises = [testContext.application1Subscription.id, testContext.application2Subscription.id].map(
        (subscriptionId) =>
          succeed(
            v2ApiSubscriptionApiAsApiUser.closeApiSubscriptionRaw({
              envId,
              subscriptionId,
              apiId: testContext.importedApi.id,
            }),
          ),
      );
      await Promise.all(closeSubscriptionPromises);

      await noContent(
        v2ApisResourceAsApiPublisher.stopApiRaw({
          envId,
          apiId: testContext.importedApi.id,
        }),
      );

      await succeed(
        v2ApisResourceAsApiPublisher.updateApiRaw({
          envId,
          apiId: testContext.importedApi.id,
          updateApi: { ...testContext.importedApi, lifecycleState: 'DEPRECATED' },
        }),
      );

      await noContent(
        v2ApisResourceAsApiPublisher.deleteApiRaw({
          envId,
          apiId: testContext.importedApi.id,
        }),
      );

      const deletePromises = [testContext.createdApplication1, testContext.createdApplication2].map(({ id }) =>
        noContent(
          applicationManagementApiAsAppUser.deleteApplicationRaw({
            orgId,
            envId,
            application: id,
          }),
        ),
      );
      await Promise.all(deletePromises);
    });
  });
});
