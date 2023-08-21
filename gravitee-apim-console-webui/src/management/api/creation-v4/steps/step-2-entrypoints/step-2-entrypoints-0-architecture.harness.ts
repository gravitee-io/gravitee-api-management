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

import { GioConnectorRadioListHarness } from '../../../../../shared/components/gio-connector-list-option/gio-connector-radio-list.harness';

export class Step2Entrypoints0ArchitectureHarness extends ComponentHarness {
  static hostSelector = 'step-2-entrypoints-0-architecture';

  private readonly selectionList = this.locatorFor(GioConnectorRadioListHarness);

  protected getButtonByText = (text: string) =>
    this.locatorFor(
      MatButtonHarness.with({
        text: text,
      }),
    )();

  async getArchitecture(): Promise<GioConnectorRadioListHarness> {
    return this.selectionList();
  }

  async clickValidate(): Promise<void> {
    return this.getButtonByText('Select my API architecture').then((button) => button.click());
  }

  async clickPrevious(): Promise<void> {
    return this.getButtonByText('Previous').then((button) => button.click());
  }

  async fillAndValidate(architecture: string): Promise<void> {
    await this.getArchitecture().then((f) => f.selectOptionById(architecture));
    return this.clickValidate();
  }
}
