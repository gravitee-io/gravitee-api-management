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
import { beforeAll } from '@jest/globals';
import { CurrentUserApi } from '../lib/management-webclient-sdk/src/lib/apis/CurrentUserApi';
import { forManagementAsApiUser, forManagementAsAppUser } from '../lib/utils/configuration';

let initialized = false;

beforeAll(async () => {
  if (initialized) {
    return;
  }

  const orgId = 'DEFAULT';
  const currentUserResourceAsApiUser = new CurrentUserApi(forManagementAsApiUser());
  const currentUserResourceAsAppUser = new CurrentUserApi(forManagementAsAppUser());

  // Ensure that API user and App User are properly initialized before using them in tests.
  // APIM should create these two users automatically
  const apiResponse = await currentUserResourceAsApiUser.getCurrentUserRaw({
    orgId,
  });
  if (apiResponse.raw.status !== 200) {
    throw new Error(`Failed to initialize API user: ${apiResponse.raw.statusText}`);
  }

  const appResponse = await currentUserResourceAsAppUser.getCurrentUserRaw({
    orgId,
  });
  if (appResponse.raw.status !== 200) {
    throw new Error(`Failed to initialize App user: ${appResponse.raw.statusText}`);
  }

  // Wait for 1 second to ensure APIM is ready for the next tests
  await new Promise((resolve) => setTimeout(resolve, 1000));
  initialized = true;
});
