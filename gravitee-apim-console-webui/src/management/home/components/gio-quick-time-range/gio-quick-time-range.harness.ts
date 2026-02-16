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

import { BaseHarnessFilters, ComponentHarness, HarnessPredicate } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatOptionHarness } from '@angular/material/core/testing';

export class GioQuickTimeRangeHarness extends ComponentHarness {
  public static hostSelector = 'gio-quick-time-range';

  /**
   * Gets a `HarnessPredicate` that can be used to search for a `GioQuickTimeRangeHarness` that meets
   * certain criteria.
   *
   * @param options Options for filtering which input instances are considered a match.
   * @return a `HarnessPredicate` configured with the given options.
   */
  public static with(options: BaseHarnessFilters = {}): HarnessPredicate<GioQuickTimeRangeHarness> {
    return new HarnessPredicate(GioQuickTimeRangeHarness, options);
  }

  protected getSelectTimeRangeElement = this.locatorFor(MatSelectHarness);
  protected getRefreshButton = this.locatorFor(MatButtonHarness.with({ selector: '.time-frame__refresh-btn' }));

  // eslint:disable-next-line:no-unused-expression
  public async getAllTimeRangeOptions(): Promise<MatOptionHarness[]> {
    return this.getSelectTimeRangeElement().then(async elt => {
      await elt.open();
      return elt.getOptions();
    });
  }

  // eslint:disable-next-line:no-unused-expression
  public async selectTimeRangeByText(text: string): Promise<void> {
    return this.getSelectTimeRangeElement().then(async elt => {
      await elt.open();
      return elt.clickOptions({ text });
    });
  }

  // eslint:disable-next-line:no-unused-expression
  public async clickOnRefreshButton(): Promise<void> {
    return this.getRefreshButton().then(btn => btn.click());
  }
}
