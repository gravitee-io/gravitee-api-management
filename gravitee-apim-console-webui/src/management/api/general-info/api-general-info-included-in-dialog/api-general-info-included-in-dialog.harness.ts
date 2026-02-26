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
/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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

import { ComponentHarness, parallel } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { DivHarness } from '@gravitee/ui-particles-angular/testing';

export class ApiGeneralInfoIncludedInDialogHarness extends ComponentHarness {
  static hostSelector = 'api-general-info-included-in-dialog';

  private getTitleElement = this.locatorFor(DivHarness.with({ selector: '[data-testid="included-in-dialog-title"]' }));
  private getCloseButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="included-in-dialog-close"]' }));
  private getSearchInput = this.locatorFor(MatInputHarness.with({ selector: '[data-testid="included-in-dialog-search"]' }));
  private getEmptyMessage = this.locatorForOptional(DivHarness.with({ selector: '[data-testid="included-in-dialog-empty"]' }));
  private getProductItems = this.locatorForAll(DivHarness.with({ selector: '.api-products-list__item' }));

  async getTitleText(): Promise<string> {
    const title = await this.getTitleElement();
    return ((await title.getText()) ?? '').trim();
  }

  async setSearchTerm(value: string): Promise<void> {
    const input = await this.getSearchInput();
    await input.setValue(value);
  }

  async getSearchValue(): Promise<string> {
    const input = await this.getSearchInput();
    return input.getValue();
  }

  async getProductNames(): Promise<string[]> {
    const items = await this.getProductItems();
    const texts = await parallel(() => items.map(item => item.getText()));
    return texts.map(t => t ?? '');
  }

  async getProductById(productId: string): Promise<DivHarness | null> {
    return this.locatorForOptional(DivHarness.with({ selector: `[data-testid="included-in-dialog-product-${productId}"]` }))();
  }

  async getEmptyMessageText(): Promise<string | null> {
    const empty = await this.getEmptyMessage();
    return empty ? empty.getText() : null;
  }
}
