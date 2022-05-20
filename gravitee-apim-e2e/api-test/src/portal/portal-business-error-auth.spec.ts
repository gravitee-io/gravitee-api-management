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
import { describe } from '@jest/globals';
import { AuthenticationApi } from '@portal-apis/AuthenticationApi';
import { forPortalAsAdminUser } from '@client-conf/*';
import { notFound } from '@lib/jest-utils';

const authenticationApiAsAdmin = new AuthenticationApi(forPortalAsAdminUser());

describe('Portal: Business Error - auth', () => {
  test('should return not found with unknown identity provider', async () => {
    const identity = 'IDENTITY';
    const expectedError = { message: `Identity provider [${identity}] can not be found.` };
    await notFound(authenticationApiAsAdmin.exchangeAuthorizationCodeRaw({ identity }), expectedError);
    await notFound(authenticationApiAsAdmin.tokenExchangeRaw({ identity, token: '' }), expectedError);
  });
});
