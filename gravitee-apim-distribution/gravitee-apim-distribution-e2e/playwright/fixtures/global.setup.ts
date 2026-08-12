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
import { test as setup } from '@playwright/test';
import { ADMIN_USER } from '@test-data/fakers/users';
import { ADMIN_AUTH_FILE, basicAuthHeader, DEFAULT_ORG_PATH, MANAGEMENT_API_URL } from '@utils/api-context';

setup('authenticate as admin', async ({ page }) => {
  const response = await page.request.post(`${MANAGEMENT_API_URL}${DEFAULT_ORG_PATH}/user/login`, {
    headers: basicAuthHeader(ADMIN_USER),
  });
  if (!response.ok()) {
    throw new Error(`Admin login failed with status ${response.status()}`);
  }
  await page.context().storageState({ path: ADMIN_AUTH_FILE });
});
