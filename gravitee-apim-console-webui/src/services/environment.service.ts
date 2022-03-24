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

import { IHttpResponse } from 'angular';
import * as _ from 'lodash';

import { EnvironmentPermissions } from '../entities/environment/environmentPermissions';
import { IdentityProviderActivation } from '../entities/identity-provider';

class EnvironmentService {
  constructor(private $http, private Constants, private $q) {
    'ngInject';
  }

  /*
   * Analytics
   */
  analytics(request) {
    let url = this.Constants.env.baseURL + '/analytics?';
    const keys = Object.keys(request);
    _.forEach(keys, (key) => {
      const val = request[key];
      if (val !== undefined) {
        url += key + '=' + val + '&';
      }
    });

    return this.$http.get(url, { timeout: this.getAnalyticsHttpTimeout() });
  }

  getAnalyticsHttpTimeout() {
    return this.Constants.env.settings.analytics.clientTimeout as number;
  }

  list() {
    return this.$http.get(`${this.Constants.org.baseURL}/environments`);
  }

  getCurrent(): ng.IPromise<any> {
    return this.$http.get(this.Constants.env.baseURL);
  }

  listEnvironmentIdentities(envId: string) {
    return this.$http.get(`${this.Constants.org.baseURL}/environments/${envId}/identities`);
  }

  updateEnvironmentIdentities(envId: string, updatedIPA: Partial<IdentityProviderActivation>[]) {
    return this.$http.put(`${this.Constants.org.baseURL}/environments/${envId}/identities`, updatedIPA);
  }

  getPermissions(envId: string): ng.IPromise<IHttpResponse<EnvironmentPermissions[]>> {
    return this.$http.get(`${this.Constants.org.baseURL}/environments/permissions?idOrHrid=${envId}`);
  }

  getFirstHridOrElseId(environment): string {
    return environment && environment.hrids && environment.hrids.length > 0 ? environment.hrids[0] : environment && environment.id;
  }

  isSameEnvironment(environment, otherEnvId) {
    return environment && (environment.id === otherEnvId || (environment.hrids && environment.hrids.includes(otherEnvId)));
  }

  getEnvironmentFromHridOrId(environments, id) {
    return environments?.find((environment) => environment.id === id || (environment.hrids && environment.hrids.includes(id)));
  }
}

export default EnvironmentService;
