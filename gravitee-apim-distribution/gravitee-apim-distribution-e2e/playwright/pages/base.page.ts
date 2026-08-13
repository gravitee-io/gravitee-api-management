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
import { Locator, Page } from '@playwright/test';

/**
 * Base for every page object.
 *
 * Page objects expose locators and actions only — never assertions. Tests own the assertions so a
 * failure points at the expectation that broke rather than at shared helper code.
 */
export abstract class BasePage {
  constructor(protected readonly page: Page) {}

  /**
   * The Console is an Angular app served under a hash route (`#!/<path>`).
   *
   * The path stays relative on purpose: a leading `/` would resolve against the origin root and
   * bypass a path-prefixed deployment such as `http://nginx/console` (see `CONSOLE_BASE_URL`).
   */
  protected async gotoHashRoute(path: string): Promise<void> {
    await this.page.goto(`#!/${path}`);
  }

  protected byTestId(testId: string): Locator {
    return this.page.getByTestId(testId);
  }

  get dialog(): Locator {
    return this.page.locator('mat-dialog-container');
  }
}
