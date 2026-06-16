/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

import { PaginatedTableHarness } from '../../paginated-table/paginated-table.harness';
import { PaginationHarness } from '../../pagination/pagination.harness';

export class ApiKeysListHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-api-keys-list';

  protected locateTable = this.locatorForOptional(PaginatedTableHarness);
  protected locatePagination = this.locatorForOptional(PaginationHarness);
  protected locateRevokeButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[data-testid="api-key-revoke-button"]' }));
  protected locateFeedback = this.locatorForOptional('[data-testid="api-key-feedback"]');
  protected locateStatusIcons = this.locatorForAll('[data-testid="api-key-status-icon"]');
  protected locateKeyCells = this.locatorForAll('td.mat-column-key');

  public async isTableShown(): Promise<boolean> {
    return !!(await this.locateTable());
  }

  public async getKeyAt(rowIndex: number): Promise<string> {
    const table = await this.locateTable();
    const keyCell = await table?.getCellElement(rowIndex, 'key');
    return (await keyCell?.text())?.trim() ?? '';
  }

  public async getStatusIcons(): Promise<string[]> {
    const icons = await this.locateStatusIcons();
    const statuses = await Promise.all(icons.map(icon => icon.getAttribute('data-status-icon')));
    return statuses.map(status => status ?? '');
  }

  public async getRevokeButtons(): Promise<MatButtonHarness[]> {
    return this.locateRevokeButtons();
  }

  public async clickRevoke(apiKey: string): Promise<void> {
    const revokeButton = await this.locatorFor(MatButtonHarness.with({ selector: `[data-api-key="${apiKey}"]` }))();
    return revokeButton.click();
  }

  public async getDisplayedKeys(): Promise<string[]> {
    const keyCells = await this.locateKeyCells();
    return Promise.all(keyCells.map(cell => cell.text().then(text => text.trim())));
  }

  public async getPagination(): Promise<PaginationHarness | null> {
    return this.locatePagination();
  }

  public async getFeedbackText(): Promise<string | undefined> {
    const feedback = await this.locateFeedback();
    return (await feedback?.text())?.trim();
  }

  public async getFeedbackAttribute(attribute: string): Promise<string | null> {
    const feedback = await this.locateFeedback();
    return feedback ? feedback.getAttribute(attribute) : null;
  }

  public async getEmptyMessage(): Promise<string | undefined> {
    const emptyMessage = await this.locatorForOptional('[data-testid="api-keys-empty-message"]')();
    return (await emptyMessage?.text())?.trim();
  }
}
