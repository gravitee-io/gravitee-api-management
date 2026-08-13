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
import { Locator } from '@playwright/test';
import { BasicAuthentication } from '@gravitee/utils/configuration';
import { BasePage } from './base.page';

export class LoginPage extends BasePage {
  // The sign-in card carries no data-testid; these app-owned BEM classes are the stable handle.
  get title(): Locator {
    return this.page.locator('.card__header__title');
  }

  get usernameInput(): Locator {
    return this.byTestId('username-input');
  }

  get passwordInput(): Locator {
    return this.byTestId('password-input');
  }

  get signInButton(): Locator {
    return this.byTestId('sign-in-button');
  }

  async goto(): Promise<void> {
    await this.page.goto('');
  }

  async signIn(user: BasicAuthentication): Promise<void> {
    await this.usernameInput.fill(user.username);
    await this.passwordInput.fill(user.password);
    await this.signInButton.click();
  }
}
