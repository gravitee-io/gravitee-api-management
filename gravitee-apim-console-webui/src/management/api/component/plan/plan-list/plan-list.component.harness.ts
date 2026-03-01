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
import { MatTableHarness } from '@angular/material/table/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatButtonToggleHarness } from '@angular/material/button-toggle/testing';
import { MatMenuHarness } from '@angular/material/menu/testing';

export class PlanListComponentHarness extends ComponentHarness {
  static hostSelector = 'plan-list';

  private readonly plansTableSelector = '#plansTable';
  private readonly addPlanButtonSelector = '[aria-label="Add new plan"]';

  async getPlansTable(): Promise<MatTableHarness> {
    return this.locatorFor(MatTableHarness.with({ selector: this.plansTableSelector }))();
  }

  async getHeaderColumnNames(): Promise<Record<string, string>> {
    const table = await this.getPlansTable();
    const headerRows = await table.getHeaderRows();
    return headerRows[0].getCellTextByColumnName();
  }

  async getTableText(): Promise<string> {
    const table = await this.getPlansTable();
    return (await table.host()).text();
  }

  async getRowCount(): Promise<number> {
    const table = await this.getPlansTable();
    const rows = await table.getRows();
    return rows.length;
  }

  async getRowCells(): Promise<string[][]> {
    const table = await this.getPlansTable();
    const rows = await table.getRows();
    return parallel(() => rows.map(row => row.getCellTextByIndex()));
  }

  async getAddPlanButton(): Promise<MatButtonHarness | null> {
    return this.locatorForOptional(MatButtonHarness.with({ selector: this.addPlanButtonSelector }))();
  }

  async isAddPlanButtonVisible(): Promise<boolean> {
    const btn = await this.getAddPlanButton();
    return btn !== null;
  }

  async clickAddPlanButton(): Promise<void> {
    const btn = await this.getAddPlanButton();
    if (!btn) {
      throw new Error('Add new plan button is not visible');
    }
    return btn.click();
  }

  async getAddPlanMenuItems(): Promise<string[]> {
    await this.clickAddPlanButton();
    const menu = await this.locatorFor(MatMenuHarness)();
    const items = await menu.getItems();
    return parallel(() => items.map(i => i.getText()));
  }

  async clickAddPlanMenuItem(text: string): Promise<void> {
    await this.clickAddPlanButton();
    const menu = await this.locatorFor(MatMenuHarness)();
    await menu.clickItem({ text });
  }

  async getStatusFilterToggles(): Promise<MatButtonToggleHarness[]> {
    return this.locatorForAll(MatButtonToggleHarness)();
  }

  async selectStatusFilter(text: string | RegExp): Promise<void> {
    const toggle = await this.locatorFor(MatButtonToggleHarness.with({ text }))();
    return toggle.toggle();
  }

  async clickEditPlanButton(): Promise<void> {
    const btn = await this.locatorForOptional(MatButtonHarness.with({ selector: '[aria-label="Edit the plan"]' }))();
    if (!btn) throw new Error('Edit the plan button not found');
    return btn.click();
  }

  async getDesignPlanButton(): Promise<MatButtonHarness | null> {
    return this.locatorForOptional(MatButtonHarness.with({ selector: '[aria-label="Design the plan"]' }))();
  }

  async isDesignPlanButtonVisible(): Promise<boolean> {
    const btn = await this.getDesignPlanButton();
    return btn !== null;
  }

  async clickDesignPlanButton(): Promise<void> {
    const btn = await this.getDesignPlanButton();
    if (!btn) throw new Error('Design the plan button not found');
    return btn.click();
  }

  async clickPublishPlanButton(): Promise<void> {
    const btn = await this.locatorForOptional(MatButtonHarness.with({ selector: '[aria-label="Publish the plan"]' }))();
    if (!btn) throw new Error('Publish the plan button not found');
    return btn.click();
  }

  async clickDeprecatePlanButton(): Promise<void> {
    const btn = await this.locatorForOptional(MatButtonHarness.with({ selector: '[aria-label="Deprecate the plan"]' }))();
    if (!btn) throw new Error('Deprecate the plan button not found');
    return btn.click();
  }

  async clickClosePlanButton(): Promise<void> {
    const btn = await this.locatorForOptional(MatButtonHarness.with({ selector: '[aria-label="Close the plan"]' }))();
    if (!btn) throw new Error('Close the plan button not found');
    return btn.click();
  }

  /** Header and row cells in the shape used by parent specs (e.g. computePlansTableCells). */
  async getTableCells(): Promise<{ headerCells: Record<string, string>[]; rowCells: string[][] }> {
    const [headerNames, rowCells] = await Promise.all([this.getHeaderColumnNames(), this.getRowCells()]);
    return { headerCells: [headerNames], rowCells };
  }
}
