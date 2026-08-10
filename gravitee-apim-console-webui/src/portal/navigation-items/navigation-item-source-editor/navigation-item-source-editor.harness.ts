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
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';

export class NavigationItemSourceEditorHarness extends ComponentHarness {
  static readonly hostSelector = 'navigation-item-source-editor';

  private readonly locateTypeSelect = this.locatorFor(MatSelectHarness.with({ selector: '[data-testid="source-type-select"]' }));
  private readonly locateAutoFetchToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[data-testid="auto-fetch-toggle"]' }));
  private readonly locateCronInput = this.locatorForOptional(MatInputHarness.with({ selector: '[data-testid="fetch-cron-input"]' }));
  private readonly locateSaveButton = this.locatorForOptional(MatButtonHarness.with({ selector: '[data-testid="save-source-button"]' }));
  private readonly locateRemoveButton = this.locatorForOptional(
    MatButtonHarness.with({ selector: '[data-testid="remove-source-button"]' }),
  );
  private readonly locateErrorBanner = this.locatorForOptional('[data-testid="source-fetch-error-banner"]');
  private readonly locateLastFetchedAt = this.locatorForOptional('[data-testid="source-last-fetched-at"]');
  private readonly locateSchemaForm = this.locatorForOptional('gio-form-json-schema');
  private readonly locateSchemaFormInputs = this.locatorForAll(MatInputHarness.with({ ancestor: 'gio-form-json-schema' }));

  async selectType(fetcherName: string): Promise<void> {
    const select = await this.locateTypeSelect();
    await select.open();
    await select.clickOptions({ text: fetcherName });
  }

  async getSelectedType(): Promise<string> {
    return (await this.locateTypeSelect()).getValueText();
  }

  async isTypeSelectDisabled(): Promise<boolean> {
    return (await this.locateTypeSelect()).isDisabled();
  }

  async hasSchemaForm(): Promise<boolean> {
    return !!(await this.locateSchemaForm());
  }

  async getSchemaFormInputValues(): Promise<string[]> {
    const inputs = await this.locateSchemaFormInputs();
    return Promise.all(inputs.map(input => input.getValue()));
  }

  async setSchemaFormInputValue(value: string, index = 0): Promise<void> {
    const inputs = await this.locateSchemaFormInputs();
    await inputs[index]?.setValue(value);
  }

  async toggleAutoFetch(): Promise<void> {
    return (await this.locateAutoFetchToggle()).toggle();
  }

  async isAutoFetchChecked(): Promise<boolean> {
    return (await this.locateAutoFetchToggle()).isChecked();
  }

  async setCron(cron: string): Promise<void> {
    const input = await this.locateCronInput();
    await input?.setValue(cron);
  }

  async getCron(): Promise<string | null> {
    const input = await this.locateCronInput();
    return input ? input.getValue() : null;
  }

  async hasCronInput(): Promise<boolean> {
    return !!(await this.locateCronInput());
  }

  async save(): Promise<void> {
    return (await this.locateSaveButton())?.click();
  }

  async isSaveDisabled(): Promise<boolean> {
    const button = await this.locateSaveButton();
    return button ? button.isDisabled() : true;
  }

  async hasSaveButton(): Promise<boolean> {
    return !!(await this.locateSaveButton());
  }

  async remove(): Promise<void> {
    return (await this.locateRemoveButton())?.click();
  }

  async hasRemoveButton(): Promise<boolean> {
    return !!(await this.locateRemoveButton());
  }

  async getLastFetchErrorText(): Promise<string | null> {
    const banner = await this.locateErrorBanner();
    return banner ? banner.text() : null;
  }

  async getLastFetchedAtText(): Promise<string | null> {
    const info = await this.locateLastFetchedAt();
    return info ? info.text() : null;
  }
}
