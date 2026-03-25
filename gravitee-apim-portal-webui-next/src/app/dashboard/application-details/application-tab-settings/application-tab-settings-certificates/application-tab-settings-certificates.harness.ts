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

export class ApplicationTabSettingsCertificatesHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-application-tab-settings-certificates';

  getEmptyState = this.locatorForOptional('.certificates__empty');
  getPaginatedTable = this.locatorForOptional('app-paginated-table');
  getErrorMessage = this.locatorForOptional('.certificates__error');
  getTabButtons = this.locatorForAll('.certificates__tabs__tab');
  getActiveTabButton = this.locatorForOptional('.certificates__tabs__tab--active');

  async clickTab(label: 'active' | 'history'): Promise<void> {
    const tabs = await this.getTabButtons();
    const index = label === 'active' ? 0 : 1;
    await tabs[index].click();
  }
}
