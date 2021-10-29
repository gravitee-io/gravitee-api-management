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
import { MatInputHarness } from '@angular/material/input/testing';
import { MatPaginatorHarness } from '@angular/material/paginator/testing';

/** A set of criteria that can be used to filter a list of `GioSaveBarHarness` instances. */
export type GioTableWrapperHarnessFilters = BaseHarnessFilters;

export class GioTableWrapperHarness extends ComponentHarness {
  static hostSelector = 'gio-table-wrapper';

  /**
   * Gets a `HarnessPredicate` that can be used to search for a `GioSaveBarHarness` that meets
   * certain criteria.
   *
   * @param options Options for filtering which input instances are considered a match.
   * @return a `HarnessPredicate` configured with the given options.
   */
  static with(options: GioTableWrapperHarnessFilters = {}): HarnessPredicate<GioTableWrapperHarness> {
    return new HarnessPredicate(GioTableWrapperHarness, options);
  }

  private readonly inputSearchSelector = '.gio-table-wrapper__header-bar__search-field ';
  private readonly topPaginatorSelector = '.gio-table-wrapper__header-bar__paginator';
  private readonly bottomPaginatorSelector = '.gio-table-wrapper__footer-bar__paginator';

  protected getInputSearch = this.locatorFor(MatInputHarness.with({ ancestor: this.inputSearchSelector }));
  protected getTopPaginatorButton = this.locatorFor(MatPaginatorHarness.with({ selector: this.topPaginatorSelector }));
  protected getBottomPaginatorButton = this.locatorFor(MatPaginatorHarness.with({ selector: this.bottomPaginatorSelector }));

  async getSearchValue(): Promise<string> {
    const inputSearch = await this.getInputSearch();
    return inputSearch.getValue();
  }

  async setSearchValue(value: string): Promise<void> {
    const inputSearch = await this.getInputSearch();
    await inputSearch.setValue(value);
  }

  getPaginator(zone: 'header' | 'footer' = 'header'): Promise<MatPaginatorHarness | null> {
    if (zone === 'header') {
      return this.getTopPaginatorButton();
    }
    if (zone === 'footer') {
      return this.getBottomPaginatorButton();
    }
  }
}
