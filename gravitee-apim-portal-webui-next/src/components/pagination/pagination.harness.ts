/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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

export class PaginationHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-pagination';

  protected locatePreviousPageButton = this.locatorFor(
    MatButtonHarness.with({ selector: 'button.pagination__nav--previous[aria-label="Previous page of results"]' }),
  );
  protected locateNextPageButton = this.locatorFor(
    MatButtonHarness.with({ selector: 'button.pagination__nav--next[aria-label="Next page of results"]' }),
  );
  protected locateCurrentPageButton = this.locatorFor(
    MatButtonHarness.with({ selector: 'button.pagination__current[aria-label="Current page of results"]' }),
  );
  protected locatePageNumberButtons = this.locatorForAll(
    MatButtonHarness.with({ selector: 'button.pagination__current, button.pagination__page' }),
  );

  async getPreviousPageButton(): Promise<MatButtonHarness> {
    return this.locatePreviousPageButton();
  }

  async getNextPageButton(): Promise<MatButtonHarness> {
    return this.locateNextPageButton();
  }

  async getCurrentPaginationPage(): Promise<MatButtonHarness> {
    return this.locateCurrentPageButton();
  }

  async getPageNumberButton(page: number): Promise<MatButtonHarness> {
    const buttons = await this.locatePageNumberButtons();
    for (const button of buttons) {
      const text = await button.getText();
      if (text.trim() === String(page)) return button;
    }
    throw new Error(`Page ${page} button not found`);
  }
}
