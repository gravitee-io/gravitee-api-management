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
import { DivHarness } from '@gravitee/ui-particles-angular/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

import { GioSelectSearchHarness } from '../../../../shared/components/gio-select-search/gio-select-search.harness';

export class ApiRuntimeLogsNativeHarness extends ComponentHarness {
  static hostSelector = 'api-runtime-logs-native';

  public reportingDisabledBanner = this.locatorForOptional(
    DivHarness.with({ selector: '[data-testid=native_logs_reporting_disabled_banner]' }),
  );
  public emptyState = this.locatorForOptional(DivHarness.with({ selector: '.table__empty-state' }));
  public configureReportingLink = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid=native_logs_configure_reporting]' }));

  public connectionStatusFilter = this.locatorFor(GioSelectSearchHarness.with({ formControlName: 'connectionStatuses' }));

  async isReportingDisabledBannerVisible(): Promise<boolean> {
    return this.reportingDisabledBanner().then(banner => banner != null);
  }

  async getEmptyStateText(): Promise<string | null> {
    return this.emptyState().then(div => (div ? div.getText() : null));
  }

  async getConfigureReportingLabel(): Promise<string> {
    return this.configureReportingLink().then(link => link.getText());
  }
}
