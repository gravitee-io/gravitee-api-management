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
import { AuthenticationStrategy } from '../entities/authentication-strategy/authenticationStrategy';

@Injectable({
  providedIn: 'root',
})
export class AuthenticationStrategyService {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  list(): Observable<AuthenticationStrategy[]> {
    return this.http.get<AuthenticationStrategy[]>(
      `${this.constants.env.baseURL}/configuration/applications/authentication-strategies`,
    );
  }

  get(id: string): Observable<AuthenticationStrategy> {
    return this.http.get<AuthenticationStrategy>(
      `${this.constants.env.baseURL}/configuration/applications/authentication-strategies/${id}`,
    );
  }

  create(strategy: AuthenticationStrategy): Observable<AuthenticationStrategy> {
    return this.http.post<AuthenticationStrategy>(
      `${this.constants.env.baseURL}/configuration/applications/authentication-strategies`,
      strategy,
    );
  }

  update(strategy: AuthenticationStrategy): Observable<AuthenticationStrategy> {
    return this.http.put<AuthenticationStrategy>(
      `${this.constants.env.baseURL}/configuration/applications/authentication-strategies/${strategy.id}`,
      {
        name: strategy.name,
        display_name: strategy.display_name,
        description: strategy.description,
        type: strategy.type,
        client_registration_provider_id: strategy.client_registration_provider_id,
        scopes: strategy.scopes,
        auth_methods: strategy.auth_methods,
        credential_claims: strategy.credential_claims,
        auto_approve: strategy.auto_approve,
        hide_credentials: strategy.hide_credentials,
      },
    );
  }

  delete(id: string): Observable<any> {
    return this.http.delete(
      `${this.constants.env.baseURL}/configuration/applications/authentication-strategies/${id}`,
    );
  }
}
