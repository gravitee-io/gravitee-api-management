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
import { expect, test } from '../../fixtures/base.fixture';
import { API_PUBLISHER_AUTH_FILE } from '@utils/config';
import { linkJira } from '@utils/jira';

test.describe('Gateway instance information as admin', () => {
  test.beforeEach(async ({ gatewaysPage }) => {
    await gatewaysPage.goto();
  });

  test('APIM-14926: lists the running gateway instances', async ({ gatewaysPage }) => {
    linkJira(test.info(), 'APIM-14926');

    await expect(gatewaysPage.heading).toBeVisible();
    await expect(gatewaysPage.instanceRows.first()).toBeVisible();
    expect(await gatewaysPage.instanceRows.count()).toBeGreaterThan(0);
  });

  test('APIM-14926: opens the overview of a selected gateway', async ({ page, gatewaysPage }) => {
    linkJira(test.info(), 'APIM-14926');

    await gatewaysPage.openFirstInstance();

    await expect(page).toHaveURL(/gateways/);
    await expect(gatewaysPage.section('Information')).toBeVisible();
    await expect(gatewaysPage.section('Plugins')).toBeVisible();
    await expect(gatewaysPage.section('System properties')).toBeVisible();
  });

  test('APIM-14926: shows monitoring information for a selected gateway', async ({ page, gatewaysPage }) => {
    linkJira(test.info(), 'APIM-14926');

    await gatewaysPage.openFirstInstance();
    await gatewaysPage.openMonitoring();

    await expect(page).toHaveURL(/monitoring/);
    await expect(gatewaysPage.monitoringBox('jvm')).toContainText('JVM');
    await expect(gatewaysPage.monitoringBox('cpu')).toContainText('CPU');
    await expect(gatewaysPage.monitoringBox('process')).toContainText('Process');
    await expect(gatewaysPage.monitoringBox('thread')).toContainText('Thread');
    await expect(gatewaysPage.monitoringBox('gc')).toContainText('Garbage collector');
  });
});

test.describe('Gateway instance information as non-admin', () => {
  test.use({ storageState: API_PUBLISHER_AUTH_FILE });

  test('APIM-14926: redirects a user without permission away from the gateways route', async ({ page, gatewaysPage }) => {
    linkJira(test.info(), 'APIM-14926');

    await gatewaysPage.goto();

    // The route guard requires `environment-instance-r`. Navigation puts /gateways in the URL, so
    // this assertion can only pass once the guard has actually redirected — which makes it a real
    // anchor for the absence check below rather than something a half-rendered page satisfies.
    await expect(page).not.toHaveURL(/gateways/);
    await expect(gatewaysPage.heading).toBeHidden();
  });
});
