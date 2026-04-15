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
import { BaseHarnessFilters, ComponentHarness, HarnessPredicate } from '@angular/cdk/testing';

export type ApiAnalyticsStatsCardsHarnessFilters = BaseHarnessFilters;

export class ApiAnalyticsStatsCardsHarness extends ComponentHarness {
  static hostSelector = 'api-analytics-stats-cards';

  static with(options: ApiAnalyticsStatsCardsHarnessFilters = {}): HarnessPredicate<ApiAnalyticsStatsCardsHarness> {
    return new HarnessPredicate(ApiAnalyticsStatsCardsHarness, options);
  }

  async getCardCount(): Promise<number> {
    return (await this.locatorForAll('.stat-card__content')()).length;
  }

  async getCardValue(label: string): Promise<string | null> {
    const valueEl = await this.locatorForOptional(`[data-testid="stat-card-${label}"] .stat-card__value`)();
    if (!valueEl) return null;
    return (await valueEl.text()).trim();
  }

  async isCardLoading(label: string): Promise<boolean> {
    return !!(await this.locatorForOptional(`[data-testid="stat-card-${label}"] gio-loader`)());
  }

  async isCardEmpty(label: string): Promise<boolean> {
    return !!(await this.locatorForOptional(`[data-testid="stat-card-${label}"] .stat-card__empty`)());
  }

  async isCardError(label: string): Promise<boolean> {
    return !!(await this.locatorForOptional(`[data-testid="stat-card-error-${label}"]`)());
  }
}
