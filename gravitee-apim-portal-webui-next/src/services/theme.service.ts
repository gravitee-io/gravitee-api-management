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
import { Injectable } from '@angular/core';
import { catchError, Observable, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { hexToHSL } from '../app/helpers/hex-to-hsl';

interface Theme {
  primary?: string;
  secondary?: string;
  tertiary?: string;
  error?: string;
  background?: string;
  banner?: {
    background?: string;
    textColor?: string;
  };
}

export const addHslToDocument = (propertyName: string, defaultHex: string, hex: string = '') => {
  const { h, s, l } = hexToHSL(hex, defaultHex);
  document.documentElement.style.setProperty(propertyName, `hsl(${h}, ${s}%, ${l}%)`);
  document.documentElement.style.setProperty(`${propertyName}-h`, `${h}`);
  document.documentElement.style.setProperty(`${propertyName}-s`, `${s}%`);
  document.documentElement.style.setProperty(`${propertyName}-l`, `${l}%`);
};

export const addHexToDocument = (propertyName: string, hex?: string) => {
  if (hex) {
    document.documentElement.style.setProperty(propertyName, hex);
  }
};

@Injectable({
  providedIn: 'root',
})
export class ThemeService {
  loadTheme(): Observable<unknown> {
    // TODO: Call backend to get dynamic theme values
    return of({
      primary: '#613CB0',
      secondary: '#958BA9',
      tertiary: '#B7818F',
      error: '#EC6152',
    }).pipe(
      tap((theme: Theme) => {
        addHexToDocument('--gio-app-background-color', theme.background);
        addHexToDocument('--gio-banner-background-color', theme.banner?.background);
        addHexToDocument('--gio-banner-text-color', theme.banner?.textColor);

        // Convert to HSL in order to create palettes
        addHslToDocument('--gio-app-primary-main-color', '#613CB0', theme.primary);
        addHslToDocument('--gio-app-secondary-main-color', '#958BA9', theme.secondary);
        addHslToDocument('--gio-app-tertiary-main-color', '#B7818F', theme.tertiary);
        addHslToDocument('--gio-app-error-main-color', '#EC6152', theme.error);
      }),
      catchError(_ => of({})),
    );
  }
}
