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
import { ErrorableManagement, RequestInfo } from '../../model/technical';
import { Api, ApiDefinition, ApiDeployment, ApiMember } from '../../model/apis';
import Chainable = Cypress.Chainable;
import Response = Cypress.Response;
import { HttpConnector } from '../../model/technical.http';

export class ApiManagementCommands extends HttpConnector {
  constructor(requestInfo: RequestInfo) {
    super(requestInfo);
  }

  getAll<T extends ErrorableManagement<Api[]> = Api[]>(): Chainable<Response<T>> {
    return this.httpClient.get('/apis');
  }

  getApiById<T extends ErrorableManagement<Api> = Api>(apiId: string): Chainable<Response<T>> {
    return this.httpClient.get(`/apis/${apiId}`);
  }

  create<T extends ErrorableManagement<Api> = Api>(api: Api): Chainable<Response<T>> {
    return this.httpClient.post('/apis', api);
  }

  delete<T extends ErrorableManagement<Api> = Api>(apiId: string): Chainable<Response<T>> {
    return this.httpClient.delete(`/apis/${apiId}`);
  }

  update<T extends ErrorableManagement<Api> = Api>(apiId: string, api: Api): Chainable<Response<T>> {
    return this.httpClient.put(`/apis/${apiId}`, api);
  }

  importApi<T extends ErrorableManagement<Api> = Api>(apiDefinition: ApiDefinition): Chainable<Response<T>> {
    return this.httpClient.post('/apis/import', apiDefinition);
  }

  start<T extends ErrorableManagement<Api> = Api>(apiId: string): Chainable<Response<T>> {
    return this.httpClient.postWithoutBody(`/apis/${apiId}?action=START`);
  }

  stop<T extends ErrorableManagement<Api> = Api>(apiId: string): Chainable<Response<T>> {
    return this.httpClient.postWithoutBody(`/apis/${apiId}?action=STOP`);
  }

  addMemberToApi<T extends ErrorableManagement<ApiMember> = ApiMember>(apiId: string, apiMember: ApiMember): Chainable<Response<T>> {
    return this.httpClient.post(`/apis/${apiId}/members`, apiMember);
  }

  deploy(apiId: string, apiDeployment: ApiDeployment) {
    return this.httpClient.post(`/apis/${apiId}/deploy`, apiDeployment);
  }
}
