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
import { afterAll, beforeAll, describe, expect } from '@jest/globals';
import { succeed } from '@lib/jest-utils';
import { ApisFaker } from '@gravitee/fixtures/management/ApisFaker';
import { ApiEntity } from '@gravitee/management-webclient-sdk/src/lib/models/ApiEntity';
import { PlansFaker } from '@gravitee/fixtures/management/PlansFaker';
import { LifecycleAction } from '@gravitee/management-webclient-sdk/src/lib/models/LifecycleAction';
import { PlanStatus } from '@gravitee/management-webclient-sdk/src/lib/models/PlanStatus';
import { fetchGatewaySuccess } from '@gravitee/utils/apim-http';
import { LoadBalancerTypeEnum } from '@gravitee/management-webclient-sdk/src/lib/models/LoadBalancer';
import { teardownApisAndApplications } from '@gravitee/utils/management';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';
const apisResourceAsPublisher = new APIsApi(forManagementAsApiUser());

describe('Configure LB to round robin weighted and use it', () => {
  let createdApi: ApiEntity;

  beforeAll(async () => {
    // create an API with a published free plan and 2 endpoints in one endpoint group (lb: weighted round robin)
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
                    weight: 2,
                    backup: false,
                    type: 'http',
                  },
                ],
                load_balancing: {
                  type: LoadBalancerTypeEnum.WEIGHTED_ROUND_ROBIN,
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

  test('Should switch between two configured endpoints using weighted round-robin', async () => {
    const contextPath = createdApi.context_path;
    let endpoint1 = 0;
    let endpoint2 = 0;

    for (let i = 0; i < 6; i++) {
      const response = await fetchGatewaySuccess({ contextPath }).then((res) => res.json());
      if (response.message.match(/Endpoint1/)) endpoint1++;
      if (response.message.match(/Endpoint2/)) endpoint2++;
    }

    expect(endpoint1).toBe(2);
    expect(endpoint2).toBe(4);
  });

  afterAll(async () => {
    await teardownApisAndApplications(orgId, envId, [createdApi.id]);
  });
});
