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
import { MatTableHarness } from '@angular/material/table/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatMenuItemHarness } from '@angular/material/menu/testing';

import { GioTableWrapperHarness } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.harness';

export class SharedPolicyGroupsHarness extends ComponentHarness {
  static readonly hostSelector = 'shared-policy-groups';

  public getAddButton = this.locatorFor(MatButtonHarness.with({ text: /Add Shared Policy Group/ }));
  public getTable = this.locatorFor(MatTableHarness.with({ selector: '[aria-label="Shared policy group"]' }));
  public getTableWrapper = this.locatorFor(GioTableWrapperHarness);

  public async getDeleteButton(index: number) {
    const table = await this.getTable();
    const rows = await table.getRows();

    return await rows[index]
      .getCells({ columnName: 'actions' })
      .then((cells) => cells[0].getHarnessOrNull(MatButtonHarness.with({ selector: '[aria-label="Button to remove"]' })));
  }

  public async clickAddButton(apiType: 'MESSAGE' | 'PROXY') {
    await this.getAddButton().then((button) => button.click());

    const root = this.documentRootLocatorFactory();
    if (apiType === 'MESSAGE') {
      await root
        .locatorFor(MatMenuItemHarness.with({ text: /Proxy API/ }))()
        .then((button) => button.click());
    }
    if (apiType === 'PROXY') {
      await root
        .locatorFor(MatMenuItemHarness.with({ text: /Message API/ }))()
        .then((button) => button.click());
    }
  }
}
