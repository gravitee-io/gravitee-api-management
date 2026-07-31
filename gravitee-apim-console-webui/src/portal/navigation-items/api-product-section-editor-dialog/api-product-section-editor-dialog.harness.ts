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
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { DivHarness, SpanHarness } from '@gravitee/ui-particles-angular/testing';

import { GioTableWrapperHarness } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.harness';

export class ApiProductSectionEditorDialogHarness extends ComponentHarness {
  static readonly hostSelector = 'api-product-section-editor-dialog';

  private readonly locateCancelButton = this.locatorFor(MatButtonHarness.with({ text: 'Cancel' }));
  private readonly locateSubmitButton = this.locatorFor(MatButtonHarness.with({ text: 'Add' }));
  private readonly locateFormTitle = this.locatorFor(DivHarness.with({ selector: '[mat-dialog-title]' }));
  private readonly locateAuthenticationToggle = this.locatorFor(MatSlideToggleHarness);
  private readonly locateTableWrapper = this.locatorFor(GioTableWrapperHarness);
  private readonly locateAlreadyAddedLabels = this.locatorForAll(
    SpanHarness.with({ selector: '[data-testid^="api-product-picker-already-added-"]' }),
  );
  private readonly locateLoadError = this.locatorForOptional(
    SpanHarness.with({ selector: '[data-testid="api-product-picker-load-error"]' }),
  );
  private readonly locateLoading = this.locatorForOptional(SpanHarness.with({ selector: '[data-testid="api-product-picker-loading"]' }));
  private readonly locateEmptyState = this.locatorForOptional(SpanHarness.with({ selector: '[data-testid="api-product-picker-empty"]' }));

  async clickCancelButton(): Promise<void> {
    return (await this.locateCancelButton()).click();
  }

  async isSubmitButtonDisabled(): Promise<boolean> {
    return (await this.locateSubmitButton()).isDisabled();
  }

  async clickSubmitButton(): Promise<void> {
    return (await this.locateSubmitButton()).click();
  }

  async getDialogTitle(): Promise<string> {
    return (await this.locateFormTitle()).getText();
  }

  async isAuthenticationToggleDisabled(): Promise<boolean> {
    return (await this.locateAuthenticationToggle()).isDisabled();
  }

  async isAuthenticationToggleChecked(): Promise<boolean> {
    return (await this.locateAuthenticationToggle()).isChecked();
  }

  async toggleAuthentication(): Promise<void> {
    return (await this.locateAuthenticationToggle()).toggle();
  }

  async setSearchValue(value: string): Promise<void> {
    return (await this.locateTableWrapper()).setSearchValue(value);
  }

  async getAlreadyAddedLabel(apiProductId: string): Promise<SpanHarness | null> {
    const expectedDataTestId = `api-product-picker-already-added-${apiProductId}`;
    for (const label of await this.locateAlreadyAddedLabels()) {
      const host = await label.host();
      if ((await host.getAttribute('data-testid')) === expectedDataTestId) {
        return label;
      }
    }
    return null;
  }

  async getLoadErrorText(): Promise<string | null> {
    return (await this.locateLoadError())?.getText() ?? null;
  }

  async getLoadingText(): Promise<string | null> {
    return (await this.locateLoading())?.getText() ?? null;
  }

  async getEmptyStateText(): Promise<string | null> {
    return (await this.locateEmptyState())?.getText() ?? null;
  }
}
