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
import { test, describe, expect } from '@jest/globals';
import { APIsApi, APIPlansApi, ApiV4, PlanSecurityType, PlanV4 } from '@gravitee/management-v2-webclient-sdk/src/lib';
import { forManagementAsAdminUser, forManagementAsApiUser, forManagementV2AsApiUser } from '@gravitee/utils/configuration';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { created, noContent, succeed } from '@lib/jest-utils';
import { MAPIV2PlansFaker } from '@gravitee/fixtures/management/MAPIV2PlansFaker';
import { APIsApi as v1APIsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/APIsApi';
import { UsersApi } from '@gravitee/management-webclient-sdk/src/lib/apis/UsersApi';
import { ConfigurationApi } from '@gravitee/management-webclient-sdk/src/lib/apis/ConfigurationApi';
import { PageEntity, RoleEntity, RoleScope, UserEntity } from '@gravitee/management-webclient-sdk/src/lib/models';
import { MAPIV2MetadataFaker } from '@gravitee/fixtures/management/MAPIV2MetadataFaker';
import { APIPagesApi } from '@gravitee/management-webclient-sdk/src/lib/apis/APIPagesApi';
import { MAPIV2PagesFaker } from '@gravitee/fixtures/management/MAPIV2PagesFaker';
import { UsersFaker } from '@gravitee/fixtures/management/UsersFaker';
import { RoleFaker } from '@gravitee/fixtures/management/RoleFaker';
import { MAPIV2MembersFaker } from '@gravitee/fixtures/management/MAPIV2MembersFaker';
import { ImagesUtils } from '@gravitee/utils/images';

const envId = 'DEFAULT';
const orgId = 'DEFAULT';

const v1ApisResourceAsApiPublisher = new v1APIsApi(forManagementAsApiUser());
const v1ConfigurationResourceAsAdmin = new ConfigurationApi(forManagementAsAdminUser());
const v1ApiPagesResourceAsApiPublisher = new APIPagesApi(forManagementAsApiUser());
const v1UsersResourceAsAdmin = new UsersApi(forManagementAsAdminUser());
const v2ApisResourceAsApiPublisher = new APIsApi(forManagementV2AsApiUser());
const v2APlansResourceAsApiPublisher = new APIPlansApi(forManagementV2AsApiUser());

describe('API - V4 - Import - Gravitee Definition - With everything', () => {
  describe('Create v4 API from import with everything', () => {
    describe('Create v4 API with two plans', () => {
      let importedApi: ApiV4;

      // plans
      let savedKeylessPlan: PlanV4;
      let savedApiKeyPlan: PlanV4;
      const keylessPlan = MAPIV2PlansFaker.planV4();
      const apiKeyPlan = MAPIV2PlansFaker.planV4({
        security: { type: PlanSecurityType.API_KEY },
      });

      // members
      const roleName = 'IMPORT_TEST_ROLE';
      let memberToImport: UserEntity;
      let customRole: RoleEntity;

      // metadata
      const metadataToImport = MAPIV2MetadataFaker.metadata({ key: 'team-everything', name: 'team-everything', value: 'API Management' });

      // pages
      const pageToImport = MAPIV2PagesFaker.page();
      const generalConditionsPageToImport = MAPIV2PagesFaker.page({ id: 'general-condition-id' });
      let importedPage: PageEntity;
      let importedGeneralConditionsPage: PageEntity;

      test('should create member and role first', async () => {
        memberToImport = await succeed(
          v1UsersResourceAsAdmin.createUserRaw({
            orgId,
            envId,
            newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity(),
          }),
        );
        customRole = await succeed(
          v1ConfigurationResourceAsAdmin.createRoleRaw({
            orgId,
            scope: RoleScope.API,
            newRoleEntity: RoleFaker.newRoleEntity({ name: roleName, scope: RoleScope.API }),
          }),
        );
      });

      test('should import v4 API with everything', async () => {
        keylessPlan.generalConditions = generalConditionsPageToImport.id;
        importedApi = await created(
          v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
            envId,
            exportApiV4: MAPIV2ApisFaker.apiImportV4({
              apiPicture: `data:image/png;base64,${ImagesUtils.fakeImage15x15}`,
              apiBackground: `data:image/png;base64,${ImagesUtils.fakeImage150x35}`,
              plans: [keylessPlan, apiKeyPlan],
              members: [
                MAPIV2MembersFaker.member({
                  id: memberToImport.id,
                  displayName: memberToImport.displayName,
                  roles: [
                    {
                      name: customRole.name,
                      scope: customRole.scope,
                    },
                  ],
                }),
              ],
              metadata: [metadataToImport],
              pages: [pageToImport, generalConditionsPageToImport],
            }),
          }),
        );
        expect(importedApi).toBeTruthy();
      });

      test('should get created v4 API with generated ID', async () => {
        const apiV4 = await succeed(
          v2ApisResourceAsApiPublisher.getApiRaw({
            envId,
            apiId: importedApi.id,
          }),
        );
        expect(apiV4).toBeTruthy();
        expect(apiV4.id).toStrictEqual(importedApi.id);
      });

      test('should get list of plans with correct data', async () => {
        const plansResponse = await succeed(
          v2APlansResourceAsApiPublisher.listApiPlansRaw({
            envId,
            apiId: importedApi.id,
          }),
        );
        const plans = plansResponse.data;
        expect(plans).toBeTruthy();
        expect(plans).toHaveLength(2);
        // Verifying keyless plan
        const keylessPlanResult = plans.find((p) => p.name === keylessPlan.name) as PlanV4;
        savedKeylessPlan = keylessPlanResult;
        expect(keylessPlanResult.name).toStrictEqual(keylessPlan.name);
        expect(keylessPlanResult.apiId).toStrictEqual(importedApi.id);
        expect(keylessPlanResult.description).toStrictEqual(keylessPlan.description);
        expect(keylessPlanResult.status).toStrictEqual(keylessPlan.status);
        expect(keylessPlanResult.validation).toStrictEqual(keylessPlan.validation);
        expect(keylessPlanResult.definitionVersion).toStrictEqual(keylessPlan.definitionVersion);
        expect(keylessPlanResult.security).toEqual(keylessPlan.security);
        expect(keylessPlanResult.mode).toStrictEqual(keylessPlan.mode);
        expect(keylessPlanResult.flows).toStrictEqual(keylessPlan.flows);
        expect(keylessPlanResult.characteristics).toStrictEqual(keylessPlan.characteristics);
        expect(keylessPlanResult.generalConditions).not.toEqual(keylessPlan.generalConditions);

        // Verifying apikey plan
        const apiKeyPlanResult = plans.find((p) => p.name === apiKeyPlan.name) as PlanV4;
        savedApiKeyPlan = apiKeyPlanResult;
        expect(apiKeyPlanResult.name).toStrictEqual(apiKeyPlan.name);
        expect(apiKeyPlanResult.apiId).toStrictEqual(importedApi.id);
        expect(apiKeyPlanResult.description).toStrictEqual(apiKeyPlan.description);
        expect(apiKeyPlanResult.status).toStrictEqual(apiKeyPlan.status);
        expect(apiKeyPlanResult.validation).toStrictEqual(apiKeyPlan.validation);
        expect(apiKeyPlanResult.definitionVersion).toStrictEqual(apiKeyPlan.definitionVersion);
        expect(apiKeyPlanResult.security).toEqual(apiKeyPlan.security);
        expect(apiKeyPlanResult.mode).toStrictEqual(apiKeyPlan.mode);
        expect(apiKeyPlanResult.flows).toStrictEqual(apiKeyPlan.flows);
        expect(apiKeyPlanResult.characteristics).toStrictEqual(apiKeyPlan.characteristics);
        expect(apiKeyPlanResult.generalConditions).toStrictEqual(apiKeyPlan.generalConditions);
      });

      test('should get API members and an additional member with custom role', async () => {
        // No v2 resource to get members of an API
        const members = await succeed(
          v1ApisResourceAsApiPublisher.getApiMembersRaw({
            orgId,
            envId,
            api: importedApi.id,
          }),
        );
        expect(members).toBeTruthy();
        expect(members).toHaveLength(2);
        const memberResult = members.find((m) => m.displayName === memberToImport.displayName);
        expect(memberResult.id).toBeTruthy();
        expect(memberResult.role).toEqual(customRole.name);
        const poMemberResult = members.find((m) => m.displayName === 'api1');
        expect(poMemberResult.id).toBeTruthy();
        expect(poMemberResult.role).toEqual('PRIMARY_OWNER');
      });

      test('should get API metadata', async () => {
        const metadata = await succeed(v1ApisResourceAsApiPublisher.getApiMetadatasRaw({ orgId, envId, api: importedApi.id }));
        expect(metadata).toBeTruthy();
        // defaultValue is not saved by the backend, so we need to ignore this field for comparison
        metadataToImport.defaultValue = undefined;
        expect(metadata).toContainEqual({
          apiId: importedApi.id,
          ...metadataToImport,
        });
      });

      test('should get list of pages with correct data', async () => {
        const pagesResponse = await succeed(
          v1ApiPagesResourceAsApiPublisher.getApiPagesRaw({
            orgId,
            envId,
            api: importedApi.id,
          }),
        );
        expect(pagesResponse).toHaveLength(3);
        // By default, one page is here a system folder
        expect(pagesResponse.filter((p) => p.type === 'SYSTEM_FOLDER')).toHaveLength(1);

        importedPage = pagesResponse.find((p) => p.name === pageToImport.name);
        expect(importedPage.id).toBeDefined();
        importedGeneralConditionsPage = pagesResponse.find((p) => p.name === generalConditionsPageToImport.name);
        expect(importedGeneralConditionsPage.id).toBeDefined();
        expect(importedGeneralConditionsPage.generalConditions).toBeTruthy();
        expect(importedGeneralConditionsPage.id).toStrictEqual(savedKeylessPlan.generalConditions);
      });

      test('should get API picture', async () => {
        const apiPictureResult = await succeed(
          v2ApisResourceAsApiPublisher.getApiPictureRaw({
            envId,
            apiId: importedApi.id,
          }),
        );
        expect(apiPictureResult).toBeTruthy();
        expect(apiPictureResult.type).toStrictEqual('image/png');
        const base64Image = await ImagesUtils.blobToBase64(apiPictureResult);
        expect(base64Image).toStrictEqual(ImagesUtils.fakeImage15x15);
      });

      test('should get API background', async () => {
        const apiBackgroundResult = await succeed(
          v2ApisResourceAsApiPublisher.getApiBackgroundRaw({
            envId,
            apiId: importedApi.id,
          }),
        );
        expect(apiBackgroundResult).toBeTruthy();
        expect(apiBackgroundResult.type).toStrictEqual('image/png');
        const base64Image = await ImagesUtils.blobToBase64(apiBackgroundResult);
        expect(base64Image).toStrictEqual(ImagesUtils.fakeImage150x35);
      });

      test('yann', async () => {
        // members
        await noContent(
          v1ConfigurationResourceAsAdmin.deleteRoleRaw({
            orgId,
            role: roleName,
            scope: RoleScope.API,
          }),
        );
        await noContent(
          v1UsersResourceAsAdmin.deleteUserRaw({
            orgId,
            envId,
            userId: memberToImport.id,
          }),
        );

        // plans
        await noContent(
          v2APlansResourceAsApiPublisher.deleteApiPlanRaw({
            envId,
            apiId: importedApi.id,
            planId: savedKeylessPlan.id,
          }),
        );
        await noContent(
          v2APlansResourceAsApiPublisher.deleteApiPlanRaw({
            envId,
            apiId: importedApi.id,
            planId: savedApiKeyPlan.id,
          }),
        );

        // pages
        await noContent(
          v1ApiPagesResourceAsApiPublisher.deleteApiPageRaw({
            orgId,
            envId,
            api: importedApi.id,
            page: importedPage.id,
          }),
        );
        await noContent(
          v1ApiPagesResourceAsApiPublisher.deleteApiPageRaw({
            orgId,
            envId,
            api: importedApi.id,
            page: importedGeneralConditionsPage.id,
          }),
        );

        // api and metadata
        await noContent(
          v2ApisResourceAsApiPublisher.deleteApiRaw({
            envId,
            apiId: importedApi.id,
          }),
        );
      });
    });
  });
});
