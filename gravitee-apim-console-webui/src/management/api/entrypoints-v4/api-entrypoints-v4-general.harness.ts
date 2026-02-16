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
import { MatButtonHarness } from '@angular/material/button/testing';

export class ApiEntrypointsV4GeneralHarness extends ComponentHarness {
  public static hostSelector = '.entrypoints';

  private tableLocator = this.locatorFor(MatTableHarness);
  private switchListenerModeLocator = this.locatorFor(MatButtonHarness.with({ selector: '#switchListenerType' }));

  async hasEntrypointsTable(): Promise<boolean> {
    return this.tableLocator()
      .then(_ => true)
      .catch(_ => false);
  }
  async getEntrypointsTableRows(): Promise<MatRowHarness[]> {
    return this.tableLocator().then(table => table.getRows());
  }

  async getDeleteBtnByRowIndex(index: number): Promise<MatButtonHarness> {
    return this.getEntrypointsTableRows()
      .then(rows => rows[index].getCells({ columnName: 'actions' }))
      .then(actionCell => actionCell[0].getAllHarnesses(MatButtonHarness))
      .then(actionButtons => actionButtons[1]);
  }
  async deleteEntrypointByIndex(index: number): Promise<void> {
    return this.getDeleteBtnByRowIndex(index).then(btn => btn.click());
  }

  async canToggleListenerMode(): Promise<boolean> {
    return this.switchListenerModeLocator()
      .then(async btn => btn != null && !(await btn.isDisabled()))
      .catch(() => false);
  }

  async getToggleBtn(): Promise<MatButtonHarness> {
    return this.switchListenerModeLocator();
  }
}
