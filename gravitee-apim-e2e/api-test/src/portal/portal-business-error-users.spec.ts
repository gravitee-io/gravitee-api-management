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
import { forManagementAsAdminUser, forPortalAsAdminUser, forPortalAsSimpleUser } from '@client-conf/*';
import { UsersApi as UsersManagementApi } from '@management-apis/UsersApi';
import { UsersFaker } from '@management-fakers/UsersFaker';
import { UserEntity } from '@management-models/UserEntity';
import { fail, notFound, unauthorized } from '@lib/jest-utils';
import { UsersApi as UsersPortalApi } from '@portal-apis/UsersApi';
import { UserApi as UserPortalApi } from '@portal-apis/UserApi';
import { PortalApi as PortalManagementApi } from '@management-apis/PortalApi';

const portalManagementApi = new PortalManagementApi(forManagementAsAdminUser());
const usersManagementApiAsAdmin = new UsersManagementApi(forManagementAsAdminUser());
const usersPortalApiAsAdmin = new UsersPortalApi(forPortalAsAdminUser());
const userPortalApiAsSimpleUser = new UserPortalApi(forPortalAsSimpleUser());
const orgId = 'DEFAULT';
const envId = 'DEFAULT';

let user: UserEntity;

describe('Portal: Business Error - users', () => {
  beforeAll(async () => {
    user = await usersManagementApiAsAdmin.createUser({ orgId, envId, newPreRegisterUserEntity: UsersFaker.newNewPreRegisterUserEntity() });
  });

  describe('400', () => {
    test('should not create user if already exists', async () => {
      const expectedError = { message: `A user [${user.email}] already exists for organization ${orgId}.` };
      await fail(
        usersPortalApiAsAdmin.registerNewUserRaw({ registerUserInput: UsersFaker.newRegisterUserInput({ email: user.email }) }),
        400,
        expectedError,
      );
    });

    test('invalid input', async () => {
      await fail(usersPortalApiAsAdmin.registerNewUserRaw({}), 400, { message: 'Input must not be null.' });
      // @ts-ignore
      await fail(usersPortalApiAsAdmin.registerNewUserRaw({ registerUserInput: {} }), 400, { code: 'registerUser.arg0.email' });
      await fail(usersPortalApiAsAdmin.registerNewUserRaw({ registerUserInput: { email: 'DUMMY EMAIL' } }), 400, {
        code: 'errors.email.invalid',
      });
      await fail(usersPortalApiAsAdmin.finalizeUserRegistrationRaw({}), 400, { message: 'Input must not be null.' });
      // @ts-ignore
      await fail(usersPortalApiAsAdmin.finalizeUserRegistrationRaw({ finalizeRegistrationInput: {} }), 400, [
        { code: 'finalizeRegistration.arg0.lastname' },
        { code: 'finalizeRegistration.arg0.firstname' },
        { code: 'finalizeRegistration.arg0.password' },
        { code: 'finalizeRegistration.arg0.token' },
      ]);
      await fail(usersPortalApiAsAdmin.resetUserPasswordRaw({}), 400, { message: 'Input must not be null.' });
    });
  });

  describe('401', () => {
    test('should not update user without id', async () => {
      await unauthorized(userPortalApiAsSimpleUser.updateCurrentUserRaw({ userInput: { id: '', avatar: '' } }));
    });
  });

  describe('404', () => {
    test('should not delete unknown notification', async () => {
      await notFound(userPortalApiAsSimpleUser.deleteCurrentUserNotificationByNotificationIdRaw({ notificationId: 'NOTIFICATION' }));
    });
  });

  describe('503', () => {
    beforeAll(async () => {
      await portalManagementApi.savePortalConfig({ orgId, envId, portalSettingsEntity: { portal: { userCreation: { enabled: false } } } });
    });

    test('should not register user if service is disabled', async () => {
      await fail(usersPortalApiAsAdmin.registerNewUserRaw({ registerUserInput: UsersFaker.newRegisterUserInput() }), 503, {
        message: 'User registration service is unavailable.',
      });
    });

    afterAll(async () => {
      await portalManagementApi.savePortalConfig({ orgId, envId, portalSettingsEntity: { portal: { userCreation: { enabled: true } } } });
    });
  });

  afterAll(async () => {
    await usersManagementApiAsAdmin.deleteUser({ orgId, envId, userId: user.id });
  });
});
