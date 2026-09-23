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
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { notificationListBreadcrumb } from './notification-breadcrumbs';
import NotificationsComponent from './notifications.component';
import { fakeApplicationsResponse } from '../../../entities/application/application.fixture';
import { fakePortalNotification, fakePortalNotificationsResponse } from '../../../entities/notification/portal-notification.fixture';
import { BreadcrumbService } from '../../../services/breadcrumb.service';
import { AppTestingModule, TESTING_BASE_URL } from '../../../testing/app-testing.module';

describe('NotificationsComponent', () => {
  let fixture: ComponentFixture<NotificationsComponent>;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [NotificationsComponent, AppTestingModule],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(NotificationsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
    localStorage.clear();
  });

  function flushPage(
    inbox = fakePortalNotificationsResponse({
      data: [
        fakePortalNotification({
          title: '[Echo API] Subscription Accepted',
          message: 'The subscription request for the application "Petstore App" to the plan "Premium" was accepted.',
        }),
      ],
    }),
    applications = fakeApplicationsResponse({ data: [] }),
  ) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/user/notifications?page=1&size=-1`).flush(inbox);
    httpTestingController.expectOne(`${TESTING_BASE_URL}/applications?page=1&size=-1`).flush(applications);
    fixture.detectChanges();
  }

  it('sets breadcrumbs and lists API and App notification columns', () => {
    const breadcrumbService = TestBed.inject(BreadcrumbService);
    expect(breadcrumbService.breadcrumbs()).toEqual([notificationListBreadcrumb()]);

    flushPage();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Notifications');
    expect(text).toContain('Date');
    expect(text).toContain('API/App name');
    expect(text).toContain('Notification Type');
    expect(text).toContain('Message');
    expect(text).toContain('Echo API');
    expect(text).toContain('Subscription Accepted');
    expect(text).toContain('Petstore App');
  });

  it('shows an empty state when there are no notifications', () => {
    flushPage(fakePortalNotificationsResponse({ data: [] }));

    expect(fixture.nativeElement.textContent).toContain('No notification received');
  });

  it('can show read notifications after they are marked as read', () => {
    flushPage();

    const markAsRead = fixture.nativeElement.querySelector('[data-testid="paginated-table-action-read"]') as HTMLButtonElement;
    expect(markAsRead).toBeTruthy();
    markAsRead.click();

    httpTestingController.expectOne(`${TESTING_BASE_URL}/user/notifications/notification-1`).flush(null);
    httpTestingController.expectOne(`${TESTING_BASE_URL}/user/notifications?page=1&size=1`).flush(fakePortalNotificationsResponse({ data: [] }));
    httpTestingController.expectOne(`${TESTING_BASE_URL}/user/notifications?page=1&size=-1`).flush(fakePortalNotificationsResponse({ data: [] }));
    httpTestingController.expectOne(`${TESTING_BASE_URL}/applications?page=1&size=-1`).flush(fakeApplicationsResponse({ data: [] }));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No notifications match the selected filters');

    fixture.componentInstance.statusFilter.setValue(['read']);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Echo API');
    expect(fixture.nativeElement.textContent).toContain('Subscription Accepted');
  });

  it('shows Preferences view with configuration copy', () => {
    flushPage();

    fixture.componentInstance.setActiveView('preferences');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-notification-preferences')).toBeTruthy();

    httpTestingController.match(() => true).forEach(request => {
      if (request.request.url.includes('/hooks')) {
        request.flush([]);
      } else if (request.request.url.includes('/subscriptions')) {
        request.flush({ data: [], links: {}, metadata: {} });
      } else {
        request.flush(fakeApplicationsResponse({ data: [] }));
      }
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Choose which events should notify you');
  });
});
