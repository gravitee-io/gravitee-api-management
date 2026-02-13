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
import { ComponentHarness } from '@angular/cdk/testing';

export class EnvLogsDetailsHarness extends ComponentHarness {
  static hostSelector = 'env-logs-details';

  private readonly backButton = this.locatorFor('.back-button');

  async clickBack(): Promise<void> {
    const button = await this.backButton();
    await button.click();
  }

  async getTitleText(): Promise<string> {
    const title = await this.locatorFor('h1')();
    return title.text();
  }

  async getOverviewUri(): Promise<string> {
    const uriDd = await this.locatorFor('[data-testid="overview-uri"]')();
    return uriDd.text();
  }
}
