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

import { IHttpPromise, IPromise } from 'angular';

import { ClientRegistrationProvider } from '../entities/clientRegistrationProvider';

class ClientRegistrationProviderService {
  constructor(private $http, private Constants) {
    'ngInject';
  }

  list(): IPromise<ClientRegistrationProvider[]> {
    return this.$http
      .get(`${this.Constants.env.baseURL}/configuration/applications/registration/providers/`)
      .then((response) => response.data);
  }

  get(id: string): IPromise<ClientRegistrationProvider> {
    return this.$http.get(`${this.Constants.env.baseURL}/configuration/applications/registration/providers/` + id).then((response) => {
      const clientRegistrationProvider = response.data;
      clientRegistrationProvider.scopes = clientRegistrationProvider.scopes || [];
      return clientRegistrationProvider;
    });
  }

  create(clientRegistrationProvider: ClientRegistrationProvider): IHttpPromise<ClientRegistrationProvider> {
    return this.$http.post(`${this.Constants.env.baseURL}/configuration/applications/registration/providers/`, clientRegistrationProvider);
  }

  update(clientRegistrationProvider: ClientRegistrationProvider): IPromise<ClientRegistrationProvider> {
    return this.$http
      .put(`${this.Constants.env.baseURL}/configuration/applications/registration/providers/` + clientRegistrationProvider.id, {
        name: clientRegistrationProvider.name,
        description: clientRegistrationProvider.description,
        discovery_endpoint: clientRegistrationProvider.discovery_endpoint,
        initial_access_token_type: clientRegistrationProvider.initial_access_token_type,
        client_id: clientRegistrationProvider.client_id,
        client_secret: clientRegistrationProvider.client_secret,
        scopes: clientRegistrationProvider.scopes,
        initial_access_token: clientRegistrationProvider.initial_access_token,
        renew_client_secret_support: clientRegistrationProvider.renew_client_secret_support,
        renew_client_secret_endpoint: clientRegistrationProvider.renew_client_secret_endpoint,
        renew_client_secret_method: clientRegistrationProvider.renew_client_secret_method,
        software_id: clientRegistrationProvider.software_id,
      })
      .then((response) => {
        const clientRegistrationProvider = response.data;
        clientRegistrationProvider.scopes = clientRegistrationProvider.scopes || [];
        return clientRegistrationProvider;
      });
  }

  delete(clientRegistrationProvider: ClientRegistrationProvider): IHttpPromise<any> {
    return this.$http.delete(
      `${this.Constants.env.baseURL}/configuration/applications/registration/providers/` + clientRegistrationProvider.id,
    );
  }
}

export default ClientRegistrationProviderService;
