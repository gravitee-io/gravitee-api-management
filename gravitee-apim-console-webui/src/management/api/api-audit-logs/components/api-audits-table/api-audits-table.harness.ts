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
import { ComponentHarness, parallel } from '@angular/cdk/testing';
import { MatTableHarness } from '@angular/material/table/testing';

import { GioTableWrapperHarness } from '../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.harness';

export class ApiAuditsTableHarness extends ComponentHarness {
  static hostSelector = 'api-audits-table';

  public async rows() {
    const table = await this.getTable();
    const rows = await table.getRows();

    return await parallel(() => rows.map(row => row.getCellTextByColumnName()));
  }

  public async paginator() {
    const table = await this.getTableWrapper();
    return await table.getPaginator();
  }

  private getTable = this.locatorFor(MatTableHarness.with({ selector: '#auditsTable' }));
  private getTableWrapper = this.locatorFor(GioTableWrapperHarness);
}
