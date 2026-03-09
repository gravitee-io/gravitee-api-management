/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatProgressBarHarness } from '@angular/material/progress-bar/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatTableHarness } from '@angular/material/table/testing';

export class MessagesBrowserHarness extends ComponentHarness {
  static hostSelector = 'gke-messages-browser';

  private readonly getBackButton = this.locatorFor(MatButtonHarness.with({ selector: '[mat-icon-button]' }));
  private readonly getFetchButton = this.locatorFor(MatButtonHarness.with({ text: /Fetch/ }));
  private readonly getProgressBar = this.locatorForOptional(MatProgressBarHarness);
  private readonly getTitle = this.locatorFor('.messages-browser__title');
  private readonly getTables = this.locatorForAll(MatTableHarness);
  private readonly getSelects = this.locatorForAll(MatSelectHarness);
  private readonly getEmptyMessage = this.locatorForOptional('.messages-browser__empty');
  private readonly getDetail = this.locatorForOptional('.messages-browser__detail');

  async getTopicName() {
    const title = await this.getTitle();
    return title.text();
  }

  async isLoading() {
    return (await this.getProgressBar()) !== null;
  }

  async clickBack() {
    const button = await this.getBackButton();
    await button.click();
  }

  async clickFetch() {
    const button = await this.getFetchButton();
    await button.click();
  }

  async getMessagesRows() {
    const tables = await this.getTables();
    if (tables.length < 1) return [];
    const rows = await tables[0].getRows();
    const allCells = await parallel(() => rows.map(row => row.getCellTextByColumnName()));
    return allCells.filter(cells => 'partition' in cells);
  }

  async clickMessageRow(index: number) {
    const rows = await this.locatorForAll('.messages-browser__row')();
    if (index < rows.length) {
      await rows[index].click();
    }
  }

  async hasEmptyMessage() {
    return (await this.getEmptyMessage()) !== null;
  }

  async hasDetail() {
    return (await this.getDetail()) !== null;
  }

  async getOffsetModeSelect() {
    const selects = await this.getSelects();
    return selects.length > 1 ? selects[1] : undefined;
  }

  async selectOffsetMode(mode: string) {
    const select = await this.getOffsetModeSelect();
    if (select) {
      await select.open();
      const options = await select.getOptions({ text: mode });
      if (options.length > 0) {
        await options[0].click();
      }
    }
  }
}
