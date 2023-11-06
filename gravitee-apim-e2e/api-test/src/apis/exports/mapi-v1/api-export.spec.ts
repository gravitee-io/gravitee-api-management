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
import { APIsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/APIsApi';
import {
  ApiEntity,
  ConsumerConsumerTypeEnum,
  NewApiEntityFlowModeEnum,
  PathOperatorOperatorEnum,
} from '@gravitee/management-webclient-sdk/src/lib/models';
import { ApisFaker } from '@gravitee/fixtures/management/ApisFaker';
import { noContent, succeed } from '@lib/jest-utils';
import { forManagementAsAdminUser } from '@gravitee/utils/configuration';
import { Flow } from '@gravitee/management-webclient-sdk/src/lib/models';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';
const apisResourceAdmin = new APIsApi(forManagementAsAdminUser());

let userApi: ApiEntity;

describe('API - Exports', () => {
  describe('Export flow based api from current version to 3.0 to 3.6 and import it', () => {
    const flow: Flow = {
      condition: '',
      consumers: [{ consumerId: 'Consumer#1', consumerType: ConsumerConsumerTypeEnum.TAG }],
      enabled: true,
      methods: [],
      name: 'Flow',
      path_operator: { operator: PathOperatorOperatorEnum.STARTS_WITH, path: '' },
      post: [],
      pre: [],
    };
    const exportedFlow = { ...flow, 'path-operator': flow.path_operator, path_operator: undefined };
    let importedApi: ApiEntity;

    beforeAll(async () => {
      userApi = await apisResourceAdmin.createApi({
        orgId,
        envId,
        newApiEntity: ApisFaker.newApi({
          gravitee: '2.0.0',
          flows: [flow],
          flow_mode: NewApiEntityFlowModeEnum.BEST_MATCH,
        }),
      });
    });

    it('should export the api without flow', async () => {
      const exportedApi = JSON.parse(
        await succeed(apisResourceAdmin.exportApiDefinitionRaw({ api: userApi.id, envId, orgId, version: '3.0' })),
      );
      expect(exportedApi).toBeTruthy();
      expect(exportedApi).toHaveProperty('flows');
      expect(exportedApi.flows).toHaveLength(1);
      const id = exportedApi.flows[0].id;
      expect(exportedApi.flows[0]).toEqual({ ...exportedFlow, id });
      expect(exportedApi).not.toHaveProperty('paths');

      await noContent(apisResourceAdmin.deleteApiRaw({ orgId, envId, api: userApi.id }));

      importedApi = await succeed(apisResourceAdmin.importApiDefinitionRaw({ orgId, envId, body: exportedApi }));
      expect(importedApi).toBeTruthy();
      expect(importedApi.crossId).toStrictEqual(exportedApi.crossId);
      expect(exportedApi.flows).toHaveLength(1);
      expect(exportedApi.flows[0]).toEqual({ ...exportedFlow, id });
      expect(importedApi.gravitee).toStrictEqual(exportedApi.gravitee);
    });

    afterAll(async () => {
      await apisResourceAdmin.deleteApi({ orgId, envId, api: importedApi.id });
    });
  });
});
