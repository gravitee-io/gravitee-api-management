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
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';

import { BASE_64_PREFIX } from './entrypoint.service';

export const ICON_NAMESPACE = 'gio-literal';

@Injectable({
  providedIn: 'root',
})
export class IconService {
  constructor(private readonly matIconRegistry: MatIconRegistry, private _sanitizer: DomSanitizer) {}

  registerSvg(id: string, icon: string): string {
    if (icon && icon.startsWith(BASE_64_PREFIX)) {
      this.matIconRegistry.addSvgIconLiteralInNamespace(
        ICON_NAMESPACE,
        id,
        // No Sonar because the bypass is deliberate and should only be used with safe data
        this._sanitizer.bypassSecurityTrustHtml(atob(icon.replace(BASE_64_PREFIX, ''))), // NOSONAR
      );
      return `${ICON_NAMESPACE}:${id}`;
    }
    return icon;
  }
}
