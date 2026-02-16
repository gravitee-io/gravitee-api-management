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
import { MatTableHarness } from '@angular/material/table/testing';

export class EnvLogsTableHarness extends ComponentHarness {
  static hostSelector = 'env-logs-table';

  private readonly getTable = this.locatorFor(MatTableHarness);

  public async getRows() {
    const table = await this.getTable();
    return await table.getRows();
  }

  public async getRowsData() {
    const rows = await this.getRows();
    return await parallel(() => rows.map(row => row.getCellTextByColumnName()));
  }

  public async getMethodBadge(rowIndex: number) {
    const selector = `tbody tr:nth-of-type(${rowIndex + 1}) [class*="gio-method-badge-"]`;
    const badge = await this.locatorForOptional(selector)();
    return badge ? badge.text() : null;
  }

  public async getStatusBadge(rowIndex: number) {
    const selector = `tbody tr:nth-of-type(${rowIndex + 1}) [class*="gio-badge-"]`;
    const badge = await this.locatorForOptional(selector)();
    return badge ? badge.text() : null;
  }

  public async getTimestampLink(rowIndex: number) {
    const selector = `tbody tr:nth-of-type(${rowIndex + 1}) a.log-timestamp-link`;
    return this.locatorForOptional(selector)();
  }
}
