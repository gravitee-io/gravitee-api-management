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

export class GmdCheckboxGroupComponentHarness extends ComponentHarness {
  static readonly hostSelector = 'gmd-checkbox-group';

  async getOptions(): Promise<string[]> {
    const labels = await this.locatorForAll('.gmd-checkbox-group__option-label')();
    const texts: string[] = [];
    for (const label of labels) {
      texts.push(await label.text());
    }
    return texts;
  }

  async isChecked(option: string): Promise<boolean> {
    const input = await this.getCheckboxByValue(option);
    if (!input) return false;
    return await input.getProperty('checked');
  }

  async toggle(option: string): Promise<void> {
    const input = await this.getCheckboxByValue(option);
    if (!input) {
      throw new Error(`Checkbox option with value "${option}" not found`);
    }

    await input.click();
  }

  async getSelectedValues(): Promise<string[]> {
    const inputs = await this.getCheckboxInputs();
    const selected: string[] = [];
    for (const input of inputs) {
      const checked = await input.getProperty('checked');
      if (checked) {
        selected.push(await input.getProperty('value'));
      }
    }
    return selected;
  }

  async getLabel(): Promise<string | null> {
    try {
      const label = await this.locatorFor('.gmd-checkbox-group__group-label')();
      return await label.text();
    } catch {
      return null;
    }
  }

  async hasError(): Promise<boolean> {
    const errors = await this.getErrorMessages();
    return errors.length > 0;
  }

  async getErrorMessages(): Promise<string[]> {
    try {
      const errorElements = await this.locatorForAll('.gmd-checkbox-group__error')();
      const messages: string[] = [];
      for (const error of errorElements) {
        messages.push(await error.text());
      }
      return messages;
    } catch {
      return [];
    }
  }

  async isDisabled(): Promise<boolean> {
    const inputs = await this.getCheckboxInputs();
    if (inputs.length === 0) return false;
    return await inputs[0].getProperty('disabled');
  }

  async hasRequiredIndicator(): Promise<boolean> {
    try {
      await this.locatorFor('.gmd-checkbox-group__required')();
      return true;
    } catch {
      return false;
    }
  }

  async blur(): Promise<void> {
    const inputs = await this.getCheckboxInputs();
    if (inputs.length > 0) {
      await inputs[0].blur();
    }
  }

  async getAriaRequired(): Promise<string | null> {
    try {
      const fieldset = await this.locatorFor('fieldset')();
      return await fieldset.getAttribute('aria-required');
    } catch {
      return null;
    }
  }

  async getAriaInvalid(): Promise<string | null> {
    try {
      const fieldset = await this.locatorFor('fieldset')();
      return await fieldset.getAttribute('aria-invalid');
    } catch {
      return null;
    }
  }

  async getAriaDescribedby(): Promise<string | null> {
    try {
      const fieldset = await this.locatorFor('fieldset')();
      return await fieldset.getAttribute('aria-describedby');
    } catch {
      return null;
    }
  }

  private async getCheckboxInputs(): Promise<TestElement[]> {
    return this.locatorForAll('input[type="checkbox"]')();
  }

  private async getCheckboxByValue(value: string): Promise<TestElement | null> {
    try {
      const inputs = await this.getCheckboxInputs();
      for (const input of inputs) {
        const inputValue = await input.getProperty('value');
        if (inputValue === value) {
          return input;
        }
      }
      return null;
    } catch {
      return null;
    }
  }
}
