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
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatRowHarness, MatTableHarness } from '@angular/material/table/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

export class ApiPortalHeaderHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-api-portal-header';

  private showTagsToggleLocator = this.locatorForOptional(MatSlideToggleHarness.with({ selector: '[data-testid=show_tags_toggle]' }));
  private showCategoriesToggleLocator = this.locatorForOptional(
    MatSlideToggleHarness.with({ selector: '[data-testid=show_categories_toggle]' }),
  );
  private promotedApiModeToggleLocator = this.locatorForOptional(
    MatSlideToggleHarness.with({ selector: '[data-testid=promoted_api_mode_toggle]' }),
  );
  private getTable = this.locatorForOptional(MatTableHarness);

  public getShowTagsToggle = async () => {
    return this.showTagsToggleLocator();
  };

  public getShowCategoriesToggle = async () => {
    return this.showCategoriesToggleLocator();
  };

  public getPromotedApiModeToggle = async () => {
    return this.promotedApiModeToggleLocator();
  };

  public isToggleChecked = async (togglePromise: Promise<MatSlideToggleHarness>): Promise<boolean> => {
    return await togglePromise.then(el => el.isChecked());
  };

  private isToggleDisabled = async (togglePromise: Promise<MatSlideToggleHarness>) => {
    return togglePromise.then(formField => formField.isDisabled());
  };

  public rowsNumber = async (): Promise<number> => {
    return this.getTable()
      .then((table: MatTableHarness) => table.getRows())
      .then((rows: MatRowHarness[]) => rows.length);
  };

  private getDeleteRowButton = async (index: number) => {
    const table = await this.getTable();
    const rows = await table.getRows();

    return await rows[index]
      .getCells({ columnName: 'actions' })
      .then(cells => cells[0].getHarnessOrNull(MatButtonHarness.with({ selector: '[data-testid=delete-button]' })));
  };

  public deleteHeader = async (index: number) => {
    const deleteButton = await this.getDeleteRowButton(index);
    await deleteButton.click();
  };

  private getUpButton = async (index: number) => {
    const table = await this.getTable();
    const rows = await table.getRows();

    return await rows[index]
      .getCells({ columnName: 'actions' })
      .then(cells => cells[0].getHarnessOrNull(MatButtonHarness.with({ selector: '[data-testid=move-up-button]' })));
  };

  public moveHeaderUp = async (index: number) => {
    const deleteButton = await this.getUpButton(index);
    await deleteButton.click();
  };

  private getDownButton = async (index: number) => {
    const table = await this.getTable();
    const rows = await table.getRows();

    return await rows[index]
      .getCells({ columnName: 'actions' })
      .then(cells => cells[0].getHarnessOrNull(MatButtonHarness.with({ selector: '[data-testid=move-down-button]' })));
  };

  public moveHeaderDown = async (index: number) => {
    const deleteButton = await this.getDownButton(index);
    await deleteButton.click();
  };

  public getEditButton = async (index: number) => {
    const table = await this.getTable();
    const rows = await table.getRows();

    return await rows[index]
      .getCells({ columnName: 'actions' })
      .then(cells => cells[0].getHarnessOrNull(MatButtonHarness.with({ selector: '[data-testid=edit-header-button]' })));
  };
}
