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
import { RequestInfo, RequestInfoHolder } from '@model/technical';
import Response = Cypress.Response;

export type ParamMap = Record<string, string>;

export class HttpClient extends RequestInfoHolder {
  constructor(requestInfo: RequestInfo) {
    super(requestInfo);
  }

  /**
   * Transform a ParamMap object in a query string
   * @example {q:'user',birthCity:'Lille'} will result in '?q=user&birthCity=Lille'
   * @param queryParams: the ParamMap object to transform into query params string
   */
  buildQueryParamString(queryParams: ParamMap) {
    return queryParams
      ? '?' +
          Object.keys(queryParams)
            .map((key) => key + '=' + queryParams[key])
            .join('&')
      : '';
  }

  post<T extends string | object, R = T>(path: string, body: T, queryParams?: ParamMap): Cypress.Chainable<Response<R>> {
    const queryParamString = this.buildQueryParamString(queryParams);
    return cy
      .request<R>({
        method: 'POST',
        url: `${this.requestInfo.baseUrl}${path}${queryParamString}`,
        auth: this.requestInfo.auth,
        body,
        failOnStatusCode: false,
      })
      .console('info');
  }

  postWithoutBody<T extends string | object, R = T>(path: string, queryParams?: ParamMap): Cypress.Chainable<Response<R>> {
    return this.post(path, {}, queryParams);
  }

  get<T extends string | object, R = T>(path: string, queryParams?: ParamMap): Cypress.Chainable<Response<R>> {
    const queryParamString = this.buildQueryParamString(queryParams);
    return cy
      .request<R>({
        method: 'GET',
        url: `${this.requestInfo.baseUrl}${path}${queryParamString}`,
        auth: this.requestInfo.auth,
        failOnStatusCode: false,
      })
      .console('info');
  }

  delete<T extends string | object, R = T>(path: string, queryParams?: ParamMap): Cypress.Chainable<Response<R>> {
    const queryParamString = this.buildQueryParamString(queryParams);
    return cy
      .request<R>({
        method: 'DELETE',
        url: `${this.requestInfo.baseUrl}${path}${queryParamString}`,
        auth: this.requestInfo.auth,
        failOnStatusCode: false,
      })
      .console('info');
  }

  put<T extends string | object, R = T>(path: string, body: T): Cypress.Chainable<Response<R>> {
    return cy
      .request<R>({
        method: 'PUT',
        url: `${this.requestInfo.baseUrl}${path}`,
        auth: this.requestInfo.auth,
        body,
        failOnStatusCode: false,
      })
      .console('info');
  }
}
