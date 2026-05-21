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

import { LoaderHarness } from '../../../../components/loader/loader.harness';
import { PaginatedTableHarness } from '../../../../components/paginated-table/paginated-table.harness';

export class ApplicationTabInvitationsComponentHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-application-tab-invitations';

  private readonly locateLoader = this.locatorForOptional(LoaderHarness);
  private readonly locateSectionTitle = this.locatorForOptional('[data-testid="invitations-section-title"]');
  private readonly locateErrorMessage = this.locatorForOptional('[data-testid="invitations-list-error"]');
  private readonly locateEmptyState = this.locatorForOptional('[data-testid="invitations-empty-state"]');
  private readonly locatePaginatedTable = this.locatorForOptional(PaginatedTableHarness);

  public async getLoader(): Promise<LoaderHarness | null> {
    return this.locateLoader();
  }

  public async getSectionTitle(): Promise<TestElement | null> {
    return this.locateSectionTitle();
  }

  public async getErrorMessage(): Promise<TestElement | null> {
    return this.locateErrorMessage();
  }

  public async getEmptyState(): Promise<TestElement | null> {
    return this.locateEmptyState();
  }

  public async getPaginatedTable(): Promise<PaginatedTableHarness | null> {
    return this.locatePaginatedTable();
  }
}
