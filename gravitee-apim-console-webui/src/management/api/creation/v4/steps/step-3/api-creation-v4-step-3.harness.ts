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
import { MatListOptionHarness, MatSelectionListHarness } from '@angular/material/list/testing';

export class ApiCreationV4Step3Harness extends ComponentHarness {
  static hostSelector = 'api-creation-v4-step-3';

  protected getPreviousButton = this.locatorFor(
    MatButtonHarness.with({
      selector: '#previous',
    }),
  );

  protected getValidateButton = this.locatorFor(
    MatButtonHarness.with({
      selector: '#validate',
    }),
  );

  protected getListOptionById = (id: string) => this.locatorFor(MatListOptionHarness.with({ selector: `#${id}` }))();

  async clickPrevious(): Promise<void> {
    return this.getPreviousButton().then((elt) => elt.click());
  }

  async clickValidate() {
    return this.getValidateButton().then((elt) => elt.click());
  }

  async fillStep(endpointIds: string[]): Promise<void> {
    await Promise.all(endpointIds.map((id) => this.markEndpointSelectedById(id)));
  }

  async markEndpointSelectedById(id: string): Promise<void> {
    const listOption = await this.getListOptionById(id);
    await listOption.select();
  }

  async deselectEndpointById(id: string): Promise<void> {
    const listOption = await this.getListOptionById(id);
    await listOption.deselect();
  }

  async getEndpointsList(filters?: { selected?: boolean }): Promise<string[]> {
    const list = await this.locatorFor(MatSelectionListHarness.with({ selector: '.gio-selection-list' }))();

    const options = (await list.getItems(filters?.selected ? { selected: true } : {})).map(
      async (option) => await (await option.host()).getAttribute('id'),
    );

    return Promise.all(options);
  }
}
