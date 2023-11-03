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
import { test, describe, expect, afterAll } from '@jest/globals';
import { APIsApi, APIPlansApi, ApiV4, PlanSecurityType, PlanV4 } from '../../../../../../lib/management-v2-webclient-sdk/src/lib';
import { forManagementV2AsApiUser } from '@gravitee/utils/configuration';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { created, noContent, succeed } from '@lib/jest-utils';
import { MAPIV2PlansFaker } from '@gravitee/fixtures/management/MAPIV2PlansFaker';

const envId = 'DEFAULT';

const v2ApisResourceAsApiPublisher = new APIsApi(forManagementV2AsApiUser());
const v2APlansResourceAsApiPublisher = new APIPlansApi(forManagementV2AsApiUser());

describe('API - V4 - Import - Gravitee Definition - With plans', () => {
  describe('Create v4 API from import with plans', () => {
    describe('Create v4 API with two plans', () => {
      const keylessPlan = MAPIV2PlansFaker.planV4();
      const apiKeyPlan = MAPIV2PlansFaker.planV4({
        security: { type: PlanSecurityType.API_KEY },
      });
      let importedApi: ApiV4;
      let savedKeylessPlanId: string;
      let savedApiKeyPlanId: string;

      test('should import v4 API with plans', async () => {
        importedApi = await created(
          v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
            envId,
            exportApiV4: MAPIV2ApisFaker.apiImportV4({
              plans: [keylessPlan, apiKeyPlan],
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
        savedKeylessPlanId = keylessPlanResult.id;
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
        expect(keylessPlanResult.generalConditions).toStrictEqual(keylessPlan.generalConditions);

        // Verifying apikey plan
        const apiKeyPlanResult = plans.find((p) => p.name === apiKeyPlan.name) as PlanV4;
        savedApiKeyPlanId = apiKeyPlanResult.id;
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

      afterAll(async () => {
        await noContent(
          v2APlansResourceAsApiPublisher.deleteApiPlanRaw({
            envId,
            apiId: importedApi.id,
            planId: savedKeylessPlanId,
          }),
        );
        await noContent(
          v2APlansResourceAsApiPublisher.deleteApiPlanRaw({
            envId,
            apiId: importedApi.id,
            planId: savedApiKeyPlanId,
          }),
        );
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
