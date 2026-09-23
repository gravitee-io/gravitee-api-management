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
import { NotificationInput, PortalNotificationHook } from '../entities/notification/portal-notification-hook';

@Injectable({
  providedIn: 'root',
})
export class ApplicationNotificationService {
  private readonly http = inject(HttpClient);
  private readonly configService = inject(ConfigService);

  getHooks(): Observable<PortalNotificationHook[]> {
    return this.http.get<PortalNotificationHook[]>(`${this.configService.baseURL}/applications/hooks`);
  }

  getNotifications(applicationId: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.configService.baseURL}/applications/${applicationId}/notifications`);
  }

  updateNotifications(applicationId: string, hooks: string[]): Observable<string[]> {
    const body: NotificationInput = { hooks };
    return this.http.put<string[]>(`${this.configService.baseURL}/applications/${applicationId}/notifications`, body);
  }
}
