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
import { inject, Injectable } from '@angular/core';

import { CurrentUserService } from './current-user.service';
import { PortalNotification } from '../entities/notification/portal-notification';

interface NotificationArchiveStore {
  items: PortalNotification[];
}

@Injectable({
  providedIn: 'root',
})
export class NotificationReadArchiveService {
  private static readonly STORAGE_PREFIX = 'portal-next.notification-archive.';
  private static readonly MAX_ITEMS = 200;

  private readonly currentUserService = inject(CurrentUserService);

  list(): PortalNotification[] {
    return this.readStore().items;
  }

  archive(notification: PortalNotification): void {
    if (!notification.id) {
      return;
    }

    const items = [notification, ...this.readStore().items.filter(item => item.id !== notification.id)].slice(
      0,
      NotificationReadArchiveService.MAX_ITEMS,
    );
    this.writeStore({ items });
  }

  private readStore(): NotificationArchiveStore {
    try {
      const raw = localStorage.getItem(this.storageKey());
      if (!raw) {
        return { items: [] };
      }
      const parsed = JSON.parse(raw) as NotificationArchiveStore;
      return { items: Array.isArray(parsed.items) ? parsed.items : [] };
    } catch {
      return { items: [] };
    }
  }

  private writeStore(store: NotificationArchiveStore): void {
    try {
      localStorage.setItem(this.storageKey(), JSON.stringify(store));
    } catch {
      // Ignore quota / private-mode failures; unread/read still works for the current session inbox.
    }
  }

  private storageKey(): string {
    return `${NotificationReadArchiveService.STORAGE_PREFIX}${this.currentUserService.user().id ?? 'anonymous'}`;
  }
}
