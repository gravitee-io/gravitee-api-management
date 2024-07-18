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
import { DivHarness } from '@gravitee/ui-particles-angular/testing';

export class ApiDocumentationV4PagesListHarness extends ComponentHarness {
  static hostSelector = 'api-documentation-v4-pages-list';

  protected tableLocator = this.locatorFor(MatTableHarness);
  protected allEditFolderButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[aria-label="Edit folder"]' }));
  protected allPublishPageButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[aria-label="Publish page"]' }));
  protected allUnpublishPageButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[aria-label="Unpublish page"]' }));
  protected allMovePageDownButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[aria-label="Move page down"]' }));
  protected allMovePageUpButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[aria-label="Move page up"]' }));
  protected allDeletePageButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[aria-label="Delete page"]' }));

  public async getNameDivByRowIndex(idx: number): Promise<DivHarness> {
    const table = await this.tableLocator();
    const rows = await table.getRows();
    const cells = await rows[idx].getCells({ columnName: 'name' });
    return cells[0].getHarness(DivHarness);
  }
  public getNameByRowIndex(idx: number): Promise<string> {
    return this.getColumnTextByIndex('name', idx);
  }

  public getVisibilityByRowIndex(idx: number): Promise<string> {
    return this.getColumnTextByIndex('visibility', idx);
  }

  public getStatusByRowIndex(idx: number): Promise<string> {
    return this.getColumnTextByIndex('status', idx);
  }

  public getEditFolderButtonByRowIndex(idx: number): Promise<MatButtonHarness> {
    return this.allEditFolderButtons().then((buttonList) => buttonList[idx]);
  }

  public getPublishPageButtonByRowIndex(idx: number): Promise<MatButtonHarness> {
    return this.allPublishPageButtons().then((buttonList) => buttonList[idx]);
  }

  public getUnpublishPageButtonByRowIndex(idx: number): Promise<MatButtonHarness> {
    return this.allUnpublishPageButtons().then((buttonList) => buttonList[idx]);
  }

  public getMovePageDownButtonByRowIndex(idx: number): Promise<MatButtonHarness> {
    return this.allMovePageDownButtons().then((buttonList) => buttonList[idx]);
  }

  public getMovePageUpButtonByRowIndex(idx: number): Promise<MatButtonHarness> {
    return this.allMovePageUpButtons().then((buttonList) => buttonList[idx]);
  }

  public getDeletePageButtonByRowIndex(idx: number): Promise<MatButtonHarness> {
    return this.allDeletePageButtons().then((buttonList) => buttonList[idx]);
  }

  private async getColumnTextByIndex(columnName: string, idx: number): Promise<string> {
    const table = await this.tableLocator();
    const rows = await table.getRows();
    const cells = await rows[idx].getCellTextByIndex({ columnName });
    return cells[0];
  }
}
