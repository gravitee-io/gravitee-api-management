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
import { MatButtonHarness } from '@angular/material/button/testing';

import { WebhookLogsListHarness } from './components/webhook-logs-list/webhook-logs-list.harness';

export class WebhookLogsHarness extends ComponentHarness {
  static hostSelector = 'webhook-logs';

  private readonly configureButton = this.locatorForOptional(MatButtonHarness.with({ text: /configure reporting/i }));
  private readonly logsList = this.locatorForOptional(WebhookLogsListHarness);

  async clickConfigureReporting(): Promise<void> {
    const button = await this.configureButton();
    if (!button) {
      throw new Error('Configure Reporting button was not found.');
    }
    await button.click();
  }

  async getLogsList(): Promise<WebhookLogsListHarness | null> {
    return this.logsList();
  }
}
