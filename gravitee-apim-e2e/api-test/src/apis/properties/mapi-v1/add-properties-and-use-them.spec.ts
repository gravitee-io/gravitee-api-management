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
import { afterAll, beforeAll, describe, expect, test } from '@jest/globals';
import { succeed } from '@lib/jest-utils';
import { APIsApi } from '../../../../../lib/management-webclient-sdk/src/lib/apis/APIsApi';
import { ApisFaker } from '@gravitee/fixtures/management/ApisFaker';
import { PlansFaker } from '@gravitee/fixtures/management/PlansFaker';
import { LifecycleAction } from '../../../../../lib/management-webclient-sdk/src/lib/models/LifecycleAction';
import { forManagementAsApiUser } from '@gravitee/utils/configuration';
import {
  ApiEntity,
  PathOperatorOperatorEnum,
  PlanSecurityType,
  PlanStatus,
  UpdateApiEntityFromJSON,
} from '../../../../../lib/management-webclient-sdk/src/lib/models';
import { teardownApisAndApplications } from '@gravitee/utils/management';
import { PropertyEntity } from '@model/apis';
import { fetchGatewaySuccess } from '@gravitee/utils/apim-http';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';
const apisManagementApiAsApiUser = new APIsApi(forManagementAsApiUser());

describe('Add properties and use them', () => {
  let createdApi: ApiEntity;

  beforeAll(async () => {
    // create APIs with a published keyless plan and mock policy
    createdApi = await apisManagementApiAsApiUser.importApiDefinition({
      envId,
      orgId,
      body: ApisFaker.apiImport({
        plans: [
          PlansFaker.plan({
            security: PlanSecurityType.KEY_LESS,
            status: PlanStatus.PUBLISHED,
            flows: [
              {
                name: '',
                path_operator: {
                  path: '/',
                  operator: PathOperatorOperatorEnum.STARTS_WITH,
                },
                condition: '',
                consumers: [],
                methods: [],
                pre: [
                  {
                    name: 'Mock',
                    description: '',
                    enabled: true,
                    policy: 'mock',
                    configuration: {
                      content: '{ "key_1": "{#properties[\'key_1\']}", "encryptedKey": "{#properties[\'encryptedKey\']}" }',
                      status: '200',
                    },
                  },
                ],
                post: [],
                enabled: true,
              },
            ],
          }),
        ],
      }),
    });

    // start it
    await apisManagementApiAsApiUser.doApiLifecycleAction({
      envId,
      orgId,
      api: createdApi.id,
      action: LifecycleAction.START,
    });
  });

  test('should set API property', async () => {
    const newProperties: PropertyEntity[] = [
      {
        key: 'key_1',
        value: 'abc',
        encrypted: false,
        dynamic: false,
        encryptable: false,
      },
      {
        key: 'encryptedKey',
        value: 'secretKeyValue',
        encrypted: true,
        dynamic: false,
        encryptable: false,
      },
    ];

    const updateApiEntity = UpdateApiEntityFromJSON({
      ...createdApi,
      properties: newProperties,
    });

    const updatedApi = await succeed(
      apisManagementApiAsApiUser.updateApiRaw({
        api: createdApi.id,
        updateApiEntity,
        orgId,
        envId,
      }),
    );

    await succeed(apisManagementApiAsApiUser.deployApiRaw({ orgId, envId, api: updatedApi.id }));
    expect(updatedApi.properties).toMatchObject([
      { key: 'key_1', value: 'abc' },
      { key: 'encryptedKey', value: 'secretKeyValue' },
    ]);
  });

  test('should be able to use encrypted and unencrypted properties inside a policy', async () => {
    const response = await fetchGatewaySuccess({
      contextPath: createdApi.context_path,
      expectedResponseValidator: async (response) => {
        const body = await response.json();
        expect(body).toEqual({
          key_1: 'abc',
          encryptedKey: 'secretKeyValue',
        });
        return true;
      },
    });
  });

  afterAll(async () => {
    await teardownApisAndApplications(orgId, envId, [createdApi.id]);
  });
});
