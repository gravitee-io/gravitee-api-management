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
import { APIsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/APIsApi';
import { forManagementAsApiUser } from '@gravitee/utils/configuration';
import { afterAll, beforeAll, describe, expect, test } from '@jest/globals';
import { succeed } from '@lib/jest-utils';
import { ApisFaker } from '@gravitee/fixtures/management/ApisFaker';
import { ApiEntity } from '@gravitee/management-webclient-sdk/src/lib/models/ApiEntity';
import { PlansFaker } from '@gravitee/fixtures/management/PlansFaker';
import { LifecycleAction } from '@gravitee/management-webclient-sdk/src/lib/models/LifecycleAction';
import { PlanStatus } from '@gravitee/management-webclient-sdk/src/lib/models/PlanStatus';
import { fetchGatewayServiceUnavailable, fetchGatewaySuccess } from '@gravitee/utils/apim-http';
import { teardownApisAndApplications } from '@gravitee/utils/management';
import { verifyWiremockRequest } from '@gravitee/utils/wiremock';
import fetchApi from 'node-fetch';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';
const apisResourceAsPublisher = new APIsApi(forManagementAsApiUser());

describe('Enable failover and trigger it', () => {
  const brokenEndpoint = {
    inherit: true,
    name: 'Broken endpoint',
    target: `${process.env.WIREMOCK_BASE_URL}/delayed`,
    weight: 1,
    backup: false,
    type: 'http',
  };

  const workingEndpoint = {
    inherit: true,
    name: 'Working endpoint',
    target: `${process.env.WIREMOCK_BASE_URL}/hello?name=endpoint`,
    weight: 1,
    backup: false,
    type: 'http',
  };

  describe('No failover, 1 broken endpoint and 1 working endpoint', () => {
    let createdApi1: ApiEntity;

    beforeAll(async () => {
      // create an API with a published free plan and 2 endpoints in one endpoint group (lb: round-robin)
      createdApi1 = await succeed(
        apisResourceAsPublisher.importApiDefinitionRaw({
          envId,
          orgId,
          body: ApisFaker.apiImport({
            plans: [PlansFaker.plan({ status: PlanStatus.PUBLISHED })],
            proxy: ApisFaker.proxy({
              groups: [
                {
                  name: 'default-group',
                  endpoints: [brokenEndpoint, workingEndpoint],
                },
              ],
            }),
          }),
        }),
      );

      // start API
      await apisResourceAsPublisher.doApiLifecycleAction({
        envId,
        orgId,
        api: createdApi1.id,
        action: LifecycleAction.START,
      });
    });

    test('Should fail to connect with the first endpoint', async () => {
      await fetchGatewayServiceUnavailable({
        contextPath: createdApi1.context_path,
        async expectedResponseValidator(response) {
          return response.headers.has('X-Wiremock');
        },
      });
    });

    afterAll(async () => {
      await teardownApisAndApplications(orgId, envId, [createdApi1.id]);
    });
  });

  describe('With failover, 2 broken endpoints and 1 working endpoint', () => {
    let createdApi2: ApiEntity;

    beforeAll(async () => {
      // create an API with a published free plan and 3 endpoints in one endpoint group (lb: round-robin) and failover
      createdApi2 = await succeed(
        apisResourceAsPublisher.importApiDefinitionRaw({
          envId,
          orgId,
          body: ApisFaker.apiImport({
            plans: [PlansFaker.plan({ status: PlanStatus.PUBLISHED })],
            proxy: ApisFaker.proxy({
              failover: { maxAttempts: 5, retryTimeout: 2000 },
              groups: [
                {
                  name: 'default-group',
                  endpoints: [brokenEndpoint, { ...brokenEndpoint, name: 'Broken endpoint2' }, workingEndpoint],
                },
              ],
            }),
          }),
        }),
      );

      // start it
      await apisResourceAsPublisher.doApiLifecycleAction({
        envId,
        orgId,
        api: createdApi2.id,
        action: LifecycleAction.START,
      });
    });

    test('Should succeed to reach the working endpoint after failover happened', async () => {
      await fetchGatewaySuccess({ contextPath: createdApi2.context_path });
      const { count: before } = await verifyWiremockRequest('/delayed', 'GET').then((res) => res.json());
      const response = await fetchApi(`${process.env.GATEWAY_BASE_URL}${createdApi2.context_path}`).then((res) => res.json());
      const { count: after } = await verifyWiremockRequest('/delayed', 'GET').then((res) => res.json());
      expect(after - before).toEqual(2);
      expect(response.message).toEqual('Hello, Endpoint!');
    });

    afterAll(async () => {
      await teardownApisAndApplications(orgId, envId, [createdApi2.id]);
    });
  });

  describe('With failover, 2 broken endpoints', () => {
    let createdApi3: ApiEntity;

    beforeAll(async () => {
      // create an API with a published free plan and 3 endpoints in one endpoint group (lb: round-robin) and failover
      createdApi3 = await succeed(
        apisResourceAsPublisher.importApiDefinitionRaw({
          envId,
          orgId,
          body: ApisFaker.apiImport({
            plans: [PlansFaker.plan({ status: PlanStatus.PUBLISHED })],
            proxy: ApisFaker.proxy({
              failover: { maxAttempts: 1, retryTimeout: 2000 },
              groups: [
                {
                  name: 'default-group',
                  http: {
                    connectTimeout: 1500,
                    readTimeout: 3000,
                  },
                  endpoints: [brokenEndpoint, { ...brokenEndpoint, name: 'Broken endpoint2' }, workingEndpoint],
                },
              ],
            }),
          }),
        }),
      );

      // start API
      await apisResourceAsPublisher.doApiLifecycleAction({
        envId,
        orgId,
        api: createdApi3.id,
        action: LifecycleAction.START,
      });
    });

    test('Should fail after trying to reach bad endpoints <MAX ATTEMPT> times', async () => {
      await fetchGatewaySuccess({ contextPath: createdApi3.context_path, maxRetries: 20 });
      const { count: before } = await verifyWiremockRequest('/delayed', 'GET').then((res) => res.json());
      const response = await fetchApi(`${process.env.GATEWAY_BASE_URL}${createdApi3.context_path}`);
      const { count: after } = await verifyWiremockRequest('/delayed', 'GET').then((res) => res.json());
      expect(after - before).toEqual(2);
      expect(response.status).toEqual(502);
    }, 35000);

    afterAll(async () => {
      await teardownApisAndApplications(orgId, envId, [createdApi3.id]);
    });
  });
});
