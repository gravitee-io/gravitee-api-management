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
import { DivHarness } from '@gravitee/ui-particles-angular/testing';

export class ApiEndpointGroupsStandardHarness extends ComponentHarness {
  static hostSelector = 'api-endpoint-groups-standard';

  private getDeleteEndpointGroupButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[aria-label="Delete endpoints group"]' }));
  private getDeleteEndpointButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[aria-label="Delete endpoint"]' }));
  private getAddEndpointButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[aria-label="Add endpoint"]' }));
  private getEditEndpointButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[aria-label="Edit endpoint"]' }));
  private getViewEndpointButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[aria-label="View endpoint"]' }));
  private getEditEndpointGroupButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[aria-label="Edit endpoint group"]' }));
  private getAddEndpointGroupButton = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Add endpoint group"]' }));
  public getWarningFailoverBanner = this.locatorForAll(DivHarness.with({ selector: '.banner__wrapper__title' }));

  public async getTableRows(index: number) {
    const table = this.locatorFor(MatTableHarness.with({ selector: `[data-testid=groupsTable-${index}]` }));
    return await table().then(t => t.getCellTextByIndex());
  }

  public async deleteEndpointGroup(index: number, rootLoader: HarnessLoader) {
    const button = (await this.getDeleteEndpointGroupButtons())[index];
    await button.click();
    return await rootLoader
      .getHarness(MatDialogHarness)
      .then(dialog => dialog.getHarness(MatButtonHarness.with({ text: /Delete/ })))
      .then(element => element.click());
  }

  public async isEndpointGroupDeleteButtonVisible(): Promise<boolean> {
    return this.getDeleteEndpointGroupButtons().then(buttons => buttons?.length > 0);
  }

  public async deleteEndpoint(index: number, rootLoader: HarnessLoader) {
    const button = (await this.getDeleteEndpointButtons())[index];
    await button.click();
    return await rootLoader
      .getHarness(MatDialogHarness)
      .then(dialog => dialog.getHarness(MatButtonHarness.with({ text: /Delete/ })))
      .then(element => element.click());
  }

  public async isEndpointDeleteDisabled(index: number) {
    return this.getDeleteEndpointButtons().then(buttons => buttons[index].isDisabled());
  }

  public async isEndpointDeleteButtonVisible(): Promise<boolean> {
    return this.getDeleteEndpointButtons().then(buttons => buttons?.length > 0);
  }

  public async clickAddEndpoint(index: number) {
    const button = (await this.getAddEndpointButtons())[index];
    return button.click();
  }

  public async isAddEndpointButtonVisible(): Promise<boolean> {
    return this.getAddEndpointButtons().then(buttons => buttons?.length > 0);
  }

  public async clickEditEndpoint(index: number) {
    const button = (await this.getEditEndpointButtons())[index];
    return button.click();
  }

  public async isEditEndpointButtonVisible(): Promise<boolean> {
    return this.getEditEndpointButtons().then(buttons => buttons?.length > 0);
  }

  public async isViewEndpointButtonVisible(): Promise<boolean> {
    return this.getViewEndpointButtons().then(buttons => buttons?.length > 0);
  }

  public async moveGroupUp(index: number) {
    const button = await this.getMoveUpButton(index);
    return button.click();
  }

  public async moveGroupDown(index: number) {
    const button = await this.getMoveDownButton(index);
    return button.click();
  }

  public async getMoveUpButton(index: number) {
    return await this.locatorForOptional(MatButtonHarness.with({ selector: `#moveUpBtn-${index}` }))();
  }

  public async getMoveDownButton(index: number) {
    return await this.locatorForOptional(MatButtonHarness.with({ selector: `#moveDownBtn-${index}` }))();
  }

  public async isEditEndpointGroupNameFieldAvailable(index: number): Promise<boolean> {
    return await this.getEditEndpointGroupButtons().then(buttons => !!buttons[index]);
  }

  public async clickEditEndpointGroup(index: number) {
    const button = await this.getEditEndpointGroupButtons();
    return button[index].click();
  }

  public async isAddEndpointGroupDisplayed(): Promise<boolean> {
    return await this.getAddEndpointGroupButton()
      .then(_ => true)
      .catch(_ => false);
  }

  public async isAddEndpointGroupClickable(): Promise<boolean> {
    const button = await this.getAddEndpointGroupButton();
    return !(await button.isDisabled());
  }

  public async clickAddEndpointGroup(): Promise<void> {
    const button = await this.getAddEndpointGroupButton();
    return await button.click();
  }
}
