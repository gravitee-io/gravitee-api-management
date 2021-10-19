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
import { isNil } from 'lodash';

import { Constants } from '../entities/Constants';
import { NotificationTemplate } from '../entities/notification/notificationTemplate';

interface SearchParams {
  hook?: string;
  scope?: string;
}

@Injectable({
  providedIn: 'root',
})
export class NotificationTemplateService {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  search(params?: SearchParams): Observable<NotificationTemplate[]> {
    const requestParams: { hook?: string; scope?: string } = {};
    if (!isNil(params?.scope)) {
      requestParams.scope = params.scope;
    }
    if (!isNil(params?.hook)) {
      requestParams.hook = params.hook;
    }
    return this.http.get<NotificationTemplate[]>(`${this.constants.org.baseURL}/configuration/notification-templates`, {
      params: requestParams,
    });
  }

  create(notificationTemplate: Omit<NotificationTemplate, 'id'>): Observable<NotificationTemplate> {
    return this.http.post<NotificationTemplate>(`${this.constants.org.baseURL}/configuration/notification-templates`, notificationTemplate);
  }

  update(notificationTemplate: NotificationTemplate): Observable<NotificationTemplate> {
    return this.http.put<NotificationTemplate>(
      `${this.constants.org.baseURL}/configuration/notification-templates/${notificationTemplate.id}`,
      notificationTemplate,
    );
  }
}
