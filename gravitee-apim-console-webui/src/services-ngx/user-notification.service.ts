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
import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable, timer } from 'rxjs';
import { switchMap } from 'rxjs/operators';

import { Constants } from '../entities/Constants';
import { PagedResult } from '../entities/pagedResult';
import { UserNotification } from '../entities/user-notification/userNotification';

@Injectable({
  providedIn: 'root',
})
export class UserNotificationService {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  private getNotificationSchedulerInSeconds(): number {
    if (this.constants.org.settings.scheduler && this.constants.org.settings.scheduler.notifications) {
      return this.constants.org.settings.scheduler.notifications;
    }
    return 10;
  }

  getNotifications(): Observable<PagedResult<UserNotification>> {
    const params = new HttpParams();
    params.set('ignoreLoadingBar', true);
    params.set('silentCall', true);
    return this.http.get<PagedResult<UserNotification>>(`${this.constants.org.baseURL}/user/notifications`, { params });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.org.baseURL}/user/notifications/${id}`);
  }

  deleteAll(): Observable<void> {
    return this.http.delete<void>(`${this.constants.org.baseURL}/user/notifications`);
  }

  getNotificationsAutoFetch(): Observable<PagedResult<UserNotification>> {
    return timer(0, this.getNotificationSchedulerInSeconds() * 1000).pipe(switchMap(() => this.getNotifications()));
  }
}
