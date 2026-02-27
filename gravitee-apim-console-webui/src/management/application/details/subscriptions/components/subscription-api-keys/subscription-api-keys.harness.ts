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
import { MatTableHarness } from '@angular/material/table/testing';

export class SubscriptionApiKeysHarness extends ComponentHarness {
  static hostSelector = 'subscription-api-keys';

  protected getRenewButton = this.locatorForOptional(MatButtonHarness.with({ selector: '[data-testid="renew-api-key-btn"]' }));
  protected getRevokeButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[data-testid="revoke-api-key-btn"]' }));
  protected getExpireButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[data-testid="expire-api-key-btn"]' }));
  protected getTable = this.locatorFor(MatTableHarness);

  public async isRenewButtonDisplayed() {
    return (await this.getRenewButton()) !== null;
  }

  public async clickRenew() {
    const btn = await this.getRenewButton();
    return btn?.click();
  }

  public async getTableRows() {
    const table = await this.getTable();
    return table.getRows();
  }
}
