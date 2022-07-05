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
import { afterAll, beforeAll, describe, expect } from '@jest/globals';
import { APIsApi } from '@management-apis/APIsApi';
import { forManagementAsApiUser } from '@client-conf/*';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { PlanSecurityType } from '@management-models/PlanSecurityType';
import { PlanStatus } from '@management-models/PlanStatus';
import { ApiEntity } from '@management-models/ApiEntity';
import { LoadBalancerTypeEnum } from '@management-models/LoadBalancer';
import { teardownApisAndApplications } from '@lib/management';

import { HealthCheckRequestMethodEnum } from '@management-models/HealthCheckRequest';
import { setWiremockState } from '@lib/wiremock';
import { LifecycleAction } from '@management-models/LifecycleAction';
import { HealthcheckType } from '@management-models/HealthcheckType';
import { fetchGatewaySuccess } from '@lib/gateway';
import { flakyRunner } from '@lib/jest-utils';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResource = new APIsApi(forManagementAsApiUser());

describe('Enable Health Check, view health of my endpoint and use it', () => {
  let createdApi: ApiEntity;

  beforeAll(async () => {
    createdApi = await apisResource.importApiDefinition({
      envId,
      orgId,
      body: ApisFaker.apiImport({
        plans: [PlansFaker.plan({ security: PlanSecurityType.KEYLESS, status: PlanStatus.PUBLISHED })],
        proxy: ApisFaker.proxy({
          groups: [
            {
              name: 'default-group',
              endpoints: [
                {
                  healthcheck: {
                    schedule: '*/1 * * * * *',
                    inherit: false,
                    steps: [
                      {
                        request: {
                          headers: [],
                          path: '/_health',
                          fromRoot: true,
                          method: HealthCheckRequestMethodEnum.GET,
                        },
                        response: {
                          assertions: ['#response.status == 200'],
                        },
                      },
                    ],
                    enabled: true,
                  },
                  backup: false,
                  inherit: true,
                  name: 'default',
                  weight: 1,
                  type: 'http',
                  target: `${process.env.WIREMOCK_BASE_URL}/hello?name=healthy`,
                },
                {
                  backup: true,
                  inherit: true,
                  name: 'failover',
                  weight: 1,
                  type: 'http',
                  target: `${process.env.WIREMOCK_BASE_URL}/hello?name=unhealthy`,
                },
              ],
              load_balancing: {
                type: LoadBalancerTypeEnum.ROUNDROBIN,
              },
              http: {
                connectTimeout: 5000,
                idleTimeout: 60000,
                keepAlive: true,
                readTimeout: 10000,
                pipelining: false,
                maxConcurrentConnections: 100,
                useCompression: true,
                followRedirects: false,
              },
            },
          ],
        }),
      }),
    });

    await apisResource.doApiLifecycleAction({
      envId,
      orgId,
      api: createdApi.id,
      action: LifecycleAction.START,
    });
  });

  describe('With Healthy Endpoint', () => {
    test('should return default endpoint response', async () => {
      const responseBody = await fetchGatewaySuccess({
        contextPath: `${createdApi.context_path}`,
      }).then((res) => res.json());

      expect(responseBody.message).toBe('Hello, Healthy!');
    });

    flakyRunner(3, 5000).test('should be 100% available', async () => {
      const metric = await apisResource.getApiHealth({ orgId, envId, api: createdApi.id, type: HealthcheckType.AVAILABILITY });
      expect(metric.global['1m']).toBe(100);
    });
  });

  describe('With Unhealthy Endpoint', () => {
    beforeAll(async () => {
      await setWiremockState('health', 'Unhealthy');
    });

    test('should return failover response', async () => {
      await fetchGatewaySuccess({
        contextPath: `${createdApi.context_path}`,
        async expectedResponseValidator(response) {
          const body = await response.json();
          return body.message === 'Hello, Unhealthy!';
        },
      });
    });

    flakyRunner(3, 5000).test('should NOT be 100% available', async () => {
      const metric = await apisResource.getApiHealth({ orgId, envId, api: createdApi.id, type: HealthcheckType.AVAILABILITY });
      expect(metric.global['1m']).not.toBe(100);
    });

    afterAll(async () => {
      await setWiremockState('health', 'Started');
    });
  });

  afterAll(async () => {
    await teardownApisAndApplications(orgId, envId, [createdApi.id]);
  });
});
