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
import { ComponentHarness, parallel } from '@angular/cdk/testing';
import { DivHarness } from '@gravitee/ui-particles-angular/testing';

import { ApiAnalyticsRequestStatsHarness } from '../components/api-analytics-requests-stats/api-analytics-request-stats.component.harness';
import { ApiAnalyticsFiltersBarHarness } from '../components/api-analytics-filters-bar/api-analytics-filters-bar.component.harness';
import { ApiAnalyticsResponseStatusRangesHarness } from '../../../../../shared/components/api-analytics-response-status-ranges/api-analytics-response-status-ranges.component.harness';

export class ApiAnalyticsMessageHarness extends ComponentHarness {
  static hostSelector = 'api-analytics-message';

  protected emptyPanelHarness = this.locatorForOptional('gio-card-empty-state');
  protected loaderElement = this.locatorForOptional('.loader gio-loader');

  protected requestStats = (testId: string) =>
    this.locatorForOptional(ApiAnalyticsRequestStatsHarness.with({ selector: `[data-testId="${testId}"]` }));
  protected responseStatusRanges = (testId: string) =>
    this.locatorForOptional(ApiAnalyticsResponseStatusRangesHarness.with({ selector: `[data-testId="${testId}"]` }));

  protected entrypointsRows = this.locatorForAll(DivHarness.with({ selector: `.entrypoints__rows__row` }));

  getFiltersBarHarness = this.locatorForOptional(ApiAnalyticsFiltersBarHarness);

  async isEmptyPanelDisplayed(): Promise<boolean> {
    return (await this.emptyPanelHarness()) !== null;
  }

  async isLoaderDisplayed(): Promise<boolean> {
    return (await this.loaderElement()) !== null;
  }

  async getRequestStatsHarness(testId: string): Promise<ApiAnalyticsRequestStatsHarness> {
    return this.requestStats(testId)();
  }

  async getEntrypointRow(): Promise<
    {
      name: string;
      isNotConfigured: boolean;
    }[]
  > {
    const rows = await this.entrypointsRows();
    return parallel(() =>
      rows.map(async (row) => ({
        name: await row.getText({ childSelector: '.gio-badge-primary' }),
        isNotConfigured: (await row.getText()).includes('Not configured'),
      })),
    );
  }

  async getResponseStatusRangesHarness(testId: string): Promise<ApiAnalyticsResponseStatusRangesHarness> {
    return this.responseStatusRanges(testId)();
  }
}
