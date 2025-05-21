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
import { succeed } from '@lib/jest-utils';
import { APIsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/APIsApi';
import { forManagementAsApiUser } from '@gravitee/utils/configuration';
import { afterAll, beforeAll, expect, test } from '@jest/globals';
import { Plan } from '@model/plan';
import { ApisFaker } from '@gravitee/fixtures/management/ApisFaker';
import { ApiEntity, EventType, LifecycleAction } from '../../../../../lib/management-webclient-sdk/src/lib/models';

describe('API - V2 - Rollback', () => {
  describe('Rollback API description,execution mode,description', () => {
    const orgId = 'DEFAULT';
    const envId = 'DEFAULT';
    const v2ApisResourceAsApiPublisher = new APIsApi(forManagementAsApiUser());
    let importedApi: ApiEntity;

    beforeAll(async () => {
      importedApi = await v2ApisResourceAsApiPublisher.createApi({
        orgId,
        envId,
        newApiEntity: ApisFaker.newApi(),
      });
    });

    afterAll(async () => {
      if (importedApi) {
        await v2ApisResourceAsApiPublisher.deleteApi({
          envId,
          orgId,
          api: importedApi.id,
        });
      }
    });

    test('should rollback "update on api"', async () => {
      // Given
      // Update the API
      const updatedApi = await succeed(
        v2ApisResourceAsApiPublisher.updateApiRaw({
          envId,
          orgId,
          api: importedApi.id,
          updateApiEntity: {
            name: 'updated-api-name-' + importedApi.name,
            version: 'updated-api-version-' + importedApi.version,
            description: 'updated-api-description',
            lifecycle_state: 'PUBLISHED',
            visibility: 'PUBLIC',
            proxy: importedApi.proxy,
            labels: ['foo', 'bar'],
            flow_mode: 'BEST_MATCH',
            execution_mode: 'v3',
          },
        }),
      );

      // Deploy
      await succeed(
        v2ApisResourceAsApiPublisher.deployApiRaw({
          envId,
          orgId,
          api: importedApi.id,
        }),
      );

      // When
      // Rollback to the first event without updating changes
      const events = await succeed(
        v2ApisResourceAsApiPublisher.getApiEventsEventsRaw({
          envId,
          orgId,
          api: importedApi.id,
          type: [EventType.PUBLISH_API],
        }),
      );
      expect(events).toHaveLength(1);
      await succeed(
        v2ApisResourceAsApiPublisher.rollbackApiRaw({
          orgId,
          envId,
          api: importedApi.id,
          rollbackApiEntity: {
            id: importedApi.id,
            name: importedApi.name,
            version: importedApi.version,
            description: importedApi.description,
            lifecycle_state: importedApi.lifecycle_state,
            visibility: importedApi.visibility,
            proxy: importedApi.proxy,
            labels: importedApi.labels,
            flow_mode: importedApi.flow_mode,
            paths: importedApi.paths,
            flows: importedApi.flows,
            plans: importedApi.plans,
            execution_mode: importedApi.execution_mode,
          },
        }),
      );

      // Then
      const api = await succeed(
        v2ApisResourceAsApiPublisher.getApiRaw({
          envId,
          api: importedApi.id,
          orgId,
        }),
      );

      // Rollbacked API values
      expect(api.name).toEqual(importedApi.name);
      expect(api.version).toEqual(importedApi.version);
      expect(api.description).toEqual(importedApi.description);
      expect(api.execution_mode).toEqual(importedApi.execution_mode);
      expect(api.visibility).not.toEqual(updatedApi.visibility);
    });
  });
});
