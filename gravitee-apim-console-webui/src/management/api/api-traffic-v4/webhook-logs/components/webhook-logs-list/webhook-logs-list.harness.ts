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
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatTableHarness } from '@angular/material/table/testing';

import { LogsListBaseHarness } from '../../../components/logs-list-base/logs-list-base.harness';

export class WebhookLogsListHarness extends LogsListBaseHarness {
  static hostSelector = 'webhook-logs-list';

  private readonly getTable = this.locatorFor(MatTableHarness);
  private readonly getDetailsButtons = this.locatorForAll(
    MatButtonHarness.with({ selector: '[data-testid="webhook_logs_details_button"]' }),
  );

  protected getLogsTable(): Promise<MatTableHarness> {
    return this.getTable();
  }

  async clickDetailsButtonAtRow(index: number) {
    const buttons = await this.getDetailsButtons();
    if (index < 0 || index >= buttons.length) {
      throw new Error(`Cannot click details button at row ${index}. Only ${buttons.length} button(s) found.`);
    }
    await buttons[index].click();
  }
}
