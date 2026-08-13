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
import { ADMIN_USER } from '@gravitee/utils/configuration';
import { expect, test } from '../../fixtures/base.fixture';
import { linkJira } from '@utils/jira';

// This suite exercises the login flow itself, so it must start without the shared admin session.
test.use({ storageState: { cookies: [], origins: [] } });

test.describe('Login', () => {
  test.beforeEach(async ({ loginPage }) => {
    await loginPage.goto();
  });

  test('APIM-14926: shows the sign-in page to an anonymous visitor', async ({ page, loginPage }) => {
    linkJira(test.info(), 'APIM-14926');

    await expect(page).toHaveURL(/login/);
    await expect(loginPage.title).toHaveText('Sign In');
  });

  test('APIM-14926: signs in with valid credentials and lands on the home overview', async ({ page, loginPage, homePage }) => {
    linkJira(test.info(), 'APIM-14926');

    await loginPage.signIn(ADMIN_USER);

    await expect(page).toHaveURL(/\/home\/overview/);
    await expect(homePage.overviewTab).toBeVisible();
    await expect(homePage.apiHealthCheckTab).toBeVisible();
    await expect(homePage.tasksTab).toBeVisible();
    await expect(homePage.broadcastsTab).toBeVisible();
    await expect(homePage.apiEventsHeading).toBeVisible();
  });
});
