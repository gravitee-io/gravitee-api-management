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

import { BaseHarnessFilters, HarnessPredicate } from '@angular/cdk/testing';
import { MatFormFieldControlHarness } from '@angular/material/form-field/testing';
import { MatButtonToggleGroupHarness } from '@angular/material/button-toggle/testing';

export class GioDiffHarness extends MatFormFieldControlHarness {
  static hostSelector = 'gio-diff';

  /**
   * Gets a `HarnessPredicate` that can be used to search for a `GioDiffHarness` that meets
   * certain criteria.
   *
   * @param options Options for filtering which input instances are considered a match.
   * @return a `HarnessPredicate` configured with the given options.
   */
  static with(options: BaseHarnessFilters = {}): HarnessPredicate<GioDiffHarness> {
    return new HarnessPredicate(GioDiffHarness, options);
  }

  protected getContentGvCodeElements = this.locatorForAll('.gio-diff__content__row__gv-code');
  protected getContentDiff2htmlElement = this.locatorForOptional('.gio-diff__content .d2h-wrapper');
  protected getD2hSideBySideDisplayElement = this.locatorForOptional('.gio-diff__content .d2h-wrapper .d2h-file-side-diff');

  protected getButtonToggleGroupHarness = this.locatorFor(
    MatButtonToggleGroupHarness.with({ selector: '.gio-diff__header__output-format' }),
  );

  async getOutputFormat(): Promise<'raw' | 'side-by-side' | 'line-by-line'> {
    if ((await this.getContentDiff2htmlElement()) && !(await this.getD2hSideBySideDisplayElement())) {
      return 'line-by-line';
    }

    if ((await this.getContentDiff2htmlElement()) && (await this.getD2hSideBySideDisplayElement())) {
      return 'side-by-side';
    }

    if (!(await this.getContentDiff2htmlElement()) && (await this.getContentGvCodeElements())) {
      return 'raw';
    }

    throw new Error('Indeterminate output format');
  }

  async selectOutputFormat(format: 'raw' | 'side-by-side' | 'line-by-line'): Promise<void> {
    const formatValueNameMap = {
      raw: 'Raw',
      'side-by-side': 'Diff Side By Side',
      'line-by-line': 'Diff Line By Line',
    };

    const buttonToggleHarness = await (
      await (await this.getButtonToggleGroupHarness()).getToggles({ text: formatValueNameMap[format] })
    )[0];
    await buttonToggleHarness.check();
  }

  /**
   * If no diff to display only one gv-code are displayed on only 'raw' output format
   */
  async hasNoDiffToDisplay(): Promise<boolean> {
    return (await this.getOutputFormat()) === 'raw' && (await this.getContentGvCodeElements()).length === 1;
  }
}
