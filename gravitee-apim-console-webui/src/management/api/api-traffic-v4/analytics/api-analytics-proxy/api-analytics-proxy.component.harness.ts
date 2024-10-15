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

import { ApiAnalyticsRequestStatsHarness } from '../components/api-analytics-requests-stats/api-analytics-request-stats.component.harness';
import { ApiAnalyticsFiltersBarHarness } from '../components/api-analytics-filters-bar/api-analytics-filters-bar.component.harness';
import { ApiAnalyticsResponseStatusRangesHarness } from '../../../../../shared/components/api-analytics-response-status-ranges/api-analytics-response-status-ranges.component.harness';

export class ApiAnalyticsProxyHarness extends ComponentHarness {
  static hostSelector = 'api-analytics-proxy';

  protected emptyPanelHarness = this.locatorForOptional('gio-card-empty-state');
  protected loaderElement = this.locatorForOptional('.loader gio-loader');
  protected requestStats = (title: string) => this.locatorForOptional(ApiAnalyticsRequestStatsHarness.with({ title }));
  protected responseStatusRanges = (title: string) => this.locatorForOptional(ApiAnalyticsResponseStatusRangesHarness.with({ title }));

  getFiltersBarHarness = this.locatorForOptional(ApiAnalyticsFiltersBarHarness);

  async isEmptyPanelDisplayed(): Promise<boolean> {
    return (await this.emptyPanelHarness()) !== null;
  }

  async isLoaderDisplayed(): Promise<boolean> {
    return (await this.loaderElement()) !== null;
  }

  async getRequestStatsHarness(title: string): Promise<ApiAnalyticsRequestStatsHarness> {
    return this.requestStats(title)();
  }

  async getResponseStatusRangesHarness(title: string): Promise<ApiAnalyticsResponseStatusRangesHarness> {
    return this.responseStatusRanges(title)();
  }
}
