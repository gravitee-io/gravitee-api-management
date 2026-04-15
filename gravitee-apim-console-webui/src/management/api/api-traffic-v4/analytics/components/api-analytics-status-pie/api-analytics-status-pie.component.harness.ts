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

import { GioChartPieHarness } from '../../../../../../shared/components/gio-chart-pie/gio-chart-pie.harness';

export type ApiAnalyticsStatusPieHarnessFilters = BaseHarnessFilters;

export class ApiAnalyticsStatusPieHarness extends ComponentHarness {
  static hostSelector = 'api-analytics-status-pie';

  static with(options: ApiAnalyticsStatusPieHarnessFilters = {}): HarnessPredicate<ApiAnalyticsStatusPieHarness> {
    return new HarnessPredicate(ApiAnalyticsStatusPieHarness, options);
  }

  private readonly chartPieHarness = this.locatorForOptional(GioChartPieHarness);

  async isLoading(): Promise<boolean> {
    return !!(await this.locatorForOptional('gio-loader')());
  }

  async isChartDisplayed(): Promise<boolean> {
    const harness = await this.chartPieHarness();
    return harness !== null && (await harness.displaysChart());
  }

  async isEmptyStateDisplayed(): Promise<boolean> {
    return !!(await this.locatorForOptional('[data-testid="empty-state"]')());
  }

  async isErrorDisplayed(): Promise<boolean> {
    return !!(await this.locatorForOptional('[data-testid="error-card"]')());
  }
}
