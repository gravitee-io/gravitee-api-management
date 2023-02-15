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

import { AsyncFactoryFn, BaseHarnessFilters, HarnessPredicate } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';

import { GioFormListenersContextPathHarness } from '../gio-form-listeners-context-path/gio-form-listeners-context-path.harness';
import { HttpListenerPath } from '../../../entities/api-v4';

export class GioFormListenersVirtualHostHarness extends GioFormListenersContextPathHarness {
  public static hostSelector = 'gio-form-listeners-virtual-host';

  /**
   * Gets a `HarnessPredicate` that can be used to search for a `GioFormListenersVirtualHostHarness` that meets
   * certain criteria.
   *
   * @param options Options for filtering which input instances are considered a match.
   * @return a `HarnessPredicate` configured with the given options.
   */
  public static with(options: BaseHarnessFilters = {}): HarnessPredicate<GioFormListenersVirtualHostHarness> {
    return new HarnessPredicate(GioFormListenersVirtualHostHarness, options);
  }

  public async getListenerRows(): Promise<
    {
      hostInput: MatInputHarness;
      pathInput: MatInputHarness;
      overrideAccessInput: MatSlideToggleHarness;
      removeButton: MatButtonHarness | null;
    }[]
  > {
    const rows = await this.getListenerRowsElement();

    return Promise.all(
      rows.map(async (_, rowIndex) => ({
        hostInput: await this.getListenerRowInputHost(rowIndex)(),
        pathInput: await this.getListenerRowInputPath(rowIndex)(),
        overrideAccessInput: await this.getListenerRowInputOverrideAccess(rowIndex)(),
        removeButton: await this.getListenerRowRemoveButton(rowIndex)(),
      })),
    );
  }

  private getListenerRowInputHost = (rowIndex: number): AsyncFactoryFn<MatInputHarness> =>
    this.locatorFor(MatInputHarness.with({ ancestor: `[ng-reflect-name="${rowIndex}"]`, selector: '[formControlName=_hostSubDomain]' }));

  private getListenerRowInputOverrideAccess = (rowIndex: number): AsyncFactoryFn<MatSlideToggleHarness> =>
    this.locatorFor(
      MatSlideToggleHarness.with({ ancestor: `[ng-reflect-name="${rowIndex}"]`, selector: '[formControlName=overrideAccess]' }),
    );

  public async getLastListenerRow(): Promise<{
    hostInput: MatInputHarness;
    pathInput: MatInputHarness;
    overrideAccessInput: MatSlideToggleHarness;
  }> {
    const rows = await this.getListenerRowsElement();

    return {
      hostInput: await this.getListenerRowInputHost(rows.length - 1)(),
      pathInput: await this.getListenerRowInputPath(rows.length - 1)(),
      overrideAccessInput: await this.getListenerRowInputOverrideAccess(rows.length - 1)(),
    };
  }

  public async getLastListenerRowValue(): Promise<{ host: string; path: string; overrideAccess: boolean }> {
    const { hostInput, pathInput, overrideAccessInput } = await this.getLastListenerRow();
    return {
      host: await hostInput.getValue(),
      path: await pathInput.getValue(),
      overrideAccess: await overrideAccessInput.isChecked(),
    };
  }

  public async addListener({ host, path, overrideAccess }: HttpListenerPath): Promise<void> {
    const { hostInput, pathInput, overrideAccessInput } = await this.getLastListenerRow();

    await hostInput.setValue(host);
    await pathInput.setValue(path);
    if (overrideAccess) {
      await overrideAccessInput.check();
    } else {
      await overrideAccessInput.uncheck();
    }
  }
}
