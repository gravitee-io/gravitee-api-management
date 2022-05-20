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
import { describe, expect, test } from '@jest/globals';

import { APIsApi } from '@management-apis/APIsApi';
import {
  forManagementAsAdminUser,
  forManagementAsApiUser,
  forManagementAsAppUser,
  forPortalAsAdminUser,
  forPortalAsApiUser,
} from '@client-conf/*';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { created, noContent, succeed, testif } from '../lib/jest-utils';
import { LifecycleAction } from '@management-models/LifecycleAction';
import { PlanStatus } from '@management-models/PlanStatus';
import { ApiEntity } from '@management-models/ApiEntity';
import { PlanEntity } from '@management-models/PlanEntity';
import { ApplicationsApi } from '@management-apis/ApplicationsApi';
import { ApplicationsFaker } from '@management-fakers/ApplicationsFaker';
import { ApplicationEntity } from '@management-models/ApplicationEntity';
import { PlanSecurityType } from '@management-models/PlanSecurityType';
import { PortalApi as PortalManagementApi } from '@management-apis/PortalApi';
import { ApiApi } from '@portal-apis/ApiApi';
import { UpdateApiEntityFromJSON } from '@management-models/UpdateApiEntity';
import { ApiLifecycleState } from '@management-models/ApiLifecycleState';
import chalk from 'chalk';

const apiManagementApiAsApiUser = new APIsApi(forManagementAsApiUser());
const applicationsManagementApiAsAppUser = new ApplicationsApi(forManagementAsAppUser());
const portalManagementApiAsAdmin = new PortalManagementApi(forManagementAsAdminUser());
const apiPortalApiAsApiUser = new ApiApi(forPortalAsApiUser());
const apiPortalApiAsAdminUser = new ApiApi(forPortalAsAdminUser());
const orgId = 'DEFAULT';
const envId = 'DEFAULT';

let createdApis: Array<{ api: ApiEntity; planId: string }> = [];
let createdApplications: Array<{ application: ApplicationEntity }> = [];

const options = {
  apis: 50,
  applications: 5,
  skipSubscriptions: false,
  skipRatings: false,
  skipStart: false,
  skipDeploy: false,
};

process.argv
  .map((arg) => arg.replace('--', '').split('='))
  .filter(([key, value]) => options[key] != null)
  .forEach(([key, value]) => {
    options[key] = key.startsWith('skip') ? value === 'true' : Number(value);
  });

log(`Bulk script started with`, options);

/**
 * This script only run with command line `npm run bulk`
 */
describe(`BULK data to APIM`, () => {
  describe('APIs', () => {
    testif(options.apis > 0)(`should create ${options.apis} API(s) with an API_KEY plan`, async () => {
      createdApis = await repeat(createWithPlan, options.apis);
      expect(createdApis).toHaveLength(options.apis);
      log(`${createdApis.length} API(s) has been created !`);
    });

    testif(options.apis > 0)(`should publish ${options.apis} API(s)`, async () => {
      const updatedApis = await Promise.all(createdApis.map(({ api }) => publish(api)));
      expect(createdApis).toHaveLength(updatedApis.length);
      log(`${options.apis} API(s) has been published !`);
    });

    testif(options.apis > 0 && !options.skipStart)(`should start ${options.apis} API(s)`, async () => {
      await Promise.all(
        createdApis.map(({ api }) =>
          apiManagementApiAsApiUser.doApiLifecycleActionRaw({ orgId, envId, api: api.id, action: LifecycleAction.START }),
        ),
      );
      log(`${options.apis} API(s) has been started !`);
    });

    testif(options.apis > 0 && !options.skipDeploy)(`should deploy ${options.apis} API(s)`, async () => {
      await Promise.all(createdApis.map(({ api }) => apiManagementApiAsApiUser.deployApiRaw({ orgId, envId, api: api.id })));
      log(`${options.apis} API(s) has been deployed !`);
    });
  });

  describe('APPLICATIONS', () => {
    testif(options.applications > 0)(`should create ${options.applications} application(s)`, async () => {
      createdApplications = await repeat(createApplication, options.applications);
      expect(createdApplications).toHaveLength(options.applications);
      log(`${options.applications} APPLICATION(S) has been created !`);
    });
  });

  describe('SUBSCRIPTIONS', () => {
    testif(options.apis > 0 && options.applications > 0 && !options.skipSubscriptions)(
      `should create a subscription to each API with each application`,
      async () => {
        const subscriptionPromises = createdApis
          .map(async ({ api, planId }) => {
            const appSubs = await Promise.all(
              createdApplications.map(({ application }) =>
                apiManagementApiAsApiUser.createSubscriptionToApiRaw({
                  orgId,
                  envId,
                  api: api.id,
                  plan: planId,
                  application: application.id,
                }),
              ),
            );
            return appSubs.flat();
          })
          .flat();

        const results = await Promise.all(subscriptionPromises);
        const resultsFlat = results.flat();
        expect(resultsFlat).toHaveLength(options.apis * options.applications);
        log(`${resultsFlat.length} SUBSCRIPTION(S) has been created !`);
      },
    );
  });

  describe('RATINGS', () => {
    testif(options.apis > 0 && !options.skipRatings)(`should rate ${options.apis} API(s) with ADMIN and API users`, async () => {
      await portalManagementApiAsAdmin.savePortalConfigRaw({
        orgId,
        envId,
        portalSettingsEntity: { portal: { rating: { enabled: true } } },
      });

      const ratingPromises = createdApis
        .map(({ api }) =>
          [apiPortalApiAsAdminUser, apiPortalApiAsApiUser].map((resource) =>
            resource.createApiRatingRaw({
              apiId: api.id,
              ratingInput: ApisFaker.newRatingInput(),
            }),
          ),
        )
        .flat();

      const results = await Promise.all(ratingPromises);
      expect(results).toHaveLength(options.apis * 2);
      log(`${results.length} RATING(S) has been created !`);
    });
  });
});

async function createWithPlan(): Promise<{ api: ApiEntity; planId: string }> {
  const api: ApiEntity = await created(
    apiManagementApiAsApiUser.createApiRaw({
      orgId,
      envId,
      newApiEntity: ApisFaker.newApi(),
    }),
  );

  const apikeyPlan: PlanEntity = await apiManagementApiAsApiUser.createApiPlan({
    orgId,
    envId,
    api: api.id,
    newPlanEntity: PlansFaker.newPlan({ status: PlanStatus.PUBLISHED, security: PlanSecurityType.APIKEY }),
  });
  return {
    api,
    planId: apikeyPlan.id,
  };
}

async function publish(api: ApiEntity) {
  const updateApiEntity = UpdateApiEntityFromJSON({ ...api, visibility: 'PUBLIC', lifecycle_state: ApiLifecycleState.PUBLISHED });
  return await succeed(apiManagementApiAsApiUser.updateApiRaw({ orgId, envId, api: api.id, updateApiEntity }));
}

async function createApplication(): Promise<{ application: ApplicationEntity }> {
  const newApplicationEntity = ApplicationsFaker.newApplication();
  const application = await created(
    applicationsManagementApiAsAppUser.createApplicationRaw({
      orgId,
      envId,
      newApplicationEntity,
    }),
  );
  return { application };
}

async function repeat(fn, times = 1) {
  const first = await fn();
  const all = await Promise.all(new Array(times - 1).fill(null).map(() => fn()));
  return [first, ...all];
}

function log(message, ...args) {
  console.log(chalk.green(`> ${message}`), args);
}
