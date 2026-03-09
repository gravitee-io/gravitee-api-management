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
import { BaseHarnessFilters, ComponentHarness, HarnessPredicate, TestElement } from '@angular/cdk/testing';

export type DivHarnessFilters = BaseHarnessFilters & {
  /** Filters based on the text */
  text?: string | RegExp;
};

export class DivHarness extends ComponentHarness {
  public static hostSelector = 'div';

  public childLocatorFor = this.locatorFor;

  public childLocatorForOptional = this.locatorForOptional;

  public childLocatorForAll = this.locatorForAll;

  /**
   * Get Harness with the given filter.
   *
   * @param options Options for filtering which input instances are considered a match.
   * @return a `HarnessPredicate` configured with the given options.
   */
  public static with(options: DivHarnessFilters = {}): HarnessPredicate<DivHarness> {
    return new HarnessPredicate(DivHarness, options).addOption('text', options.text, (harness, text) =>
      HarnessPredicate.stringMatches(harness.getText(), text),
    );
  }

  public async getText(option?: { childSelector?: string }): Promise<string | null> {
    let element: TestElement | null = await this.host();
    if (option?.childSelector) {
      element = await this.locatorForOptional(option.childSelector)();
    }

    return element ? element.text() : null;
  }
}
