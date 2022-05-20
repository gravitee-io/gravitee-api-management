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
import { afterAll, beforeAll, expect } from '@jest/globals';
import faker from '@faker-js/faker';
import { ApplicationsApi } from '@management-apis/ApplicationsApi';
import { ADMIN_USER, APP_USER, forManagementAsAdminUser, forManagementAsApiUser, forManagementAsAppUser } from '@client-conf/*';
import { succeed } from '@lib/jest-utils';
import { ApplicationEntity } from '@management-models/ApplicationEntity';

const applicationManagementApiAsAdminUser = new ApplicationsApi(forManagementAsAdminUser());
const applicationManagementApiAsAppUser = new ApplicationsApi(forManagementAsAppUser());
const applicationManagementApiAsApiUser = new ApplicationsApi(forManagementAsApiUser());

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

let createdApplication1: ApplicationEntity;
let createdApplication2: ApplicationEntity;
let createdApplication3: ApplicationEntity;
let createdApplication4: ApplicationEntity;

describe('Applications - Searching', () => {
  beforeAll(async () => {
    const createdAppsAsAppUser = [1, 2, 3]
      .map((num) => ({
        name: `${faker.random.word()}-${faker.datatype.uuid()}`,
        description: `Application ${num} for "Applications searching scenario"`,
      }))
      .map(
        async (newApplicationEntity) =>
          await applicationManagementApiAsAppUser.createApplication({
            orgId,
            envId,
            newApplicationEntity,
          }),
      );

    [createdApplication1, createdApplication2, createdApplication3] = await Promise.all(createdAppsAsAppUser);

    const createdAppsAsApiUser = [4]
      .map((num) => ({
        name: `${faker.random.word()}-${faker.datatype.uuid()}`,
        description: `Application ${num} for "Applications searching scenario"`,
      }))
      .map(
        async (newApplicationEntity) =>
          await applicationManagementApiAsApiUser.createApplication({
            orgId,
            envId,
            newApplicationEntity,
          }),
      );

    [createdApplication4] = await Promise.all(createdAppsAsApiUser);
  });

  test(`Search all for ${APP_USER.username}`, async () => {
    const applications = await succeed(applicationManagementApiAsAppUser.getApplicationsRaw({ orgId, envId }));
    expect(applications.length).toBeGreaterThanOrEqual(3);
    expect(applications.map(({ id }) => id)).toEqual(
      expect.arrayContaining([createdApplication1, createdApplication2, createdApplication3].map(({ id }) => id)),
    );
  });

  test(`Search an app by name for ${APP_USER.username}`, async () => {
    const applications = await succeed(
      applicationManagementApiAsAppUser.getApplicationsRaw({ orgId, envId, query: createdApplication3.name }),
    );
    expect(applications.length).toBeGreaterThanOrEqual(1);
    expect(applications[0].name).toEqual(createdApplication3.name);
  });

  test(`Search all for ${ADMIN_USER.username}`, async () => {
    const applications = await succeed(applicationManagementApiAsAdminUser.getApplicationsRaw({ orgId, envId }));
    expect(applications.length).toBeGreaterThanOrEqual(4);
    expect(applications.map(({ id }) => id)).toEqual(
      expect.arrayContaining([createdApplication1, createdApplication2, createdApplication3, createdApplication4].map(({ id }) => id)),
    );
  });

  afterAll(async () => {
    const deletedAppsAsAppUser = [createdApplication1, createdApplication2, createdApplication3].map(
      async (application) =>
        await applicationManagementApiAsAppUser.deleteApplication({
          orgId,
          envId,
          application: application.id,
        }),
    );
    await Promise.all(deletedAppsAsAppUser);
    const deletedAppsAsApiUser = [createdApplication4].map(
      async (application) =>
        await applicationManagementApiAsApiUser.deleteApplication({
          orgId,
          envId,
          application: application.id,
        }),
    );
    await Promise.all(deletedAppsAsApiUser);
  });
});
