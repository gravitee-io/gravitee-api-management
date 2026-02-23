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

export class GmdInputComponentHarness extends ComponentHarness {
  static hostSelector = 'gmd-input';

  async getValue(): Promise<string> {
    const input = await this.getInput();
    return (await input.getProperty('value')) as string;
  }

  async setValue(value: string): Promise<void> {
    const input = await this.getInput();
    await input.clear();
    await input.sendKeys(value);
  }

  async getPlaceholder(): Promise<string | null> {
    const input = await this.getInput();
    return await input.getAttribute('placeholder');
  }

  async isDisabled(): Promise<boolean> {
    const input = await this.getInput();
    return (await input.getProperty('disabled')) as boolean;
  }

  async getLabel(): Promise<string | null> {
    try {
      const label = await this.locatorFor('.gmd-input__label')();
      return await label.text();
    } catch {
      return null;
    }
  }

  async hasRequiredIndicator(): Promise<boolean> {
    try {
      await this.locatorFor('.gmd-input__required')();
      return true;
    } catch {
      return false;
    }
  }

  async getMinLength(): Promise<string | null> {
    const input = await this.getInput();
    return await input.getAttribute('minlength');
  }

  async getMaxLength(): Promise<string | null> {
    const input = await this.getInput();
    return await input.getAttribute('maxlength');
  }

  async getPattern(): Promise<string | null> {
    const input = await this.getInput();
    return await input.getAttribute('pattern');
  }

  async getErrorMessages(): Promise<string[]> {
    try {
      const errorElements = await this.locatorForAll('.gmd-input__error')();
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
    const input = await this.getInput();
    await input.blur();
  }

  async focus(): Promise<void> {
    const input = await this.getInput();
    await input.focus();
  }

  async isReadonly(): Promise<boolean> {
    const input = await this.getInput();
    return (await input.getProperty('readOnly')) as boolean;
  }

  async getAriaRequired(): Promise<string | null> {
    const input = await this.getInput();
    return await input.getAttribute('aria-required');
  }

  async getAriaInvalid(): Promise<string | null> {
    const input = await this.getInput();
    return await input.getAttribute('aria-invalid');
  }

  async getAriaDescribedby(): Promise<string | null> {
    const input = await this.getInput();
    return await input.getAttribute('aria-describedby');
  }

  private async getInput(): Promise<TestElement> {
    return this.locatorFor('input')();
  }
}
