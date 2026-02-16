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

import { Injectable } from '@angular/core';
import { CanActivate } from '@angular/router';
import { of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { EnvironmentSettingsService } from '../../services-ngx/environment-settings.service';

@Injectable({
  providedIn: 'root',
})
export class ApiScoringGuard implements CanActivate {
  constructor(private readonly environmentSettingsService: EnvironmentSettingsService) {}

  canActivate() {
    return this.environmentSettingsService.get().pipe(
      map(envSettings => envSettings.apiScore.enabled),
      catchError(_ => of(false)),
    );
  }
}
