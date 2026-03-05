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
import { MatButtonToggleGroupHarness } from '@angular/material/button-toggle/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

export class TransferOwnershipDialogHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-transfer-ownership-dialog';

  private readonly getToggleGroup = this.locatorFor(MatButtonToggleGroupHarness);
  private readonly getMemberSelect = this.locatorForOptional(MatSelectHarness.with({ selector: '[data-testid="member-select"]' }));
  private readonly getTransferButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="transfer-button"]' }));
  private readonly getCancelButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="cancel-button"]' }));
  private readonly getWarningBox = this.locatorForOptional('[data-testid="warning-box"]');

  async selectMode(value: string): Promise<void> {
    const group = await this.getToggleGroup();
    const toggles = await group.getToggles();
    for (const toggle of toggles) {
      if ((await toggle.getText()).toLowerCase().includes(value.toLowerCase())) {
        await toggle.check();
        return;
      }
    }
    throw new Error(`Toggle "${value}" not found`);
  }

  async getActiveMode(): Promise<string> {
    const group = await this.getToggleGroup();
    const toggles = await group.getToggles({ checked: true });
    return toggles.length > 0 ? toggles[0].getText() : '';
  }

  async selectMember(name: string): Promise<void> {
    const select = await this.getMemberSelect();
    if (!select) throw new Error('Member select not found');
    await select.open();
    const options = await select.getOptions({ text: name });
    if (options.length === 0) throw new Error(`Member "${name}" not found`);
    await options[0].click();
  }

  async isWarningVisible(): Promise<boolean> {
    const warning = await this.getWarningBox();
    return warning !== null;
  }

  async isTransferDisabled(): Promise<boolean> {
    const btn = await this.getTransferButton();
    return btn.isDisabled();
  }

  async transfer(): Promise<void> {
    const btn = await this.getTransferButton();
    await btn.click();
  }

  async cancel(): Promise<void> {
    const btn = await this.getCancelButton();
    await btn.click();
  }
}
