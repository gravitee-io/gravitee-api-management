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

import { ApiAnalyticsProxyFilterBarHarness } from '../components/api-analytics-proxy-filter-bar/api-analytics-proxy-filter-bar.harness';
import { ApiAnalyticsWidgetHarness } from '../components/api-analytics-widget/api-analytics-widget.harness';

export class ApiAnalyticsProxyHarness extends ComponentHarness {
  static hostSelector = 'api-analytics-proxy';

  protected emptyPanelHarness = this.locatorForOptional('gio-card-empty-state');

  getFiltersBarHarness = this.locatorForOptional(ApiAnalyticsProxyFilterBarHarness);
  protected getWidgets = this.locatorForAll(ApiAnalyticsWidgetHarness);

  async getWidgetByTitle(title: string): Promise<ApiAnalyticsWidgetHarness> {
    for (const widget of await this.getWidgets()) {
      if ((await widget.getTitleText()) === title) {
        return widget;
      }
    }
    throw new Error(`No widget titled "${title}"`);
  }

  async isEmptyPanelDisplayed(): Promise<boolean> {
    return (await this.emptyPanelHarness()) !== null;
  }
}
