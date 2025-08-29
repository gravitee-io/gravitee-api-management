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

import { BaseHarnessFilters, ComponentHarness, HarnessPredicate } from '@angular/cdk/testing';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatCardHarness } from '@angular/material/card/testing';

export type ClusterGroupMembersHarnessFilters = BaseHarnessFilters & {
  groupId?: string;
};

export class ClusterGroupMembersHarness extends ComponentHarness {
  static readonly hostSelector = 'cluster-group-members';

  private getGroupTable = () => this.locatorFor(MatTableHarness)();
  private getTitleEl = () => this.locatorForOptional('mat-card-title')();
  private getCard = () => this.locatorForOptional(MatCardHarness)();

  static with(options: ClusterGroupMembersHarnessFilters = {}): HarnessPredicate<ClusterGroupMembersHarness> {
    return new HarnessPredicate(ClusterGroupMembersHarness, options).addOption('groupId', options.groupId, async (harness, groupId) => {
      const host = await harness.host();
      const id = await host.getAttribute('data-testid');
      return id === groupId;
    });
  }

  /**
   * Returns the MatTableHarness for the members table.
   * Throws if the table is not present.
   */
  public async getGroupTableHarness(): Promise<MatTableHarness> {
    return this.getGroupTable();
  }

  /**
   * Checks group members is displayed.
   */
  public async isGroupMembersDisplayed(): Promise<boolean> {
    const card = await this.getCard();
    return card !== null;
  }

  /**
   * Retrieves the title text displayed in the card header.
   */
  public async getGroupTitle(): Promise<string | null> {
    const title = await this.getTitleEl();
    return title ? title.text() : null;
  }

  /**
   * Gets group members name and role from the table.
   */
  public async getGroupMembersNamesAndRoles(): Promise<{ name: string; role: string }[]> {
    const table = await this.getGroupTableHarness();
    const rows = await table.getRows();
    const members: { name: string; role: string }[] = [];
    for (const row of rows) {
      const nameCell = (await row.getCells({ columnName: 'displayName' }))[0];
      const roleCell = (await row.getCells({ columnName: 'role' }))[0];
      members.push({ name: await nameCell.getText(), role: await roleCell.getText() });
    }
    return members;
  }
}
