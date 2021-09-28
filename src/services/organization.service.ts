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

import { IdentityProviderActivation } from '../entities/identity-provider';

export class Organization {
  hrids: string[];
  domainRestrictions: string[];
  name: string;
  description: string;
  flows: any[];
  flowMode: string;
}

class OrganizationService {
  constructor(private $http, private Constants) {
    'ngInject';
  }

  listSocialIdentityProviders() {
    return this.$http.get(`${this.Constants.org.baseURL}` + '/social-identities');
  }

  listOrganizationIdentities() {
    return this.$http.get(`${this.Constants.org.baseURL}/identities`);
  }

  updateOrganizationIdentities(updatedIPA: Partial<IdentityProviderActivation>[]) {
    return this.$http.put(`${this.Constants.org.baseURL}/identities`, updatedIPA);
  }

  update(organization: Organization) {
    return this.$http.put(`${this.Constants.org.baseURL}`, organization);
  }

  get() {
    return this.$http.get(`${this.Constants.org.baseURL}`);
  }
}

export default OrganizationService;
