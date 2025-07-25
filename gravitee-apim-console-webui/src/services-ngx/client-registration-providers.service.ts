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
import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Constants } from '../entities/Constants';
import { ClientRegistrationProvider } from '../entities/client-registration-provider/clientRegistrationProvider';

@Injectable({
  providedIn: 'root',
})
export class ClientRegistrationProvidersService {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  list(): Observable<ClientRegistrationProvider[]> {
    return this.http.get<ClientRegistrationProvider[]>(`${this.constants.env.baseURL}/configuration/applications/registration/providers`);
  }

  get(id: string): Observable<ClientRegistrationProvider> {
    return this.http.get<ClientRegistrationProvider>(
      `${this.constants.env.baseURL}/configuration/applications/registration/providers/${id}`,
    );
  }

  create(clientRegistrationProvider: ClientRegistrationProvider): Observable<ClientRegistrationProvider> {
    return this.http.post<ClientRegistrationProvider>(
      `${this.constants.env.baseURL}/configuration/applications/registration/providers`,
      clientRegistrationProvider,
    );
  }

  update(clientRegistrationProvider: ClientRegistrationProvider): Observable<ClientRegistrationProvider> {
    return this.http.put<ClientRegistrationProvider>(
      `${this.constants.env.baseURL}/configuration/applications/registration/providers/${clientRegistrationProvider.id}`,
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
        software_id: clientRegistrationProvider.software_id,
        trust_store: clientRegistrationProvider.trust_store,
        key_store: clientRegistrationProvider.key_store,
      },
    );
  }

  delete(clientRegistrationProviderId: string): Observable<any> {
    return this.http.delete(
      `${this.constants.env.baseURL}/configuration/applications/registration/providers/${clientRegistrationProviderId}`,
    );
  }
}
