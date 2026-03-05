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
import { MatSelectHarness } from '@angular/material/select/testing';

export class EditMemberRoleDialogHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-edit-member-role-dialog';

  private readonly getSelectHarness = this.locatorFor(MatSelectHarness);
  private readonly getSaveButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="save-button"]' }));
  private readonly getCancelButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="cancel-button"]' }));
  private readonly getRoleLabel = this.locatorFor('.edit-member-role-dialog__label');

  async getRoleLabelText(): Promise<string> {
    const el = await this.getRoleLabel();
    return el.text();
  }

  async getSelectedRole(): Promise<string> {
    const select = await this.getSelectHarness();
    return select.getValueText();
  }

  async getRoleOptions(): Promise<string[]> {
    const select = await this.getSelectHarness();
    await select.open();
    const options = await select.getOptions();
    const labels = await Promise.all(options.map(o => o.getText()));
    await select.close();
    return labels;
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

  async isSaveDisabled(): Promise<boolean> {
    const btn = await this.getSaveButton();
    return btn.isDisabled();
  }

  async save(): Promise<void> {
    const btn = await this.getSaveButton();
    await btn.click();
  }

  async cancel(): Promise<void> {
    const btn = await this.getCancelButton();
    await btn.click();
  }
}
