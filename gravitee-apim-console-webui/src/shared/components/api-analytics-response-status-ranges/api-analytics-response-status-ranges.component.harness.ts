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

import { GioChartPieHarness } from '../gio-chart-pie/gio-chart-pie.harness';

export type ApiAnalyticsResponseStatusRangesHarnessFilters = BaseHarnessFilters & {
  title?: string | RegExp;
};

export class ApiAnalyticsResponseStatusRangesHarness extends ComponentHarness {
  static hostSelector = 'api-analytics-response-status-ranges';

  protected chartPieHarness = this.locatorForOptional(GioChartPieHarness);

  static with(options: ApiAnalyticsResponseStatusRangesHarnessFilters = {}): HarnessPredicate<ApiAnalyticsResponseStatusRangesHarness> {
    return new HarnessPredicate(ApiAnalyticsResponseStatusRangesHarness, options).addOption('title', options.title, (harness, title) =>
      HarnessPredicate.stringMatches(harness.getTitle(), title),
    );
  }

  async getTitle(): Promise<string> {
    const el = await this.locatorForOptional('.title')();
    return el.text();
  }

  async hasResponseStatusWithValues(): Promise<boolean> {
    const chartPieHarness = await this.chartPieHarness();
    return chartPieHarness !== null && (await chartPieHarness.displaysChart());
  }
}
