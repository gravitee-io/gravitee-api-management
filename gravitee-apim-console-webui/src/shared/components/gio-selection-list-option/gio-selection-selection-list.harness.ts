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
import { ListOptionHarnessFilters, MatSelectionListHarness } from '@angular/material/list/testing';

export class GioSelectionSelectionListHarness extends MatSelectionListHarness {
  static override hostSelector = '.gio-selection-list';

  async selectOptionsByIds(ids: string[]): Promise<void> {
    const filters: ListOptionHarnessFilters[] = ids.map((id) => ({ selector: `[ng-reflect-value=${id}]` }));
    await this.selectItems(...filters);
  }

  async deselectOptionByValue(value: string): Promise<void> {
    return this.deselectItems({ selector: `[ng-reflect-value=${value}]` });
  }

  async getListValues(filters?: { selected?: boolean }): Promise<string[]> {
    const options = (await this.getItems(filters?.selected !== undefined ? { selected: filters.selected } : {})).map(
      async (option) => await (await option.host()).getAttribute('ng-reflect-value'),
    );

    return Promise.all(options);
  }
}
