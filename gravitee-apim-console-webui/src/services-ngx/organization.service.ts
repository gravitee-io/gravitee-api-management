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
import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Constants } from '../entities/Constants';
import { IdentityProviderActivation } from '../entities/identity-provider';
import { Organization } from '../entities/organization/organization';

@Injectable({
  providedIn: 'root',
})
export class OrganizationService {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  listActivatedIdentityProviders(): Observable<IdentityProviderActivation[]> {
    return this.http.get<IdentityProviderActivation[]>(`${this.constants.org.baseURL}/identities`);
  }

  updateActivatedIdentityProviders(idpsToActivate: { identityProvider: string }[]): Observable<void> {
    return this.http.put<void>(`${this.constants.org.baseURL}/identities`, idpsToActivate);
  }

  get(): Observable<Organization> {
    return this.http.get<Organization>(`${this.constants.org.baseURL}`);
  }

  update(organization: Organization): Observable<void> {
    return this.http.put<void>(`${this.constants.org.baseURL}`, organization);
  }
}
