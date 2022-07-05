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
import { ApisFaker } from '@management-fakers/ApisFaker';
import { PathOperatorOperatorEnum } from '@management-models/PathOperator';
import { ApiEntity, ApiEntityToJSON } from '@management-models/ApiEntity';
import { APIsApi } from '@management-apis/APIsApi';
import { forManagementAsApiUser } from '@client-conf/*';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { PlanSecurityType } from '@management-models/PlanSecurityType';
import { PlanStatus } from '@management-models/PlanStatus';
import { UpdateApiEntityFromJSON } from '@management-models/UpdateApiEntity';
import { LoadBalancerTypeEnum } from '@management-models/LoadBalancer';
import { LifecycleAction } from '@management-models/LifecycleAction';
import { fetchGatewaySuccess } from '@lib/gateway';
import { teardownApisAndApplications } from '@lib/management';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';
const apiManagementApiAsApiUser = new APIsApi(forManagementAsApiUser());
let createdApi: ApiEntity;

describe('Create several endpoint groups and use them', () => {
  beforeAll(async () => {
    createdApi = await apiManagementApiAsApiUser.importApiDefinition({
      envId,
      orgId,
      body: ApisFaker.apiImport({
        plans: [PlansFaker.plan({ security: PlanSecurityType.KEYLESS, status: PlanStatus.PUBLISHED })],
      }),
    });

    await apiManagementApiAsApiUser.updateApi({
      orgId,
      envId,
      api: createdApi.id,
      updateApiEntity: {
        ...UpdateApiEntityFromJSON(ApiEntityToJSON(createdApi)),
        proxy: {
          virtual_hosts: [
            {
              path: createdApi.context_path,
            },
          ],
          strip_context_path: false,
          preserve_host: false,
          groups: [
            {
              name: 'default-group',
              endpoints: [
                {
                  backup: false,
                  inherit: true,
                  name: 'endpoint1',
                  weight: 1,
                  type: 'http',
                  target: `${process.env.WIREMOCK_BASE_URL}/hello?name=endpoint1`,
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
            {
              name: 'endpoint_group_2',
              endpoints: [
                {
                  backup: false,
                  inherit: true,
                  name: 'endpoint2',
                  weight: 1,
                  type: 'http',
                  target: `${process.env.WIREMOCK_BASE_URL}/hello?name=endpoint2`,
                },
              ],
              load_balancing: {
                type: LoadBalancerTypeEnum.ROUNDROBIN,
              },
              services: {
                discovery: {
                  enabled: false,
                },
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
        },
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
            pre: [
              {
                name: 'Dynamic Routing',
                description: '',
                enabled: true,
                policy: 'dynamic-routing',
                configuration: {
                  rules: [
                    {
                      pattern: '/route1',
                      url: "{#endpoints['default-group']}",
                    },
                    {
                      pattern: '/route2',
                      url: "{#endpoints['endpoint_group_2']}",
                    },
                  ],
                },
              },
            ],
            post: [],
            enabled: true,
          },
        ],
      },
    });

    await apiManagementApiAsApiUser.doApiLifecycleAction({
      envId,
      orgId,
      api: createdApi.id,
      action: LifecycleAction.START,
    });
  });

  test('should reach endpoint1 by calling default-group', async () => {
    const response = await fetchGatewaySuccess({ contextPath: `${createdApi.context_path}/route1` }).then((res) => res.json());
    expect(response.message).toEqual('Hello, Endpoint1!');
  });

  test('should reach endpoint2 by calling endpoint group 2', async () => {
    const response = await fetchGatewaySuccess({ contextPath: `${createdApi.context_path}/route2` }).then((res) => res.json());
    expect(response.message).toEqual('Hello, Endpoint2!');
  });

  afterAll(async () => {
    await teardownApisAndApplications(orgId, envId, [createdApi.id]);
  });
});
