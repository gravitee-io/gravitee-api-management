/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { ComponentHarness, TestElement } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

export class PaginatedTableHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-paginated-table';

  protected locateNavigableRows = this.locatorForAll('.paginated-table__row--navigable');
  protected locateExpandColumns = this.locatorForAll('.paginated-table__column-expand');
  protected locateActionButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[data-testid^="paginated-table-action-"]' }));

  async getNavigableRows(): Promise<TestElement[]> {
    return this.locateNavigableRows();
  }

  async getExpandColumns(): Promise<TestElement[]> {
    return this.locateExpandColumns();
  }

  async getActionButtons(): Promise<MatButtonHarness[]> {
    return this.locateActionButtons();
  }

  async getActionButton(actionId: string): Promise<MatButtonHarness | null> {
    return this.locatorForOptional(MatButtonHarness.with({ selector: `[data-testid="paginated-table-action-${actionId}"]` }))();
  }
}
