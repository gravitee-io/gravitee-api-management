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

import { ComponentHarness, parallel } from '@angular/cdk/testing';
import { GioFormTagsInputHarness } from '@gravitee/ui-particles-angular';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatTableHarness } from '@angular/material/table/testing';

export class ApplicationSubscriptionListHarness extends ComponentHarness {
  static hostSelector = 'application-subscription-list';

  public getApiSelectInput = this.locatorFor(GioFormTagsInputHarness);
  public getStatusSelect = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="status"]' }));
  public getApiKeyInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="apiKey"]' }));
  public getResetFilterButton = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Reset filters"]' }));
  public getTable = this.locatorFor(MatTableHarness.with({ selector: '#subscriptionsTable' }));
  public getSubscriptionDetailButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[aria-label="Subscription details"]' }));
  public getCreateButton = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Create a subscription"]' }));

  public async getApis(): Promise<string[]> {
    const matSelectHarness = await this.getApiSelectInput();
    return matSelectHarness.getTags();
  }

  public async computeSubscriptionsTableCells() {
    const table = await this.getTable();
    const headerRows = await table.getHeaderRows();
    const headerCells = await parallel(() => headerRows.map(row => row.getCellTextByColumnName()));
    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map(row => row.getCellTextByIndex()));
    return { headerCells, rowCells };
  }

  async selectStatus(text: string) {
    return this.getStatusSelect().then(select => select.clickOptions({ text }));
  }

  async addApiKey(key: string) {
    return this.getApiKeyInput().then(input => input.setValue(key));
  }

  async selectApi(text: string) {
    const autocomplete = await this.getApiSelectInput().then(input => input.getMatAutocompleteHarness());
    const autocompleteOptions = await autocomplete.getOptions();
    const options = await parallel(() => autocompleteOptions.map(async option => ({ text: await option.getText(), option })));
    const option = options.find(option => option.text === text);
    return option.option.click();
  }

  async searchApi(apiName: string) {
    return this.getApiSelectInput()
      .then(input => input.getMatAutocompleteHarness())
      .then(autocomplete => autocomplete.enterText(apiName));
  }

  async getApiTags() {
    return this.getApiSelectInput().then(input => input.getTags());
  }

  async createSubscription() {
    return this.getCreateButton().then(btn => btn.click());
  }

  private async getEditButton(index: number) {
    return this.getSubscriptionDetailButtons().then(buttons => buttons[index]);
  }
}
