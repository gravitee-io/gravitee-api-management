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
import { ComponentHarness, TestElement } from '@angular/cdk/testing';

export class ApplicationTabSettingsCertificatesHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-application-tab-settings-certificates';

  protected locateEmptyState = this.locatorForOptional('.certificates__empty');
  protected locatePaginatedTable = this.locatorForOptional('app-paginated-table');
  protected locateErrorMessage = this.locatorForOptional('.certificates__error');
  protected locateTabButtons = this.locatorForAll('.certificates__tabs__tab');
  protected locateActiveTabButton = this.locatorForOptional('.certificates__tabs__tab--active');
  protected locateUploadButton = this.locatorForOptional('[data-testid="upload-certificate-button"]');

  public async getEmptyState(): Promise<TestElement | null> {
    return this.locateEmptyState();
  }

  public async getPaginatedTable(): Promise<TestElement | null> {
    return this.locatePaginatedTable();
  }

  public async getErrorMessage(): Promise<TestElement | null> {
    return this.locateErrorMessage();
  }

  public async getActiveTabButton(): Promise<TestElement | null> {
    return this.locateActiveTabButton();
  }

  public async getUploadButton(): Promise<TestElement | null> {
    return this.locateUploadButton();
  }

  public async clickUploadButton(): Promise<void> {
    const btn = await this.locateUploadButton();
    await btn?.click();
  }

  public async clickTab(label: 'active' | 'history'): Promise<void> {
    const tabs = await this.locateTabButtons();
    const index = label === 'active' ? 0 : 1;
    await tabs[index].click();
  }
}
