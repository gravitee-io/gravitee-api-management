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

import { APIsApi } from '@management-apis/APIsApi';
import { forManagementAsApiUser } from '@client-conf/*';
import { afterAll, beforeAll, describe, expect } from '@jest/globals';
import { succeed } from '@lib/jest-utils';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { ApiEntity } from '@management-models/ApiEntity';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { LifecycleAction } from '@management-models/LifecycleAction';
import { PlanStatus } from '@management-models/PlanStatus';
import { fetchGatewaySuccess } from '@lib/gateway';
import { teardownApisAndApplications } from '@lib/management';
import { LoadBalancerTypeEnum } from '@management-models/LoadBalancer';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';
const apisResourceAsPublisher = new APIsApi(forManagementAsApiUser());

describe('Configure LB to round robin and use it', () => {
  let createdApi: ApiEntity;

  beforeAll(async () => {
    // create an API with a published free plan and 2 endpoints in one endpoint group (lb: random)
    createdApi = await succeed(
      apisResourceAsPublisher.importApiDefinitionRaw({
        envId,
        orgId,
        body: ApisFaker.apiImport({
          plans: [PlansFaker.plan({ status: PlanStatus.PUBLISHED })],
          proxy: ApisFaker.proxy({
            groups: [
              {
                name: 'default-group',
                endpoints: [
                  {
                    inherit: true,
                    name: 'default',
                    target: `${process.env.WIREMOCK_BASE_URL}/hello?name=endpoint0`,
                    weight: 1,
                    backup: false,
                    type: 'http',
                  },
                  {
                    inherit: true,
                    name: 'endpoint2',
                    target: `${process.env.WIREMOCK_BASE_URL}/hello?name=endpoint1`,
                    weight: 2,
                    backup: false,
                    type: 'http',
                  },
                ],
                load_balancing: {
                  type: LoadBalancerTypeEnum.WEIGHTEDRANDOM,
                },
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
      api: createdApi.id,
      action: LifecycleAction.START,
    });
  });

  test('Should switch between two configured endpoints using a random loadbalancer', async () => {
    expect(true).toBe(true);
    const contextPath = createdApi.context_path;
    const numberOfSendRequests = 30; // amount of request to be sent
    let endpointHits: number[] = []; // array containing information about which endpoint was hit
    let results: number[] = []; // array containing the sums of two consecutive numbers (target
    let endpointCounter: number[] = [0, 0]; // array counting hits of each endpoint

    for (let i = 0; i < numberOfSendRequests; i++) {
      const response = await fetchGatewaySuccess({ contextPath }).then((res) => res.json());
      let match = response.message.match(/Endpoint(\d)/);
      endpointHits.push(match[1]);
      endpointCounter[match[1]]++;
    }

    // sum up 2 consecutive numbers to check the distribution (verify randomness)
    // 0: endpoint0 was reached twice in a row
    // 1: endpoints alternated
    // 2: endpoint1 was reached twice in a row
    for (let i = 1; i < endpointHits.length; i++) {
      results.push(+endpointHits[i - 1] + +endpointHits[i]);
    }

    expect(endpointCounter[0]).toBe(numberOfSendRequests / 3);
    expect(endpointCounter[1]).toBe((numberOfSendRequests * 2) / 3);
    expect(endpointHits.length).toBe(numberOfSendRequests);
    expect(results.includes(0) || results.includes(2)).toBe(true);
    expect(results).toContain(1);
  });

  afterAll(async () => {
    await teardownApisAndApplications(orgId, envId, [createdApi.id]);
  });
});
