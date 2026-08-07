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

import { ContentContainerComponentHarness } from '@angular/cdk/testing';

import { OverflowLabelsHarness } from '../overflow-labels/overflow-labels.harness';

export class ApiProductCardHarness extends ContentContainerComponentHarness {
  static readonly hostSelector = 'app-api-product-card';

  private readonly locateCard = this.locatorFor('.api-product-card');
  private readonly locateTitle = this.locatorFor('[data-testid="api-product-card-title"]');
  private readonly locateDescription = this.locatorFor('.api-product-card__description');
  private readonly locateTypeBadge = this.locatorFor('[data-testid="api-product-type-badge"]');
  private readonly locateApiCount = this.locatorFor('[data-testid="api-product-api-count"]');
  private readonly locateApiLabels = this.locatorForOptional(OverflowLabelsHarness);

  async select(): Promise<void> {
    return (await this.locateCard()).click();
  }

  async getTitle(): Promise<string> {
    return (await this.locateTitle()).text();
  }

  async getDescription(): Promise<string> {
    return (await this.locateDescription()).text();
  }

  async getType(): Promise<string> {
    return (await this.locateTypeBadge()).text();
  }

  async getApiCount(): Promise<string> {
    return (await this.locateApiCount()).text();
  }

  async hasApiLabels(): Promise<boolean> {
    return (await this.locateApiLabels()) !== null;
  }
}
