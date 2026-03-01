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
import { MatSelectHarness } from '@angular/material/select/testing';

export class PaginationHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-pagination';

  protected locatePreviousPageButton = this.locatorFor(
    MatButtonHarness.with({ selector: '[aria-label="Previous page of results"].pagination__nav-button' }),
  );
  protected locateNextPageButton = this.locatorFor(
    MatButtonHarness.with({ selector: '[aria-label="Next page of results"].pagination__nav-button' }),
  );
  protected locateCurrentPageButton = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Current page of results"]' }));
  protected locatePageSizeSelect = this.locatorForOptional(MatSelectHarness.with({ ancestor: '.pagination__page-size' }));

  async getPreviousPageButton(): Promise<MatButtonHarness> {
    return this.locatePreviousPageButton();
  }

  async getNextPageButton(): Promise<MatButtonHarness> {
    return this.locateNextPageButton();
  }

  async getCurrentPaginationPage(): Promise<MatButtonHarness> {
    return this.locateCurrentPageButton();
  }

  async getPageButtonByNumber(pageNumber: number): Promise<MatButtonHarness> {
    return this.locatePageButtonByLabel(`${pageNumber}`);
  }

  async getPageSizeSelect(): Promise<MatSelectHarness | null> {
    return this.locatePageSizeSelect();
  }

  async getSelectedPageSize(): Promise<string | null> {
    const select = await this.locatePageSizeSelect();
    if (!select) return null;
    return select.getValueText();
  }

  async changePageSize(size: number): Promise<void> {
    const select = await this.locatePageSizeSelect();
    if (!select) throw new Error('Page size selector not found');
    await select.open();
    await select.clickOptions({ text: `${size}` });
  }

  protected locatePageButtonByLabel = (label: string) => this.locatorFor(MatButtonHarness.with({ text: label }))();
}
