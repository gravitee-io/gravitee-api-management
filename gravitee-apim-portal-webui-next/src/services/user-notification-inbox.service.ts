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
import { inject, Injectable, signal } from '@angular/core';
import { catchError, map, Observable, take, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { UserNotificationService } from './user-notification.service';

@Injectable({
  providedIn: 'root',
})
export class UserNotificationInboxService {
  private readonly userNotificationService = inject(UserNotificationService);

  readonly unreadCount = signal(0);

  fetchCount(): Observable<number> {
    return this.userNotificationService.list(1, 1).pipe(
      map(response => response.metadata?.pagination?.total ?? response.data?.length ?? 0),
      tap(count => this.unreadCount.set(count)),
      catchError(() => {
        this.unreadCount.set(0);
        return of(0);
      }),
    );
  }

  refresh(): void {
    this.fetchCount().pipe(take(1)).subscribe();
  }

  clear(): void {
    this.unreadCount.set(0);
  }
}
