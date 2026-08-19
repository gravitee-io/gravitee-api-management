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

export class ApiProductSubscriptionDetailsHarness extends ComponentHarness {
  static hostSelector = 'app-api-product-subscription-details';

  private readonly summary = this.locatorFor('[data-testid="product-subscription-summary"]');
  private readonly credentials = this.locatorFor('[data-testid="product-subscription-credentials"]');
  private readonly apiCards = this.locatorForAll('app-api-product-subscription-api-access');
  private readonly applicationLink = this.locatorForOptional('a[href*="/dashboard/applications/"]');

  async getSummaryText(): Promise<string> {
    return (await this.summary()).text();
  }

  async getCredentialsText(): Promise<string> {
    return (await this.credentials()).text();
  }

  async getApiCount(): Promise<number> {
    return (await this.apiCards()).length;
  }

  async getApplicationLink(): Promise<string | null> {
    return (await this.applicationLink())?.getAttribute('href') ?? null;
  }
}
