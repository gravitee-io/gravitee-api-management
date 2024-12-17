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
import { MatRowHarness, MatTableHarness } from '@angular/material/table/testing';
import { MatPaginatorHarness } from '@angular/material/paginator/testing';

export class TopApisWidgetHarness extends ComponentHarness {
  static hostSelector = 'app-top-apis-widget';

  private tableLocator = this.locatorForOptional(MatTableHarness);
  private paginationLocator = this.locatorForOptional(MatPaginatorHarness);

  public rowsNumber = async (): Promise<number> => {
    return this.tableLocator()
      .then((table: MatTableHarness) => table.getRows())
      .then((rows: MatRowHarness[]) => rows.length);
  };

  public getPagination = async (): Promise<MatPaginatorHarness> => {
    return await this.paginationLocator();
  };
}