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
import { ComponentHarness, TestElement } from '@angular/cdk/testing';

export class GmdCardHarness extends ComponentHarness {
  static hostSelector = 'gmd-card';

  async getCardTitle(): Promise<TestElement | null> {
    try {
      return await this.locatorFor('gmd-card-title')();
    } catch {
      return null;
    }
  }

  async getCardSubtitle(): Promise<TestElement | null> {
    try {
      return await this.locatorFor('gmd-card-subtitle')();
    } catch {
      return null;
    }
  }

  async getCardTitleText(): Promise<string> {
    const title = await this.getCardTitle();
    return title ? title.text() : '';
  }

  async getCardSubtitleText(): Promise<string> {
    const subtitle = await this.getCardSubtitle();
    return subtitle ? subtitle.text() : '';
  }

  async getBackgroundColor(): Promise<string> {
    const host = await this.host();
    return host.getCssValue('--gmd-card-container-color');
  }

  async getTextColor(): Promise<string> {
    const host = await this.host();
    return host.getCssValue('--gmd-card-text-color');
  }
}
