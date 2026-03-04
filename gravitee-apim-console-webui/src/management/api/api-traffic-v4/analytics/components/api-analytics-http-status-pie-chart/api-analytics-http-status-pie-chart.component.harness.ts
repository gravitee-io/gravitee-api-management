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

import { GioChartPieHarness } from '../../../../../../shared/components/gio-chart-pie/gio-chart-pie.harness';

export class ApiAnalyticsHttpStatusPieChartHarness extends ComponentHarness {
  static hostSelector = 'api-analytics-http-status-pie-chart';

  protected loaderElement = this.locatorForOptional('gio-loader');
  protected emptyStateElement = this.locatorForOptional('gio-card-empty-state');
  protected chartPieHarness = this.locatorForOptional(GioChartPieHarness);

  async isLoaderDisplayed(): Promise<boolean> {
    return (await this.loaderElement()) !== null;
  }

  async isEmptyStateDisplayed(): Promise<boolean> {
    return (await this.emptyStateElement()) !== null;
  }

  async isChartDisplayed(): Promise<boolean> {
    const chart = await this.chartPieHarness();
    return chart !== null && (await chart.displaysChart());
  }
}
