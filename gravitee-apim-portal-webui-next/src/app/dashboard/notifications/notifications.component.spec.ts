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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpRequest, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import NotificationsComponent from './notifications.component';
import { NotificationsComponentHarness } from './notifications.component.harness';
import {
  fakePortalNotification,
  fakePortalNotificationFilters,
  fakePortalNotificationsResponse,
  PortalNotificationFilters,
  PortalNotificationsResponse,
} from '../../../entities/notification';
import { ConfigService } from '../../../services/config.service';
import { TESTING_BASE_URL } from '../../../testing/app-testing.module';

const NOTIFICATIONS_URL = `${TESTING_BASE_URL}/user/notifications`;

describe('NotificationsComponent', () => {
  let routerHarness: RouterTestingHarness;
  let harness: NotificationsComponentHarness;
  let http: HttpTestingController;

  const isListRequest = (request: HttpRequest<unknown>) => request.method === 'GET' && request.url === NOTIFICATIONS_URL;

  async function init(
    params: {
      url?: string;
      notifications?: PortalNotificationsResponse;
      filters?: PortalNotificationFilters;
      listFails?: boolean;
      listFailureStatus?: number;
      filtersFail?: boolean;
    } = {},
  ): Promise<TestRequest> {
    const {
      url = '/notifications',
      notifications = fakePortalNotificationsResponse(),
      filters = fakePortalNotificationFilters(),
      listFails = false,
      listFailureStatus = 500,
      filtersFail = false,
    } = params;
    await TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([{ path: 'notifications', component: NotificationsComponent }]),
        { provide: ConfigService, useValue: { baseURL: TESTING_BASE_URL } },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);

    routerHarness = await RouterTestingHarness.create();
    await routerHarness.navigateByUrl(url);
    const filtersRequest = await expectRequest(request => request.url === `${NOTIFICATIONS_URL}/_filters`);
    if (filtersFail) {
      filtersRequest.flush(null, { status: 500, statusText: 'Server Error' });
    } else {
      filtersRequest.flush(filters);
    }
    const listRequest = await expectRequest(isListRequest);
    if (listFails) {
      listRequest.flush(null, { status: listFailureStatus, statusText: 'Error' });
    } else {
      listRequest.flush(notifications);
    }
    // A 400 sends the page back to page 1: stabilizing would wait for that new request, which the test flushes itself.
    if (!(listFails && listFailureStatus === 400)) {
      await settle();
    }
    harness = await TestbedHarnessEnvironment.harnessForFixture(routerHarness.fixture, NotificationsComponentHarness);
    return listRequest;
  }

  /** Runs change detection until a request matching the predicate is sent, as navigation and resources settle asynchronously. */
  async function expectRequest(predicate: (request: HttpRequest<unknown>) => boolean): Promise<TestRequest> {
    for (let attempt = 0; attempt < 50; attempt++) {
      routerHarness.detectChanges();
      const requests = http.match(predicate).filter(request => !request.cancelled);
      if (requests.length > 0) {
        return requests[requests.length - 1];
      }
      await new Promise(resolve => setTimeout(resolve, 0));
    }
    throw new Error('Expected request was not sent');
  }

  async function flushList(response: PortalNotificationsResponse = fakePortalNotificationsResponse()): Promise<TestRequest> {
    const request = await expectRequest(isListRequest);
    request.flush(response);
    await settle();
    return request;
  }

  async function settle(): Promise<void> {
    await routerHarness.fixture.whenStable();
    routerHarness.detectChanges();
  }

  function currentQueryParams(): Record<string, string> {
    return TestBed.inject(Router).parseUrl(TestBed.inject(Router).url).queryParams;
  }

  afterEach(() => {
    http.verify();
  });

  describe('table', () => {
    it('should describe the page', async () => {
      await init();

      expect(await harness.getDescription()).toBe('API and application notifications for your account');
    });

    it('should display one row per notification with date, API and application, type and message', async () => {
      await init({
        notifications: fakePortalNotificationsResponse([
          fakePortalNotification({ id: 'n-1', message: 'Your subscription was accepted.', hook: 'SUBSCRIPTION_ACCEPTED', read: false }),
          fakePortalNotification({
            id: 'n-2',
            message: 'Old message',
            hook: undefined,
            api: undefined,
            application: undefined,
            read: true,
            created_at: '2026-10-29T08:30:15Z',
          }),
        ]),
      });

      const rows = await harness.getRows();
      expect(rows).toHaveLength(2);

      expect(await rows[0].getApi()).toContain('My API');
      expect(await rows[0].getApplication()).toContain('My app');
      expect(await rows[0].getType()).toBe('Subscription accepted');
      expect(await rows[0].getMessage()).toBe('Your subscription was accepted.');

      expect(await rows[1].getDate()).toMatch(/\d{1,2}:\d{2}:\d{2}/);
      expect(await rows[1].getType()).toBe('Notification');
      expect(await rows[1].getApi()).toBeNull();
      expect(await rows[1].getApplication()).toBeNull();
    });

    it('should badge the API and the application of a notification', async () => {
      await init({
        notifications: fakePortalNotificationsResponse([
          fakePortalNotification({ id: 'api-only', hook: 'API_STOPPED', application: undefined }),
        ]),
      });

      const [row] = await harness.getRows();
      expect(await row.getApi()).toMatch(/My API\s*API/);
      expect(await row.getApplication()).toBeNull();
    });

    it('should render unread rows in bold and read rows in regular weight', async () => {
      await init({
        notifications: fakePortalNotificationsResponse([
          fakePortalNotification({ id: 'unread', read: false }),
          fakePortalNotification({ id: 'read', read: true }),
        ]),
      });

      const rows = await harness.getRows();
      expect(await rows[0].isUnread()).toBe(true);
      expect(await rows[1].isUnread()).toBe(false);
    });

    it('should label a deleted API and a deleted application', async () => {
      await init({
        notifications: fakePortalNotificationsResponse([
          fakePortalNotification({ api: { id: 'api-gone', name: null }, application: { id: 'app-gone', name: null } }),
        ]),
        filters: { apis: [{ id: 'api-gone', name: null }], applications: [{ id: 'app-gone', name: null }] },
      });

      const [row] = await harness.getRows();
      expect(await row.getApi()).toContain('Deleted API');
      expect(await row.getApplication()).toContain('Deleted application');
      const apiFilter = await harness.getApiFilter();
      expect(await apiFilter.getOptionsLabels()).toEqual(['Deleted API']);
      await apiFilter.close();
      expect(await (await harness.getApplicationFilter()).getOptionsLabels()).toEqual(['Deleted application']);
    });

    it('should request the first page of 10 unread notifications by default', async () => {
      const request = await init();

      expect(request.request.params.get('page')).toBe('1');
      expect(request.request.params.get('size')).toBe('10');
      expect(request.request.params.get('read')).toBe('false');
      expect(await (await harness.getStatusFilter()).getTriggerText()).toContain('Unread');
    });

    it('should request the page and size found in the URL', async () => {
      const request = await init({ url: '/notifications?page=2&size=20' });

      expect(request.request.params.get('page')).toBe('2');
      expect(request.request.params.get('size')).toBe('20');
    });

    it('should paginate with the server total', async () => {
      await init({ notifications: fakePortalNotificationsResponse([fakePortalNotification()], 25) });

      expect(await harness.hasPageButton(3)).toBe(true);
      expect(await harness.hasPageButton(4)).toBe(false);
    });

    it('should keep the page in the URL when changing page', async () => {
      await init({ notifications: fakePortalNotificationsResponse([fakePortalNotification()], 25) });

      await harness.goToPageWithoutStabilizing(2);
      const request = await flushList();

      expect(request.request.params.get('page')).toBe('2');
      expect(currentQueryParams()).toEqual(expect.objectContaining({ page: '2' }));
    });

    it('should go back to the first page when changing the page size', async () => {
      await init({ url: '/notifications?page=2', notifications: fakePortalNotificationsResponse([fakePortalNotification()], 25) });

      await harness.changePageSizeWithoutStabilizing(20);
      const request = await flushList();

      expect(request.request.params.get('size')).toBe('20');
      expect(request.request.params.get('page')).toBe('1');
      expect(currentQueryParams()).toEqual(expect.objectContaining({ page: '1', size: '20' }));
    });

    it('should show the error, not loop, when the first page itself is refused', async () => {
      await init({ listFails: true, listFailureStatus: 400 });
      await settle();

      expect(await harness.getLoadErrorText()).toContain('An error occurred while loading notifications');
      http.expectNone(isListRequest);
    });

    it('should go back to the first page when the page in the URL is past the last one', async () => {
      await init({ url: '/notifications?page=3', listFails: true, listFailureStatus: 400 });

      const request = await flushList();

      expect(request.request.params.get('page')).toBe('1');
      expect(currentQueryParams()).toEqual(expect.objectContaining({ page: '1' }));
      expect(await harness.getLoadErrorText()).toBeNull();
    });
  });

  describe('filters', () => {
    it('should send apiId when an API is selected and keep it in the URL', async () => {
      await init({ filters: fakePortalNotificationFilters({ apis: [{ id: 'api-1', name: 'My API' }] }) });

      await harness.selectFilterOptionWithoutStabilizing(await harness.getApiFilter(), 'My API');
      const request = await flushList();

      expect(request.request.params.get('apiId')).toBe('api-1');
      expect(request.request.params.get('page')).toBe('1');
      expect(currentQueryParams()).toEqual(expect.objectContaining({ apiId: 'api-1', page: '1' }));
    });

    it('should send applicationId when an application is selected and keep it in the URL', async () => {
      await init();

      await harness.selectFilterOptionWithoutStabilizing(await harness.getApplicationFilter(), 'My app');
      const request = await flushList();

      expect(request.request.params.get('applicationId')).toBe('app-1');
      expect(currentQueryParams()).toEqual(expect.objectContaining({ applicationId: 'app-1' }));
    });

    it('should send read=true and keep status=read in the URL when the Read status is selected', async () => {
      await init();

      await harness.selectFilterOptionWithoutStabilizing(await harness.getStatusFilter(), 'Read');
      const request = await flushList();

      expect(request.request.params.get('read')).toBe('true');
      expect(currentQueryParams()).toEqual(expect.objectContaining({ status: 'read' }));
    });

    it('should not send read and keep status=all in the URL when the All status is selected', async () => {
      await init();

      await harness.selectFilterOptionWithoutStabilizing(await harness.getStatusFilter(), 'All');
      const request = await flushList();

      expect(request.request.params.has('read')).toBe(false);
      expect(currentQueryParams()).toEqual(expect.objectContaining({ status: 'all' }));
    });

    it('should go back to unread, the default, without a status in the URL', async () => {
      await init({ url: '/notifications?status=all' });

      await harness.selectFilterOptionWithoutStabilizing(await harness.getStatusFilter(), 'Unread');
      const request = await flushList();

      expect(request.request.params.get('read')).toBe('false');
      expect(currentQueryParams()['status']).toBeUndefined();
    });

    it('should apply the filters found in the URL on load', async () => {
      const request = await init({ url: '/notifications?apiId=api-1&applicationId=app-1&status=read&page=2' });

      expect(request.request.params.get('apiId')).toBe('api-1');
      expect(request.request.params.get('applicationId')).toBe('app-1');
      expect(request.request.params.get('read')).toBe('true');
      expect(request.request.params.get('page')).toBe('2');
      expect(await (await harness.getApiFilter()).getTriggerText()).toContain('My API');
      expect(await (await harness.getApplicationFilter()).getTriggerText()).toContain('My app');
      expect(await (await harness.getStatusFilter()).getTriggerText()).toContain('Read');
    });

    it('should reset API and application filters and go back to unread when clearing filters', async () => {
      await init({ url: '/notifications?apiId=api-1&applicationId=app-1&status=read&page=2' });

      await harness.clearFiltersWithoutStabilizing();
      const request = await flushList();

      expect(request.request.params.has('apiId')).toBe(false);
      expect(request.request.params.has('applicationId')).toBe(false);
      expect(request.request.params.get('read')).toBe('false');
      expect(request.request.params.get('page')).toBe('1');
      expect(currentQueryParams()).toEqual({ page: '1' });
    });
  });

  describe('reading a notification', () => {
    it('should mark an unread notification as read from its row action without reloading the list', async () => {
      await init({
        url: '/notifications?status=all',
        notifications: fakePortalNotificationsResponse([fakePortalNotification({ id: 'n-1', read: false })]),
      });

      const [row] = await harness.getRows();
      await row.markAsReadWithoutStabilizing();
      const readRequest = await expectRequest(request => request.url === `${NOTIFICATIONS_URL}/n-1/_read`);
      expect(readRequest.request.method).toBe('POST');
      readRequest.flush(null, { status: 204, statusText: 'No Content' });
      await settle();

      const [updatedRow] = await harness.getRows();
      expect(await updatedRow.isUnread()).toBe(false);
      expect(await updatedRow.hasMarkAsReadAction()).toBe(false);
      http.expectNone(isListRequest);
    });

    it('should remove a notification marked as read from the unread list', async () => {
      await init({
        notifications: fakePortalNotificationsResponse(
          [
            fakePortalNotification({ id: 'n-1', message: 'First', read: false }),
            fakePortalNotification({ id: 'n-2', message: 'Second', read: false }),
          ],
          12,
        ),
      });

      await (await harness.getRows())[0].markAsReadWithoutStabilizing();
      (await expectRequest(request => request.url === `${NOTIFICATIONS_URL}/n-1/_read`)).flush(null, {
        status: 204,
        statusText: 'No Content',
      });
      await settle();

      const rows = await harness.getRows();
      expect(rows).toHaveLength(1);
      expect(await rows[0].getMessage()).toBe('Second');
      expect(await harness.hasPageButton(2)).toBe(true);
      http.expectNone(isListRequest);
    });

    it('should reload the first page when its last unread notification is marked as read and others remain', async () => {
      await init({ notifications: fakePortalNotificationsResponse([fakePortalNotification({ id: 'n-1', read: false })], 12) });

      await (await harness.getRows())[0].markAsReadWithoutStabilizing();
      (await expectRequest(request => request.url === `${NOTIFICATIONS_URL}/n-1/_read`)).flush(null, {
        status: 204,
        statusText: 'No Content',
      });
      const reload = await flushList(fakePortalNotificationsResponse([fakePortalNotification({ id: 'n-2', message: 'Next' })], 11));

      expect(reload.request.params.get('page')).toBe('1');
      expect(await (await harness.getRows())[0].getMessage()).toBe('Next');
    });

    it('should not offer to mark an already read notification as read', async () => {
      await init({
        url: '/notifications?status=all',
        notifications: fakePortalNotificationsResponse([fakePortalNotification({ id: 'n-1', read: true })]),
      });

      expect(await (await harness.getRows())[0].hasMarkAsReadAction()).toBe(false);
    });

    it('should show an inline error and keep the row unread when marking as read fails', async () => {
      await init({ notifications: fakePortalNotificationsResponse([fakePortalNotification({ id: 'n-1', read: false })]) });

      await (await harness.getRows())[0].markAsReadWithoutStabilizing();
      (await expectRequest(request => request.url.endsWith('/n-1/_read'))).flush(null, { status: 500, statusText: 'Server Error' });
      await settle();

      expect(await harness.getActionErrorText()).toContain('could not be marked as read');
      expect(await (await harness.getRows())[0].isUnread()).toBe(true);
    });
  });

  describe('mark all as read', () => {
    it('should leave nothing in the unread list, other pages included', async () => {
      await init({
        notifications: fakePortalNotificationsResponse(
          [fakePortalNotification({ id: 'n-1', read: false }), fakePortalNotification({ id: 'n-2', read: false })],
          25,
        ),
      });

      await harness.markAllAsReadWithoutStabilizing();
      (await expectRequest(req => req.url === `${NOTIFICATIONS_URL}/_read`)).flush(null, { status: 204, statusText: 'No Content' });
      await settle();

      expect(await harness.isNoUnreadStateDisplayed()).toBe(true);
      http.expectNone(isListRequest);
    });

    it('should mark every row as read in the list of all notifications', async () => {
      await init({
        url: '/notifications?status=all',
        notifications: fakePortalNotificationsResponse([
          fakePortalNotification({ id: 'n-1', read: false }),
          fakePortalNotification({ id: 'n-2', read: false }),
        ]),
      });

      await harness.markAllAsReadWithoutStabilizing();
      const request = await expectRequest(req => req.url === `${NOTIFICATIONS_URL}/_read`);
      expect(request.request.method).toBe('POST');
      request.flush(null, { status: 204, statusText: 'No Content' });
      await settle();

      const rows = await harness.getRows();
      expect(await rows[0].isUnread()).toBe(false);
      expect(await rows[1].isUnread()).toBe(false);
    });

    it('should show an inline error when marking all as read fails', async () => {
      await init({ notifications: fakePortalNotificationsResponse([fakePortalNotification({ read: false })]) });

      await harness.markAllAsReadWithoutStabilizing();
      (await expectRequest(req => req.url === `${NOTIFICATIONS_URL}/_read`)).flush(null, { status: 500, statusText: 'Server Error' });
      await settle();

      expect(await harness.getActionErrorText()).toContain('could not be marked as read');
      expect(await (await harness.getRows())[0].isUnread()).toBe(true);
    });
  });

  describe('empty and error states', () => {
    it('should tell the user there is no unread notification, with the filters to see read ones', async () => {
      await init({ notifications: fakePortalNotificationsResponse([]) });

      expect(await harness.isNoUnreadStateDisplayed()).toBe(true);
      expect(await harness.isEmptyStateDisplayed()).toBe(false);
      expect(await harness.isMarkAllAsReadDisplayed()).toBe(false);
      expect(await (await harness.getStatusFilter()).getTriggerText()).toContain('Unread');
    });

    it('should show the empty state when the user has no notification at all', async () => {
      await init({ url: '/notifications?status=all', notifications: fakePortalNotificationsResponse([]) });

      expect(await harness.isEmptyStateDisplayed()).toBe(true);
      expect(await harness.isNoUnreadStateDisplayed()).toBe(false);
      expect(await harness.isNoMatchStateDisplayed()).toBe(false);
    });

    it('should show the no-match state when an API or application filter excludes every notification', async () => {
      await init({ url: '/notifications?apiId=api-1', notifications: fakePortalNotificationsResponse([]) });

      expect(await harness.isNoMatchStateDisplayed()).toBe(true);
      expect(await harness.isNoUnreadStateDisplayed()).toBe(false);
      expect(await harness.isEmptyStateDisplayed()).toBe(false);
    });

    it('should show the no-match state when no notification is read yet', async () => {
      await init({ url: '/notifications?status=read', notifications: fakePortalNotificationsResponse([]) });

      expect(await harness.isNoMatchStateDisplayed()).toBe(true);
    });

    it('should show an inline error when the filter options cannot be loaded, and still list notifications', async () => {
      await init({ filtersFail: true });

      expect(await harness.getFiltersErrorText()).toContain('The filter options could not be loaded');
      expect(await harness.getRows()).toHaveLength(1);
    });

    it('should show an inline error when the notifications cannot be loaded', async () => {
      await init({ listFails: true });

      expect(await harness.getLoadErrorText()).toContain('An error occurred while loading notifications');
      expect(await harness.isEmptyStateDisplayed()).toBe(false);
    });
  });
});
