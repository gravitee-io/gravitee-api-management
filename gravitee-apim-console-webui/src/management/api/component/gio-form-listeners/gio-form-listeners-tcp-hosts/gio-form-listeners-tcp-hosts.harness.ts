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

import { AsyncFactoryFn, BaseHarnessFilters, ComponentHarness, HarnessPredicate } from '@angular/cdk/testing';
import { MatLegacyButtonHarness as MatButtonHarness } from '@angular/material/legacy-button/testing';
import { MatLegacyInputHarness as MatInputHarness } from '@angular/material/legacy-input/testing';

import { TcpHost } from '../../../../../entities/management-api-v2/api/v4/tcpHost';

export class GioFormListenersTcpHostsHarness extends ComponentHarness {
  public static hostSelector = 'gio-form-listeners-tcp-hosts';

  /**
   * Gets a `HarnessPredicate` that can be used to search for a `GioFormListenersContextPathHarness` that meets
   * certain criteria.
   *
   * @param options Options for filtering which input instances are considered a match.
   * @return a `HarnessPredicate` configured with the given options.
   */
  public static with(options: BaseHarnessFilters = {}): HarnessPredicate<GioFormListenersTcpHostsHarness> {
    return new HarnessPredicate(GioFormListenersTcpHostsHarness, options);
  }

  protected getListenerRowsElement = this.locatorForAll('tr.gio-form-listeners__table__row');
  protected addButton = this.locatorFor(MatButtonHarness.with({ text: 'Add host' }));

  protected getListenerRowInputHost = (rowIndex: number): AsyncFactoryFn<MatInputHarness> =>
    this.locatorFor(MatInputHarness.with({ ancestor: `[ng-reflect-name="${rowIndex}"]`, selector: '[formControlName=host]' }));

  protected getListenerRowRemoveButton = (rowIndex: number): AsyncFactoryFn<MatButtonHarness | null> =>
    this.locatorForOptional(MatButtonHarness.with({ ancestor: `tr[ng-reflect-name="${rowIndex}"]`, selector: '[aria-label="Delete"]' }));

  public async getListenerRows(): Promise<
    {
      hostInput: MatInputHarness;
      removeButton: MatButtonHarness | null;
    }[]
  > {
    const rows = await this.getListenerRowsElement();

    return Promise.all(
      rows.map(async (_, rowIndex) => ({
        hostInput: await this.getListenerRowInputHost(rowIndex)(),
        removeButton: await this.getListenerRowRemoveButton(rowIndex)(),
      })),
    );
  }

  public async getLastListenerRow(): Promise<{ hostInput: MatInputHarness }> {
    const rows = await this.getListenerRowsElement();

    return {
      hostInput: await this.getListenerRowInputHost(rows.length - 1)(),
    };
  }

  public async addListenerRow(): Promise<void> {
    const addButton = await this.addButton();

    await addButton.click();
  }

  public async addListener({ host }: TcpHost): Promise<void> {
    await this.addListenerRow();

    const { hostInput } = await this.getLastListenerRow();

    await hostInput.setValue(host);
  }

  public async getAddButton(): Promise<MatButtonHarness> {
    return this.addButton();
  }
}
