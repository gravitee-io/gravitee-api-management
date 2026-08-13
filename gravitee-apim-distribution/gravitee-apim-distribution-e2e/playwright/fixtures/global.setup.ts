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
import { ADMIN_USER } from '@gravitee/utils/configuration';
import { ADMIN_AUTH_FILE } from '@utils/config';

/**
 * Logs in through the management API so the Console session cookies land in the browser context,
 * then saves them for every other project to reuse via `storageState`.
 *
 * This runs in a browser context on purpose: the Console authenticates with the
 * `Auth-Graviteeio-APIM` cookie, which `page.request` stores and `storageState()` can capture.
 * API-only setup/teardown must not go through here — it uses the generated SDK, which
 * authenticates statelessly per request (see README).
 */
setup('authenticate as admin', async ({ page }) => {
  const response = await page.request.post(`${process.env.MANAGEMENT_BASE_URL}/organizations/DEFAULT/user/login`, {
    headers: {
      Authorization: `Basic ${Buffer.from(`${ADMIN_USER.username}:${ADMIN_USER.password}`).toString('base64')}`,
    },
  });
  if (!response.ok()) {
    throw new Error(`Admin login failed with status ${response.status()}`);
  }
  await page.context().storageState({ path: ADMIN_AUTH_FILE });
});
