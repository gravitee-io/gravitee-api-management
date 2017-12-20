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

import * as _ from 'lodash';
import { PagedResult } from "../entities/pagedResult";

export class LogsQuery {
  from: number;
  to: number;
  query?: string;
  page: number;
  size: number;
}

interface IMember {
  username: string;
  type: string;
  role: string;
}

class ApplicationService {
  private applicationsURL: string;

  constructor(private $http: ng.IHttpService, Constants) {
    'ngInject';
    this.applicationsURL = `${Constants.baseURL}applications/`;
  }

  private subscriptionsURL(applicationId: string): string {
    return `${this.applicationsURL}${applicationId}/subscriptions/`;
  }

	get(applicationId: string): ng.IHttpPromise<any> {
    return this.$http.get(this.applicationsURL + applicationId);
  }

	getMembers(applicationId): ng.IHttpPromise<any> {
		return this.$http.get(this.applicationsURL + applicationId + '/members');
	}

	addOrUpdateMember(applicationId: string, member: IMember): ng.IHttpPromise<any> {
    const url = `${this.applicationsURL}${applicationId}/members?user=${member.username}&type=${member.type}&rolename=${member.role}`;
    return this.$http.post(url, '');
	}

	deleteMember(applicationId, memberUsername): ng.IHttpPromise<any> {
		return this.$http.delete(this.applicationsURL + applicationId + '/members?user=' + memberUsername);
	}

  transferOwnership(applicationId, memberUsername, newRole: string): ng.IHttpPromise<any> {
    return this.$http.post(this.applicationsURL + applicationId + '/members/transfer_ownership?user=' + memberUsername, {
      role: newRole
    });
  }

  list(): ng.IHttpPromise<any> {
    return this.$http.get(this.applicationsURL);
  }

  listByGroup(group) {
    return this.$http.get(this.applicationsURL + "?group=" + group);
  }

	create(application): ng.IHttpPromise<any> {
    return this.$http.post(this.applicationsURL, application);
  }

  update(application): ng.IHttpPromise<any> {
    return this.$http.put(
      this.applicationsURL + application.id,
      {
        'name': application.name,
        'description': application.description,
        'type': application.type,
        'clientId': application.clientId,
        'groups': application.groups
      }
    );
  }

  delete(applicationId: string): ng.IHttpPromise<any> {
    return this.$http.delete(this.applicationsURL + applicationId);
  }

  search(query) {
    return this.$http.get(this.applicationsURL + "?query=" + query);
  }

  /*
   * Subscriptions
   */
  subscribe(applicationId, planId): ng.IHttpPromise<any> {
    return this.$http.post(this.subscriptionsURL(applicationId) + '?plan=' + planId, '');
  }

  listSubscriptions(applicationId: string, query?: string): ng.IHttpPromise<PagedResult> {
    let req = this.subscriptionsURL(applicationId);
    if (query !== undefined) {
      req += query;
    }

    return this.$http.get(req);
  }

  getSubscription(applicationId, subscriptionId) {
    return this.$http.get(this.subscriptionsURL(applicationId) + subscriptionId);
  }

  listApiKeys(applicationId, subscriptionId): ng.IHttpPromise<any> {
    return this.$http.get(this.subscriptionsURL(applicationId) + subscriptionId + '/keys');
  }

  renewApiKey(applicationId, subscriptionId) {
    return this.$http.post(this.subscriptionsURL(applicationId) + subscriptionId, '');
  }

  revokeApiKey(applicationId, subscriptionId, apiKey) {
    return this.$http.delete(this.subscriptionsURL(applicationId) + subscriptionId + '/keys/' + apiKey);
  }

  /*
   * Analytics
   */
  analytics(application, request) {
    var url = this.applicationsURL + application + '/analytics?';

    var keys = Object.keys(request);
    _.forEach(keys, function (key) {
      var val = request[key];
      if (val !== undefined && val !== '') {
        url += key + '=' + val + '&';
      }
    });

    return this.$http.get(url, {timeout: 30000});
  }

  /*
   * Logs
   */
  findLogs(api: string, query: LogsQuery): any {
    var url = this.applicationsURL + api + '/logs?';

    var keys = Object.keys(query);
    _.forEach(keys, function (key) {
      var val = query[key];
      if (val !== undefined && val !== '') {
        url += key + '=' + val + '&';
      }
    });

    return this.$http.get(url, {timeout: 30000});
  }

  getLog(api, logId) {
    return this.$http.get(this.applicationsURL + api + '/logs/' + logId);
  }

  getPermissions(application) {
    return this.$http.get(this.applicationsURL + application + '/members/permissions');
  }
}

export default ApplicationService;
