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

export class ApiGeneralGroupMembersHarness extends ComponentHarness {
  static readonly hostSelector = 'api-general-group-members';

  private getGroupTable = () => this.locatorFor(MatTableHarness)();
  private getDoNotHavePermissionMessage = () => this.locatorFor('#cannot-view-members')();
  private groupTableNameSelector = this.locatorFor('.title');

  public getGroupTableByGroupName(): Promise<MatTableHarness> {
    return this.getGroupTable();
  }

  public groupTableExistsByGroupName(): Promise<boolean> {
    return this.getGroupTable()
      .then((_) => true)
      .catch((_) => false);
  }

  public async isLoading(): Promise<boolean> {
    const groupTable = await this.getGroupTable();
    return (await groupTable.getAllChildLoaders('gio-loader')).length > 0;
  }

  public userCannotViewGroupMembers(): Promise<boolean> {
    return this.getDoNotHavePermissionMessage()
      .then((_) => true)
      .catch((_) => false);
  }

  public getGroupTableName(): Promise<string> {
    return this.groupTableNameSelector().then((tableName) => tableName.text());
  }
}
