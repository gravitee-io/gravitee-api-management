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
import { Api, HttpListener } from '../../entities/management-api-v2';

@Pipe({ name: 'apiContextPath', standalone: true })
export class ApiContextPathPipe implements PipeTransform {
  transform(api: Api): string | undefined {
    if (!api) return undefined;
    if ('contextPath' in api && api.contextPath) {
      return api.contextPath;
    }
    if (api.definitionVersion === 'V4' && 'listeners' in api) {
      const apiV4 = api as Api & { listeners?: Array<{ type: string } & Partial<HttpListener>> };
      const httpListener = apiV4.listeners?.find((listener) => listener.type === 'HTTP') as
        | HttpListener
        | undefined;
      if (httpListener?.paths?.length) {
        return httpListener.paths[0]?.path;
      }
    }
    return undefined;
  }
}
