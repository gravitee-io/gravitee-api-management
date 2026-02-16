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
export class ApiNotificationSettingsService {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  getAll(apiId: string): Observable<NotificationSettings[]> {
    return this.http.get<NotificationSettings[]>(`${this.constants.env.baseURL}/apis/${apiId}/notificationsettings`);
  }

  getSingleNotificationSetting(apiId: string, selectedId: string): Observable<NotificationSettings> {
    return this.http.get<NotificationSettings[]>(`${this.constants.env.baseURL}/apis/${apiId}/notificationsettings`).pipe(
      map((notifications: NotificationSettings[]) => {
        return notifications.find(notification => notification.id === selectedId) || notifications[0];
      }),
    );
  }

  delete(apiId: string, id: string): Observable<NotificationSettings[]> {
    return this.http.delete<NotificationSettings[]>(`${this.constants.env.baseURL}/apis/${apiId}/notificationsettings/${id}`);
  }

  getNotifiers(apiId: string): Observable<Notifier[]> {
    return this.http.get<Notifier[]>(`${this.constants.env.baseURL}/apis/${apiId}/notifiers`);
  }

  create(apiId: string, notificationConfig: NewNotificationSettings): Observable<NotificationSettings> {
    return this.http.post<NotificationSettings>(`${this.constants.env.baseURL}/apis/${apiId}/notificationsettings`, notificationConfig);
  }

  update(apiId: string, notificationId: string, notificationConfig: NotificationSettings): Observable<NotificationSettings> {
    return this.http.put<NotificationSettings>(
      `${this.constants.env.baseURL}/apis/${apiId}/notificationsettings/${notificationId === 'PORTAL' ? '' : notificationId}`,
      notificationConfig,
    );
  }

  getHooks(): Observable<Hooks[]> {
    return this.http.get<Hooks[]>(`${this.constants.env.baseURL}/apis/hooks`);
  }
}
