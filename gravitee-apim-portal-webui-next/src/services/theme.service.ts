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
import { Injectable, signal } from '@angular/core';
import { catchError, Observable, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { ConfigService } from './config.service';
import { hexToHSL } from '../app/helpers/hex-to-hsl';

export interface Theme {
  definition: {
    color?: {
      primary?: string;
      secondary?: string;
      tertiary?: string;
      error?: string;
      background?: {
        card?: string;
        page?: string;
      };
    };
    font: {
      fontFamily?: string;
    };
    customCss?: string;
  };
  _links?: {
    logo?: string;
  };
}

export const addHslToDocument = (propertyName: string, hex: string = '') => {
  if (!hex) {
    return;
  }
  const { h, s, l } = hexToHSL(hex);
  document.documentElement.style.setProperty(propertyName, `hsl(${h}, ${s}%, ${l}%)`);
  document.documentElement.style.setProperty(`${propertyName}-h`, `${h}`);
  document.documentElement.style.setProperty(`${propertyName}-s`, `${s}%`);
  document.documentElement.style.setProperty(`${propertyName}-l`, `${l}%`);
};

export const addPropertyToDocument = (propertyName: string, value?: string) => {
  if (value) {
    document.documentElement.style.setProperty(propertyName, value);
  }
};

@Injectable({
  providedIn: 'root',
})
export class ThemeService {
  logo = signal('assets/images/logo.png');

  constructor(
    private readonly http: HttpClient,
    private configService: ConfigService,
  ) {}
  loadTheme(): Observable<unknown> {
    return this.http.get<Theme>(`${this.configService.baseURL}/theme?type=PORTAL_NEXT`).pipe(
      tap(({ definition }: Theme) => {
        addPropertyToDocument('--gio-app-background-color', definition.color?.background?.page);
        addPropertyToDocument('--gio-app-card-background-color', definition.color?.background?.card);

        // Convert to HSL in order to create palettes
        addHslToDocument('--gio-app-primary-main-color', definition.color?.primary);
        addHslToDocument('--gio-app-secondary-main-color', definition.color?.secondary);
        addHslToDocument('--gio-app-tertiary-main-color', definition.color?.tertiary);
        addHslToDocument('--gio-app-error-main-color', definition.color?.error);

        addPropertyToDocument('--gio-app-font-family', definition.font.fontFamily);
        if (definition.customCss) {
          const style = document.createElement('style');
          style.innerText = definition.customCss;
          const body = document.body || document.getElementsByTagName('body')[0];
          body.append(style);
        }
      }),
      tap(({ _links }) => {
        if (_links?.logo) {
          this.logo.set(_links.logo);
        }
      }),
      catchError(_ => of({})),
    );
  }
}
