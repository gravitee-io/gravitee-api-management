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
import { AsyncFactoryFn, ComponentHarness, TestElement } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatRowHarness, MatTableHarness } from '@angular/material/table/testing';
import { MatPaginatorHarness } from '@angular/material/paginator/testing';

export class IntegrationOverviewHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-integration-overview';

  private badgeErrorLocator: AsyncFactoryFn<TestElement> = this.locatorForOptional('.gio-badge-error');
  private badgeSuccessLocator: AsyncFactoryFn<TestElement> = this.locatorForOptional('.gio-badge-success');
  private discoverButtonLocator: AsyncFactoryFn<MatButtonHarness> = this.locatorForOptional(
    MatButtonHarness.with({ selector: '[data-testid=discover-button]' }),
  );
  private errorBannerLocator = this.locatorForOptional('gio-banner-error');
  private jobPendingBanner = this.locatorForOptional('.pending-job-banner');

  public getTable = this.locatorForOptional(MatTableHarness);
  private getPaginationLocator = this.locatorForOptional(MatPaginatorHarness);

  public getErrorBadge = async (): Promise<TestElement> => {
    return this.badgeErrorLocator();
  };

  public getErrorBanner = async (): Promise<TestElement> => {
    return this.errorBannerLocator();
  };

  public getPendingJobBanner = async (): Promise<TestElement> => {
    return this.jobPendingBanner();
  };

  public getSuccessBadge = async (): Promise<TestElement> => {
    return this.badgeSuccessLocator();
  };

  public getDiscoverButton = async (): Promise<MatButtonHarness> => {
    return this.discoverButtonLocator();
  };

  public rowsNumber = async (): Promise<number> => {
    return this.getTable()
      .then((table: MatTableHarness) => table.getRows())
      .then((rows: MatRowHarness[]) => rows.length);
  };

  public getPagination = async (): Promise<MatPaginatorHarness> => {
    return await this.getPaginationLocator();
  };
}
