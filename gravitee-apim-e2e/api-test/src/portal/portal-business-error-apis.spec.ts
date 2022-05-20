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
import { afterAll, beforeAll, describe, test } from '@jest/globals';

import { forManagementAsAdminUser, forPortalAsAdminUser, forPortalAsAppUser } from '@client-conf/*';
import { PortalApi as PortalManagementApi } from '@management-apis/PortalApi';
import { APIsApi } from '@management-apis/APIsApi';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { UpdateApiEntityFromJSON } from '@management-models/UpdateApiEntity';
import { ApiApi } from '@portal-apis/ApiApi';
import { fail, notFound } from '@lib/jest-utils';
import { ApiEntity } from '@management-models/ApiEntity';
import { RatingEntity } from '@management-models/RatingEntity';
import { PortalApiFaker } from '@management-fakers/PortalApiFaker';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const portalManagementApiAsAdmin = new PortalManagementApi(forManagementAsAdminUser());
const apisManagementApiAsAdmin = new APIsApi(forManagementAsAdminUser());
const apiPortalApiAsAdmin = new ApiApi(forPortalAsAdminUser());
const apiPortalApiAsAppUser = new ApiApi(forPortalAsAppUser());

describe('Portal: Business Error - apis', () => {
  let createdApi: ApiEntity;
  let createdApiRating: RatingEntity;
  beforeAll(async () => {
    createdApi = await apisManagementApiAsAdmin.createApi({
      orgId,
      envId,
      newApiEntity: ApisFaker.newApi(),
    });
    await apisManagementApiAsAdmin.updateApi({
      orgId,
      envId,
      api: createdApi.id,
      updateApiEntity: UpdateApiEntityFromJSON({ ...createdApi, lifecycle_state: 'published' }),
    });
  });

  describe('400', () => {
    beforeAll(async () => {
      await portalManagementApiAsAdmin.savePortalConfig({
        orgId,
        envId,
        portalSettingsEntity: { portal: { rating: { enabled: true } } },
      });
      createdApiRating = await apisManagementApiAsAdmin.createApiRating({
        orgId,
        envId,
        api: createdApi.id,
        newRatingEntity: ApisFaker.newRating(),
      });
    });

    test('Rating already exists', async () => {
      await fail(
        apiPortalApiAsAdmin.createApiRatingRaw({
          apiId: createdApi.id,
          ratingInput: PortalApiFaker.newRatingInput(),
        }),
        400,
        {
          code: 'errors.rating.exists',
          message: `Rating already exists for api [${createdApi.id}] and user [${createdApi.owner.id}].`,
          parameters: {
            api: createdApi.id,
            user: createdApi.owner.id,
          },
        },
      );
    });

    afterAll(async () => {
      await apisManagementApiAsAdmin.deleteApiRating({
        orgId,
        envId,
        api: createdApi.id,
        rating: createdApiRating.id,
      });
    });
  });

  describe('403', () => {
    beforeAll(async () => {
      await portalManagementApiAsAdmin.savePortalConfig({
        orgId,
        envId,
        portalSettingsEntity: { portal: { rating: { enabled: false } } },
      });
    });
    test('should not create rating if service is disabled', async () => {
      await fail(
        apiPortalApiAsAppUser.createApiRatingRaw({
          apiId: createdApi.id,
          ratingInput: PortalApiFaker.newRatingInput(),
        }),
        403,
        {
          code: 'errors.forbidden',
          message: 'You do not have sufficient rights to access this resource',
          status: '403',
        },
      );
    });
  });

  describe('404', () => {
    test('Api not found', async () => {
      const apiId = 'API';
      await Promise.all(
        [
          apiPortalApiAsAdmin.getApiByApiIdRaw({ apiId }),
          apiPortalApiAsAdmin.getPagesByApiIdRaw({ apiId }),
          apiPortalApiAsAdmin.getPageByApiIdAndPageIdRaw({ apiId, pageId: 'pageId' }),
          apiPortalApiAsAdmin.getPageContentByApiIdAndPageIdRaw({ apiId, pageId: 'pageId' }),
          apiPortalApiAsAdmin.getPictureByApiIdRaw({ apiId }),
          apiPortalApiAsAdmin.getApiPlansByApiIdRaw({ apiId }),
          apiPortalApiAsAdmin.getApiMetricsByApiIdRaw({ apiId }),
          apiPortalApiAsAdmin.getApiRatingsByApiIdRaw({ apiId }),
          apiPortalApiAsAdmin.getSubscriberApplicationsByApiIdRaw({ apiId }),
          apiPortalApiAsAdmin.createApiRatingRaw({ apiId, ratingInput: PortalApiFaker.newRatingInput() }),
        ].map((p) => notFound(p)),
      );
    });

    test('Page not found', async () => {
      const apiId = createdApi.id;
      await Promise.all(
        [
          apiPortalApiAsAdmin.getPageByApiIdAndPageIdRaw({ apiId, pageId: 'pageId' }),
          apiPortalApiAsAdmin.getPageContentByApiIdAndPageIdRaw({ apiId, pageId: 'pageId' }),
        ].map((p) => notFound(p)),
      );
    });

    test('Api not visible for user', async () => {
      const apiId = createdApi.id;
      await Promise.all(
        [
          apiPortalApiAsAppUser.getApiByApiIdRaw({ apiId }),
          apiPortalApiAsAppUser.getPagesByApiIdRaw({ apiId }),
          apiPortalApiAsAppUser.getPageByApiIdAndPageIdRaw({ apiId, pageId: 'pageId' }),
          apiPortalApiAsAppUser.getPageContentByApiIdAndPageIdRaw({ apiId, pageId: 'pageId' }),
          apiPortalApiAsAppUser.getPictureByApiIdRaw({ apiId }),
          apiPortalApiAsAppUser.getApiPlansByApiIdRaw({ apiId }),
          apiPortalApiAsAppUser.getApiMetricsByApiIdRaw({ apiId }),
          apiPortalApiAsAppUser.getApiRatingsByApiIdRaw({ apiId }),
          apiPortalApiAsAppUser.getSubscriberApplicationsByApiIdRaw({ apiId }),
        ].map((p) => notFound(p)),
      );
    });
  });

  describe('503', () => {
    beforeAll(async () => {
      await portalManagementApiAsAdmin.savePortalConfig({
        orgId,
        envId,
        portalSettingsEntity: { portal: { rating: { enabled: false } } },
      });
    });
    test('Rating service is disabled', async () => {
      await fail(
        apiPortalApiAsAdmin.createApiRatingRaw({
          apiId: createdApi.id,
          ratingInput: PortalApiFaker.newRatingInput(),
        }),
        503,
        {
          code: 'errors.rating.disabled',
          message: 'API rating service is unavailable.',
        },
      );
    });
  });

  afterAll(async () => {
    await apisManagementApiAsAdmin.deleteApi({ orgId, envId, api: createdApi.id });
  });
});
