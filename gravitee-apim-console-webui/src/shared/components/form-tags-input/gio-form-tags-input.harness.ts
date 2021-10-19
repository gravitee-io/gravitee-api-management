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

import { BaseHarnessFilters, ComponentHarness, HarnessPredicate, parallel, TestKey } from '@angular/cdk/testing';
import { MatChipListHarness } from '@angular/material/chips/testing';

export type GioFormTagsInputHarnessFilters = BaseHarnessFilters;

export class GioFormTagsInputHarness extends ComponentHarness {
  static hostSelector = 'gio-form-tags-input';

  /**
   * Gets a `HarnessPredicate` that can be used to search for a `GioFormColorInputHarness` that meets
   * certain criteria.
   *
   * @param options Options for filtering which input instances are considered a match.
   * @return a `HarnessPredicate` configured with the given options.
   */
  static with(options: GioFormTagsInputHarnessFilters = {}): HarnessPredicate<GioFormTagsInputHarness> {
    return new HarnessPredicate(GioFormTagsInputHarness, options);
  }

  protected getMatChipListHarness = this.locatorFor(MatChipListHarness);

  async getTags(): Promise<string[]> {
    const matChipList = await this.getMatChipListHarness();

    const chips = await matChipList.getChips();

    return parallel(() => chips.map(async (chip) => await chip.getText()));
  }

  async addTag(tag: string, separatorKey: TestKey | 'blur' = TestKey.ENTER): Promise<void> {
    const matChipList = await this.getMatChipListHarness();

    const chipInput = await matChipList.getInput();
    await chipInput.setValue(tag);
    if (separatorKey === 'blur') {
      await chipInput.blur();
    } else {
      await chipInput.sendSeparatorKey(separatorKey);
    }
  }

  async removeTag(tag: string): Promise<void> {
    const matChipList = await this.getMatChipListHarness();

    const chips = await matChipList.getChips({ text: tag });
    if (chips[0]) {
      await chips[0].remove();
    }
  }
}
