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
import { forManagementAsApiUser, forPortalAsApiUser } from '@gravitee/utils/configuration';
import { afterAll, beforeAll, describe, expect } from '@jest/globals';
import { fail, succeed } from '@lib/jest-utils';
import { ApisFaker } from '@gravitee/fixtures/management/ApisFaker';
import { ApiEntity } from '@gravitee/management-webclient-sdk/src/lib/models/ApiEntity';
import { PlansFaker } from '@gravitee/fixtures/management/PlansFaker';
import { LifecycleAction } from '@gravitee/management-webclient-sdk/src/lib/models/LifecycleAction';
import { PlanStatus } from '@gravitee/management-webclient-sdk/src/lib/models/PlanStatus';
import { fetchGatewaySuccess } from '@gravitee/utils/gateway';
import { teardownApisAndApplications } from '@gravitee/utils/management';
import { ApiLifecycleState, UpdateApiEntityFromJSON, Visibility } from '@gravitee/management-webclient-sdk/src/lib/models';
import { ApiApi } from '@gravitee/portal-webclient-sdk/src/lib';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';
const apisResourceAsPublisher = new APIsApi(forManagementAsApiUser());
const portalApiResourceAsPublisher = new ApiApi(forPortalAsApiUser());

describe('Deprecate API and test the portal and API', () => {
  let createdApi: ApiEntity;

  beforeAll(async () => {
    // create an API with a published free plan
    createdApi = await succeed(
      apisResourceAsPublisher.importApiDefinitionRaw({
        envId,
        orgId,
        body: ApisFaker.apiImport({
          plans: [PlansFaker.plan({ status: PlanStatus.PUBLISHED })],
        }),
      }),
    );

    // start API
    await apisResourceAsPublisher.doApiLifecycleAction({
      envId,
      orgId,
      api: createdApi.id,
      action: LifecycleAction.START,
    });

    // make sure API endpoint is working
    await fetchGatewaySuccess({ contextPath: createdApi.context_path }).then((res) => res.json());

    // publish API
    await apisResourceAsPublisher.updateApi({
      api: createdApi.id,
      updateApiEntity: UpdateApiEntityFromJSON({
        ...createdApi,
        lifecycle_state: ApiLifecycleState.PUBLISHED,
        visibility: Visibility.PUBLIC,
      }),
      orgId,
      envId,
    });

    // make sure API can be found in the portal
    await portalApiResourceAsPublisher.getApiByApiId({ apiId: createdApi.id });

    // deprecate the API
    await apisResourceAsPublisher.updateApi({
      envId,
      orgId,
      api: createdApi.id,
      updateApiEntity: {
        lifecycle_state: ApiLifecycleState.DEPRECATED,
        description: createdApi.description,
        name: createdApi.name,
        proxy: createdApi.proxy,
        version: createdApi.version,
        visibility: createdApi.visibility,
      },
    });
  });

  test('Should be able to connect to deprecated API', async () => {
    await fetchGatewaySuccess({ contextPath: createdApi.context_path }).then((res) => res.json());
  });

  test('Should find deprecated API in management console', async () => {
    let responseApi = await succeed(apisResourceAsPublisher.getApiRaw({ orgId, envId, api: createdApi.id }));
    expect(responseApi.lifecycle_state).toEqual(ApiLifecycleState.DEPRECATED);
  });

  test('Should fail to find deprecated API in portal', async () => {
    await fail(portalApiResourceAsPublisher.getApiByApiId({ apiId: createdApi.id }), 404);
  });

  afterAll(async () => {
    await teardownApisAndApplications(orgId, envId, [createdApi.id]);
  });
});
