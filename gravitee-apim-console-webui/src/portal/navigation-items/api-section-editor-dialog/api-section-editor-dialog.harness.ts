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

export class ApiSectionEditorDialogHarness extends ComponentHarness {
  static hostSelector = 'api-section-editor-dialog';

  private locateCancelButton = this.locatorFor(MatButtonHarness.with({ text: 'Cancel' }));
  private locateSubmitButton = this.locatorFor(MatButtonHarness.with({ text: /Add|Save/ }));
  private locateFormTitle = this.locatorFor(DivHarness.with({ selector: '[mat-dialog-title]' }));
  private locateAuthenticationToggle = this.locatorFor(MatSlideToggleHarness);
  private locateAlreadyAddedLabels = this.locatorForAll(SpanHarness.with({ selector: '[data-testid^="api-picker-already-added-"]' }));

  async clickCancelButton(): Promise<void> {
    const cancelButton = await this.locateCancelButton();
    return cancelButton.click();
  }

  async isSubmitButtonDisabled(): Promise<boolean> {
    const submitButton = await this.locateSubmitButton();
    return await submitButton.isDisabled();
  }

  async clickSubmitButton(): Promise<void> {
    const submitButton = await this.locateSubmitButton();
    return submitButton.click();
  }

  async getDialogTitle(): Promise<string> {
    const titleElement = await this.locateFormTitle();
    return titleElement.getText();
  }

  async getAuthenticationToggle(): Promise<MatSlideToggleHarness> {
    return this.locateAuthenticationToggle();
  }

  async isAuthenticationToggleDisabled(): Promise<boolean> {
    const toggle = await this.locateAuthenticationToggle();
    return toggle.isDisabled();
  }

  async isAuthenticationToggleChecked(): Promise<boolean> {
    const toggle = await this.locateAuthenticationToggle();
    return toggle.isChecked();
  }

  async toggleAuthentication(): Promise<void> {
    const toggle = await this.locateAuthenticationToggle();
    return toggle.toggle();
  }

  async getAlreadyAddedLabel(apiId: string): Promise<SpanHarness | null> {
    const labels = await this.getAlreadyAddedLabels();
    const expectedDataTestId = `api-picker-already-added-${apiId}`;
    for (const label of labels) {
      const host = await label.host();
      const dataTestId = await host.getAttribute('data-testid');
      if (dataTestId === expectedDataTestId) {
        return label;
      }
    }
    return null;
  }

  async getAlreadyAddedLabels(): Promise<SpanHarness[]> {
    return this.locateAlreadyAddedLabels();
  }
}
