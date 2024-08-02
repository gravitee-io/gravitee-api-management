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
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatRowHarness, MatTableHarness } from '@angular/material/table/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';

export class DiscoveryPreviewHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-discovery-preview';

  private cancelButtonLocator = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid=cancel-button]' }));

  private proceedButtonLocator = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid=proceed-button]' }));

  private newItemsToggleLocator = this.locatorFor(MatSlideToggleHarness.with({ selector: '[data-testid=new-items-toggle]' }));

  private updateItemsToggleLocator = this.locatorFor(MatSlideToggleHarness.with({ selector: '[data-testid=update-items-toggle]' }));

  public getNewItemsToggle = () => {
    return this.newItemsToggleLocator();
  };

  public getUpdateItemsToggle = () => {
    return this.updateItemsToggleLocator();
  };

  public getCancelButton = (): Promise<MatButtonHarness> => {
    return this.cancelButtonLocator();
  };

  public getProceedButton = (): Promise<MatButtonHarness> => {
    return this.proceedButtonLocator();
  };

  public getTable = this.locatorForOptional(MatTableHarness);

  public rowsNumber = async (): Promise<number> => {
    return this.getTable()
      .then((table: MatTableHarness) => table.getRows())
      .then((rows: MatRowHarness[]) => rows.length);
  };
}
