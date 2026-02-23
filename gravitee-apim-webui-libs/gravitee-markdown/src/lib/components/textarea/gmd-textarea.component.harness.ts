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

export class GmdTextareaComponentHarness extends ComponentHarness {
  static readonly hostSelector = 'gmd-textarea';

  async getValue(): Promise<string> {
    const textarea = await this.getTextarea();
    return await textarea.getProperty('value');
  }

  async setValue(value: string): Promise<void> {
    const textarea = await this.getTextarea();
    await textarea.clear();
    await textarea.sendKeys(value);
  }

  async getPlaceholder(): Promise<string | null> {
    const textarea = await this.getTextarea();
    return await textarea.getAttribute('placeholder');
  }

  async getRows(): Promise<number> {
    const textarea = await this.getTextarea();
    const rows = await textarea.getAttribute('rows');
    return rows ? parseInt(rows, 10) : 0;
  }

  async isDisabled(): Promise<boolean> {
    const textarea = await this.getTextarea();
    return await textarea.getProperty('disabled');
  }

  async isReadonly(): Promise<boolean> {
    const textarea = await this.getTextarea();
    return await textarea.getProperty('readOnly');
  }

  async getLabel(): Promise<string | null> {
    try {
      const label = await this.locatorFor('.gmd-textarea__label')();
      return await label.text();
    } catch {
      return null;
    }
  }

  async hasRequiredIndicator(): Promise<boolean> {
    try {
      await this.locatorFor('.gmd-textarea__required')();
      return true;
    } catch {
      return false;
    }
  }

  async getMinLength(): Promise<string | null> {
    const textarea = await this.getTextarea();
    return await textarea.getAttribute('minlength');
  }

  async getMaxLength(): Promise<string | null> {
    const textarea = await this.getTextarea();
    return await textarea.getAttribute('maxlength');
  }

  async getErrorMessages(): Promise<string[]> {
    try {
      const errorElements = await this.locatorForAll('.gmd-textarea__error')();
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
    const textarea = await this.getTextarea();
    await textarea.blur();
  }

  async focus(): Promise<void> {
    const textarea = await this.getTextarea();
    await textarea.focus();
  }

  async getAriaRequired(): Promise<string | null> {
    const textarea = await this.getTextarea();
    return await textarea.getAttribute('aria-required');
  }

  async getAriaInvalid(): Promise<string | null> {
    const textarea = await this.getTextarea();
    return await textarea.getAttribute('aria-invalid');
  }

  async getAriaDescribedby(): Promise<string | null> {
    const textarea = await this.getTextarea();
    return await textarea.getAttribute('aria-describedby');
  }

  private async getTextarea(): Promise<TestElement> {
    return this.locatorFor('textarea')();
  }
}
