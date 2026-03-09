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
import { MatInputHarness } from '@angular/material/input/testing';
import { DivHarness } from '@gravitee/ui-particles-angular/testing';
import { GioFormSelectionInlineCardHarness } from '@gravitee/ui-particles-angular';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';

export class SectionEditorDialogHarness extends ComponentHarness {
  static hostSelector = 'section-editor-dialog';
  private locateTitleInput = this.locatorFor(MatInputHarness.with({ selector: '[formcontrolname="title"]' }));
  private locateUrlInput = this.locatorFor(MatInputHarness.with({ selector: '[formcontrolname="url"]' }));
  private locateCancelButton = this.locatorFor(MatButtonHarness.with({ text: 'Cancel' }));
  private locateSubmitButton = this.locatorFor(MatButtonHarness.with({ text: /Add|Save/ }));
  private locateFormTitle = this.locatorFor(DivHarness.with({ selector: '[mat-dialog-title]' }));
  private locateAuthenticationToggle = this.locatorFor(MatSlideToggleHarness);
  private locatePageTypeCards = this.locatorForAll(
    GioFormSelectionInlineCardHarness.with({ ancestor: '.section-editor-dialog__page-types' }),
  );
  private locatePageTypeSection = this.locatorForOptional('.section-editor-dialog__page-types');

  async getTitleInput(): Promise<MatInputHarness> {
    return this.locateTitleInput();
  }

  async setTitleInputValue(value: string): Promise<void> {
    const titleInput = await this.locateTitleInput();
    return titleInput.setValue(value);
  }

  async getTitleInputValue(): Promise<string> {
    const titleInput = await this.locateTitleInput();
    return titleInput.getValue();
  }

  async getUrlInputValue(): Promise<string> {
    const urlInput = await this.locateUrlInput();
    return urlInput.getValue();
  }

  async setUrlInputValue(value: string): Promise<void> {
    const urlInput = await this.locateUrlInput();
    return urlInput.setValue(value);
  }

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
    return await this.locateAuthenticationToggle();
  }

  async toggleAuthentication(): Promise<void> {
    const toggle = await this.locateAuthenticationToggle();
    return toggle.toggle();
  }

  async isPageTypeSelectionVisible(): Promise<boolean> {
    const section = await this.locatePageTypeSection();
    return section != null;
  }

  /**
   * Select a page content type (only when "Add page" dialog shows Page Type section).
   */
  async selectPageType(value: 'GRAVITEE_MARKDOWN' | 'OPENAPI'): Promise<void> {
    const cards = await this.locatePageTypeCards();
    const card = await this.findCardByValue(cards, value);
    if (!card) {
      throw new Error(`Page type card with value "${value}" not found`);
    }
    const host = await card.host();
    await host.click();
  }

  private async findCardByValue(
    cards: GioFormSelectionInlineCardHarness[],
    value: string,
  ): Promise<GioFormSelectionInlineCardHarness | undefined> {
    for (const card of cards) {
      if ((await card.getValue()) === value) {
        return card;
      }
    }
    return undefined;
  }
}
