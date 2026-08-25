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

export class ApiProductSubscriptionDetailsHarness extends ComponentHarness {
  static hostSelector = 'app-api-product-subscription-details';

  private readonly summary = this.locatorFor('[data-testid="product-subscription-summary"]');
  private readonly credentials = this.locatorFor('[data-testid="product-subscription-credentials"]');
  private readonly apiCards = this.locatorForAll('app-api-product-subscription-api-access');
  private readonly applicationLink = this.locatorForOptional('a[href*="/dashboard/applications/"]');
  private readonly pauseButton = this.locatorForOptional(MatButtonHarness.with({ selector: '[data-testid="pause-subscription"]' }));
  private readonly resumeButton = this.locatorForOptional(MatButtonHarness.with({ selector: '[data-testid="resume-subscription"]' }));
  private readonly retryButton = this.locatorForOptional(MatButtonHarness.with({ selector: '[data-testid="retry-subscription"]' }));
  private readonly closeButton = this.locatorForOptional(MatButtonHarness.with({ selector: '[data-testid="close-subscription"]' }));
  private readonly feedback = this.locatorForOptional('[data-testid="subscription-action-feedback"]');
  private readonly renewApiKeyButton = this.locatorForOptional(MatButtonHarness.with({ selector: '[data-testid="renew-api-key-button"]' }));
  private readonly apiKeyFeedback = this.locatorForOptional('[data-testid="api-key-feedback"]');

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

  async getPauseButton(): Promise<MatButtonHarness | null> {
    return this.pauseButton();
  }

  async getResumeButton(): Promise<MatButtonHarness | null> {
    return this.resumeButton();
  }

  async getRetryButton(): Promise<MatButtonHarness | null> {
    return this.retryButton();
  }

  async getCloseButton(): Promise<MatButtonHarness | null> {
    return this.closeButton();
  }

  async getFeedbackText(): Promise<string | null> {
    return (await this.feedback())?.text() ?? null;
  }

  async getFeedbackAttribute(attribute: string): Promise<string | null> {
    return (await this.feedback())?.getAttribute(attribute) ?? null;
  }

  async hasRenewApiKeyButton(): Promise<boolean> {
    return !!(await this.renewApiKeyButton());
  }

  async clickRenewApiKey(): Promise<void> {
    await (await this.renewApiKeyButton())?.click();
  }

  async isRenewApiKeyButtonDisabled(): Promise<boolean | null> {
    return (await this.renewApiKeyButton())?.isDisabled() ?? null;
  }

  async getApiKeyFeedbackText(): Promise<string | null> {
    return (await this.apiKeyFeedback())?.text() ?? null;
  }

  async getApiKeyFeedbackAttribute(attribute: string): Promise<string | null> {
    return (await this.apiKeyFeedback())?.getAttribute(attribute) ?? null;
  }
}
