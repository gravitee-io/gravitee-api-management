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

import { readYaml } from '../app/helpers/yaml-parser';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
declare let Redoc: any;

@Injectable({
  providedIn: 'root',
})
export class RedocService {
  constructor() {}

  init(content: string | undefined, options: unknown, elementId: unknown): void {
    if (content) {
      const swaggerSpec = this.parseContent(content);
      Redoc.init(swaggerSpec, options, elementId);
    }
  }

  private parseContent(content: string): unknown {
    try {
      return JSON.parse(content);
    } catch (e) {
      return readYaml(content);
    }
  }
}
