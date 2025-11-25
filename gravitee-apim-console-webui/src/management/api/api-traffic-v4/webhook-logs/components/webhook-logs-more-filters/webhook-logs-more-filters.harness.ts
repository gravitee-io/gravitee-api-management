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
import { MatSelectHarness } from '@angular/material/select/testing';

export class WebhookLogsMoreFiltersHarness extends ComponentHarness {
  static hostSelector = 'webhook-logs-more-filters';

  private getClearAllButton = this.locatorFor(MatButtonHarness.with({ text: 'Clear all' }));
  private getApplyButton = this.locatorFor(MatButtonHarness.with({ text: 'Show results' }));
  private getCloseButton = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Close"]' }));
  private getCallbackSelect = this.locatorFor(MatSelectHarness.with({ selector: 'mat-select[formControlName="callbackUrls"]' }));

  async clickClearAll(): Promise<void> {
    return (await this.getClearAllButton()).click();
  }

  async clickApply(): Promise<void> {
    return (await this.getApplyButton()).click();
  }

  async clickClose(): Promise<void> {
    return (await this.getCloseButton()).click();
  }

  async selectCallbackUrls(urls: string[]): Promise<void> {
    const select = await this.getCallbackSelect();
    await select.open();
    for (const url of urls) {
      await select.clickOptions({ text: url });
    }
  }
}
