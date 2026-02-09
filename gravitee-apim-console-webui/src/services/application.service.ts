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
import { clone, forEach, startsWith } from 'lodash';

import { PagedResult } from '../entities/pagedResult';
import { ApiKeyMode } from '../entities/application/Application';

export class LogsQuery {
  from: number;
  to: number;
  query?: string;
  page: number;
  size: number;
  field: string;
  order: boolean;
}

interface IMembership {
  id?: string;
  reference?: string;
  role: string;
}

export type ApplicationExcludeFilter = 'owner' | 'picture';

class ApplicationService {
  constructor(
    private $http: ng.IHttpService,
    private Constants,
  ) {}

  getAnalyticsHttpTimeout() {
    return this.Constants.env.settings.analytics.clientTimeout as number;
  }

  get(applicationId: string): ng.IHttpPromise<any> {
    return this.$http.get(`${this.applicationURL(applicationId)}`);
  }

  getApplicationType(applicationId: string): ng.IHttpPromise<any> {
    return this.$http.get(`${this.applicationURL(applicationId)}` + '/configuration');
  }

  getMembers(applicationId): ng.IHttpPromise<any> {
    return this.$http.get(`${this.applicationURL(applicationId)}` + '/members');
  }

  addOrUpdateMember(applicationId: string, membership: IMembership): ng.IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/applications/${applicationId}/members`, membership);
  }

  deleteMember(applicationId: string, userId: string): ng.IHttpPromise<any> {
    return this.$http.delete(`${this.applicationURL(applicationId)}` + '/members?user=' + userId);
  }

  transferOwnership(applicationId: string, ownership: IMembership): ng.IHttpPromise<any> {
    return this.$http.post(`${this.applicationURL(applicationId)}` + '/members/transfer_ownership', ownership);
  }

  listByIdIn(ids: string[] = [], status = 'active'): ng.IPromise<any> {
    if (ids.length === 0) {
      return Promise.resolve({ data: [] });
    }
    return this.list(null, null, status, ids);
  }

  list(exclude: ApplicationExcludeFilter[] = [], query = '', status = 'active', ids?: string[]): ng.IHttpPromise<any> {
    let url = `${this.Constants.env.baseURL}/applications/?status=${status}`;

    if (query && query.trim() !== '') {
      url += `&query=${query}`;
    }

    if (ids) {
      url += '&' + ids.map((i) => `ids=${i}`).join('&');
    }

    if (exclude && exclude.length > 0) {
      const excludeFilters = exclude.map((filter) => `exclude=${filter}`).join('&');
      url += `&${excludeFilters}`;
    }
    return this.$http.get(url);
  }

  searchPage(query, page = 1, size = 100) {
    return this.listPage(['picture'], query, page, size);
  }

  listPage(
    exclude: ApplicationExcludeFilter[] = [],
    query = '',
    page?: number,
    size?: number,
    order?: string,
    status = 'active',
  ): ng.IHttpPromise<any> {
    let url = `${this.Constants.env.baseURL}/applications/_paged?status=${status}`;

    if (query.trim() !== '') {
      url += `&query=${query}`;
    }

    if (page != null) {
      url += `&page=${page}`;
    }

    if (size != null) {
      url += `&size=${size}`;
    }

    if (order != null) {
      url += `&order=${order}`;
    }

    if (exclude.length > 0) {
      const excludeFilters = exclude.map((filter) => `exclude=${filter}`).join('&');
      url += `&${excludeFilters}`;
    }

    return this.$http.get(url);
  }

  create(application): ng.IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/applications/`, application);
  }

  update(application): ng.IHttpPromise<any> {
    return this.$http.put(`${this.Constants.env.baseURL}/applications/` + application.id, {
      name: application.name,
      description: application.description,
      domain: application.domain,
      groups: application.groups,
      settings: application.settings,
      picture: application.picture,
      picture_url: application.picture_url,
      disable_membership_notifications: application.disable_membership_notifications,
      api_key_mode: application.api_key_mode,
      background: application.background,
    });
  }

  delete(applicationId: string): ng.IHttpPromise<any> {
    return this.$http.delete(`${this.applicationURL(applicationId)}`);
  }

  search(query) {
    return this.list(['picture'], query);
  }

  /*
   * Subscriptions
   */
  subscribe(applicationId: string, planId: string, request?: string, apiKeyMode?: ApiKeyMode): ng.IHttpPromise<any> {
    const data = {
      ...(request ? { request } : ''),
      ...(apiKeyMode ? { apiKeyMode } : ''),
    };
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
    return this.$http.get(`${this.applicationURL(applicationId)}` + '/subscribed');
  }

  getSubscription(applicationId, subscriptionId) {
    return this.$http.get(this.subscriptionsURL(applicationId) + subscriptionId);
  }

  closeSubscription(applicationId, subscriptionId) {
    return this.$http.delete(this.subscriptionsURL(applicationId) + subscriptionId);
  }

  listApiKeys(application, subscription): ng.IHttpPromise<any> {
    if (subscription) {
      return this.$http.get(this.subscriptionsURL(application.id) + subscription.id + '/apikeys');
    } else {
      return this.$http.get(this.applicationURL(application.id) + '/apikeys');
    }
  }

  renewApiKey(application, subscription) {
    if (subscription) {
      return this.$http.post(this.subscriptionsURL(application.id) + subscription.id + '/apikeys/_renew', '');
    } else {
      return this.$http.post(this.applicationURL(application.id) + '/apikeys/_renew', '');
    }
  }

  revokeApiKey(application, subscription, apiKeyId) {
    if (subscription) {
      return this.$http.delete(this.subscriptionsURL(application.id) + subscription.id + '/apikeys/' + apiKeyId);
    } else {
      return this.$http.delete(this.applicationURL(application.id) + '/apikeys/' + apiKeyId);
    }
  }

  /*
   * Analytics
   */
  analytics(application, request) {
    let url = `${this.Constants.env.baseURL}/applications/` + application + '/analytics?';

    const keys = Object.keys(request);
    forEach(keys, (key) => {
      const val = request[key];
      if (val !== undefined && val !== '') {
        url += key + '=' + val + '&';
      }
    });

    return this.$http.get(url, { timeout: this.getAnalyticsHttpTimeout() });
  }

  findLogs(application: string, query: LogsQuery): ng.IPromise<any> {
    return this.$http.get(
      this.buildURLWithQuery(this.cloneQuery(query), `${this.Constants.env.baseURL}/applications/` + application + '/logs?'),
      { timeout: 30000 },
    );
  }

  exportLogsAsCSV(application: string, query: LogsQuery): ng.IPromise<any> {
    const logsQuery = this.cloneQuery(query);
    logsQuery.page = 1;
    logsQuery.size = 10000;
    return this.$http.get(
      this.buildURLWithQuery(logsQuery, `${this.Constants.env.baseURL}/applications/` + application + '/logs/export?'),
      { timeout: 30000 },
    );
  }

  getLog(api, logId, timestamp) {
    return this.$http.get(
      `${this.Constants.env.baseURL}/applications/` + api + '/logs/' + logId + (timestamp ? '?timestamp=' + timestamp : ''),
    );
  }

  getPermissions(application): ng.IPromise<IHttpResponse<any>> {
    return this.$http.get(`${this.Constants.env.baseURL}/applications/` + application + '/members/permissions');
  }

  renewClientSecret(applicationId: string): ng.IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/applications/${applicationId}/renew_secret`, {});
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
    return this.$http.get(`${this.applicationURL(applicationId)}` + '/metadata');
  }

  createMetadata(applicationId, metadata): ng.IPromise<any> {
    return this.$http.post(`${this.applicationURL(applicationId)}` + '/metadata', metadata);
  }

  updateMetadata(applicationId, metadata): ng.IPromise<any> {
    return this.$http.put(`${this.applicationURL(applicationId)}` + '/metadata/' + metadata.key, metadata);
  }

  deleteMetadata(applicationId, metadataId): ng.IPromise<any> {
    return this.$http.delete(`${this.applicationURL(applicationId)}` + '/metadata/' + metadataId);
  }

  private applicationURL(applicationId: string): string {
    return `${this.Constants.env.baseURL}/applications/${applicationId}`;
  }

  private subscriptionsURL(applicationId: string): string {
    return `${this.applicationURL(applicationId)}/subscriptions/`;
  }

  /*
   * Logs
   */
  private buildURLWithQuery(query: LogsQuery, url) {
    const keys = Object.keys(query);
    forEach(keys, (key) => {
      const val = query[key];
      if (val !== undefined && val !== '') {
        url += key + '=' + val + '&';
      }
    });
    return url;
  }

  private cloneQuery(query: LogsQuery) {
    const clonedQuery = clone(query);
    if (startsWith(clonedQuery.field, '-')) {
      clonedQuery.order = false;
      clonedQuery.field = clonedQuery.field.substring(1);
    } else {
      clonedQuery.order = true;
    }
    return clonedQuery;
  }
}
ApplicationService.$inject = ['$http', 'Constants'];

export default ApplicationService;
