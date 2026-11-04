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
import { MatTableHarness } from '@angular/material/table/testing';

export class ApiV4PathMappingsHarness extends ComponentHarness {
  static readonly hostSelector = 'api-v4-path-mappings';

  private readonly banner = this.locatorForOptional('[data-testid="path-mappings-banner"]');
  private readonly emptyState = this.locatorForOptional('[data-testid="path-mappings-empty"]');
  private readonly loadingState = this.locatorForOptional('[data-testid="path-mappings-loading"]');
  private readonly loadFailedBanner = this.locatorForOptional('[data-testid="path-mappings-load-failed"]');
  private readonly addButton = this.locatorForOptional(MatButtonHarness.with({ selector: '[data-testid="add-path-mapping"]' }));
  private readonly table = this.locatorFor(MatTableHarness.with({ selector: '[data-testid="path-mappings-table"]' }));

  public async bannerText(): Promise<string> {
    const banner = await this.banner();
    return banner ? (await banner.text()).trim() : '';
  }

  public async emptyStateText(): Promise<string | null> {
    const empty = await this.emptyState();
    return empty ? (await empty.text()).trim() : null;
  }

  public async isLoadingVisible(): Promise<boolean> {
    return (await this.loadingState()) !== null;
  }

  public async loadFailedText(): Promise<string | null> {
    const banner = await this.loadFailedBanner();
    return banner ? (await banner.text()).trim() : null;
  }

  public async hasTable(): Promise<boolean> {
    try {
      await this.table();
      return true;
    } catch {
      return false;
    }
  }

  public async pathValues(): Promise<string[]> {
    const table = await this.table();
    const rows = await table.getRows();
    return Promise.all(
      rows.map(async row => {
        const cells = await row.getCellTextByColumnName();
        return cells['path'];
      }),
    );
  }

  public async hasAddButton(): Promise<boolean> {
    return (await this.addButton()) !== null;
  }

  public async clickAdd(): Promise<void> {
    const button = await this.addButton();
    if (!button) {
      throw new Error('Add path mapping button is not available');
    }
    return button.click();
  }

  public async clickEdit(index: number): Promise<void> {
    const buttons = await this.locatorForAll(MatButtonHarness.with({ selector: '[data-testid="edit-path-mapping"]' }))();
    return buttons[index].click();
  }

  public async clickDelete(index: number): Promise<void> {
    const buttons = await this.locatorForAll(MatButtonHarness.with({ selector: '[data-testid="delete-path-mapping"]' }))();
    return buttons[index].click();
  }

  public async hasEditButtons(): Promise<boolean> {
    const buttons = await this.locatorForAll(MatButtonHarness.with({ selector: '[data-testid="edit-path-mapping"]' }))();
    return buttons.length > 0;
  }

  public async hasDeleteButtons(): Promise<boolean> {
    const buttons = await this.locatorForAll(MatButtonHarness.with({ selector: '[data-testid="delete-path-mapping"]' }))();
    return buttons.length > 0;
  }
}
