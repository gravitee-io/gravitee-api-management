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

import { LoaderHarness } from '../../../../components/loader/loader.harness';
import { PaginatedTableHarness } from '../../../../components/paginated-table/paginated-table.harness';
import { UserCellHarness } from '../../../../components/user-cell/user-cell.harness';

export class ApplicationTabMembersComponentHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-application-tab-members';

  private readonly locateLoader = this.locatorForOptional(LoaderHarness);
  private readonly locateSectionTitle = this.locatorForOptional('[data-testid="members-section-title"]');
  private readonly locateErrorMessage = this.locatorForOptional('[data-testid="members-list-error"]');
  private readonly locateEmptyState = this.locatorForOptional('[data-testid="members-empty-state"]');
  private readonly locatePaginatedTable = this.locatorForOptional(PaginatedTableHarness);
  private readonly locateUserCells = this.locatorForAll(UserCellHarness);
  private readonly locatePrimaryOwnerRoleCell = this.locatorForOptional('[data-testid="members-role-primary-owner"]');
  private readonly locateAddMembersButton = this.locatorForOptional(
    MatButtonHarness.with({ selector: '[data-testid="members-add-button"]' }),
  );
  private readonly locateTransferOwnershipButton = this.locatorForOptional(
    MatButtonHarness.with({ selector: '[data-testid="members-transfer-ownership-button"]' }),
  );

  public async getLoader(): Promise<LoaderHarness | null> {
    return this.locateLoader();
  }

  public async getSectionTitle(): Promise<TestElement | null> {
    return this.locateSectionTitle();
  }

  public async getErrorMessage(): Promise<TestElement | null> {
    return this.locateErrorMessage();
  }

  public async getEmptyState(): Promise<TestElement | null> {
    return this.locateEmptyState();
  }

  public async getPaginatedTable(): Promise<PaginatedTableHarness | null> {
    return this.locatePaginatedTable();
  }

  public async getAddMembersButton(): Promise<MatButtonHarness | null> {
    return this.locateAddMembersButton();
  }

  public async clickAddMembersButton(): Promise<void> {
    return (await this.locateAddMembersButton())!.click();
  }

  public async getTransferOwnershipButton(): Promise<MatButtonHarness | null> {
    return this.locateTransferOwnershipButton();
  }

  public async clickTransferOwnershipButton(): Promise<void> {
    return (await this.locateTransferOwnershipButton())!.click();
  }

  public async getUserCells(): Promise<UserCellHarness[]> {
    return this.locateUserCells();
  }

  public async getFirstUserCell(): Promise<UserCellHarness> {
    const cells = await this.locateUserCells();
    if (cells.length === 0) {
      throw new Error('Expected at least one app-user-cell to be rendered');
    }
    return cells[0];
  }

  public async isPrimaryOwnerRoleVisible(): Promise<boolean> {
    return !!(await this.locatePrimaryOwnerRoleCell());
  }
}
