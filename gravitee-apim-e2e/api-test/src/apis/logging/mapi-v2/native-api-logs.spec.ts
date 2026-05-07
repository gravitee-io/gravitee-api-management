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
import { afterAll, beforeAll, describe, expect, test } from '@jest/globals';
import fetch from 'node-fetch';
import { APIAnalyticsApi, APIsApi, ApiV4 } from '@gravitee/management-v2-webclient-sdk/src/lib';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { created, fail, noContent, succeed } from '@lib/jest-utils';
import { API_USER, forManagementV2AsApiUser, forManagementV2AsAppUser } from '@gravitee/utils/configuration';

const ENV_ID = 'DEFAULT';
const FROM = 0;
const TO = Date.now();

const apisAsApiUser = new APIsApi(forManagementV2AsApiUser());
const analyticsAsApiUser = new APIAnalyticsApi(forManagementV2AsApiUser());
const analyticsAsAppUser = new APIAnalyticsApi(forManagementV2AsAppUser());

const baseUrl = process.env.MANAGEMENT_V2_BASE_URL;
const basicAuthHeader = `Basic ${Buffer.from(`${API_USER.username}:${API_USER.password}`).toString('base64')}`;

const rawApiUserGet = (path: string) =>
  fetch(`${baseUrl}/environments/${ENV_ID}/apis${path}`, {
    method: 'GET',
    headers: { Authorization: basicAuthHeader },
  });

describe('API V4 - Native API Logs', () => {
  let api: ApiV4;

  beforeAll(async () => {
    expect(baseUrl).toBeTruthy();
    api = await created(
      apisAsApiUser.createApiWithImportDefinitionRaw({
        envId: ENV_ID,
        exportApiV4: MAPIV2ApisFaker.apiImportV4({ api: MAPIV2ApisFaker.apiV4NativeKafka() }),
      }),
    );
  });

  afterAll(async () => {
    if (!api?.id) {
      return;
    }
    await noContent(apisAsApiUser.deleteApiRaw({ envId: ENV_ID, apiId: api.id }));
  });

  describe('search', () => {
    test('returns empty page when no data', async () => {
      const response = await succeed(analyticsAsApiUser.getNativeApiLogsRaw({ envId: ENV_ID, apiId: api.id, from: FROM, to: TO }));

      expect(response.data).toHaveLength(0);
      expect(response.pagination.totalCount).toEqual(0);
    });

    test('accepts application, plan and connection-status filters', async () => {
      const response = await succeed(
        analyticsAsApiUser.getNativeApiLogsRaw({
          envId: ENV_ID,
          apiId: api.id,
          from: FROM,
          to: TO,
          applicationIds: ['app-1'],
          planIds: ['plan-1'],
          connectionStatuses: ['CONNECTED', 'INTERNAL_ERROR'],
        }),
      );

      expect(response.data).toHaveLength(0);
      expect(response.pagination.totalCount).toEqual(0);
    });

    test('rejects with 400 when from and to are missing', async () => {
      const response = await rawApiUserGet(`/${api.id}/logs/native`);
      expect(response.status).toEqual(400);
    });

    test('rejects with 403 when caller lacks permission', async () => {
      await fail(analyticsAsAppUser.getNativeApiLogsRaw({ envId: ENV_ID, apiId: api.id, from: FROM, to: TO }), 403);
    });
  });

  describe('summary', () => {
    test('returns empty counts when no data', async () => {
      const response = await succeed(analyticsAsApiUser.getNativeApiLogsSummaryRaw({ envId: ENV_ID, apiId: api.id, from: FROM, to: TO }));

      expect(response.countByConnectionStatus ?? {}).toEqual({});
    });

    test('rejects with 400 when from and to are missing', async () => {
      const response = await rawApiUserGet(`/${api.id}/logs/native/summary`);
      expect(response.status).toEqual(400);
    });

    test('rejects with 403 when caller lacks permission', async () => {
      await fail(analyticsAsAppUser.getNativeApiLogsSummaryRaw({ envId: ENV_ID, apiId: api.id, from: FROM, to: TO }), 403);
    });
  });

  describe('find', () => {
    test('returns 404 for unknown request id', async () => {
      await fail(
        analyticsAsApiUser.getNativeApiLogRaw({
          envId: ENV_ID,
          apiId: api.id,
          requestId: 'unknown-request-id',
          from: FROM,
          to: TO,
        }),
        404,
      );
    });

    test('rejects with 400 when from and to are missing', async () => {
      const response = await rawApiUserGet(`/${api.id}/logs/native/unknown-request-id`);
      expect(response.status).toEqual(400);
    });

    test('rejects with 403 when caller lacks permission', async () => {
      await fail(
        analyticsAsAppUser.getNativeApiLogRaw({
          envId: ENV_ID,
          apiId: api.id,
          requestId: 'unknown-request-id',
          from: FROM,
          to: TO,
        }),
        403,
      );
    });
  });
});
