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
import { MatLegacyButtonHarness as MatButtonHarness } from '@angular/material/legacy-button/testing';
import { MatLegacyInputHarness as MatInputHarness } from '@angular/material/legacy-input/testing';
import { MatLegacySlideToggleHarness as MatSlideToggleHarness } from '@angular/material/legacy-slide-toggle/testing';
import { DivHarness, SpanHarness } from '@gravitee/ui-particles-angular/testing';

import { GioFormListenersContextPathHarness } from '../gio-form-listeners-context-path/gio-form-listeners-context-path.harness';
import { PathV4 } from '../../../../../entities/management-api-v2';

export class GioFormListenersVirtualHostHarness extends GioFormListenersContextPathHarness {
  public static override hostSelector = 'gio-form-listeners-virtual-host';

  /**
   * Gets a `HarnessPredicate` that can be used to search for a `GioFormListenersVirtualHostHarness` that meets
   * certain criteria.
   *
   * @param options Options for filtering which input instances are considered a match.
   * @return a `HarnessPredicate` configured with the given options.
   */
  public static override with(options: BaseHarnessFilters = {}): HarnessPredicate<GioFormListenersVirtualHostHarness> {
    return new HarnessPredicate(GioFormListenersVirtualHostHarness, options);
  }

  public override async getListenerRows(): Promise<
    {
      hostDomainSuffix: DivHarness;
      hostSubDomainInput: MatInputHarness;
      pathInput: MatInputHarness;
      overrideAccessInput: MatSlideToggleHarness;
      removeButton: MatButtonHarness | null;
    }[]
  > {
    const rows = await this.getListenerRowsElement();

    return Promise.all(
      rows.map(async (_, rowIndex) => ({
        hostDomainSuffix: await this.getListenerRowInputHostDomain(rowIndex)(),
        hostSubDomainInput: await this.getListenerRowInputHostSubDomain(rowIndex)(),
        pathInput: await this.getListenerRowInputPath(rowIndex)(),
        overrideAccessInput: await this.getListenerRowInputOverrideAccess(rowIndex)(),
        removeButton: await this.getListenerRowRemoveButton(rowIndex)(),
      })),
    );
  }

  private getListenerRowInputHostDomain = (rowIndex: number): AsyncFactoryFn<DivHarness> =>
    this.locatorFor(DivHarness.with({ ancestor: `[ng-reflect-name="${rowIndex}"]`, selector: '.mat-form-field-suffix' }));

  private getListenerRowInputHostSubDomain = (rowIndex: number): AsyncFactoryFn<MatInputHarness> =>
    this.locatorFor(MatInputHarness.with({ ancestor: `[ng-reflect-name="${rowIndex}"]`, selector: '[formControlName=_hostSubDomain]' }));

  private getListenerRowInputOverrideAccess = (rowIndex: number): AsyncFactoryFn<MatSlideToggleHarness> =>
    this.locatorFor(
      MatSlideToggleHarness.with({ ancestor: `[ng-reflect-name="${rowIndex}"]`, selector: '[formControlName=overrideAccess]' }),
    );

  public override async getLastListenerRow(): Promise<{
    hostDomainInput: SpanHarness;
    hostSubDomainInput: MatInputHarness;
    pathInput: MatInputHarness;
    overrideAccessInput: MatSlideToggleHarness;
  }> {
    const rows = await this.getListenerRowsElement();

    return {
      hostDomainInput: await this.getListenerRowInputHostDomain(rows.length - 1)(),
      hostSubDomainInput: await this.getListenerRowInputHostSubDomain(rows.length - 1)(),
      pathInput: await this.getListenerRowInputPath(rows.length - 1)(),
      overrideAccessInput: await this.getListenerRowInputOverrideAccess(rows.length - 1)(),
    };
  }

  public override async addListener({ host, path, overrideAccess }: PathV4): Promise<void> {
    await this.addListenerRow();

    const { hostSubDomainInput, pathInput, overrideAccessInput } = await this.getLastListenerRow();

    await hostSubDomainInput.setValue(host);
    await pathInput.setValue(path);
    if (overrideAccess) {
      await overrideAccessInput.check();
    } else {
      await overrideAccessInput.uncheck();
    }
  }
}
