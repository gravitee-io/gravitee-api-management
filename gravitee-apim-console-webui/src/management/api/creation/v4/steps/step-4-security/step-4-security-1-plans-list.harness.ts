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

export class Step4Security1PlansListHarness extends ComponentHarness {
  static hostSelector = 'step-4-security-1-plans-list';

  private readonly table = this.locatorFor(MatTableHarness);

  protected getTable(): Promise<MatTableHarness> {
    return this.table();
  }

  protected getButtonBySelector = (selector: string) => this.locatorFor(MatButtonHarness.with({ selector }))();

  protected getButtonByText = (text: string) =>
    this.locatorFor(
      MatButtonHarness.with({
        text: text,
      }),
    )();

  private async getTextByColumnAndRowIndex(index: number, column: string): Promise<string> {
    return this.getTable()
      .then((x) => x.getRows())
      .then((rows) => rows[index])
      .then((row) => row.getCellTextByIndex({ columnName: column }))
      .then((text) => text[0]);
  }

  async getNameByRowIndex(index: number): Promise<string> {
    return this.getTextByColumnAndRowIndex(index, 'name');
  }

  async getSecurityTypeByRowIndex(index: number): Promise<string> {
    return this.getTextByColumnAndRowIndex(index, 'security');
  }

  async countNumberOfRows(): Promise<number> {
    return this.table()
      .then((table) => table.getRows())
      .then((rows) => rows.length);
  }

  async clickRemovePlanButton(): Promise<void> {
    return this.getButtonBySelector('[aria-label="Remove plan"]').then((btn) => btn.click());
  }

  async clickPrevious() {
    return (await this.getButtonByText('Previous')).click();
  }

  async clickValidate() {
    return (await this.getButtonByText('Validate my plans')).click();
  }

  async fillAndValidate(): Promise<void> {
    return this.clickValidate();
  }
}
