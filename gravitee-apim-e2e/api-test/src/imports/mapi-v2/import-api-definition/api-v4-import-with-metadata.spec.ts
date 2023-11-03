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
import { test, describe, afterAll, expect } from '@jest/globals';
import { APIsApi, ApiV4 } from '../../../../../lib/management-v2-webclient-sdk/src/lib';
import { forManagementAsAdminUser, forManagementAsApiUser, forManagementV2AsApiUser } from '@gravitee/utils/configuration';
import { created, noContent, succeed } from '@lib/jest-utils';
import { APIsApi as v1APIsApi } from '../../../../../lib/management-webclient-sdk/src/lib/apis/APIsApi';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { MAPIV2MetadataFaker } from '@gravitee/fixtures/management/MAPIV2MetadataFaker';
import { MetadataApi } from '../../../../../lib/management-webclient-sdk/src/lib/apis/MetadataApi';
import { MetadataFaker } from '@gravitee/fixtures/management/MetadataFaker';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const v1ApisResourceAsApiPublisher = new v1APIsApi(forManagementAsApiUser());
const v2ApisResourceAsApiPublisher = new APIsApi(forManagementV2AsApiUser());
const v1MetadataResourceAsApiPublisher = new MetadataApi(forManagementAsAdminUser());

describe('API - V4 - Import - Gravitee Definition - With metadata', () => {
  describe('Create v4 API from import with metadata', () => {
    let importedApiWithApimTeam, importedSecondApiWithApimTeam, importedApiWithAmTeam, importedApiWithCockpitTeam: ApiV4;

    const apimTeamMetadata = MAPIV2MetadataFaker.metadata({ key: 'team', name: 'team', value: 'API Management' });
    const amTeamMetadata = MAPIV2MetadataFaker.metadata({ key: 'team', name: 'team', value: 'Access Management' });
    const cockpitTeamMetadata = MAPIV2MetadataFaker.metadata({ key: 'team', name: 'team', value: 'Cockpit' });

    test('should create environment metadata', async () => {
      v1MetadataResourceAsApiPublisher.createMetadataRaw({
        orgId,
        envId,
        newMetadataEntity: MetadataFaker.newMetadata({
          name: cockpitTeamMetadata.name,
          value: `${cockpitTeamMetadata.value}-default`,
        }),
      });
    });

    test('should import v4 APIs with metadata', async () => {
      importedApiWithApimTeam = await created(
        v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
          envId,
          exportApiV4: MAPIV2ApisFaker.apiImportV4({
            metadata: [apimTeamMetadata],
          }),
        }),
      );
      expect(importedApiWithApimTeam).toBeTruthy();

      importedApiWithAmTeam = await created(
        v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
          envId,
          exportApiV4: MAPIV2ApisFaker.apiImportV4({
            metadata: [amTeamMetadata],
          }),
        }),
      );
      expect(importedApiWithAmTeam).toBeTruthy();

      importedSecondApiWithApimTeam = await created(
        v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
          envId,
          exportApiV4: MAPIV2ApisFaker.apiImportV4({
            metadata: [apimTeamMetadata],
          }),
        }),
      );
      expect(importedSecondApiWithApimTeam).toBeTruthy();

      importedApiWithCockpitTeam = await created(
        v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
          envId,
          exportApiV4: MAPIV2ApisFaker.apiImportV4({
            metadata: [
              // Api with medata having an undefined key at creation will be created with its name as a key
              { ...cockpitTeamMetadata, key: undefined },
            ],
          }),
        }),
      );
      expect(importedApiWithCockpitTeam).toBeTruthy();
    });

    // HACK:
    // with test.each, jest is using the initial value of variables
    // in this case, they are not yet initialized (it's done in the previous step)
    // using functions allow to delay the access to variables
    const importedApiWithApimTeamFn = () => importedApiWithApimTeam.id;
    const importedSecondApiWithApimTeamFn = () => importedSecondApiWithApimTeam.id;
    const importedApiWithAmTeamFn = () => importedApiWithAmTeam.id;
    const importedApiWithCockpitTeamFn = () => importedApiWithCockpitTeam.id;

    test.each`
      apiIdFn                            | expectedMetadata       | name
      ${importedApiWithApimTeamFn}       | ${apimTeamMetadata}    | ${'apiWithApimTeam'}
      ${importedSecondApiWithApimTeamFn} | ${apimTeamMetadata}    | ${'secondApiWithApimTeam'}
      ${importedApiWithAmTeamFn}         | ${amTeamMetadata}      | ${'apiWithAmTeam'}
      ${importedApiWithCockpitTeamFn}    | ${cockpitTeamMetadata} | ${'apiWithCockpitTeam'}
    `('should get API metadata $name', async ({ apiIdFn, expectedMetadata, name }) => {
      const apiId = apiIdFn();
      const metadata = await succeed(v1ApisResourceAsApiPublisher.getApiMetadatasRaw({ orgId, envId, api: apiId }));
      expect(metadata).toBeTruthy();
      // A metadata with same key has been created for environment with 'Cockpit-value' as value, meaning this value is used as default value at api level
      expectedMetadata.defaultValue = 'Cockpit-default';

      expect(metadata).toContainEqual({ apiId, ...expectedMetadata });
    });

    afterAll(async () => {
      await noContent(
        v2ApisResourceAsApiPublisher.deleteApiRaw({
          envId,
          apiId: importedApiWithApimTeam.id,
        }),
      );
      await noContent(
        v2ApisResourceAsApiPublisher.deleteApiRaw({
          envId,
          apiId: importedSecondApiWithApimTeam.id,
        }),
      );
      await noContent(
        v2ApisResourceAsApiPublisher.deleteApiRaw({
          envId,
          apiId: importedApiWithAmTeam.id,
        }),
      );
      await noContent(
        v2ApisResourceAsApiPublisher.deleteApiRaw({
          envId,
          apiId: importedApiWithCockpitTeam.id,
        }),
      );

      await noContent(
        v1MetadataResourceAsApiPublisher.deleteMetadataRaw({
          orgId,
          envId,
          metadata: cockpitTeamMetadata.key,
        }),
      );
    });
  });
});
