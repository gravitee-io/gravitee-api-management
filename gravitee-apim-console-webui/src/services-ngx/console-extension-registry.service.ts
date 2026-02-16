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

import { ConsoleExtension } from './console-extension-loader';

export interface ResolvedExtensionComponent {
  pluginId: string;
  tagName: string;
  label?: string;
  icon?: string;
}

@Injectable({ providedIn: 'root' })
export class ConsoleExtensionRegistryService {
  private extensions: ConsoleExtension[] = (window as any).__GRAVITEE_CONSOLE_EXTENSIONS__ ?? [];

  getComponentsByPlacement(placement: string): ResolvedExtensionComponent[] {
    const result: ResolvedExtensionComponent[] = [];
    for (const ext of this.extensions) {
      if (ext.manifest == null) continue;
      for (const component of ext.manifest.components) {
        if (component.placement === placement) {
          result.push({
            pluginId: ext.id,
            tagName: component.tagName,
            label: component.label,
            icon: component.icon,
          });
        }
      }
    }
    return result;
  }
}
