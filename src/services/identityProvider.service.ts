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

import { IdentityProvider } from '../entities/identityProvider';
import { IHttpPromise, IPromise } from 'angular';

class IdentityProviderService {
  private URL: string;

  constructor(private $http, Constants) {
    'ngInject';
    this.URL = `${Constants.orgBaseURL}/configuration/identities/`;
  }

  list(): IPromise<IdentityProvider[]> {
    return this.$http.get(this.URL).then(response => response.data);
  }

  get(id: string): IPromise<IdentityProvider> {
    return this.$http.get(this.URL + id).then(response => {
      let identityProvider = response.data;
      identityProvider.configuration = identityProvider.configuration || {};
      identityProvider.configuration.scopes = identityProvider.configuration.scopes || [];

      // Init group mapping
      identityProvider.groupMappings = identityProvider.groupMappings || [];

      // Init role mapping
      identityProvider.roleMappings = identityProvider.roleMappings || [];

      // Init user mapping
      identityProvider.userProfileMapping = identityProvider.userProfileMapping || {};

      return identityProvider;
    });
  }

  create(identityProvider: IdentityProvider): IHttpPromise<IdentityProvider> {
    return this.$http.post(this.URL, identityProvider);
  }

  update(identityProvider: IdentityProvider): IPromise<IdentityProvider> {

    return this.$http.put(this.URL + identityProvider.id,
      {
        name: identityProvider.name,
        description: identityProvider.description,
        configuration: identityProvider.configuration,
        enabled: identityProvider.enabled,
        groupMappings: identityProvider.groupMappings,
        roleMappings: identityProvider.roleMappings,
        userProfileMapping: identityProvider.userProfileMapping,
        emailRequired: identityProvider.emailRequired,
        syncMappings: identityProvider.syncMappings
      }).then(response => {
      let identityProvider = response.data;

      identityProvider.configuration = identityProvider.configuration || {};
      identityProvider.configuration.scopes = identityProvider.configuration.scopes || [];

      // Init group mapping
      identityProvider.groupMappings = identityProvider.groupMappings || [];

      // Init role mapping
      identityProvider.roleMappings = identityProvider.roleMappings || [];

      // Init user mapping
      identityProvider.userProfileMapping = identityProvider.userProfileMapping || {};

      return identityProvider;
    });
  }

  delete(identityProvider: IdentityProvider): IHttpPromise<any> {
    return this.$http.delete(this.URL + identityProvider.id);
  }
}

export default IdentityProviderService;
