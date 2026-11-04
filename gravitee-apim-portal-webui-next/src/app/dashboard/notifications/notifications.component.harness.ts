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
import { ComponentHarness, HarnessPredicate, TestElement } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { DropdownSearchComponentHarness } from '../../../components/dropdown-search/dropdown-search.component.harness';
import { PaginationHarness } from '../../../components/pagination/pagination.harness';

// Clicks that start an HTTP request must not wait for stability: harness stabilization waits for the
// pending request, which the test can only flush after the click returns.
function clickWithoutStabilizing(element: TestElement): void {
  (TestbedHarnessEnvironment.getNativeElement(element) as HTMLElement).click();
}

export class NotificationRowHarness extends ComponentHarness {
  public static hostSelector = '.paginated-table__row';

  private readonly locateDate = this.locatorFor('[data-testid="notification-date"]');
  private readonly locateType = this.locatorFor('[data-testid="notification-type"]');
  private readonly locateApi = this.locatorForOptional('[data-testid="notification-api"]');
  private readonly locateApplication = this.locatorForOptional('[data-testid="notification-application"]');
  private readonly locateMessage = this.locatorFor('[data-testid="notification-message"]');
  private readonly locateMarkAsRead = this.locatorForOptional('[data-testid="paginated-table-action-read"]');

  public async getDate(): Promise<string> {
    return (await this.locateDate()).text();
  }

  public async getType(): Promise<string> {
    return (await this.locateType()).text();
  }

  /** API name followed by its badge, e.g. "My API API"; null when the notification is about no API. */
  public async getApi(): Promise<string | null> {
    return (await this.locateApi())?.text() ?? null;
  }

  /** Application name followed by its badge, e.g. "My app App"; null when the notification is about no application. */
  public async getApplication(): Promise<string | null> {
    return (await this.locateApplication())?.text() ?? null;
  }

  public async getMessage(): Promise<string> {
    return (await this.locateMessage()).text({ exclude: '.cdk-visually-hidden' });
  }

  /** Unread rows are rendered in bold. */
  public async isUnread(): Promise<boolean> {
    return (await this.locateType()).hasClass('next-gen-small-bold');
  }

  public async hasMarkAsReadAction(): Promise<boolean> {
    return (await this.locateMarkAsRead()) !== null;
  }

  public async markAsReadWithoutStabilizing(): Promise<void> {
    const button = await this.locateMarkAsRead();
    if (!button) {
      throw new Error('Mark as read action not found');
    }
    clickWithoutStabilizing(button);
  }
}

export class NotificationsComponentHarness extends ComponentHarness {
  public static hostSelector = 'app-notifications';

  private readonly locateDescription = this.locatorFor('[data-testid="notifications-description"]');
  private readonly locateEmptyState = this.locatorForOptional('[data-testid="notifications-empty-state"]');
  private readonly locateNoUnread = this.locatorForOptional('[data-testid="notifications-no-unread"]');
  private readonly locateNoMatch = this.locatorForOptional('[data-testid="notifications-no-match"]');
  private readonly locateLoadError = this.locatorForOptional('[data-testid="notifications-load-error"]');
  private readonly locateFiltersError = this.locatorForOptional('[data-testid="notifications-filters-error"]');
  private readonly locateActionError = this.locatorForOptional('[data-testid="notifications-action-error"]');
  private readonly locateMarkAllAsRead = this.locatorForOptional('[data-testid="notifications-mark-all-as-read"]');
  private readonly locateClearFilters = this.locatorFor('[data-testid="notifications-clear-filters"]');
  private readonly locatePagination = this.locatorFor(PaginationHarness);
  private readonly locateRows = this.locatorForAll(NotificationRowHarness);

  public async getDescription(): Promise<string> {
    return (await this.locateDescription()).text();
  }

  public async isEmptyStateDisplayed(): Promise<boolean> {
    return (await this.locateEmptyState()) !== null;
  }

  public async isNoUnreadStateDisplayed(): Promise<boolean> {
    return (await this.locateNoUnread()) !== null;
  }

  public async isNoMatchStateDisplayed(): Promise<boolean> {
    return (await this.locateNoMatch()) !== null;
  }

  public async hasPageButton(pageNumber: number): Promise<boolean> {
    try {
      await (await this.locatePagination()).getPageButtonByNumber(pageNumber);
      return true;
    } catch {
      return false;
    }
  }

  public async getLoadErrorText(): Promise<string | null> {
    return (await this.locateLoadError())?.text() ?? null;
  }

  public async getFiltersErrorText(): Promise<string | null> {
    return (await this.locateFiltersError())?.text() ?? null;
  }

  public async goToPageWithoutStabilizing(pageNumber: number): Promise<void> {
    const pageButton = await (await this.locatePagination()).getPageButtonByNumber(pageNumber);
    clickWithoutStabilizing(await pageButton.host());
  }

  public async changePageSizeWithoutStabilizing(size: number): Promise<void> {
    const select = await (await this.locatePagination()).getPageSizeSelect();
    if (!select) {
      throw new Error('Page size select not found');
    }
    await select.open();
    const options = await select.getOptions({ text: String(size) });
    if (options.length === 0) {
      throw new Error(`Page size option ${size} not found`);
    }
    clickWithoutStabilizing(await options[0].host());
  }

  public async getActionErrorText(): Promise<string | null> {
    return (await this.locateActionError())?.text() ?? null;
  }

  public async getRows(): Promise<NotificationRowHarness[]> {
    return this.locateRows();
  }

  public async getApiFilter(): Promise<DropdownSearchComponentHarness> {
    return this.locatorFor(this.dropdownWithSelector('[data-testid="notifications-api-filter"]'))();
  }

  public async getApplicationFilter(): Promise<DropdownSearchComponentHarness> {
    return this.locatorFor(this.dropdownWithSelector('[data-testid="notifications-application-filter"]'))();
  }

  public async getStatusFilter(): Promise<DropdownSearchComponentHarness> {
    return this.locatorFor(this.dropdownWithSelector('[data-testid="notifications-status-filter"]'))();
  }

  /** Opens the dropdown and picks the option with the given label, without waiting for the request it triggers. */
  public async selectFilterOptionWithoutStabilizing(filter: DropdownSearchComponentHarness, label: string): Promise<void> {
    await filter.open();
    const options = await this.documentRootLocatorFactory().locatorForAll('.portal-dropdown-search-overlay__option')();
    for (const option of options) {
      if ((await option.text()).trim() === label) {
        clickWithoutStabilizing(option);
        return;
      }
    }
    throw new Error(`Option with label "${label}" not found`);
  }

  public async clearFiltersWithoutStabilizing(): Promise<void> {
    clickWithoutStabilizing(await this.locateClearFilters());
  }

  public async isMarkAllAsReadDisplayed(): Promise<boolean> {
    return (await this.locateMarkAllAsRead()) !== null;
  }

  public async markAllAsReadWithoutStabilizing(): Promise<void> {
    const button = await this.locateMarkAllAsRead();
    if (!button) {
      throw new Error('Mark all as read button not found');
    }
    clickWithoutStabilizing(button);
  }

  private dropdownWithSelector(selector: string): HarnessPredicate<DropdownSearchComponentHarness> {
    return new HarnessPredicate(DropdownSearchComponentHarness, { selector });
  }
}
