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
import { test as base } from '@playwright/test';
import { GatewaysPage } from '../pages/gateways.page';
import { HomePage } from '../pages/home.page';
import { LoginPage } from '../pages/login.page';

/**
 * Every spec imports `test` and `expect` from here rather than from `@playwright/test`, so page
 * objects and shared setup arrive through one door and new fixtures reach all specs at once.
 */
export const test = base.extend<{
  loginPage: LoginPage;
  homePage: HomePage;
  gatewaysPage: GatewaysPage;
}>({
  loginPage: async ({ page }, use) => {
    await use(new LoginPage(page));
  },
  homePage: async ({ page }, use) => {
    await use(new HomePage(page));
  },
  gatewaysPage: async ({ page }, use) => {
    await use(new GatewaysPage(page));
  },
});

export { expect } from '@playwright/test';
