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
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatListOptionHarness, MatSelectionListHarness } from '@angular/material/list/testing';
import { ComponentHarness } from '@angular/cdk/testing';

export class ApiCreationV4FormSelectionListHarness extends ComponentHarness {
  static hostSelector = 'form';

  protected getList = this.locatorFor(MatSelectionListHarness.with({ selector: '.gio-selection-list' }));

  protected getButtonByText = (text: string) =>
    this.locatorFor(
      MatButtonHarness.with({
        text: text,
      }),
    )();

  protected getListOptionByValue = (value: string) =>
    this.locatorFor(MatListOptionHarness.with({ selector: `[ng-reflect-value=${value}]` }))();

  async clickButtonByText(text: string): Promise<void> {
    return this.getButtonByText(text).then((elt) => elt.click());
  }

  async fillStep(ids: string[]): Promise<void> {
    await Promise.all(ids.map((id) => this.markOptionSelectedByValue(id)));
  }

  async markOptionSelectedByValue(value: string): Promise<void> {
    const listOption = await this.getListOptionByValue(value);
    await listOption.select();
  }

  async deselectOptionByValue(value: string): Promise<void> {
    const listOption = await this.getListOptionByValue(value);
    await listOption.deselect();
  }

  async getListOptions(filters?: { selected?: boolean }): Promise<string[]> {
    const list = await this.getList();

    const options = (await list.getItems(filters?.selected ? { selected: true } : {})).map(
      async (option) => await (await option.host()).getAttribute('ng-reflect-value'),
    );

    return Promise.all(options);
  }
}
