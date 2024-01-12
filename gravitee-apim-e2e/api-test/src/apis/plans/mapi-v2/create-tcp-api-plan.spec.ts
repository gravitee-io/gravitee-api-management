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

import { forManagementV2AsApiUser } from '@gravitee/utils/configuration';
import { APIPlansApi, APIsApi, ApiV4, ListenerType, PlanSecurityType } from '../../../../../lib/management-v2-webclient-sdk/src/lib';
import { afterAll, beforeAll, expect, test } from '@jest/globals';
import { MAPIV2PlansFaker } from '@gravitee/fixtures/management/MAPIV2PlansFaker';
import { created, fail } from '@lib/jest-utils';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import faker from '@faker-js/faker';

const envId = 'DEFAULT';

const v2ApisResourceAsApiPublisher = new APIsApi(forManagementV2AsApiUser());
const v2APlansResourceAsApiPublisher = new APIPlansApi(forManagementV2AsApiUser());

describe('With a TCP API', () => {
  let importedApi: ApiV4;

  beforeAll(async () => {
    importedApi = await created(
      v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
        envId,
        exportApiV4: MAPIV2ApisFaker.apiImportV4({
          plans: [MAPIV2PlansFaker.planV4()],
          api: MAPIV2ApisFaker.apiV4({
            listeners: [
              {
                type: ListenerType.TCP,
                hosts: [faker.internet.domainName()],
                entrypoints: [
                  {
                    type: 'tcp-proxy',
                  },
                ],
              },
            ],
          }),
        }),
      }),
    );
    expect(importedApi).toBeTruthy();
  });

  afterAll(async () => {
    if (importedApi) {
      await v2ApisResourceAsApiPublisher.deleteApi({
        envId,
        apiId: importedApi.id,
      });
    }
  });

  test('should be able to add a keyless plan', async () => {
    const createdPlan = await created(
      v2APlansResourceAsApiPublisher.createApiPlanRaw({
        envId,
        apiId: importedApi.id,
        createPlan: MAPIV2PlansFaker.newPlanV4(),
      }),
    );

    expect(createdPlan).toBeTruthy();
  });

  test.each([PlanSecurityType.API_KEY, PlanSecurityType.JWT, PlanSecurityType.OAUTH2])(
    'should not be able to add a plan with type %s',
    async (type: PlanSecurityType) => {
      await fail(
        v2APlansResourceAsApiPublisher.createApiPlanRaw({
          envId,
          apiId: importedApi.id,
          createPlan: MAPIV2PlansFaker.newPlanV4({
            security: { type },
          }),
        }),
        400,
        'Security type not allowed',
      );
    },
  );
});
