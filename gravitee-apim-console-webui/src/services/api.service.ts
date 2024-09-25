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
import { IHttpPromise, IHttpResponse, IHttpService, IPromise, IRootScopeService } from 'angular';

import { clone } from 'lodash';

import { ApplicationExcludeFilter } from './application.service';

import { Constants } from '../entities/Constants';
import { PagedResult } from '../entities/pagedResult';
import { IfMatchEtagInterceptor } from '../shared/interceptors/if-match-etag.interceptor';
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

export class ApiService {
  constructor(
    private readonly $http: IHttpService,
    private readonly $rootScope: IRootScopeService,
    private readonly Constants: Constants,
    private readonly ngIfMatchEtagInterceptor: IfMatchEtagInterceptor,
  ) {}

  defaultHttpHeaders(): string[] {
    return [
      'Accept',
      'Accept-Charset',
      'Accept-Encoding',
      'Accept-Language',
      'Accept-Ranges',
      'Access-Control-Allow-Credentials',
      'Access-Control-Allow-Headers',
      'Access-Control-Allow-Methods',
      'Access-Control-Allow-Origin',
      'Access-Control-Expose-Headers',
      'Access-Control-Max-Age',
      'Access-Control-Request-Headers',
      'Access-Control-Request-Method',
      'Age',
      'Allow',
      'Authorization',
      'Cache-Control',
      'Connection',
      'Content-Disposition',
      'Content-Encoding',
      'Content-ID',
      'Content-Language',
      'Content-Length',
      'Content-Location',
      'Content-MD5',
      'Content-Range',
      'Content-Type',
      'Cookie',
      'Date',
      'ETag',
      'Expires',
      'Expect',
      'Forwarded',
      'From',
      'Host',
      'If-Match',
      'If-Modified-Since',
      'If-None-Match',
      'If-Unmodified-Since',
      'Keep-Alive',
      'Last-Modified',
      'Location',
      'Link',
      'Max-Forwards',
      'MIME-Version',
      'Origin',
      'Pragma',
      'Proxy-Authenticate',
      'Proxy-Authorization',
      'Proxy-Connection',
      'Range',
      'Referer',
      'Retry-After',
      'Server',
      'Set-Cookie',
      'Set-Cookie2',
      'TE',
      'Trailer',
      'Transfer-Encoding',
      'Upgrade',
      'User-Agent',
      'Vary',
      'Via',
      'Warning',
      'WWW-Authenticate',
      'X-Forwarded-For',
      'X-Forwarded-Proto',
      'X-Forwarded-Server',
      'X-Forwarded-Host',
    ];
  }

  get(name: string): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + name);
  }

  getAnalyticsHttpTimeout(): number {
    return this.Constants.env.settings.analytics.clientTimeout as number;
  }

  listByIdIn(ids: string[] = []): IPromise<any> {
    if (ids.length === 0) {
      return Promise.resolve({ data: [] });
    }
    return this.list(null, null, null, null, null, ids);
  }

  list(category?: string, portal?: boolean, page?: any, order?: string, opts?: any, ids?: string[], size?: number): IHttpPromise<any> {
    let url = `${this.Constants.env.baseURL}/apis/`;

    // Fallback to paginated list if a page parameter is provided.
    if (page != null) {
      url += '_paged';
    }

    opts = opts || {};
    opts.params = {
      category: category,
      portal: portal,
      page: page,
      size: size,
      order: order,
      ids: ids,
    };

    return this.$http.get(url, opts);
  }

  searchApis(query?: string, page?: any, order?: string, opts?: any, size?: number, manageOnly?: boolean): IHttpPromise<any> {
    let url = `${this.Constants.env.baseURL}/apis/_search/`;

    // Fallback to paginated search if a page parameter is provided.
    if (page != null) {
      url += '_paged';
    }

    opts = opts || {};
    opts.params = {
      q: query ? query : '*',
      page: page,
      ...(order ? { order: order } : {}),
      size,
      ...(manageOnly ? {} : { manageOnly: false }),
    };

    return this.$http.post(url, {}, opts);
  }

  listTopAPIs(): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + '?top=true');
  }

  listByGroup(group: string): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + '?group=' + group);
  }

  start(api: { id: string }): IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + api.id + '?action=START', {});
  }

  stop(api: { id: string }): IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + api.id + '?action=STOP', {});
  }

  reload(name: string): IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/reload/${name}`, {});
  }

  migrateApiToPolicyStudio(apiId: string): IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/${apiId}/_migrate`, {});
  }

  update(api): IHttpPromise<any> {
    return this.$http.put(`${this.Constants.env.baseURL}/apis/` + api.id, {
      version: api.version,
      description: api.description,
      proxy: api.proxy,
      paths: api.paths,
      flows: api.flows,
      plans: api.plans,
      private: api.private,
      visibility: api.visibility,
      name: api.name,
      services: api.services,
      properties: api.properties,
      tags: api.tags,
      picture: api.picture,
      picture_url: api.picture_url,
      background: api.background,
      background_url: api.background_url,
      resources: api.resources,
      categories: api.categories,
      groups: api.groups,
      labels: api.labels,
      path_mappings: api.path_mappings,
      response_templates: api.response_templates,
      lifecycle_state: api.lifecycle_state,
      disable_membership_notifications: api.disable_membership_notifications,
      flow_mode: api.flow_mode,
      gravitee: api.gravitee,
      execution_mode: api.execution_mode,
    });
  }

  delete(name: string): IHttpPromise<any> {
    return this.$http.delete(`${this.Constants.env.baseURL}/apis/` + name);
  }

  listPolicies(apiName: string): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + apiName + '/policies');
  }

  isAPISynchronized(apiId: string): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + apiId + '/state');
  }

  deploy(apiId: string, apiDeployment?: { deploymentLabel: any }): IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + apiId + '/deploy', apiDeployment);
  }

  rollback(apiId: string, apiDescriptor: any): IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + apiId + '/rollback', apiDescriptor);
  }

  import(apiId: string, apiDefinition: any, definitionVersion: string, isUrl: boolean): IHttpPromise<any> {
    const headers = { 'Content-Type': isUrl ? 'text/plain' : 'application/json' };
    const endpoint = isUrl ? 'import-url' : 'import';

    if (apiId) {
      return this.$http.put(`${this.Constants.env.baseURL}/apis/${apiId}/${endpoint}`, apiDefinition, { headers });
    }
    const params = definitionVersion ? `?definitionVersion=${definitionVersion}` : '';
    return this.$http.post(`${this.Constants.env.baseURL}/apis/${endpoint}${params}`, apiDefinition, { headers });
  }

  importSwagger(apiId: string, swaggerDescriptor: string, definitionVersion?: string, config?): IHttpPromise<any> {
    const params = definitionVersion ? `?definitionVersion=${definitionVersion}` : '';
    if (apiId) {
      return this.$http.put(`${this.Constants.env.baseURL}/apis/${apiId}/import/swagger${params}`, swaggerDescriptor, config);
    }
    return this.$http.post(`${this.Constants.env.baseURL}/apis/import/swagger${params}`, swaggerDescriptor, config);
  }

  export(apiId: string, exclude: any[], exportVersion: string): IHttpPromise<any> {
    return this.$http.get(
      `${this.Constants.env.baseURL}/apis/` +
        apiId +
        '/export?exclude=' +
        exclude.join(',') +
        (exportVersion ? '&version=' + exportVersion : ''),
    );
  }

  exportCrd(apiId: string): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + apiId + '/crd');
  }

  verify(criteria: any, config?: any): IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/verify`, criteria, config);
  }

  importPathMappings(apiId: string, page: any, definitionVersion?: string): IHttpPromise<any> {
    let params = `?page=${page}`;

    if (definitionVersion) {
      params += `&definitionVersion=${definitionVersion}`;
    }

    return this.$http.post(`${this.Constants.env.baseURL}/apis/${apiId}/import-path-mappings${params}`, {});
  }

  duplicate(apiId: any, config: { context_path: any; version: any; filtered_fields: any[] }): IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/${apiId}/duplicate`, config);
  }

  /*
   * Analytics
   */
  analytics(apiId: string, request: any): IHttpPromise<any> {
    let url = `${this.Constants.env.baseURL}/apis/${apiId}/analytics?`;

    Object.keys(request).forEach((key) => {
      const val = request[key];
      if (val !== undefined && val !== '') {
        url += key + '=' + val + '&';
      }
    });

    return this.$http.get(url, { timeout: this.getAnalyticsHttpTimeout() });
  }

  findLogs(api: string, query: LogsQuery): IHttpPromise<any> {
    return this.$http.get(this.buildURLWithQuery(this.cloneQuery(query), `${this.Constants.env.baseURL}/apis/${api}/logs?`), {
      timeout: 30000,
    });
  }

  exportLogsAsCSV(api: string, query: LogsQuery): IHttpPromise<any> {
    const logsQuery = this.cloneQuery(query);
    logsQuery.page = 1;
    logsQuery.size = 10000;
    return this.$http.get(this.buildURLWithQuery(logsQuery, `${this.Constants.env.baseURL}/apis/${api}/logs/export?`), {
      timeout: 30000,
    });
  }

  getLog(api: any, logId: any, timestamp: string): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/${api}/logs/${logId}${timestamp ? '?timestamp=' + timestamp : ''}`);
  }

  /*
   * Members
   */
  getMembers(api: string): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/${api}/members`);
  }

  addOrUpdateMember(api: string, membership: IMembership): IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/${api}/members`, membership);
  }

  deleteMember(api: string, userId: string): IHttpPromise<any> {
    return this.$http.delete(`${this.Constants.env.baseURL}/apis/${api}/members?user=${userId}`);
  }

  transferOwnership(api: string, ownership: IMembership): IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/${api}/members/transfer_ownership`, ownership);
  }

  /*
   * Groups
   */
  getGroupsWithMembers(api: string): ng.IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/${api}/groups`);
  }

  /*
   * API events
   */
  getApiEvents(api: any, eventTypes: any): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/${api}/events?type=${eventTypes}`);
  }

  searchApiEvents(type: any, api: any, from: any, to: any, page: any, size: any, withPayload = false): IHttpPromise<any> {
    const params = {
      type,
      page,
      size,
    };
    if (from) {
      params['from'] = from;
    }
    if (to) {
      params['to'] = to;
    }
    if (withPayload) {
      params['withPayload'] = withPayload;
    }
    return this.$http.get(
      `${this.Constants.env.baseURL}/apis/${api}/events/search?${Object.entries(params)
        .map(([key, value]) => `${key}=${value}`)
        .join('&')}`,
    );
  }

  /*
   * API plans
   */
  getApiPlans(apiId: string, status?: string, security?: string): IHttpPromise<any> {
    return this.$http.get(
      `${this.Constants.env.baseURL}/apis/${apiId}/plans?status=${status ? status : 'staging,published,closed,deprecated'}${
        security ? '&security=' + security : ''
      }`,
    );
  }

  getApiPlan(apiId: string, planId: string): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/${apiId}/plans/${planId}`);
  }

  getPublishedApiPlans(apiId: string): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/${apiId}/plans?status=published`);
  }

  savePlan(api: { id: string }, plan: any): IHttpPromise<any> {
    let promise = null;
    if (plan.id) {
      promise = this.$http.put(`${this.Constants.env.baseURL}/apis/${api.id}/plans/${plan.id}`, {
        id: plan.id,
        name: plan.name,
        description: plan.description,
        validation: plan.validation,
        policies: plan.policies,
        securityDefinition: plan.securityDefinition,
        characteristics: plan.characteristics,
        order: plan.order,
        paths: plan.paths,
        flows: plan.flows,
        excluded_groups: plan.excluded_groups,
        comment_required: plan.comment_required,
        comment_message: plan.comment_message,
        tags: plan.tags,
        selection_rule: plan.selection_rule,
        general_conditions: plan.general_conditions,
      });
    } else {
      promise = this.$http.post(`${this.Constants.env.baseURL}/apis/${api.id}/plans`, {
        name: plan.name,
        description: plan.description,
        api: plan.api,
        validation: plan.validation,
        policies: plan.policies,
        characteristics: plan.characteristics,
        type: plan.type,
        paths: plan.paths,
        flows: plan.flows,
        security: plan.security,
        securityDefinition: plan.securityDefinition,
        excluded_groups: plan.excluded_groups,
        comment_required: plan.comment_required,
        comment_message: plan.comment_message,
        tags: plan.tags,
        selection_rule: plan.selection_rule,
        general_conditions: plan.general_conditions,
      });
    }
    return promise.then(async (response) => {
      await this.syncV2Api(api);
      return response;
    });
  }

  closePlan(api: { id: any }, planId: any): IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/${api.id}/plans/${planId}/_close`, {}).then(async (response) => {
      await this.syncV2Api(api);
      return response;
    });
  }

  publishPlan(api: { id: any }, planId: any): IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/${api.id}/plans/${planId}/_publish`, {}).then(async (response) => {
      await this.syncV2Api(api);
      return response;
    });
  }

  deprecatePlan(api: { id: any }, planId: any): IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/${api.id}/plans/${planId}/_deprecate`, {}).then(async (response) => {
      await this.syncV2Api(api);
      return response;
    });
  }

  /*
   * API subscriptions
   */
  getSubscriptions(apiId: string, query?: string): IHttpPromise<PagedResult> {
    let req = `${this.Constants.env.baseURL}/apis/${apiId}/subscriptions`;
    if (query !== undefined) {
      req += query;
    }

    return this.$http.get(req);
  }

  exportSubscriptionsAsCSV(apiId: string, query?: string): IPromise<string> {
    let req = `${this.Constants.env.baseURL}/apis/${apiId}/subscriptions/export`;
    if (query !== undefined) {
      req += query;
    }

    return this.$http.get<string>(req, { timeout: 30000 }).then((response) => response.data);
  }

  getSubscribers(apiId: string, query?: string, page?: number, size?: number, exclude: ApplicationExcludeFilter[] = []): IHttpPromise<any> {
    const queryParams: string[] = [];
    if (query) {
      queryParams.push(`query=${query}`);
    }
    if (page) {
      queryParams.push(`page=${page}`);
    }
    if (size) {
      queryParams.push(`size=${size}`);
    }
    if (exclude && exclude.length > 0) {
      queryParams.push(exclude.map((filter) => `exclude=${filter}`).join('&'));
    }
    return this.$http.get(`${this.Constants.env.baseURL}/apis/${apiId}/subscribers${queryParams ? '?' + queryParams.join('&') : ''}`);
  }

  getSubscription(apiId, subscriptionId): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/${apiId}/subscriptions/${subscriptionId}`);
  }

  closeSubscription(apiId, subscriptionId): IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/${apiId}/subscriptions/${subscriptionId}/status?status=CLOSED`, {});
  }

  pauseSubscription(apiId, subscriptionId) {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/${apiId}/subscriptions/${subscriptionId}/status?status=PAUSED`, {});
  }

  updateSubscription(apiId, subscription) {
    return this.$http.put(`${this.Constants.env.baseURL}/apis/${apiId}/subscriptions/${subscription.id}`, {
      id: subscription.id,
      starting_at: subscription.starting_at,
      ending_at: subscription.ending_at,
    });
  }

  resumeSubscription(apiId, subscriptionId) {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/${apiId}/subscriptions/${subscriptionId}/status?status=RESUMED`, {});
  }

  processSubscription(apiId, subscriptionId, processSubscription): IHttpPromise<any> {
    return this.$http.post(
      `${this.Constants.env.baseURL}/apis/` + apiId + '/subscriptions/' + subscriptionId + '/_process',
      processSubscription,
    );
  }

  transferSubscription(apiId, subscriptionId, transferSubscription): IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/${apiId}/subscriptions/${subscriptionId}/_transfer`, {
      id: transferSubscription.id,
      plan: transferSubscription.plan,
    });
  }

  getPlanSubscriptions(apiId, planId): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/${apiId}/subscriptions?plan=${planId}`);
  }

  getAllPlanSubscriptions(apiId, planId): IHttpPromise<any> {
    return this.$http.get(
      `${this.Constants.env.baseURL}/apis/${apiId}/subscriptions?plan=${planId}&status=accepted,pending,rejected,closed,paused`,
    );
  }

  subscribe(apiId: string, applicationId: string, planId: string, customApiKey: string, apiKeyMode?: ApiKeyMode): IHttpPromise<any> {
    const params = {
      params: {
        plan: planId,
        application: applicationId,
        customApiKey: customApiKey,
        ...(apiKeyMode ? { apiKeyMode } : ''),
      },
    };
    return this.$http.post(`${this.Constants.env.baseURL}/apis/${apiId}/subscriptions`, null, params);
  }

  listApiKeys(apiId: string, subscriptionId: string): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/${apiId}/subscriptions/${subscriptionId}/apikeys`);
  }

  revokeApiKey(apiId: string, subscriptionId: string, apiKeyId: string): IHttpPromise<any> {
    return this.$http.delete(`${this.Constants.env.baseURL}/apis/${apiId}/subscriptions/${subscriptionId}/apikeys/${apiKeyId}`);
  }

  reactivateApiKey(apiId: string, subscriptionId: string, apiKeyId: string): IHttpPromise<any> {
    return this.$http.post(
      `${this.Constants.env.baseURL}/apis/${apiId}/subscriptions/${subscriptionId}/apikeys/${apiKeyId}/_reactivate`,
      '',
    );
  }

  renewApiKey(apiId: string, subscriptionId: string, customApiKey: any): IHttpPromise<any> {
    const params = {
      params: {
        customApiKey: customApiKey,
      },
    };
    return this.$http.post(`${this.Constants.env.baseURL}/apis/${apiId}/subscriptions/${subscriptionId}/apikeys/_renew`, null, params);
  }

  updateApiKey(apiId: string, subscriptionId: string, apiKey: { id: string }): IHttpPromise<any> {
    return this.$http.put(`${this.Constants.env.baseURL}/apis/${apiId}/subscriptions/${subscriptionId}/apikeys/${apiKey.id}`, apiKey);
  }

  listApiMetadata(apiId: string): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/${apiId}/metadata`);
  }

  createMetadata(apiId: string, metadata: any): IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/${apiId}/metadata`, metadata);
  }

  updateMetadata(apiId: string, metadata: { key: string }): IHttpPromise<any> {
    return this.$http.put(`${this.Constants.env.baseURL}/apis/${apiId}/metadata/${metadata.key}`, metadata);
  }

  deleteMetadata(apiId: string, metadataId: string): IHttpPromise<any> {
    return this.$http.delete(`${this.Constants.env.baseURL}/apis/${apiId}/metadata/${metadataId}`);
  }

  getPermissions(api: string): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/${api}/members/permissions`);
  }

  /*
   * Health-check
   */
  apiHealth(api: string, type?: string, field?: string): IHttpPromise<any> {
    let req = `${this.Constants.env.baseURL}/apis/${api}/health`;
    if (type !== undefined) {
      req += '?type=' + type;
    }
    if (field !== undefined) {
      req += '&field=' + field;
    }

    return this.$http.get(req, { timeout: 30000 });
  }

  apiHealthLogs(api: string, query: LogsQuery): IHttpPromise<any> {
    let url = `${this.Constants.env.baseURL}/apis/${api}/health/logs?`;

    Object.keys(query).forEach((key) => {
      const val = query[key];
      if (val !== undefined && val !== '') {
        url += key + '=' + val + '&';
      }
    });

    return this.$http.get(url, { timeout: 30000 });
  }

  getHealthLog(api: string, log: string): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/${api}/health/logs/${log}`);
  }

  apiHealthAverage(api, request): IHttpPromise<any> {
    let url = `${this.Constants.env.baseURL}/apis/${api}/health/average?`;

    Object.keys(request).forEach((key) => {
      const val = request[key];
      if (val !== undefined && val !== '') {
        url += key + '=' + val + '&';
      }
    });

    return this.$http.get(url, { timeout: 30000 });
  }

  /*
   * API ratings
   */

  isRatingEnabled(): boolean {
    return this.Constants.env.settings.portal.rating.enabled;
  }

  getApiRatings(apiId: string, pageNumber: string): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/${apiId}/ratings?pageSize=10&pageNumber=${pageNumber}`);
  }

  getApiRatingForConnectedUser(apiId: string): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/${apiId}/ratings/current`);
  }

  getApiRatingSummaryByApi(apiId: string): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/${apiId}/ratings/summary`);
  }

  createRating(apiId: string, rating: any): IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/${apiId}/ratings`, rating);
  }

  createRatingAnswer(apiId: string, ratingId: string, ratingAnswer: any): IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/${apiId}/ratings/${ratingId}/answers`, ratingAnswer);
  }

  updateRating(apiId: string, rating: any): IHttpPromise<any> {
    return this.$http.put(`${this.Constants.env.baseURL}/apis/${apiId}/ratings/${rating.id}`, {
      rate: rating.rate,
      title: rating.title,
      comment: rating.comment,
    });
  }

  deleteRating(apiId: string, ratingId: string): IHttpPromise<any> {
    return this.$http.delete(`${this.Constants.env.baseURL}/apis/${apiId}/ratings/${ratingId}`);
  }

  deleteRatingAnswer(apiId, ratingId, answerId): IHttpPromise<any> {
    return this.$http.delete(`${this.Constants.env.baseURL}/apis/${apiId}/ratings/${ratingId}/answers/${answerId}`);
  }

  /*
   * Quality Metrics
   */
  getQualityMetrics(apiId: string): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/${apiId}/quality`);
  }

  getQualityMetricCssClass(score?: number): string | undefined {
    if (score !== undefined) {
      if (score < 50) {
        return 'gravitee-qm-score-bad';
      } else if (score >= 50 && score < 80) {
        return 'gravitee-qm-score-medium';
      } else {
        return 'gravitee-qm-score-good';
      }
    }
    return;
  }

  getPortalHeaders(apiId: string): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/${apiId}/headers`);
  }

  isEndpointNameAlreadyUsed(api: { proxy: { groups: any[] } }, name: string): boolean {
    const endpointsName: string[] = [];
    api.proxy.groups.forEach((group) => {
      endpointsName.push(group.name);
      group.endpoints?.forEach((endpoint) => {
        endpointsName.push(endpoint.name);
      });
    });
    return endpointsName.filter((endpointName) => name === endpointName).length > 1;
  }

  askForReview(api: { id: string }, message?: any): IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/${api.id}/reviews?action=ASK`, { message });
  }

  acceptReview(api: { id: string }, message: any): IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/${api.id}/reviews?action=ACCEPT`, { message });
  }

  rejectReview(api: { id: string }, message: any): IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/${api.id}/reviews?action=REJECT`, { message: message });
  }

  /*
   * API Keys
   */
  verifyApiKey(apiId: string, applicationId: string, apiKey: string): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/${apiId}/subscriptions/_canCreate`, {
      params: { key: apiKey, application: applicationId },
    });
  }

  getFlowSchemaForm(): IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/schema`);
  }

  picture(apiId: string, hash: number): IPromise<string> {
    return this.$http
      .get(`${this.Constants.env.baseURL}/apis/${apiId}/picture?hash=${hash}`, {
        responseType: 'blob',
      })
      .then((response: IHttpResponse<Blob>) => blobToBase64(response.data));
  }

  background(apiId: string, hash: number): IPromise<string> {
    return this.$http
      .get(`${this.Constants.env.baseURL}/apis/${apiId}/background?hash=${hash}`, {
        responseType: 'blob',
      })
      .then((response: IHttpResponse<Blob>) => blobToBase64(response.data));
  }

  private async syncV2Api(api: { id: string }): Promise<boolean> {
    if (this.isV2(api)) {
      const updatedApi = await this.get(api.id);
      this.$rootScope.$broadcast('apiChangeSuccess', { api: updatedApi.data });
      return true;
    }
    return false;
  }

  /*
   * Logs
   */
  private buildURLWithQuery(query: LogsQuery, url: string): string {
    Object.keys(query).forEach((key) => {
      const val = query[key];
      if (val !== undefined && val !== '') {
        url += key + '=' + encodeURIComponent(val) + '&';
      }
    });
    return url;
  }

  private cloneQuery(query: LogsQuery): LogsQuery {
    const clonedQuery = clone(query);
    if (clonedQuery.field.startsWith('-')) {
      clonedQuery.order = false;
      clonedQuery.field = clonedQuery.field.substring(1);
    } else {
      clonedQuery.order = true;
    }
    return clonedQuery;
  }

  public isV2(api: any): boolean {
    return api && api.gravitee === '2.0.0';
  }
}
ApiService.$inject = ['$http', '$rootScope', 'Constants', 'ngIfMatchEtagInterceptor'];

const blobToBase64 = (blob: Blob): Promise<string> => {
  return new Promise<string>((resolve, reject) => {
    if (!blob || blob.size === 0) {
      reject('Blob is empty');
      return;
    }
    const reader = new FileReader();
    reader.onloadend = () => resolve(reader.result as string);
    reader.onerror = reject;
    reader.readAsDataURL(blob);
  });
};
