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

import { ClientRegistrationProvider } from '../entities/clientRegistrationProvider';
import { IHttpPromise, IPromise } from 'angular';

class ClientRegistrationProviderService {
  private URL: string;

  constructor(private $http, Constants) {
    'ngInject';
    this.URL = `${Constants.envBaseURL}/configuration/applications/registration/providers/`;
  }

  list(): IPromise<ClientRegistrationProvider[]> {
    return this.$http.get(this.URL).then(response => response.data);
  }

  get(id: string): IPromise<ClientRegistrationProvider> {
    return this.$http.get(this.URL + id).then(response => {
      let clientRegistrationProvider = response.data;
      clientRegistrationProvider.scopes = clientRegistrationProvider.scopes || [];
      return clientRegistrationProvider;
    });
  }

  create(clientRegistrationProvider: ClientRegistrationProvider): IHttpPromise<ClientRegistrationProvider> {
    return this.$http.post(this.URL, clientRegistrationProvider);
  }

  update(clientRegistrationProvider: ClientRegistrationProvider): IPromise<ClientRegistrationProvider> {

    return this.$http.put(this.URL + clientRegistrationProvider.id,
      {
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
        software_id: clientRegistrationProvider.software_id
      }).then(response => {
      let clientRegistrationProvider = response.data;
      clientRegistrationProvider.scopes = clientRegistrationProvider.scopes || [];
      return clientRegistrationProvider;
    });
  }

  delete(clientRegistrationProvider: ClientRegistrationProvider): IHttpPromise<any> {
    return this.$http.delete(this.URL + clientRegistrationProvider.id);
  }
}

export default ClientRegistrationProviderService;
