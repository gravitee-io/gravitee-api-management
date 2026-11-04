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
import { PortalNotification, PortalNotificationFilters, PortalNotificationsResponse } from './portal-notification';

export function fakePortalNotification(modifier?: Partial<PortalNotification>): PortalNotification {
  const base: PortalNotification = {
    id: 'notification-1',
    title: 'Subscription accepted',
    message: 'Your subscription to My API has been accepted.',
    created_at: '2026-10-30T10:00:00Z',
    read: false,
    hook: 'SUBSCRIPTION_ACCEPTED',
    api: { id: 'api-1', name: 'My API' },
    application: { id: 'app-1', name: 'My app' },
  };

  return { ...base, ...modifier };
}

export function fakePortalNotificationsResponse(
  notifications: PortalNotification[] = [fakePortalNotification()],
  total: number = notifications.length,
): PortalNotificationsResponse {
  return {
    data: notifications,
    metadata: {
      data: { total },
      pagination: {
        current_page: 1,
        size: notifications.length,
        total,
        total_pages: Math.max(1, Math.ceil(total / 10)),
        first: notifications.length ? 1 : 0,
        last: notifications.length,
      },
    },
    links: {},
  };
}

export function fakePortalNotificationFilters(modifier?: Partial<PortalNotificationFilters>): PortalNotificationFilters {
  return {
    apis: [{ id: 'api-1', name: 'My API' }],
    applications: [{ id: 'app-1', name: 'My app' }],
    ...modifier,
  };
}
