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

import { BaseHarnessFilters, HarnessPredicate, parallel } from '@angular/cdk/testing';
import { MatFormFieldControlHarness } from '@angular/material/form-field/testing';

/** A set of criteria that can be used to filter a list of `GioFormColorInputHarness` instances. */
export interface GioFormColorInputHarnessFilters extends BaseHarnessFilters {
  /** Filters based on the value of the input. */
  value?: string | RegExp;
  /** Filters based on the placeholder text of the input. */
  placeholder?: string | RegExp;
}

export class GioFormColorInputHarness extends MatFormFieldControlHarness {
  static hostSelector = 'gio-form-color-input';

  /**
   * Gets a `HarnessPredicate` that can be used to search for a `GioFormColorInputHarness` that meets
   * certain criteria.
   *
   * @param options Options for filtering which input instances are considered a match.
   * @return a `HarnessPredicate` configured with the given options.
   */
  static with(options: GioFormColorInputHarnessFilters = {}): HarnessPredicate<GioFormColorInputHarness> {
    return new HarnessPredicate(GioFormColorInputHarness, options)
      .addOption('value', options.value, (harness, value) => {
        return HarnessPredicate.stringMatches(harness.getValue(), value);
      })
      .addOption('placeholder', options.placeholder, (harness, placeholder) => {
        return HarnessPredicate.stringMatches(harness.getPlaceholder(), placeholder);
      });
  }

  protected getInputElement = this.locatorFor('input.form-color-input__text-input');

  /** Gets the value of the input. */
  async getValue(): Promise<string> {
    // The "value" property of the native input is never undefined.
    const inputElement = await this.getInputElement();
    return inputElement.getProperty('value');
  }

  /** Gets the placeholder of the input. */
  async getPlaceholder(): Promise<string> {
    const inputElement = await this.getInputElement();
    const [nativePlaceholder, fallback] = await parallel(() => [
      inputElement.getProperty('placeholder'),
      inputElement.getAttribute('data-placeholder'),
    ]);
    return nativePlaceholder || fallback || '';
  }

  /**
   * Sets the value of the input. The value will be set by simulating
   * keypresses that correspond to the given value.
   */
  async setValue(newValue: string): Promise<void> {
    const inputEl = await this.getInputElement();
    await inputEl.clear();
    // We don't want to send keys for the value if the value is an empty
    // string in order to clear the value. Sending keys with an empty string
    // still results in unnecessary focus events.
    if (newValue) {
      await inputEl.sendKeys(newValue);
    }

    // Some input types won't respond to key presses (e.g. `color`) so to be sure that the
    // value is set, we also set the property after the keyboard sequence. Note that we don't
    // want to do it before, because it can cause the value to be entered twice.
    await inputEl.setInputValue(newValue);
  }

  /** Whether the input is disabled. */
  async isDisabled(): Promise<boolean> {
    const inputEl = await this.getInputElement();
    return inputEl.getProperty<boolean>('disabled');
  }
}
