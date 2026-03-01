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

export class ApiEndpointGroupsLlmHarness extends ComponentHarness {
  static hostSelector = 'api-endpoint-groups-llm';

  private getProviderCardsLocator = this.locatorForAll('.endpoint-group-card');
  private getAddProviderButton = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Add provider"]' }));
  private getEditButtons = this.locatorForAll(MatButtonHarness.with({ text: /Edit|View/ }));
  private getDeleteButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[aria-label="Delete provider"]' }));

  public async getProviderCards(): Promise<number> {
    const cards = await this.getProviderCardsLocator();
    return cards.length;
  }

  public async getProviderName(index: number): Promise<string> {
    const titleElement = await this.locatorForOptional(
      `.endpoint-group-card:nth-child(${index + 1}) .endpoint-group-card__header__title .mat-h3`,
    )();
    return titleElement ? await titleElement.text() : '';
  }

  public async getProviderType(index: number): Promise<string> {
    const badgeElement = await this.locatorForOptional(`.endpoint-group-card:nth-child(${index + 1}) .gio-badge-primary`)();
    return badgeElement ? (await badgeElement.text()).trim() : '';
  }

  public async getModelsTable(index: number): Promise<MatTableHarness | null> {
    return await this.locatorForOptional(MatTableHarness.with({ selector: `#groupsTable-${index}` }))();
  }

  public async getModelsTableRows(index: number): Promise<string[][]> {
    const table = await this.getModelsTable(index);
    if (!table) return [];
    return await table.getCellTextByIndex();
  }

  public async isAddProviderButtonVisible(): Promise<boolean> {
    try {
      await this.getAddProviderButton();
      return true;
    } catch {
      return false;
    }
  }

  public async clickAddProviderButton(): Promise<void> {
    const button = await this.getAddProviderButton();
    return await button.click();
  }

  public async isEditButtonVisible(index: number): Promise<boolean> {
    const buttons = await this.getEditButtons();
    return buttons.length > index;
  }

  public async clickEditButton(index: number): Promise<void> {
    const buttons = await this.getEditButtons();
    return await buttons[index].click();
  }

  public async isDeleteButtonVisible(index: number): Promise<boolean> {
    const buttons = await this.getDeleteButtons();
    return buttons.length > index;
  }

  public async clickDeleteButton(index: number, rootLoader: HarnessLoader): Promise<void> {
    const buttons = await this.getDeleteButtons();
    await buttons[index].click();

    const dialog = await rootLoader.getHarness(MatDialogHarness);
    const confirmButton = await dialog.getHarness(MatButtonHarness.with({ text: /Delete/ }));
    return await confirmButton.click();
  }
}
