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
import { MatTableHarness } from '@angular/material/table/testing';

export class ApiAnalyticsWidgetTableHarness extends ComponentHarness {
  static hostSelector = 'api-analytics-widget-table';

  protected getTableWrapper = this.locatorFor('.table-widget');
  protected getTable = this.locatorFor(MatTableHarness);
  protected getNoDataRow = this.locatorForOptional('.mat-mdc-row .mat-body');

  /**
   * Gets the table harness
   */
  async getTableHarness(): Promise<MatTableHarness> {
    return this.getTable();
  }

  /**
   * Gets the total number of rows in the table
   */
  async getRowCount(): Promise<number> {
    const table = await this.getTable();
    return table.getRows().then(rows => rows.length);
  }

  /**
   * Gets the total number of columns in the table
   */
  async getColumnCount(): Promise<number> {
    const table = await this.getTable();
    return table.getHeaderRows().then(headerRows => {
      if (headerRows.length === 0) return 0;
      return headerRows[0].getCells().then(cells => cells.length);
    });
  }

  /**
   * Gets all header cell texts
   */
  async getHeaderTexts(): Promise<string[]> {
    const table = await this.getTable();
    const headerRows = await table.getHeaderRows();
    if (headerRows.length === 0) return [];

    const headerCells = await headerRows[0].getCells();
    const texts: string[] = [];
    for (const cell of headerCells) {
      texts.push(await cell.getText());
    }
    return texts;
  }

  /**
   * Gets all data from the table as an array of row objects
   */
  async getTableData(): Promise<string[][]> {
    const table = await this.getTable();
    const rows = await table.getRows();
    const data: string[][] = [];

    for (const row of rows) {
      const cells = await row.getCells();
      const rowData: string[] = [];
      for (const cell of cells) {
        rowData.push(await cell.getText());
      }
      data.push(rowData);
    }

    return data;
  }

  /**
   * Gets the text of a specific cell by row and column index
   */
  async getCellText(rowIndex: number, columnIndex: number): Promise<string> {
    const table = await this.getTable();
    const rows = await table.getRows();
    if (rowIndex >= rows.length) {
      throw new Error(`Row index ${rowIndex} is out of bounds`);
    }

    const cells = await rows[rowIndex].getCells();
    if (columnIndex >= cells.length) {
      throw new Error(`Column index ${columnIndex} is out of bounds`);
    }

    return cells[columnIndex].getText();
  }

  /**
   * Checks if the table has any data rows
   */
  async hasData(): Promise<boolean> {
    const rowCount = await this.getRowCount();
    return rowCount > 0;
  }

  /**
   * Checks if the table shows the "No content to display" message
   */
  async hasNoDataMessage(): Promise<boolean> {
    const noDataRow = await this.getNoDataRow();
    return noDataRow !== null;
  }

  /**
   * Gets the "No content to display" message text
   */
  async getNoDataMessage(): Promise<string | null> {
    const noDataRow = await this.getNoDataRow();
    if (!noDataRow) {
      return null;
    }
    return noDataRow.text();
  }

  /**
   * Gets the total length value displayed in the table wrapper
   */
  async getTotalLength(): Promise<number> {
    return this.getRowCount();
  }

  /**
   * Checks if the table wrapper is present
   */
  async hasTableWrapper(): Promise<boolean> {
    const tableWrapper = await this.getTableWrapper();
    return tableWrapper !== null;
  }
}
