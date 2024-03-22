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
import { ApplicationsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/ApplicationsApi';
import {
  forManagement,
  forManagementAsAdminUser,
  forManagementAsApiUser,
  forManagementAsAppUser,
  forManagementAsSimpleUser,
} from '@gravitee/utils/configuration';
import { created, fail, noContent, unauthorized } from '@lib/jest-utils';
import { ApplicationEntity } from '@gravitee/management-webclient-sdk/src/lib/models/ApplicationEntity';
import { ApplicationsFaker } from '@gravitee/fixtures/management/ApplicationsFaker';
import { NewApiEntityFlowModeEnum, NewApplicationEntityOriginEnum } from '../../../lib/management-webclient-sdk/src/lib/models';

const managementApplicationsApiAsAdminUser = new ApplicationsApi(forManagementAsAdminUser());
const managementApplicationsApiAsAppUser = new ApplicationsApi(forManagementAsAppUser());
const managementApplicationsApiAsApiUser = new ApplicationsApi(forManagementAsApiUser());
const managementApplicationsApiAsSimpleUser = new ApplicationsApi(forManagementAsSimpleUser());
const managementApplicationsApiAsUnknownUser = new ApplicationsApi(forManagement({ username: 'unknown', password: 'dummyPwd' }));

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

describe('API Management Application tests', function () {
  describe('Application tests with authorized users', () => {
    let application: ApplicationEntity;

    describe.each`
      user             | applicationResource
      ${'ADMIN'}       | ${managementApplicationsApiAsAdminUser}
      ${'api_user'}    | ${managementApplicationsApiAsApiUser}
      ${'app_user'}    | ${managementApplicationsApiAsAppUser}
      ${'simple_user'} | ${managementApplicationsApiAsSimpleUser}
    `('As $user user', ({ applicationResource }) => {
      test('should create an application', async () => {
        application = await created(
          applicationResource.createApplicationRaw({
            orgId,
            envId,
            newApplicationEntity: ApplicationsFaker.newApplication(),
          }),
        );
        expect(application.status).toEqual('ACTIVE');
      });

      test('should delete an application', async () => {
        await noContent(
          applicationResource.deleteApplicationRaw({
            orgId,
            envId,
            application: application.id,
          }),
        );
      });

      test('should fail because of origin is invalid', async () => {
        await fail(
          applicationResource.createApplicationRaw({
            orgId,
            envId,
            newApplicationEntity: ApplicationsFaker.newApplication({ origin: 'dummy' as NewApplicationEntityOriginEnum }),
          }),
          400,
          'Cannot deserialize value of type `io.gravitee.definition.model.Origin` from String "dummy": not one of the values accepted for Enum class: [MANAGEMENT, KUBERNETES]',
        );
      });
    });
  });

  describe('Application tests with unauthorized users', () => {
    let application: ApplicationEntity;

    beforeAll(async () => {
      application = await managementApplicationsApiAsAdminUser.createApplication({
        orgId,
        envId,
        newApplicationEntity: ApplicationsFaker.newApplication(),
      });
    });

    test('should fail to create an application as an unauthorized user', async () => {
      await unauthorized(
        managementApplicationsApiAsUnknownUser.createApplicationRaw({
          orgId,
          envId,
          newApplicationEntity: ApplicationsFaker.newApplication(),
        }),
      );
    });

    test('should fail to delete an application as an unauthorized user', async () => {
      await unauthorized(
        managementApplicationsApiAsUnknownUser.deleteApplicationRaw({
          orgId,
          envId,
          application: application.id,
        }),
      );
    });

    afterAll(async () => {
      await managementApplicationsApiAsAdminUser.deleteApplication({
        orgId,
        envId,
        application: application.id,
      });
    });
  });
});
