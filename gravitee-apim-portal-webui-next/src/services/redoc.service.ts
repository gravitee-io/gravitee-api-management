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
import { DOCUMENT } from '@angular/common';
import { inject, Injectable } from '@angular/core';

import { readYaml } from '../app/helpers/yaml-parser';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
declare let Redoc: any;

const DEFAULT_PRIMARY_COLOR = '#32329f';

@Injectable({
  providedIn: 'root',
})
export class RedocService {
  private readonly document = inject(DOCUMENT);

  init(content: string | undefined, options: Record<string, unknown>, element: HTMLElement): void {
    if (content) {
      const swaggerSpec = this.parseContent(content);
      const mergedOptions = this.mergeThemeWithPrimary(options);
      Redoc.init(swaggerSpec, mergedOptions, element);
    }
  }

  private getPrimaryColor(): string {
    const value = this.document.defaultView
      ?.getComputedStyle(this.document.documentElement)
      ?.getPropertyValue('--gio-app-primary-main-color')
      ?.trim();
    return value || DEFAULT_PRIMARY_COLOR;
  }

  private mergeThemeWithPrimary(options: Record<string, unknown>): Record<string, unknown> {
    const primary = this.getPrimaryColor();
    const baseTheme = (options['theme'] as Record<string, unknown>) ?? {};
    const baseColors = (baseTheme['colors'] as Record<string, unknown>) ?? {};
    return {
      ...options,
      theme: {
        ...baseTheme,
        colors: {
          ...baseColors,
          primary: {
            main: primary,
          },
          http: {
            get: primary,
            post: primary,
            put: primary,
            patch: primary,
            delete: primary,
            options: primary,
            head: primary,
            link: primary,
            basic: primary,
          },
        },
      },
    };
  }

  private parseContent(content: string): unknown {
    try {
      return JSON.parse(content);
    } catch (e) {
      return readYaml(content);
    }
  }
}
