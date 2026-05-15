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

import { AnalyticsDashboardCardHarness } from '../../../components/analytics-dashboard-card/analytics-dashboard-card.harness';
import { LoaderHarness } from '../../../components/loader/loader.harness';

export class AnalyticsComponentHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-analytics';

  private readonly locateLoader = this.locatorForOptional(LoaderHarness);
  private readonly locateError = this.locatorForOptional('[data-testid="analytics-list-error"]');
  private readonly locateEmptyState = this.locatorForOptional('.cards-grid__empty-state');
  private readonly locateGridCards = this.locatorForAll(AnalyticsDashboardCardHarness.with({ ancestor: '.analytics-list__grid' }));
  private readonly locateTitle = this.locatorForOptional('.next-gen-h3');
  private readonly locatePinnedCards = this.locatorForAll(AnalyticsDashboardCardHarness.with({ ancestor: '.analytics-list__pinned-row' }));

  public async getLoader(): Promise<LoaderHarness | null> {
    return this.locateLoader();
  }

  public async getErrorMessage(): Promise<string | null> {
    const element = await this.locateError();
    return element ? element.text() : null;
  }

  public async getCards(): Promise<AnalyticsDashboardCardHarness[]> {
    return this.locateGridCards();
  }

  public async isEmptyStateDisplayed(): Promise<boolean> {
    return !!(await this.locateEmptyState());
  }

  public async getTitle(): Promise<string | null> {
    const element = await this.locateTitle();
    return element ? element.text() : null;
  }

  public async getPinnedDashboards(): Promise<AnalyticsDashboardCardHarness[]> {
    return this.locatePinnedCards();
  }
}
