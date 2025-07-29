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
import { MatCardHarness } from '@angular/material/card/testing';
import { MatIconHarness } from '@angular/material/icon/testing';

export class GioWidgetLayoutHarness extends ComponentHarness {
  static hostSelector = 'gio-widget-layout';

  protected getCardElement = this.locatorFor(MatCardHarness);
  protected getTitleElement = this.locatorFor('mat-card-title');
  protected getTooltipIcon = this.locatorForOptional(MatIconHarness.with({ selector: '[svgIcon="gio:info"]' }));
  protected getLoader = this.locatorForOptional('gio-loader');
  protected getErrorContainer = this.locatorForOptional('.error');
  protected getErrorIcon = this.locatorForOptional(MatIconHarness.with({ selector: '[svgIcon="gio:error"]' }));
  protected getErrorTextElement = this.locatorForOptional('.error span');
  protected getEmptyContainer = this.locatorForOptional('.empty');
  protected getEmptyTextElement = this.locatorForOptional('.empty span');
  protected getContent = this.locatorForOptional('[gioWidgetLayoutChart]');

  /**
   * Gets the card title text
   */
  async getTitleText(): Promise<string> {
    const titleElement = await this.getTitleElement();
    return titleElement.text();
  }

  /**
   * Checks if the tooltip icon is present
   */
  async hasTooltipIcon(): Promise<boolean> {
    const tooltipIcon = await this.getTooltipIcon();
    return tooltipIcon !== null;
  }

  /**
   * Checks if the loading state is displayed
   */
  async isLoading(): Promise<boolean> {
    const loader = await this.getLoader();
    return loader !== null;
  }

  /**
   * Checks if the error state is displayed
   */
  async hasError(): Promise<boolean> {
    const errorContainer = await this.getErrorContainer();
    return errorContainer !== null;
  }

  /**
   * Gets the error text
   */
  async getErrorText(): Promise<string | null> {
    const errorTextElement = await this.getErrorTextElement();
    if (!errorTextElement) {
      return null;
    }
    return errorTextElement.text();
  }

  /**
   * Checks if the error icon is present
   */
  async hasErrorIcon(): Promise<boolean> {
    const errorIcon = await this.getErrorIcon();
    return errorIcon !== null;
  }

  /**
   * Checks if the empty state is displayed
   */
  async isEmpty(): Promise<boolean> {
    const emptyContainer = await this.getEmptyContainer();
    return emptyContainer !== null;
  }

  /**
   * Gets the empty state text
   */
  async getEmptyText(): Promise<string | null> {
    const emptyTextElement = await this.getEmptyTextElement();
    if (!emptyTextElement) {
      return null;
    }
    return emptyTextElement.text();
  }

  /**
   * Checks if the success state is displayed (has content)
   */
  async hasContent(): Promise<boolean> {
    const content = await this.getContent();
    return content !== null;
  }

  /**
   * Gets the card element
   */
  async getCard(): Promise<MatCardHarness> {
    return this.getCardElement();
  }

  /**
   * Gets the card title
   */
  async getCardTitle(): Promise<string> {
    const card = await this.getCardElement();
    return card.getTitleText();
  }

  /**
   * Gets the card subtitle
   */
  async getCardSubtitle(): Promise<string> {
    const card = await this.getCardElement();
    return card.getSubtitleText();
  }
}
