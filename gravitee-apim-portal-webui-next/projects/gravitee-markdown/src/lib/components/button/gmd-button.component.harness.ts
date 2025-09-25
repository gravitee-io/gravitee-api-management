/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { ComponentHarness } from '@angular/cdk/testing';

export class GmdButtonComponentHarness extends ComponentHarness {
  static hostSelector = 'gmd-button';

  async getText(): Promise<string> {
    const button = await this.locatorFor('a')();
    return await button.text();
  }

  async click(): Promise<void> {
    const button = await this.locatorFor('a')();
    return await button.click();
  }

  async getAppearance(): Promise<string> {
    const button = await this.locatorFor('a')();
    const classList = await button.getAttribute('class');

    if (classList?.includes('gmd-button--filled')) {
      return 'filled';
    } else if (classList?.includes('gmd-button--outlined')) {
      return 'outlined';
    } else if (classList?.includes('gmd-button--text')) {
      return 'text';
    }

    return 'unknown';
  }

  async getHref(): Promise<string | null> {
    const button = await this.locatorFor('a')();
    return await button.getAttribute('href');
  }

  async getTarget(): Promise<string | null> {
    const button = await this.locatorFor('a')();
    return await button.getAttribute('target');
  }
}
