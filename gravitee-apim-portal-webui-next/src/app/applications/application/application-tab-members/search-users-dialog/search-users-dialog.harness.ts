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
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatChipHarness, MatChipSetHarness } from '@angular/material/chips/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

export class SearchUsersDialogHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-search-users-dialog';

  private readonly getSelectHarness = this.locatorFor(MatSelectHarness);
  private readonly getSearchInput = this.locatorFor(MatInputHarness.with({ selector: '[data-testid="user-search-input"]' }));
  private readonly getAddButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="add-button"]' }));
  private readonly getCancelButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="cancel-button"]' }));
  private readonly getChipSet = this.locatorForOptional(MatChipSetHarness);

  async getSelectedRole(): Promise<string> {
    const select = await this.getSelectHarness();
    return select.getValueText();
  }

  async selectRole(roleName: string): Promise<void> {
    const select = await this.getSelectHarness();
    await select.open();
    const options = await select.getOptions({ text: roleName });
    if (options.length === 0) {
      throw new Error(`Role option "${roleName}" not found`);
    }
    await options[0].click();
  }

  async typeSearchQuery(query: string): Promise<void> {
    const input = await this.getSearchInput();
    await input.setValue(query);
  }

  async getChipTexts(): Promise<string[]> {
    const chipSet = await this.getChipSet();
    if (!chipSet) return [];
    const chips: MatChipHarness[] = await chipSet.getChips();
    return Promise.all(chips.map(c => c.getText()));
  }

  async removeChip(text: string): Promise<void> {
    const chipSet = await this.getChipSet();
    if (!chipSet) throw new Error('No chip set found');
    const chips = await chipSet.getChips({ text: new RegExp(text) });
    if (chips.length === 0) throw new Error(`Chip "${text}" not found`);
    await chips[0].remove();
  }

  async isAddDisabled(): Promise<boolean> {
    const btn = await this.getAddButton();
    return btn.isDisabled();
  }

  async add(): Promise<void> {
    const btn = await this.getAddButton();
    await btn.click();
  }

  async cancel(): Promise<void> {
    const btn = await this.getCancelButton();
    await btn.click();
  }
}
