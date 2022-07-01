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
import { fail, noContent, succeed } from '@lib/jest-utils';
import { ApiEntity } from '@management-models/ApiEntity';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResourceAsPublisher = new APIsApi(forManagementAsApiUser());
let createdApi: ApiEntity;
let updatedApi: ApiEntity;

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
    expect(createdApi.plans).toHaveLength(2);
  });

  test('should update an API from url', async () => {
    updatedApi = await succeed(
      apisResourceAsPublisher.updateApiWithDefinitionRaw({
        envId,
        orgId,
        api: createdApi.id,
        body: `${process.env.WIREMOCK_BASE_PATH}/api-whattimeisit.json`,
      }),
    );
    expect(updatedApi).toBeTruthy();
    expect(updatedApi.plans).toHaveLength(2);
  });

  test('should delete plans', async () => {
    for (const plan of updatedApi.plans) {
      await apisResourceAsPublisher.deleteApiPlanRaw({ orgId, envId, api: updatedApi.id, plan: plan.id });
    }
  });

  test('should delete api', async () => {
    await noContent(apisResourceAsPublisher.deleteApiRaw({ envId, orgId, api: updatedApi.id }));
  });
});
