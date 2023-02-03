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
import { failIf } from '@helpers/k6.helper';
import { RefinedResponse } from 'k6/http';

export class HttpHelper {
  /**
   * Replaces all the path params with provided values.
   * Usage: replacePathParams('http://localhost:8083/management/organizations/:orgId/environments/:envId', [':orgId', ':envId'], ['defaultOrga', 'defaultEnv'])
   * @param url in which replace params
   * @param params the params identifiers to replace
   * @param values the replacement values for the params. Should be in the same order as params
   */
  static replacePathParams(url: string, params: string[], values: string[]): string {
    let newUrl = '';
    failIf(
      params.length !== values.length,
      `Impossible to compute path params for ${url}: params array and values array do not have the same length`,
    );
    params.forEach((param, index) => {
      newUrl = url.replace(param, values[index]);
    });
    return newUrl;
  }

  static parseBody<T>(response: RefinedResponse<any>): T {
    return JSON.parse(response.body as string) as T;
  }
}
