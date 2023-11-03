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
import { APIsApi } from '../../../../../../lib/management-webclient-sdk/src/lib/apis/APIsApi';
import { APIsApi as APIsApiV2 } from '../../../../../../lib/management-v2-webclient-sdk/src/lib';
import { forManagementAsApiUser, forManagementV2AsAdminUser } from '@gravitee/utils/configuration';
import { ApiImportEntity, ApisFaker } from '@gravitee/fixtures/management/ApisFaker';
import { succeed } from '@lib/jest-utils';
import { ApiEntity, ApiEntityStateEnum } from '../../../../../../lib/management-webclient-sdk/src/lib/models/ApiEntity';
import { Visibility } from '../../../../../../lib/management-webclient-sdk/src/lib/models/Visibility';
import { ApiLifecycleState } from '../../../../../../lib/management-webclient-sdk/src/lib/models/ApiLifecycleState';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResourceApiUser = new APIsApi(forManagementAsApiUser());
const v2ApisResourceAsAdmin = new APIsApiV2(forManagementV2AsAdminUser());

describe('API - Imports', () => {
  describe('API definition import', () => {
    let apiToImport: ApiImportEntity = ApisFaker.apiImport();
    let importedApi: ApiEntity;

    test('should import API with management V1 and get it from management V2', async () => {
      delete apiToImport['groups'];

      importedApi = await succeed(
        apisResourceApiUser.importApiDefinitionRaw({
          orgId,
          envId,
          body: apiToImport,
        }),
      );

      expect(importedApi.state).toBe(ApiEntityStateEnum.STOPPED);
      expect(importedApi.visibility).toBe(Visibility.PRIVATE);
      expect(importedApi.lifecycle_state).toBe(ApiLifecycleState.CREATED);

      let foundApi = await succeed(
        v2ApisResourceAsAdmin.getApiRaw({
          envId,
          apiId: importedApi.id,
        }),
      );

      expect(foundApi).toBeTruthy();
      expect(foundApi.id).toBe(importedApi.id);
      expect(foundApi.state).toBe(ApiEntityStateEnum.STOPPED);
      expect(foundApi.visibility).toBe(Visibility.PRIVATE);
      expect(foundApi.lifecycleState).toBe(ApiLifecycleState.CREATED);
      expect(foundApi.groups).toHaveLength(0);
    });

    afterAll(async () => {
      if (importedApi) {
        await v2ApisResourceAsAdmin.deleteApi({
          envId,
          apiId: importedApi.id,
        });
      }
    });
  });
});
