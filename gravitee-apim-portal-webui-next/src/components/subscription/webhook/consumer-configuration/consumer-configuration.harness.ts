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

import { ComponentHarness, parallel } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatErrorHarness } from '@angular/material/form-field/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatTableHarness } from '@angular/material/table/testing';

export class ConsumerConfigurationComponentHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-consumer-configuration';

  async getInputTextFromControlName(formControlName: string): Promise<string> {
    const input: MatInputHarness = await this.locatorFor(MatInputHarness.with({ selector: `[formcontrolname='${formControlName}']` }))();
    return input.getValue();
  }

  async setInputTextValueFromControlName(formControlName: string, newValue: string): Promise<void> {
    const input: MatInputHarness = await this.locatorFor(MatInputHarness.with({ selector: `[formcontrolname='${formControlName}']` }))();
    await input.setValue(newValue);
    return input.blur();
  }

  async selectOption(formControlName: string, newValue: string): Promise<void> {
    const select = await this.locatorFor(MatSelectHarness.with({ selector: `[formcontrolname='${formControlName}']` }))();
    return select.clickOptions({ text: newValue });
  }

  async selectOptionById(id: string, newValue: string): Promise<void> {
    const select = await this.locatorFor(MatSelectHarness.with({ selector: `#${id}` }))();
    return select.clickOptions({ text: newValue });
  }

  async getSelectedOption(formControlName: string): Promise<string> {
    const select = await this.locatorFor(MatSelectHarness.with({ selector: `[formcontrolname='${formControlName}']` }))();
    return select.getValueText();
  }

  async getError(): Promise<string> {
    const matError = await this.locatorFor(MatErrorHarness)();
    return matError.getText();
  }

  async isSaveButtonDisabled(): Promise<boolean> {
    const saveBtn = await this.locatorFor(MatButtonHarness.with({ selector: '[data-testId="save"]' }))();
    return saveBtn.isDisabled();
  }

  async save(): Promise<void> {
    const saveBtn = await this.locatorFor(MatButtonHarness.with({ selector: '[data-testId="save"]' }))();
    return saveBtn.click();
  }

  async reset(): Promise<void> {
    const saveBtn = await this.locatorFor(MatButtonHarness.with({ selector: '[data-testId="discard"]' }))();
    return saveBtn.click();
  }

  async isResetButtonDisabled(): Promise<boolean> {
    const resetBtn = await this.locatorFor(MatButtonHarness.with({ selector: '[data-testId="discard"]' }))();
    return resetBtn.isDisabled();
  }

  async computeHeadersTableCells(): Promise<{ name: string; value: string }[]> {
    const table = await this.locatorFor(MatTableHarness)();
    const rows = await table.getRows();
    return await parallel(() =>
      rows.map(async (row, index) => {
        const nameInput = await this.locatorFor(MatInputHarness.with({ selector: `#header-name-${index}` }))();
        const valueInput = await this.locatorFor(MatInputHarness.with({ selector: `#header-value-${index}` }))();
        const name = await nameInput.getValue();
        const value = await valueInput.getValue();
        return { name, value };
      }),
    );
  }

  async toggle(selector: string) {
    const matSlideToggle = await this.locatorFor(MatSlideToggleHarness.with({ selector }))();
    return matSlideToggle.toggle();
  }
}
