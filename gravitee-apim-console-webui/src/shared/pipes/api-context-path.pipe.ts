/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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

import { Pipe, PipeTransform } from '@angular/core';

import { Api, ApiV2, ApiV4, HttpListener } from '../../entities/management-api-v2';

@Pipe({
  name: 'apiContextPath',
  standalone: true,
})
export class ApiContextPathPipe implements PipeTransform {
  transform(api: Api | null | undefined): string | null {
    if (!api) {
      return null;
    }
    if (api.definitionVersion === 'V2') {
      return (api as ApiV2).contextPath ?? null;
    }
    if (api.definitionVersion === 'V4') {
      const apiV4 = api as ApiV4;
      const httpListener = apiV4.listeners?.find((listener) => listener.type === 'HTTP') as HttpListener | undefined;
      if (httpListener?.paths?.length > 0) {
        return httpListener.paths[0]?.path ?? null;
      }
    }
    return null;
  }
}
