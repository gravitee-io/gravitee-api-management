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
import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { Constants } from '../entities/Constants';
import { NotificationSettings } from '../entities/notification/notificationSettings';
import { Notifier } from '../entities/notification/notifier';
import { NewNotificationSettings } from '../entities/notification/newNotificationSettings';
import { Hooks } from '../entities/notification/hooks';

@Injectable({
  providedIn: 'root',
})
export class EnvironmentNotificationSettingsService {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  getAll(): Observable<NotificationSettings[]> {
    return this.http.get<NotificationSettings[]>(`${this.constants.env.baseURL}/configuration/notificationsettings`);
  }

  getSingleNotificationSetting(selectedId: string): Observable<NotificationSettings> {
    return this.http.get<NotificationSettings[]>(`${this.constants.env.baseURL}/configuration/notificationsettings`).pipe(
      map((notifications: NotificationSettings[]) => {
        return notifications.find(notification => notification.id === selectedId) || notifications[0];
      }),
    );
  }

  delete(id: string): Observable<NotificationSettings[]> {
    return this.http.delete<NotificationSettings[]>(`${this.constants.env.baseURL}/configuration/notificationsettings/${id}`);
  }

  getNotifiers(): Observable<Notifier[]> {
    return this.http.get<Notifier[]>(`${this.constants.env.baseURL}/configuration/notifiers`);
  }

  create(notificationConfig: NewNotificationSettings): Observable<NewNotificationSettings> {
    return this.http.post<NewNotificationSettings>(`${this.constants.env.baseURL}/configuration/notificationsettings`, notificationConfig);
  }

  update(notificationId: string, notificationConfig: NotificationSettings): Observable<NotificationSettings> {
    return this.http.put<NotificationSettings>(
      `${this.constants.env.baseURL}/configuration/notificationsettings/${notificationId === 'PORTAL' ? '' : notificationId}`,
      notificationConfig,
    );
  }

  getHooks(): Observable<Hooks[]> {
    return this.http.get<Hooks[]>(`${this.constants.env.baseURL}/configuration/hooks`);
  }
}
