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
import { MatLegacyPaginatorHarness as MatPaginatorHarness } from '@angular/material/legacy-paginator/testing';
import { MatLegacyButtonHarness as MatButtonHarness } from '@angular/material/legacy-button/testing';
import { DivHarness } from '@gravitee/ui-particles-angular/testing';

import { ApiRuntimeLogsEmptyHarness } from '../api-runtime-logs-empty';
import { ApiRuntimeLogsListRowHarness } from '../api-runtime-logs-list-row';

export class ApiRuntimeLogsListHarness extends ComponentHarness {
  static hostSelector = 'api-runtime-logs-list';

  private emptyPanel = this.locatorForOptional(ApiRuntimeLogsEmptyHarness);
  private impactBanner = this.locatorForOptional(DivHarness.with({ selector: '.banner' }));
  private openSettingsButtonInBanner = this.locatorFor(MatButtonHarness.with({ selector: '[data-testId=banner-open-settings-button]' }));

  public paginator = this.locatorForOptional(MatPaginatorHarness);
  public rows = this.locatorForAll(ApiRuntimeLogsListRowHarness);

  async isEmptyPanelDisplayed(): Promise<boolean> {
    return (await this.emptyPanel()) !== null;
  }

  async isImpactBannerDisplayed(): Promise<boolean> {
    return (await this.impactBanner()) !== null;
  }

  async clickOpenSettings(): Promise<void> {
    const panel = await this.emptyPanel();
    const impactBanner = await this.impactBanner();

    if (panel != null) {
      return panel.openSettingsButton().then((button) => button.click());
    }
    if (impactBanner != null) {
      return this.openSettingsButtonInBanner().then((button) => button.click());
    }
  }
}
