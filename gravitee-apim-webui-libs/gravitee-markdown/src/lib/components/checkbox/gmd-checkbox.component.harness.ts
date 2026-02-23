/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { ComponentHarness, TestElement } from '@angular/cdk/testing';

export class GmdCheckboxComponentHarness extends ComponentHarness {
  static hostSelector = 'gmd-checkbox';

  async isChecked(): Promise<boolean> {
    const checkbox = await this.getCheckbox();
    return (await checkbox.getProperty('checked')) as boolean;
  }

  async setChecked(checked: boolean): Promise<void> {
    const checkbox = await this.getCheckbox();
    const isCurrentlyChecked = await this.isChecked();
    if (isCurrentlyChecked !== checked) {
      await checkbox.click();
    }
  }

  async click(): Promise<void> {
    const checkbox = await this.getCheckbox();
    await checkbox.click();
  }

  async isDisabled(): Promise<boolean> {
    const checkbox = await this.getCheckbox();
    return (await checkbox.getProperty('disabled')) as boolean;
  }

  async isRequired(): Promise<boolean> {
    const checkbox = await this.getCheckbox();
    return (await checkbox.getProperty('required')) as boolean;
  }

  async isReadonly(): Promise<boolean> {
    const checkbox = await this.getCheckbox();
    return (await checkbox.getProperty('readOnly')) as boolean;
  }

  async getLabel(): Promise<string | null> {
    try {
      const label = await this.locatorFor('.gmd-checkbox__label-text')();
      return await label.text();
    } catch {
      return null;
    }
  }

  async hasRequiredIndicator(): Promise<boolean> {
    try {
      await this.locatorFor('.gmd-checkbox__required')();
      return true;
    } catch {
      return false;
    }
  }

  async getErrorMessages(): Promise<string[]> {
    try {
      const errorElements = await this.locatorForAll('.gmd-checkbox__error')();
      const messages: string[] = [];
      for (const error of errorElements) {
        messages.push(await error.text());
      }
      return messages;
    } catch {
      return [];
    }
  }

  async hasErrors(): Promise<boolean> {
    const errors = await this.getErrorMessages();
    return errors.length > 0;
  }

  async blur(): Promise<void> {
    const checkbox = await this.getCheckbox();
    await checkbox.blur();
  }

  async getAriaRequired(): Promise<string | null> {
    const checkbox = await this.getCheckbox();
    return await checkbox.getAttribute('aria-required');
  }

  async getAriaInvalid(): Promise<string | null> {
    const checkbox = await this.getCheckbox();
    return await checkbox.getAttribute('aria-invalid');
  }

  async getAriaDescribedby(): Promise<string | null> {
    const checkbox = await this.getCheckbox();
    return await checkbox.getAttribute('aria-describedby');
  }

  private async getCheckbox(): Promise<TestElement> {
    return this.locatorFor('input[type="checkbox"]')();
  }
}
