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
import { ApiEntity } from '@management-models/ApiEntity';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { PlanSecurityType } from '@management-models/PlanSecurityType';
import { PlanStatus } from '@management-models/PlanStatus';
import { LoadBalancerTypeEnum } from '@management-models/LoadBalancer';
import { LifecycleAction } from '@management-models/LifecycleAction';
import { teardownApisAndApplications } from '@lib/management';
import { fetchGatewayBadRequest, fetchGatewaySuccess } from '@lib/gateway';
import { PathOperatorOperatorEnum } from '@management-models/PathOperator';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResource = new APIsApi(forManagementAsApiUser());

describe('Enable CORS on an API and use it', () => {
  let createdApi: ApiEntity;

  const expectNoAccessControlHeaders = (preflightResponse) => {
    expect(preflightResponse.headers.has('Access-Control-Allow-Headers')).toBe(false);
    expect(preflightResponse.headers.has('Access-Control-Allow-Methods')).toBe(false);
  };

  describe('Running policies on preflight requests', () => {
    beforeAll(async () => {
      createdApi = await apisResource.importApiDefinition({
        envId,
        orgId,
        body: ApisFaker.apiImport({
          plans: [PlansFaker.plan({ security: PlanSecurityType.KEYLESS, status: PlanStatus.PUBLISHED })],
          flows: [
            {
              name: '',
              path_operator: {
                path: '/',
                operator: PathOperatorOperatorEnum.STARTSWITH,
              },
              condition: '',
              consumers: [],
              methods: [],
              pre: [],
              post: [
                {
                  name: 'Transform Headers',
                  description: '',
                  enabled: true,
                  policy: 'transform-headers',
                  configuration: {
                    addHeaders: [
                      {
                        name: 'x-cors-enabled',
                        value: 'true',
                      },
                    ],
                    scope: 'RESPONSE',
                  },
                },
              ],
              enabled: true,
            },
          ],
          proxy: ApisFaker.proxy({
            groups: [
              {
                name: 'default-group',
                endpoints: [
                  {
                    backup: false,
                    inherit: true,
                    name: 'default',
                    weight: 1,
                    type: 'http',
                    target: `${process.env.WIREMOCK_BASE_URL}/hello`,
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
            cors: {
              enabled: true,
              allowCredentials: true,
              allowOrigin: ['https://gravitee.io'],
              allowHeaders: ['x-gravitee-test'],
              allowMethods: ['GET'],
              runPolicies: true,
              maxAge: -1,
            },
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

    test('should accept preflight request, and return cross origin headers in response', async () => {
      const preflightResponse = await fetchGatewaySuccess({
        contextPath: createdApi.context_path,
        method: 'OPTIONS',
        headers: {
          Origin: 'https://gravitee.io',
          'Access-Control-Request-Method': 'GET',
          'Access-Control-Request-Headers': 'x-gravitee-test',
        },
      });
      expect(preflightResponse.headers.get('Access-Control-Allow-Origin')).toBe('https://gravitee.io');
      expect(preflightResponse.headers.get('Access-Control-Allow-Headers')).toBe('x-gravitee-test');
      expect(preflightResponse.headers.get('Access-Control-Allow-Methods')).toBe('GET');
      expect(preflightResponse.headers.get('Access-Control-Allow-Credentials')).toBe('true');
      expect(preflightResponse.headers.get('x-cors-enabled')).toBe('true');
    });

    test('should reject preflight request with 200 with forbidden origin', async () => {
      const preflightResponse = await fetchGatewaySuccess({
        contextPath: createdApi.context_path,
        method: 'OPTIONS',
        headers: {
          Origin: 'https://gravitee.dev',
          'Access-Control-Request-Method': 'GET',
          'Access-Control-Request-Headers': 'x-gravitee-test',
        },
      });
      expectNoAccessControlHeaders(preflightResponse);
      expect(preflightResponse.headers.get('x-cors-enabled')).toBe('true');
    });

    test('should reject preflight request with 200 with forbidden header', async () => {
      const preflightResponse = await fetchGatewaySuccess({
        contextPath: createdApi.context_path,
        method: 'OPTIONS',
        headers: {
          origin: 'https://gravitee.io',
          'Access-Control-Request-Method': 'GET',
          'Access-Control-Request-Headers': 'x-gravitee-test, x-gravitee-dev',
        },
      });
      expect(preflightResponse.headers.get('Access-Control-Allow-Origin')).toBe('https://gravitee.io');
      expect(preflightResponse.headers.get('Access-Control-Allow-Credentials')).toBe('true');
      expect(preflightResponse.headers.has('Access-Control-Allow-Headers')).toBe(false);
      expect(preflightResponse.headers.has('Access-Control-Allow-Methods')).toBe(false);
      expect(preflightResponse.headers.get('x-cors-enabled')).toBe('true');
    });

    test('should reject preflight request with 200 with forbidden method', async () => {
      const preflightResponse = await fetchGatewaySuccess({
        contextPath: createdApi.context_path,
        method: 'OPTIONS',
        headers: {
          Origin: 'https://gravitee.io',
          'Access-Control-Request-Method': 'PUT',
          'Access-Control-Request-Headers': 'x-gravitee-test',
        },
      });
      expect(preflightResponse.headers.get('Access-Control-Allow-Origin')).toBe('https://gravitee.io');
      expect(preflightResponse.headers.get('Access-Control-Allow-Credentials')).toBe('true');
      expect(preflightResponse.headers.has('Access-Control-Allow-Headers')).toBe(false);
      expect(preflightResponse.headers.has('Access-Control-Allow-Methods')).toBe(false);
      expect(preflightResponse.headers.get('x-cors-enabled')).toBe('true');
    });

    afterAll(async () => {
      await teardownApisAndApplications(orgId, envId, [createdApi.id]);
    });
  });

  describe('Without running policies on preflight requests', () => {
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
                    backup: false,
                    inherit: true,
                    name: 'default',
                    weight: 1,
                    type: 'http',
                    target: `${process.env.WIREMOCK_BASE_URL}/hello`,
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
            cors: {
              enabled: true,
              allowCredentials: true,
              allowOrigin: ['https://gravitee.io'],
              allowHeaders: ['x-gravitee-test'],
              allowMethods: ['GET', 'PUT'],
              runPolicies: false,
              maxAge: -1,
            },
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

    test('should accept preflight request, and return cross origin headers in response', async () => {
      const preflightResponse = await fetchGatewaySuccess({
        contextPath: createdApi.context_path,
        method: 'OPTIONS',
        headers: {
          Origin: 'https://gravitee.io',
          'Access-Control-Request-Method': 'PUT',
          'Access-Control-Request-Headers': 'x-gravitee-test',
        },
      });
      expect(preflightResponse.headers.get('Access-Control-Allow-Origin')).toBe('https://gravitee.io');
      expect(preflightResponse.headers.get('Access-Control-Allow-Headers')).toBe('x-gravitee-test');
      expect(preflightResponse.headers.get('Access-Control-Allow-Methods')).toBe('GET, PUT');
      expect(preflightResponse.headers.get('Access-Control-Allow-Credentials')).toBe('true');
    });

    test('should reject preflight request with 400 with forbidden origin', async () => {
      const preflightResponse = await fetchGatewayBadRequest({
        contextPath: createdApi.context_path,
        method: 'OPTIONS',
        headers: {
          Origin: 'https://gravitee.dev',
          'Access-Control-Request-Method': 'GET',
          'Access-Control-Request-Headers': 'x-gravitee-test',
        },
      });
      expectNoAccessControlHeaders(preflightResponse);
    });

    test('should reject preflight request with 400 with forbidden header', async () => {
      const preflightResponse = await fetchGatewayBadRequest({
        contextPath: createdApi.context_path,
        method: 'OPTIONS',
        headers: {
          origin: 'https://gravitee.io',
          'Access-Control-Request-Method': 'GET',
          'Access-Control-Request-Headers': 'x-gravitee-test, x-gravitee-dev',
        },
      });
      expectNoAccessControlHeaders(preflightResponse);
    });

    test('should reject preflight request with 400 with forbidden method', async () => {
      const preflightResponse = await fetchGatewayBadRequest({
        contextPath: createdApi.context_path,
        method: 'OPTIONS',
        headers: {
          Origin: 'https://gravitee.io',
          'Access-Control-Request-Method': 'HEAD',
          'Access-Control-Request-Headers': 'x-gravitee-test',
        },
      });
      expectNoAccessControlHeaders(preflightResponse);
    });

    afterAll(async () => {
      await teardownApisAndApplications(orgId, envId, [createdApi.id]);
    });
  });
});
