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
import { afterAll, describe, expect, test } from '@jest/globals';
import { APIsApi } from '@management-apis/APIsApi';
import { forManagementAsApiUser } from '@client-conf/*';
import { fail, succeed } from '@lib/jest-utils';
import { ApiEntity } from '@management-models/ApiEntity';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResourceAsPublisher = new APIsApi(forManagementAsApiUser());
let createdApi: ApiEntity;

describe('API - Imports by url', () => {
  test('should create an API from url', async () => {
    createdApi = await succeed(
      apisResourceAsPublisher.importApiDefinitionRaw({
        envId,
        orgId,
        body: `${process.env.WIREMOCK_BASE_PATH}/api-whattimeisit.json`,
      }),
    );
    expect(createdApi).toBeTruthy();
  });

  test('should update an API from url', async () => {
    await succeed(
      apisResourceAsPublisher.updateWithDefinitionPUTRaw({
        envId,
        orgId,
        api: createdApi.id,
        body: `${process.env.WIREMOCK_BASE_PATH}/api-whattimeisit.json`,
      }),
    );
  });

  afterAll(async () => {
    for (const plan of createdApi.plans) {
      await apisResourceAsPublisher.deleteApiPlan({ orgId, envId, api: createdApi.id, plan: plan.id });
    }
    await apisResourceAsPublisher.deleteApiRaw({ envId, orgId, api: createdApi.id });
  });
});
