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
import { ComponentHarness, HarnessLoader } from '@angular/cdk/testing';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

export class ApiEndpointsGroupsHarness extends ComponentHarness {
  static hostSelector = 'api-endpoints-groups';

  private getDeleteEndpointGroupButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[aria-label="Delete endpoints group"]' }));
  private getDeleteEndpointButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[aria-label="Delete endpoint"]' }));
  private getAddEndpointButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[aria-label="Add endpoint"]' }));

  public async getTableRows(index: number) {
    const table = this.locatorFor(MatTableHarness.with({ selector: `#groupsTable-${index}` }));
    return await table().then((t) => t.getCellTextByIndex());
  }

  public async deleteEndpointGroup(index: number, rootLoader: HarnessLoader) {
    const button = (await this.getDeleteEndpointGroupButtons())[index];
    await button.click();
    return await rootLoader
      .getHarness(MatDialogHarness)
      .then((dialog) => dialog.getHarness(MatButtonHarness.with({ text: /Delete/ })))
      .then((element) => element.click());
  }

  public async deleteEndpoint(index: number, rootLoader: HarnessLoader) {
    const button = (await this.getDeleteEndpointButtons())[index];
    await button.click();
    return await rootLoader
      .getHarness(MatDialogHarness)
      .then((dialog) => dialog.getHarness(MatButtonHarness.with({ text: /Delete/ })))
      .then((element) => element.click());
  }

  public async clickAddEndpoint(index: number) {
    const button = (await this.getAddEndpointButtons())[index];
    return button.click();
  }
}
