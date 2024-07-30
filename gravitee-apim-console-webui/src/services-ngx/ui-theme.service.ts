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
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';

import { Constants } from '../entities/Constants';
import { Theme, ThemeType, UpdateTheme } from '../entities/management-api-v2';

@Injectable({ providedIn: 'root' })
export class UiPortalThemeService {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  getDefaultTheme(themeType: ThemeType = 'PORTAL'): Observable<Theme> {
    return this.http.get<Theme>(`${this.constants.env.v2BaseURL}/ui/themes/_default?type=${themeType}`);
  }

  getCurrentTheme(themeType: ThemeType = 'PORTAL'): Observable<Theme> {
    return this.http.get<Theme>(`${this.constants.env.v2BaseURL}/ui/themes/_current?type=${themeType}`);
  }

  updateTheme(updateTheme: UpdateTheme): Observable<Theme> {
    return this.http.put<Theme>(`${this.constants.env.v2BaseURL}/ui/themes/${updateTheme.id}`, updateTheme);
  }
}
