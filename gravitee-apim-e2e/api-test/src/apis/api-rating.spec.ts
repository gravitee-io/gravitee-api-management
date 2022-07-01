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
import { forManagementAsAdminUser, forManagementAsApiUser } from '@client-conf/*';
import { afterAll, beforeAll, describe, expect } from '@jest/globals';
import { PortalApi } from '@management-apis/PortalApi';
import { APIsApi } from '@management-apis/APIsApi';
import { ApiEntity } from '@management-models/ApiEntity';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { fail, succeed } from '@lib/jest-utils';
import faker from '@faker-js/faker';
import { ApiApi } from '@portal-apis/ApiApi';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const portalResourceAsAdmin = new PortalApi(forManagementAsAdminUser());
const apisResourceAsAdmin = new APIsApi(forManagementAsAdminUser());
const apisResourceAsApiUser = new APIsApi(forManagementAsApiUser());

let createdApi: ApiEntity;

describe('API - Rating', () => {
  beforeAll(async () => {
    // create an API
    createdApi = await apisResourceAsApiUser.importApiDefinition({
      envId,
      orgId,
      body: ApisFaker.apiImport({ description: 'This is an API' }),
    });
  });

  describe('API rating disabled', () => {
    beforeAll(async () => {
      // disable API rating
      await portalResourceAsAdmin.savePortalConfig({
        envId,
        orgId,
        portalSettingsEntity: {
          portal: {
            rating: {
              enabled: false,
              comment: {
                mandatory: false,
              },
            },
          },
        },
      });
    });

    describe.each`
      user          | apisResource
      ${'ADMIN'}    | ${apisResourceAsAdmin}
      ${'API_USER'} | ${apisResourceAsApiUser}
    `('As $user user', ({ apisResource }) => {
      test('Create API rating should throw 503 error', async () => {
        await fail(
          apisResource.createApiRating({
            envId,
            orgId,
            api: createdApi.id,
            newRatingEntity: { rate: faker.datatype.number({ min: 1, max: 5 }).toString() },
          }),
          503,
        );
      });
    });
  });

  describe('API rating enabled', () => {
    beforeAll(async () => {
      // enable API rating
      await portalResourceAsAdmin.savePortalConfig({
        envId,
        orgId,
        portalSettingsEntity: {
          portal: {
            rating: {
              enabled: true,
              comment: {
                mandatory: false,
              },
            },
          },
        },
      });
    });

    describe.each`
      user          | apisResource
      ${'ADMIN'}    | ${apisResourceAsAdmin}
      ${'API_USER'} | ${apisResourceAsApiUser}
    `('As $user user', ({ apisResource }: { apisResource: APIsApi }) => {
      test('Create API rating should succeed', async () => {
        let rate = faker.datatype.number({ min: 1, max: 5 });
        let createdRating = await succeed(
          apisResource.createApiRatingRaw({
            envId,
            orgId,
            api: createdApi.id,
            newRatingEntity: { rate: `${rate}` },
          }),
        );
        expect(createdRating.rate).toBe(rate);
      });

      test('Create API rating once again should throw 400 error', async () => {
        let rate = faker.datatype.number({ min: 1, max: 5 });
        await fail(
          apisResource.createApiRating({
            envId,
            orgId,
            api: createdApi.id,
            newRatingEntity: { rate: `${rate}` },
          }),
          400,
        );
      });
    });
  });

  afterAll(async () => {
    // disable API rating
    await portalResourceAsAdmin.savePortalConfig({
      envId,
      orgId,
      portalSettingsEntity: {
        portal: {
          rating: {
            enabled: false,
            comment: {
              mandatory: false,
            },
          },
        },
      },
    });

    // delete created API
    await apisResourceAsApiUser.deleteApi({
      envId,
      orgId,
      api: createdApi.id,
    });
  });
});
