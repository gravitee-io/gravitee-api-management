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
import { PagedResult } from '../entities/pagedResult';
import { IHttpResponse } from 'angular';

export class LogsQuery {
  from: number;
  to: number;
  query?: string;
  page: number;
  size: number;
  field: string;
  order: boolean;
}

interface IMember {
  username: string;
  type: string;
  role: string;
}

interface IMembership {
  id?: string;
  reference?: string;
  role: string;
}

class ApplicationService {
  private applicationsURL: string;

  constructor(private $http: ng.IHttpService, private Constants) {
    'ngInject';
    this.applicationsURL = `${Constants.env.baseURL}/applications/`;
  }

  getAnalyticsHttpTimeout() {
    return this.Constants.env.settings.analytics.clientTimeout as number;
  }


  get(applicationId: string): ng.IHttpPromise<any> {
    return this.$http.get(this.applicationsURL + applicationId);
  }

  getApplicationType(applicationId: string): ng.IHttpPromise<any> {
    return this.$http.get(this.applicationsURL + applicationId + '/configuration');
  }

  getMembers(applicationId): ng.IHttpPromise<any> {
    return this.$http.get(this.applicationsURL + applicationId + '/members');
  }

  addOrUpdateMember(applicationId: string, membership: IMembership): ng.IHttpPromise<any> {
    return this.$http.post(`${this.applicationsURL}${applicationId}/members`, membership);
  }

  deleteMember(applicationId: string, userId: string): ng.IHttpPromise<any> {
    return this.$http.delete(this.applicationsURL + applicationId + '/members?user=' + userId);
  }

  transferOwnership(applicationId: string, ownership: IMembership): ng.IHttpPromise<any> {
    return this.$http.post(this.applicationsURL + applicationId + '/members/transfer_ownership', ownership);
  }

  list(): ng.IHttpPromise<any> {
    return this.$http.get(this.applicationsURL);
  }

  listByGroup(group) {
    return this.$http.get(this.applicationsURL + '?group=' + group);
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
        'groups': application.groups,
        'settings': application.settings,
        'picture': application.picture,
        'picture_url': application.picture_url,
        'disable_membership_notifications': application.disable_membership_notifications,
        'background': application.background
      }
    );
  }

  delete(applicationId: string): ng.IHttpPromise<any> {
    return this.$http.delete(this.applicationsURL + applicationId);
  }

  search(query) {
    return this.$http.get(this.applicationsURL + '?query=' + query);
  }

  /*
   * Subscriptions
   */
  subscribe(applicationId: string, planId: string, request?: string): ng.IHttpPromise<any> {
    let data;
    if (request) {
      data = { request: request };
    } else {
      data = '';
    }
    return this.$http.post(this.subscriptionsURL(applicationId) + '?plan=' + planId, data);
  }

  listSubscriptions(applicationId: string, query?: string): ng.IHttpPromise<PagedResult> {
    let req = this.subscriptionsURL(applicationId);
    if (query !== undefined) {
      req += query;
    }

    return this.$http.get(req);
  }

  getSubscribedAPI(applicationId: string): ng.IHttpPromise<any> {
    return this.$http.get(this.applicationsURL + applicationId + '/subscribed');
  }

  getSubscription(applicationId, subscriptionId) {
    return this.$http.get(this.subscriptionsURL(applicationId) + subscriptionId);
  }

  closeSubscription(applicationId, subscriptionId) {
    return this.$http.delete(this.subscriptionsURL(applicationId) + subscriptionId);
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

    return this.$http.get(url, { timeout: this.getAnalyticsHttpTimeout() });
  }

  findLogs(application: string, query: LogsQuery): ng.IPromise<any> {
    return this.$http.get(this.buildURLWithQuery(this.cloneQuery(query), this.applicationsURL + application + '/logs?'), { timeout: 30000 });
  }

  exportLogsAsCSV(application: string, query: LogsQuery): ng.IPromise<any> {
    return this.$http.get(this.buildURLWithQuery(this.cloneQuery(query), this.applicationsURL + application + '/logs/export?'), { timeout: 30000 });
  }

  getLog(api, logId, timestamp) {
    return this.$http.get(this.applicationsURL + api + '/logs/' + logId + ((timestamp) ? '?timestamp=' + timestamp : ''));
  }

  getPermissions(application): ng.IPromise<IHttpResponse<any>> {
    return this.$http.get(this.applicationsURL + application + '/members/permissions');
  }

  renewClientSecret(applicationId: string): ng.IHttpPromise<any> {
    return this.$http.post(`${this.applicationsURL}${applicationId}/renew_secret`, {});
  }

  getType(application: any): string {
    let applicationType = 'Simple';
    if (application.settings) {
      if (application.settings.app && application.settings.app.type) {
        applicationType = application.settings.app.type;
      } else if (application.settings.oauth) {
        switch (application.settings.oauth.application_type) {
          case 'backend_to_backend':
            applicationType = 'Backend to backend';
            break;
          case 'browser':
            applicationType = 'Browser';
            break;
          case 'native':
            applicationType = 'Native';
            break;
          case 'web':
            applicationType = 'Web';
            break;
        }
      }
    }
    return applicationType;
  }

  listMetadata(applicationId): ng.IPromise<any> {
    return this.$http.get(this.applicationsURL + applicationId + '/metadata');
  }

  createMetadata(applicationId, metadata): ng.IPromise<any> {
    return this.$http.post(this.applicationsURL + applicationId + '/metadata', metadata);
  }

  updateMetadata(applicationId, metadata): ng.IPromise<any> {
    return this.$http.put(this.applicationsURL + applicationId + '/metadata/' + metadata.key, metadata);
  }

  deleteMetadata(applicationId, metadataId): ng.IPromise<any> {
    return this.$http.delete(this.applicationsURL + applicationId + '/metadata/' + metadataId);
  }

  private subscriptionsURL(applicationId: string): string {
    return `${this.applicationsURL}${applicationId}/subscriptions/`;
  }

  /*
   * Logs
   */
  private buildURLWithQuery(query: LogsQuery, url) {
    var keys = Object.keys(query);
    _.forEach(keys, function (key) {
      var val = query[key];
      if (val !== undefined && val !== '') {
        url += key + '=' + val + '&';
      }
    });
    return url;
  }

  private cloneQuery(query: LogsQuery) {
    let clonedQuery = _.clone(query);
    if (_.startsWith(clonedQuery.field, '-')) {
      clonedQuery.order = false;
      clonedQuery.field = clonedQuery.field.substring(1);
    } else {
      clonedQuery.order = true;
    }
    return clonedQuery;
  }
}

export default ApplicationService;
