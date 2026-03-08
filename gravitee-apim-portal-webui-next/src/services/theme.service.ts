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

export interface ThemeColorDefinition {
  primary?: string;
  secondary?: string;
  tertiary?: string;
  error?: string;
  background?: {
    card?: string;
    page?: string;
  };
}

export interface Theme {
  definition: {
    color?: ThemeColorDefinition;
    font: {
      fontFamily?: string;
    };
    customCss?: string;
    dark?: {
      color?: ThemeColorDefinition;
      customCss?: string;
    };
  };
  _links?: {
    logo?: string;
    favicon?: string;
  };
}

const THEME_MODE_STORAGE_KEY = 'gio-portal-theme-mode';
const CUSTOM_CSS_STYLE_ID = 'gio-theme-custom-css';

export type ThemeMode = 'light' | 'dark' | 'system';

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
  favicon = signal('assets/images/favicon.png');
  themeMode = signal<ThemeMode>('system');
  darkMode = signal(false);

  private lightDefinition?: Theme['definition'];
  private darkDefinition?: Theme['definition']['dark'];

  constructor(
    private readonly http: HttpClient,
    private configService: ConfigService,
  ) {}

  loadTheme(): Observable<unknown> {
    return this.http.get<Theme>(`${this.configService.baseURL}/theme?type=PORTAL_NEXT`).pipe(
      tap(({ definition }: Theme) => {
        this.lightDefinition = definition;
        this.darkDefinition = definition.dark;

        addPropertyToDocument('--gio-app-font-family', definition.font.fontFamily);

        const { mode, effective } = this.resolveInitialMode();
        this.themeMode.set(mode);
        this.applyTheme(effective);
        this.subscribeToSystemPreference();
      }),
      tap(({ _links }) => {
        if (_links?.logo) {
          this.logo.set(_links.logo);
        }
        if (_links?.favicon) {
          this.favicon.set(_links.favicon);
        }
      }),
      catchError(_ => of({})),
    );
  }

  setThemeMode(mode: ThemeMode): void {
    this.themeMode.set(mode);
    localStorage.setItem(THEME_MODE_STORAGE_KEY, mode);

    const effective = this.getEffectiveMode(mode);
    document.documentElement.classList.add('transitioning');
    this.applyTheme(effective);
    setTimeout(() => document.documentElement.classList.remove('transitioning'), 350);
  }

  private getEffectiveMode(mode: ThemeMode): 'light' | 'dark' {
    if (mode === 'system') {
      return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }
    return mode;
  }

  private applyTheme(mode: 'light' | 'dark'): void {
    const isDark = mode === 'dark';
    this.darkMode.set(isDark);

    const colors = isDark ? this.darkDefinition?.color : this.lightDefinition?.color;

    addPropertyToDocument('--gio-app-background-color', colors?.background?.page);
    addPropertyToDocument('--gio-app-card-background-color', colors?.background?.card);
    addHslToDocument('--gio-app-primary-main-color', colors?.primary);
    addHslToDocument('--gio-app-secondary-main-color', colors?.secondary);
    addHslToDocument('--gio-app-tertiary-main-color', colors?.tertiary);
    addHslToDocument('--gio-app-error-main-color', colors?.error);

    if (isDark) {
      document.documentElement.classList.add('dark-mode');
    } else {
      document.documentElement.classList.remove('dark-mode');
    }

    this.applyCustomCss(isDark);
  }

  private applyCustomCss(isDark: boolean): void {
    const existing = document.getElementById(CUSTOM_CSS_STYLE_ID);
    if (existing) {
      existing.remove();
    }

    const css = isDark ? this.darkDefinition?.customCss : this.lightDefinition?.customCss;
    if (css) {
      const style = document.createElement('style');
      style.id = CUSTOM_CSS_STYLE_ID;
      style.innerText = css;
      document.body.append(style);
    }
  }

  private resolveInitialMode(): { mode: ThemeMode; effective: 'light' | 'dark' } {
    const stored = localStorage.getItem(THEME_MODE_STORAGE_KEY);
    const mode: ThemeMode = stored === 'light' || stored === 'dark' || stored === 'system' ? stored : 'system';
    const effective = this.getEffectiveMode(mode);
    return { mode, effective };
  }

  private subscribeToSystemPreference(): void {
    const mediaQuery = window.matchMedia?.('(prefers-color-scheme: dark)');
    if (!mediaQuery) return;

    mediaQuery.addEventListener('change', () => {
      if (this.themeMode() !== 'system') return;
      const effective = mediaQuery.matches ? 'dark' : 'light';
      this.applyTheme(effective);
    });
  }
}
