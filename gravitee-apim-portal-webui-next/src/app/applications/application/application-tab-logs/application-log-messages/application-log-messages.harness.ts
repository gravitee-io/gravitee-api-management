/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { MatTableHarness } from '@angular/material/table/testing';

import { PaginationHarness } from '../../../../../components/pagination/pagination.harness';
import { DivHarness } from '../../../../../testing/div.harness';

export class ApplicationLogMessagesHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-application-log-messages';

  protected locateTable = this.locatorFor(MatTableHarness);
  protected locatePagination = this.locatorFor(PaginationHarness);
  protected locateNoLogsFound = this.locatorFor(DivHarness.with({ selector: '.no-logs-found' }));
  protected locateErrorMessage = this.locatorFor(DivHarness.with({ selector: '#application-log-messages-error' }));

  async getNumberOfRows(): Promise<number> {
    return await this.locateTable()
      .then(table => table.getRows())
      .then(rows => rows.length);
  }

  async isNoLogsMessageShown(): Promise<boolean> {
    return this.locateNoLogsFound()
      .then(div => div.getText().then(text => !!text?.includes('No message logs found.')))
      .catch(_ => false);
  }

  async isErrorMessageShown(): Promise<boolean> {
    return this.locateErrorMessage()
      .then(div => div.getText().then(text => !!text?.includes('An error has occurred when returning message logs. Try again later.')))
      .catch(_ => false);
  }

  async getCorrelationIdByRow(rowIndex: number): Promise<string> {
    return this.locateTable()
      .then(table => table.getRows())
      .then(rows => rows[rowIndex].getCellTextByIndex({ columnName: 'correlationId' }))
      .then(cells => cells[0]);
  }

  async getPagination(): Promise<PaginationHarness> {
    return this.locatePagination();
  }

  async clickOnRowByRowIndex(rowIndex: number): Promise<void> {
    return this.locateTable()
      .then(table => table.getRows())
      .then(rows => rows[rowIndex].host())
      .then(host => host.click());
  }
}
