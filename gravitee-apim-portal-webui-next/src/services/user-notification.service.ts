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
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ConfigService } from './config.service';
import { PortalNotificationsResponse } from '../entities/notification/portal-notification';

@Injectable({
  providedIn: 'root',
})
export class UserNotificationService {
  private readonly http = inject(HttpClient);
  private readonly configService = inject(ConfigService);

  list(page = 1, size = 10): Observable<PortalNotificationsResponse> {
    return this.http.get<PortalNotificationsResponse>(`${this.configService.baseURL}/user/notifications`, {
      params: { page, size },
    });
  }

  markAsRead(notificationId: string): Observable<void> {
    return this.http.delete<void>(`${this.configService.baseURL}/user/notifications/${notificationId}`);
  }

  deleteAll(): Observable<void> {
    return this.http.delete<void>(`${this.configService.baseURL}/user/notifications`);
  }
}
