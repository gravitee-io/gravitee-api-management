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
import { fetchGatewaySuccess } from '@gravitee/utils/gateway';
import { teardownApisAndApplications } from '@gravitee/utils/management';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';
const apisResourceAsPublisher = new APIsApi(forManagementAsApiUser());

const updateApi = async (api: ApiEntity) => {
  const updateApiEntity = {
    description: api.description,
    version: api.version,
    name: api.name,
    proxy: api.proxy,
    visibility: api.visibility,
    plans: api.plans,
  };

  await succeed(
    apisResourceAsPublisher.updateApiRaw({
      envId,
      orgId,
      api: api.id,
      updateApiEntity,
    }),
  );

  return succeed(apisResourceAsPublisher.deployApiRaw({ orgId, envId, api: api.id }));
};

describe('Add/edit/remove several endpoints to group and use them', () => {
  let createdApi: ApiEntity;

  beforeAll(async () => {
    // create an API
    createdApi = await succeed(
      apisResourceAsPublisher.importApiDefinitionRaw({
        envId,
        orgId,
        body: ApisFaker.apiImport({
          plans: [PlansFaker.plan({ status: PlanStatus.PUBLISHED })],
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

  test('Should add another endpoint to the default-group', async () => {
    // new endpoint
    createdApi.proxy.groups[0].endpoints[1] = {
      inherit: true,
      name: 'endpoint2',
      target: `${process.env.WIREMOCK_BASE_URL}/hello?name=endpoint2`,
      type: 'http',
      weight: 1,
    };

    const updatedApi = await updateApi(createdApi);
    expect(updatedApi.proxy.groups[0].endpoints).toHaveLength(2);
    const response1 = await fetchGatewaySuccess({ contextPath: createdApi.context_path }).then((res) => res.json());
    expect(response1.message).toBe('Hello, World!');
    const response2 = await fetchGatewaySuccess({ contextPath: createdApi.context_path }).then((res) => res.json());
    await fetchGatewaySuccess({
      contextPath: createdApi.context_path,
      expectedResponseValidator: async (response) => {
        const body = await response.json();
        expect(body.message).toBe('Hello, Endpoint2!');
        return true;
      },
    });
  });

  test('Should modify existing endpoint ', async () => {
    // modify endpoint2
    createdApi.proxy.groups[0].endpoints[1] = {
      inherit: true,
      name: 'updated endpoint2',
      target: `${process.env.WIREMOCK_BASE_URL}/hello?name=endpoint2update`,
      type: 'http',
      weight: 1,
    };

    const updatedApi = await updateApi(createdApi);
    expect(updatedApi.proxy.groups[0].endpoints).toHaveLength(2);
    const response1 = await fetchGatewaySuccess({ contextPath: createdApi.context_path }).then((res) => res.json());
    expect(response1.message).toBe('Hello, World!');

    await fetchGatewaySuccess({
      contextPath: createdApi.context_path,
      expectedResponseValidator: async (response) => {
        const body = await response.json();
        expect(body.message).toBe('Hello, Endpoint2update!');
        return true;
      },
    });
  });

  test('Should remove an endpoint', async () => {
    createdApi.proxy.groups[0].endpoints.splice(-1); // delete endpoint2
    const updatedApi = await updateApi(createdApi);
    expect(updatedApi.proxy.groups[0].endpoints).toHaveLength(1);
    const response = await fetchGatewaySuccess({ contextPath: createdApi.context_path }).then((res) => res.json());
    expect(response.message).toBe('Hello, World!');
  });

  afterAll(async () => {
    await teardownApisAndApplications(orgId, envId, [createdApi.id]);
  });
});
