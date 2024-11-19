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
import { ApiLogsResponse, ApiMessageLogsResponse, APIsApi, HttpListener } from '@gravitee/management-v2-webclient-sdk/src/lib';
import { forManagementV2AsApiUser } from '@gravitee/utils/configuration';
import { afterAll, describe, expect, test } from '@jest/globals';
import { created, noContent } from '@lib/jest-utils';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { APIAnalyticsApi, APIPlansApi, ApiV4, Pagination } from '@gravitee/management-v2-webclient-sdk/src/lib';
import { MAPIV2PlansFaker } from '@gravitee/fixtures/management/MAPIV2PlansFaker';
import { fetchGatewaySuccess, fetchRestApiSuccess } from '@gravitee/utils/apim-http';
import { DateUtils } from '@gravitee/utils/dates';

const envId = 'DEFAULT';
const v2ApisResourceAsApiPublisher = new APIsApi(forManagementV2AsApiUser());
const v2PlansResourceAsApiPublisher = new APIPlansApi(forManagementV2AsApiUser());
const v2ApiLogsResourceAsApiPublisher = new APIAnalyticsApi(forManagementV2AsApiUser());

describe('API - V4 - MESSAGE - Search logs', () => {
  describe('Search Message API connection and message logs', () => {
    let importedApi: ApiV4;
    let apiPath;
    let keylessPlanId;
    let requestId;

    test('should import v4 API with full logging enabled and KEYLESS plan', async () => {
      importedApi = await created(
        v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
          envId,
          exportApiV4: MAPIV2ApisFaker.apiImportV4({
            plans: [MAPIV2PlansFaker.planV4()],
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
      apiPath = (importedApi.listeners[0] as HttpListener).paths[0].path;
    });

    test('should deploy v4 api', async () => {
      await noContent(
        v2ApisResourceAsApiPublisher.startApiRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
    });

    test('should call gateway two times', async () => {
      await fetchGatewaySuccess({ contextPath: apiPath })
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
            sourceTimestamp: expect.any(Number),
          });
        });
      await fetchGatewaySuccess({ contextPath: apiPath })
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
            sourceTimestamp: expect.any(Number),
          });
        });
    });

    test('should search connection logs', async () => {
      let apiLogsResponse = await fetchRestApiSuccess<ApiLogsResponse>({
        restApiHttpCall: () =>
          v2ApiLogsResourceAsApiPublisher.getApiLogsRaw({
            envId,
            apiId: importedApi.id,
          }),
        maxRetries: 10,
        expectedResponseValidator: async (response) => {
          const body = response.value;
          // Retry until response contains the two expected connection logs
          return body.data.length === 2;
        },
      });
      expect(apiLogsResponse.data).toHaveLength(2);
      keylessPlanId = apiLogsResponse.data[0].plan.id;
      requestId = apiLogsResponse.data[0].requestId;

      for (let connectionLog of apiLogsResponse.data) {
        expect(connectionLog.plan.security.type).toEqual('KEY_LESS');
        expect(connectionLog.application.id).toEqual('1');
        expect(connectionLog.application.name).toEqual('Unknown');
        expect(connectionLog.status).toEqual(connectionLog.requestEnded ? 200 : 0);
        expect(connectionLog.method).toEqual('GET');
      }
      // First element should be the most recent.
      expect(apiLogsResponse.data[0].timestamp.getTime()).toBeGreaterThan(apiLogsResponse.data[1].timestamp.getTime());
      expect(apiLogsResponse.pagination).toEqual(<Pagination>{
        page: 1,
        perPage: 10,
        pageCount: 1,
        pageItemsCount: 2,
        totalCount: 2,
      });
      expect(apiLogsResponse.links.self).toContain(`/apis/${importedApi.id}/logs`);
    });

    /**
     * Without the message sampling set to COUNT = 10, and the HTTP-GET configured with a messageLimitCount of 40,
     * we can expect 4 message logs.
     */
    test('should search message logs', async () => {
      let apiMessageLogsResponse = await fetchRestApiSuccess<ApiMessageLogsResponse>({
        restApiHttpCall: () =>
          v2ApiLogsResourceAsApiPublisher.getApiMessageLogsRaw({
            envId,
            apiId: importedApi.id,
            requestId,
            perPage: 2,
          }),
        maxRetries: 10,
        expectedResponseValidator: async (response) => {
          const body = response.value;
          // Retry until response contains all the four expected message logs for the page
          return body.pagination.totalCount === 4;
        },
      });
      expect(apiMessageLogsResponse.data).toHaveLength(2);

      expect(DateUtils.isReverseChronological(apiMessageLogsResponse.data.map((data) => data.timestamp))).toBeTruthy();
      apiMessageLogsResponse.data.forEach((messageLog) => {
        expect(messageLog.requestId).toEqual(requestId);
        expect(messageLog.operation).toEqual('SUBSCRIBE');
        expect(messageLog.entrypoint).toEqual(
          expect.objectContaining({
            connectorId: 'http-get',
            payload: 'Mock message',
            headers: {
              'X-Header': ['header-value'],
            },
            metadata: {
              Metadata: 'metadata-value',
              sourceTimestamp: expect.any(String),
            },
            isError: undefined,
          }),
        );
        expect(messageLog.endpoint).toEqual(
          expect.objectContaining({
            connectorId: 'mock',
            payload: 'Mock message',
            headers: {
              'X-Header': ['header-value'],
            },
            metadata: {
              Metadata: 'metadata-value',
              sourceTimestamp: expect.any(String),
            },
            isError: undefined,
          }),
        );
      });
      // First element should be the most recent.
      expect(apiMessageLogsResponse.pagination).toEqual(<Pagination>{
        page: 1,
        perPage: 2,
        pageCount: 2,
        pageItemsCount: 2,
        totalCount: 4,
      });
      expect(apiMessageLogsResponse.links.self).toContain(`/apis/${importedApi.id}/logs/${requestId}/messages`);
    });

    afterAll(async () => {
      await noContent(
        v2PlansResourceAsApiPublisher.deleteApiPlanRaw({
          envId,
          apiId: importedApi.id,
          planId: keylessPlanId,
        }),
      );
      await noContent(
        v2ApisResourceAsApiPublisher.stopApiRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
      await noContent(
        v2ApisResourceAsApiPublisher.deleteApiRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
    });
  });
});
