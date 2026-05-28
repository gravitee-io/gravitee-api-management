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

import { LoaderHarness } from '../../../../components/loader/loader.harness';

export class AnalyticsDetailsHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-analytics-details';

  private readonly locateLoader = this.locatorForOptional(LoaderHarness);
  private readonly locateDashboard = this.locatorForOptional('gd-dashboard');
  private readonly locateError = this.locatorForOptional('[data-testid="analytics-details-error"]');
  private readonly locateTimeframeSelector = this.locatorForOptional('[data-testid="analytics-details-timeframe"]');
  private readonly locateFilterBar = this.locatorForOptional('[data-testid="analytics-details-filters"]');

  public async getLoader(): Promise<LoaderHarness | null> {
    return this.locateLoader();
  }

  public async isDashboardDisplayed(): Promise<boolean> {
    return !!(await this.locateDashboard());
  }

  public async isTimeframeSelectorDisplayed(): Promise<boolean> {
    return !!(await this.locateTimeframeSelector());
  }

  public async getErrorMessage(): Promise<string | null> {
    const error = await this.locateError();
    return error ? error.text() : null;
  }

  public async isFilterBarDisplayed(): Promise<boolean> {
    return !!(await this.locateFilterBar());
  }
}
