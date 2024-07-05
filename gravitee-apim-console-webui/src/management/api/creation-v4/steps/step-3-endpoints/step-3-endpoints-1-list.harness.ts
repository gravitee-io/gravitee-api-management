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
import { ComponentHarness } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

import { GioSelectionSelectionListHarness } from '../../../../../shared/components/gio-selection-list-option/gio-selection-selection-list.harness';

export class Step3EndpointListHarness extends ComponentHarness {
  static hostSelector = 'step-3-endpoints-1-list';
  private readonly selectionList = this.locatorFor(GioSelectionSelectionListHarness);

  protected getButtonByText = (text: string) =>
    this.locatorFor(
      MatButtonHarness.with({
        text: text,
      }),
    )();

  async getEndpoints(): Promise<GioSelectionSelectionListHarness> {
    return this.selectionList();
  }

  async clickPrevious() {
    return this.getButtonByText('Previous').then((button) => button.click());
  }

  async clickValidate() {
    return this.getButtonByText('Select my endpoints').then((button) => button.click());
  }

  async fillAndValidate(endpoints: string[]): Promise<void> {
    await this.selectionList().then((f) => f.selectOptionsByIds(endpoints));

    return this.clickValidate();
  }
}
