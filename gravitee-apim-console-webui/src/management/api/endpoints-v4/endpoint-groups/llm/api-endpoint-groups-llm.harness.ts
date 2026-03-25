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
  private getGroupEditButtons = this.locatorForAll(
    MatButtonHarness.with({ selector: '[aria-label="Edit provider group"], [aria-label="View provider group"]' }),
  );
  private getGroupDeleteButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[aria-label="Delete provider"]' }));
  private getEndpointEditButtons = this.locatorForAll(
    MatButtonHarness.with({ selector: '[aria-label="Edit endpoint"], [aria-label="View endpoint"]' }),
  );
  private getEndpointDeleteButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[aria-label="Delete endpoint"]' }));

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

  public async getEndpointName(cardIndex: number, endpointIndex: number): Promise<string> {
    const selector = `.endpoint-group-card:nth-child(${cardIndex + 1}) .endpoint-group-card__endpoint:nth-child(${endpointIndex + 2}) .endpoint-group-card__endpoint__header__title .mat-h4`;
    const element = await this.locatorForOptional(selector)();
    return element ? await element.text() : '';
  }

  public async getEndpointProviderType(cardIndex: number, endpointIndex: number): Promise<string> {
    const selector = `.endpoint-group-card:nth-child(${cardIndex + 1}) .endpoint-group-card__endpoint:nth-child(${endpointIndex + 2}) .gio-badge-primary`;
    const element = await this.locatorForOptional(selector)();
    return element ? await element.text() : '';
  }

  public async getEndpointCount(cardIndex: number): Promise<number> {
    const selector = `.endpoint-group-card:nth-child(${cardIndex + 1}) .endpoint-group-card__endpoint`;
    const elements = await this.locatorForAll(selector)();
    return elements.length;
  }

  public async getModelsTable(groupIndex: number, endpointIndex: number): Promise<MatTableHarness | null> {
    return await this.locatorForOptional(MatTableHarness.with({ selector: `#groupsTable-${groupIndex}-${endpointIndex}` }))();
  }

  public async getModelsTableRows(groupIndex: number, endpointIndex: number): Promise<string[][]> {
    const table = await this.getModelsTable(groupIndex, endpointIndex);
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

  public async isGroupEditButtonVisible(index: number): Promise<boolean> {
    const buttons = await this.getGroupEditButtons();
    return buttons.length > index;
  }

  public async isGroupDeleteButtonVisible(index: number): Promise<boolean> {
    const buttons = await this.getGroupDeleteButtons();
    return buttons.length > index;
  }

  public async isEndpointEditButtonVisible(index: number): Promise<boolean> {
    const buttons = await this.getEndpointEditButtons();
    return buttons.length > index;
  }

  public async isEndpointDeleteButtonVisible(index: number): Promise<boolean> {
    const buttons = await this.getEndpointDeleteButtons();
    return buttons.length > index;
  }

  public async clickGroupDeleteButton(index: number, rootLoader: HarnessLoader): Promise<void> {
    const buttons = await this.getGroupDeleteButtons();
    await buttons[index].click();

    const dialog = await rootLoader.getHarness(MatDialogHarness);
    const confirmButton = await dialog.getHarness(MatButtonHarness.with({ text: /Delete/ }));
    return await confirmButton.click();
  }

  public async clickEndpointDeleteButton(index: number, rootLoader: HarnessLoader): Promise<void> {
    const buttons = await this.getEndpointDeleteButtons();
    await buttons[index].click();

    const dialog = await rootLoader.getHarness(MatDialogHarness);
    const confirmButton = await dialog.getHarness(MatButtonHarness.with({ text: /Delete/ }));
    return await confirmButton.click();
  }
}
