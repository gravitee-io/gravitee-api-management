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
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ConfigService } from './config.service';
import { PortalSettings } from '../entities/portal/portalSettings';

/**
 *  Portal Settings Service is used to return the settings for users that have the READ permission for settings at the org or env level
 *
 *  Use {@link PortalConfigurationService} to access settings open to all users.
 */
@Injectable({
  providedIn: 'root',
})
export class PortalSettingsService {
  constructor(
    private readonly http: HttpClient,
    private configService: ConfigService,
  ) {}

  get(): Observable<PortalSettings> {
    return this.http.get<PortalSettings>(`${this.configService.baseURL}/portal`);
  }
}
