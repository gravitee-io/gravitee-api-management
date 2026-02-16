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
import { MatSelectionListHarness } from '@angular/material/list/testing';
import { MatIconHarness } from '@angular/material/icon/testing';

export class GioSelectionSelectionListHarness extends MatSelectionListHarness {
  static override hostSelector = '.gio-selection-list';

  async selectOptionsByIds(ids: string[]): Promise<void> {
    // Select list options by their mat icon name value
    const options = await this.getItems();
    for (const option of options) {
      const icon = await option.getHarness(MatIconHarness);
      const iconName = await icon.getName();
      if (ids.includes(iconName)) {
        await option.select();
      }
    }
  }

  async deselectOptionByValue(value: string): Promise<void> {
    const options = await this.getItems();
    for (const option of options) {
      const icon = await option.getHarness(MatIconHarness);
      const iconName = await icon.getName();
      if (value === iconName) {
        await option.deselect();
      }
    }
  }

  async getListValues(filters?: { selected?: boolean }): Promise<string[]> {
    const items = await this.getItems(filters?.selected !== undefined ? { selected: filters.selected } : {});

    return Promise.all(
      items.map(async item => {
        const icon = await item.getHarness(MatIconHarness);
        return await icon.getName();
      }),
    );
  }
}
