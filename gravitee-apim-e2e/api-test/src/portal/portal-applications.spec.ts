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

import { afterAll, beforeAll, describe } from '@jest/globals';
import {
  ANONYMOUS,
  forPortal,
  forPortalAsAdminUser,
  forPortalAsApiUser,
  forPortalAsAppUser,
  forPortalAsSimpleUser,
} from '@gravitee/utils/configuration';
import { created, fail, noContent, unauthorized } from '@lib/jest-utils';
import { ApplicationApi } from '@gravitee/portal-webclient-sdk/src/lib/apis/ApplicationApi';
import { Application } from '@gravitee/portal-webclient-sdk/src/lib/models/Application';
import { PortalApplicationFaker } from '@gravitee/fixtures/portal/PortalApplicationFaker';
import { ApiKeyModeEnum } from '../../../lib/portal-webclient-sdk/src/lib';

const portalApplicationApiAsAdminUser = new ApplicationApi(forPortalAsAdminUser());
const portalApplicationApiAsAppUser = new ApplicationApi(forPortalAsAppUser());
const portalApplicationApiAsApiUser = new ApplicationApi(forPortalAsApiUser());
const portalApplicationApiAsSimpleUser = new ApplicationApi(forPortalAsSimpleUser());
const portalApplicationApiAsAnonymous = new ApplicationApi(forPortal({ auth: ANONYMOUS }));

describe('Portal application tests', () => {
  describe('Portal application tests with authorized users', () => {
    let application: Application;

    describe.each`
      user             | applicationResource
      ${'ADMIN'}       | ${portalApplicationApiAsAdminUser}
      ${'api_user'}    | ${portalApplicationApiAsApiUser}
      ${'app_user'}    | ${portalApplicationApiAsAppUser}
      ${'simple_user'} | ${portalApplicationApiAsSimpleUser}
    `('As $user user', ({ applicationResource }) => {
      test('should create an application', async () => {
        application = await created(
          applicationResource.createApplicationRaw({
            applicationInput: PortalApplicationFaker.newApplicationInput(),
          }),
        );
      });

      test('should delete an application', async () => {
        await noContent(
          applicationResource.deleteApplicationByApplicationIdRaw({
            applicationId: application.id,
          }),
        );
      });
    });

    test('should fail because of api key mode is invalid', async () => {
      await fail(
        portalApplicationApiAsAdminUser.createApplicationRaw({
          applicationInput: PortalApplicationFaker.newApplicationInput({ api_key_mode: 'dummy' as ApiKeyModeEnum }),
        }),
        400,
        "Cannot construct instance of `io.gravitee.rest.api.portal.rest.model.ApiKeyModeEnum`, problem: Unexpected value 'dummy'",
      );
    });
  });

  describe('Portal application tests with unauthorized users', () => {
    let application: Application;

    beforeAll(async () => {
      application = await portalApplicationApiAsAdminUser.createApplication({
        applicationInput: PortalApplicationFaker.newApplicationInput(),
      });
    });

    test('should fail to create an application as an unauthorized user', async () => {
      await unauthorized(
        portalApplicationApiAsAnonymous.createApplicationRaw({
          applicationInput: PortalApplicationFaker.newApplicationInput(),
        }),
      );
    });

    test('should fail to delete an application as an unauthorized user', async () => {
      await unauthorized(
        portalApplicationApiAsAnonymous.deleteApplicationByApplicationIdRaw({
          applicationId: application.id,
        }),
      );
    });

    afterAll(async () => {
      await portalApplicationApiAsAdminUser.deleteApplicationByApplicationId({
        applicationId: application.id,
      });
    });
  });
});
