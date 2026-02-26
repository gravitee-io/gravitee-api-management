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
import { MatTableHarness } from '@angular/material/table/testing';

export class BrokerDetailHarness extends ComponentHarness {
  static hostSelector = 'gke-broker-detail';

  private readonly getLogDirTable = this.locatorForOptional(MatTableHarness.with({ selector: '#log-dir-table' }));
  private readonly getConfigTable = this.locatorForOptional(MatTableHarness.with({ selector: '#config-table' }));
  private readonly getBackButton = this.locatorFor(MatButtonHarness.with({ selector: '[mat-icon-button]' }));
  private readonly getProgressBar = this.locatorForOptional(MatProgressBarHarness);
  private readonly getTitle = this.locatorFor('.broker-detail__title');
  private readonly getControllerBadge = this.locatorForOptional('.broker-detail__controller-badge');
  private readonly getEmptyMessage = this.locatorForOptional('.broker-detail__empty-message');

  async getBrokerTitle() {
    const title = await this.getTitle();
    return title.text();
  }

  async isController() {
    return (await this.getControllerBadge()) !== null;
  }

  async isLoading() {
    return (await this.getProgressBar()) !== null;
  }

  async clickBack() {
    const button = await this.getBackButton();
    await button.click();
  }

  async getLogDirEmptyMessage() {
    const el = await this.getEmptyMessage();
    return el ? el.text() : null;
  }

  async getLogDirRows() {
    const table = await this.getLogDirTable();
    if (!table) return [];
    const rows = await table.getRows();
    return parallel(() => rows.map(row => row.getCellTextByColumnName()));
  }

  async getConfigRows() {
    const table = await this.getConfigTable();
    if (!table) return [];
    const rows = await table.getRows();
    return parallel(() => rows.map(row => row.getCellTextByColumnName()));
  }
}
