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
import { BaseHarnessFilters, ComponentHarness, HarnessPredicate, parallel } from '@angular/cdk/testing';
import { DivHarness } from '@gravitee/ui-particles-angular/testing';

export type ApiAnalyticsRequestStatsHarnessFilters = BaseHarnessFilters & {
  title?: string | RegExp;
};
export class ApiAnalyticsRequestStatsHarness extends ComponentHarness {
  static hostSelector = 'app-api-analytics-request-stats';

  static with(options: ApiAnalyticsRequestStatsHarnessFilters = {}): HarnessPredicate<ApiAnalyticsRequestStatsHarness> {
    return new HarnessPredicate(ApiAnalyticsRequestStatsHarness, options).addOption('title', options.title, (harness, title) =>
      HarnessPredicate.stringMatches(harness.getTitle(), title),
    );
  }
  protected rows = this.locatorForAll(DivHarness.with({ selector: '.list__row' }));

  async getTitle(): Promise<string> {
    const el = await this.locatorForOptional('.title')();
    return el.text();
  }

  async getValues(): Promise<{ label: string; value: string; isLoading: boolean }[]> {
    const rows = await this.rows();

    return parallel(() => {
      return rows.map(async row => {
        return {
          label: await row.getText({ childSelector: '.list__row__label' }),
          value: await row.getText({ childSelector: '.list__row__value' }),
          isLoading: !!(await row.childLocatorForOptional('gio-loader')()),
        };
      });
    });
  }
}
