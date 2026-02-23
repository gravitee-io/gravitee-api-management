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

export class GmdRadioComponentHarness extends ComponentHarness {
  static readonly hostSelector = 'gmd-radio';

  async getSelectedValue(): Promise<string | null> {
    const inputs = await this.getRadioInputs();
    for (const input of inputs) {
      const checked = await input.getProperty('checked');
      if (checked) {
        return await input.getProperty('value');
      }
    }
    return null;
  }

  async selectOption(value: string): Promise<void> {
    const input = await this.getRadioInputByValue(value);
    if (input) {
      await input.click();
    } else {
      throw new Error(`Radio option with value "${value}" not found`);
    }
  }

  async getOptionCount(): Promise<number> {
    const inputs = await this.getRadioInputs();
    return inputs.length;
  }

  async getOptionLabels(): Promise<string[]> {
    const labels = await this.locatorForAll('.gmd-radio__option-label')();
    const labelTexts: string[] = [];
    for (const label of labels) {
      labelTexts.push(await label.text());
    }
    return labelTexts;
  }

  async isDisabled(): Promise<boolean> {
    const inputs = await this.getRadioInputs();
    if (inputs.length === 0) {
      return false;
    }
    return await inputs[0].getProperty('disabled');
  }

  async isRequired(): Promise<boolean> {
    const inputs = await this.getRadioInputs();
    if (inputs.length === 0) {
      return false;
    }
    return await inputs[0].getProperty('required');
  }

  async getLabel(): Promise<string | null> {
    try {
      const label = await this.locatorFor('.gmd-radio__group-label')();
      return await label.text();
    } catch {
      return null;
    }
  }

  async hasRequiredIndicator(): Promise<boolean> {
    try {
      await this.locatorFor('.gmd-radio__required')();
      return true;
    } catch {
      return false;
    }
  }

  async getErrorMessages(): Promise<string[]> {
    try {
      const errorElements = await this.locatorForAll('.gmd-radio__error')();
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
    const inputs = await this.getRadioInputs();
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

  private async getRadioInputs(): Promise<TestElement[]> {
    return this.locatorForAll('input[type="radio"]')();
  }

  private async getRadioInputByValue(value: string): Promise<TestElement | null> {
    try {
      const inputs = await this.getRadioInputs();
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
