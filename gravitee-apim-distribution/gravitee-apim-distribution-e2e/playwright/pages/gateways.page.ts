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
import { Locator } from '@playwright/test';
import { BasePage } from './base.page';

export class GatewaysPage extends BasePage {
  get heading(): Locator {
    return this.page.getByRole('heading', { name: 'Gateways' });
  }

  get instanceRows(): Locator {
    return this.byTestId('instance-list-table-row');
  }

  get instanceDetailsLinks(): Locator {
    return this.byTestId('instance-list-row-instance-details-link');
  }

  get monitoringTab(): Locator {
    return this.byTestId('instances-detail-monitoring');
  }

  /** Detail sections rendered as plain text — the Console exposes no test ids for them. */
  section(name: 'Information' | 'Plugins' | 'System properties'): Locator {
    return this.page.getByText(name).first();
  }

  monitoringBox(box: 'jvm' | 'cpu' | 'process' | 'thread' | 'gc'): Locator {
    return this.byTestId(`instance-monitoring_${box}-box`);
  }

  async goto(): Promise<void> {
    await this.gotoHashRoute('DEFAULT/gateways');
  }

  async openFirstInstance(): Promise<void> {
    await this.instanceDetailsLinks.first().click();
  }

  async openMonitoring(): Promise<void> {
    await this.monitoringTab.click();
  }
}
