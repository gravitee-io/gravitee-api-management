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

import { IdentityProviderActivation } from '../entities/identityProvider';

class ConsoleService {
  private consoleURL: string;

  constructor(private $http, Constants, private $q) {
    'ngInject';
    this.consoleURL = `${Constants.org.baseURL}`;
  }

  listSocialIdentityProviders() {
    return this.$http.get(this.consoleURL + '/social-identities');
  }

  listOrganizationIdentities() {
    return this.$http.get(`${this.consoleURL}/identities`);
  }

  updateOrganizationIdentities(updatedIPA: IdentityProviderActivation[]) {
    return this.$http.put(`${this.consoleURL}/identities`, updatedIPA);
  }
}

export default ConsoleService;
