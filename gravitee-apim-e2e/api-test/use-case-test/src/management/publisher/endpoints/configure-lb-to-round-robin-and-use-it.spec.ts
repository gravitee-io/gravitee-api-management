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

const orgId = 'DEFAULT';
const envId = 'DEFAULT';
const apisResourceAsPublisher = new APIsApi(forManagementAsApiUser());

describe('Configure LB to round robin and use it', () => {
  let createdApi: ApiEntity;

  beforeAll(async () => {
    // create an API with a published free plan and 3 endpoints in one endpoint group (lb: round robin)
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
                    target: `${process.env.WIREMOCK_BASE_URL}/hello?name=endpoint1`,
                    weight: 1,
                    backup: false,
                    type: 'http',
                  },
                  {
                    inherit: true,
                    name: 'endpoint2',
                    target: `${process.env.WIREMOCK_BASE_URL}/hello?name=endpoint2`,
                    weight: 1,
                    backup: false,
                    type: 'http',
                  },
                  {
                    inherit: true,
                    name: 'endpoint3',
                    target: `${process.env.WIREMOCK_BASE_URL}/hello?name=endpoint3`,
                    weight: 1,
                    backup: false,
                    type: 'http',
                  },
                ],
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

  test('Should switch between three configured endpoints using round-robin', async () => {
    const endpointArray = [1, 2, 3, 1];
    const contextPath = createdApi.context_path;

    for (let endpointNumber of endpointArray) {
      const response = await fetchGatewaySuccess({ contextPath }).then((res) => res.json());
      expect(response.message).toEqual(`Hello, Endpoint${endpointNumber}!`);
    }
  });

  afterAll(async () => {
    await teardownApisAndApplications(orgId, envId, [createdApi.id]);
  });
});
