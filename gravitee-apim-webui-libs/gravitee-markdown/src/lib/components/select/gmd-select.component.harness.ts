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

export class GmdSelectComponentHarness extends ComponentHarness {
  static readonly hostSelector = 'gmd-select';

  async getValue(): Promise<string> {
    const select = await this.getSelect();
    return await select.getProperty('value');
  }

  async setValue(value: string): Promise<void> {
    await this.selectOptionByValue(value);
  }

  async getOptionValues(): Promise<string[]> {
    const options = await this.getOptions();
    const values: string[] = [];
    for (const option of options) {
      const value = await option.getAttribute('value');
      if (value) {
        values.push(value);
      }
    }
    return values;
  }

  async getOptionLabels(): Promise<string[]> {
    const options = await this.getOptions();
    const texts: string[] = [];
    for (const option of options) {
      texts.push(await option.text());
    }
    return texts;
  }

  async selectOptionByValue(value: string): Promise<void> {
    const select = await this.getSelect();
    const options = await this.getOptions();
    let optionIndex = -1;
    for (let i = 0; i < options.length; i++) {
      const optionValue = await options[i].getAttribute('value');
      if (optionValue === value) {
        optionIndex = i;
        break;
      }
    }
    if (optionIndex === -1) {
      throw new Error(`Select option with value "${value}" not found`);
    }
    await select.selectOptions(optionIndex);
    await select.dispatchEvent('change');
  }

  async getOptionCount(): Promise<number> {
    const options = await this.getOptions();
    return options.length;
  }

  async isDisabled(): Promise<boolean> {
    const select = await this.getSelect();
    return await select.getProperty('disabled');
  }

  async isRequired(): Promise<boolean> {
    const select = await this.getSelect();
    return await select.getProperty('required');
  }

  async getLabel(): Promise<string | null> {
    try {
      const label = await this.locatorFor('.gmd-select__label')();
      return await label.text();
    } catch {
      return null;
    }
  }

  async hasRequiredIndicator(): Promise<boolean> {
    try {
      await this.locatorFor('.gmd-select__required')();
      return true;
    } catch {
      return false;
    }
  }

  async getErrorMessages(): Promise<string[]> {
    try {
      const errorElements = await this.locatorForAll('.gmd-select__error')();
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
    const select = await this.getSelect();
    await select.blur();
  }

  async focus(): Promise<void> {
    const select = await this.getSelect();
    await select.focus();
  }

  async getAriaRequired(): Promise<string | null> {
    const select = await this.getSelect();
    return await select.getAttribute('aria-required');
  }

  async getAriaInvalid(): Promise<string | null> {
    const select = await this.getSelect();
    return await select.getAttribute('aria-invalid');
  }

  async getAriaDescribedby(): Promise<string | null> {
    const select = await this.getSelect();
    return await select.getAttribute('aria-describedby');
  }

  private async getSelect(): Promise<TestElement> {
    return this.locatorFor('select')();
  }

  private async getOptions(): Promise<TestElement[]> {
    return this.locatorForAll('option')();
  }
}
