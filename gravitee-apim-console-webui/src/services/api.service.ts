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

interface IMembership {
  id?: string;
  reference?: string;
  role: string;
}

class ApiService {
  constructor(private $http, private $rootScope, private Constants) {
    'ngInject';
  }

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

  get(name): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + name);
  }

  getAnalyticsHttpTimeout() {
    return this.Constants.env.settings.analytics.clientTimeout as number;
  }

  list(category?: string, portal?: boolean, page?: any, order?: string, opts?: any): ng.IPromise<any> {
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
      order: order,
    };

    return this.$http.get(url, opts);
  }

  searchApis(query?: string, page?: any, order?: string, opts?: any): ng.IPromise<any> {
    let url = `${this.Constants.env.baseURL}/apis/_search/`;

    // Fallback to paginated search if a page parameter is provided.
    if (page != null) {
      url += '_paged';
    }

    opts = opts || {};
    opts.params = {
      q: query ? query : '*',
      page: page,
      order: order,
    };

    return this.$http.post(url, {}, opts);
  }

  listTopAPIs(): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + '?top=true');
  }

  listByGroup(group): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + '?group=' + group);
  }

  start(api): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + api.id + '?action=START', {}, { headers: { 'If-Match': api.etag } });
  }

  stop(api): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + api.id + '?action=STOP', {}, { headers: { 'If-Match': api.etag } });
  }

  reload(name): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + 'reload/' + name);
  }

  migrateApiToPolicyStudio(apiId): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + apiId + '/_migrate');
  }

  update(api): ng.IPromise<any> {
    // clean endpoint http proxy
    if (api.proxy && api.proxy.endpoints) {
      _.forEach(api.proxy.endpoints, (endpoint) => {
        if (endpoint.proxy && !endpoint.proxy.useSystemProxy && (!endpoint.proxy.host || !endpoint.proxy.port)) {
          delete endpoint.proxy;
        }
      });
    }

    return this.$http.put(
      `${this.Constants.env.baseURL}/apis/` + api.id,
      {
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
      },
      { headers: { 'If-Match': api.etag } },
    );
  }

  delete(name): ng.IPromise<any> {
    return this.$http.delete(`${this.Constants.env.baseURL}/apis/` + name);
  }

  listPolicies(apiName): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + apiName + '/policies');
  }

  isAPISynchronized(apiId): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + apiId + '/state');
  }

  deploy(apiId, apiDeployment?): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + apiId + '/deploy', apiDeployment);
  }

  rollback(apiId, apiDescriptor): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + apiId + '/rollback', apiDescriptor);
  }

  import(apiId: string, apiDefinition: any, definitionVersion?: string): ng.IPromise<any> {
    if (apiId) {
      return this.$http.put(`${this.Constants.env.baseURL}/apis/${apiId}/import`, apiDefinition);
    }
    const params = definitionVersion ? `?definitionVersion=${definitionVersion}` : '';
    return this.$http.post(`${this.Constants.env.baseURL}/apis/import${params}`, apiDefinition);
  }

  importSwagger(apiId: string, swaggerDescriptor: string, definitionVersion?: string, config?): ng.IPromise<any> {
    const params = definitionVersion ? `?definitionVersion=${definitionVersion}` : '';
    if (apiId) {
      return this.$http.put(`${this.Constants.env.baseURL}/apis/${apiId}/import/swagger${params}`, swaggerDescriptor, config);
    }
    return this.$http.post(`${this.Constants.env.baseURL}/apis/import/swagger${params}`, swaggerDescriptor, config);
  }

  export(apiId, exclude, exportVersion): ng.IPromise<any> {
    return this.$http.get(
      `${this.Constants.env.baseURL}/apis/` +
        apiId +
        '/export?exclude=' +
        exclude.join(',') +
        (exportVersion ? '&version=' + exportVersion : ''),
    );
  }

  verify(criteria, config?): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + 'verify', criteria, config);
  }

  importPathMappings(apiId, page, definitionVersion?: string): ng.IPromise<any> {
    let params = `?page=${page}`;

    if (definitionVersion) {
      params += `&definitionVersion=${definitionVersion}`;
    }

    return this.$http.post(`${this.Constants.env.baseURL}/apis/${apiId}/import-path-mappings${params}`, {});
  }

  duplicate(apiId, config): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + apiId + '/duplicate', config);
  }

  /*
   * Analytics
   */
  analytics(api, request): ng.IPromise<any> {
    let url = `${this.Constants.env.baseURL}/apis/` + api + '/analytics?';

    const keys = Object.keys(request);
    _.forEach(keys, (key) => {
      const val = request[key];
      if (val !== undefined && val !== '') {
        url += key + '=' + val + '&';
      }
    });

    return this.$http.get(url, { timeout: this.getAnalyticsHttpTimeout() });
  }

  findLogs(api: string, query: LogsQuery): ng.IPromise<any> {
    return this.$http.get(this.buildURLWithQuery(this.cloneQuery(query), `${this.Constants.env.baseURL}/apis/` + api + '/logs?'), {
      timeout: 30000,
    });
  }

  exportLogsAsCSV(api: string, query: LogsQuery): ng.IPromise<any> {
    const logsQuery = this.cloneQuery(query);
    logsQuery.page = 1;
    logsQuery.size = 10000;
    return this.$http.get(this.buildURLWithQuery(logsQuery, `${this.Constants.env.baseURL}/apis/` + api + '/logs/export?'), {
      timeout: 30000,
    });
  }

  getLog(api, logId, timestamp): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + api + '/logs/' + logId + (timestamp ? '?timestamp=' + timestamp : ''));
  }

  /*
   * Members
   */
  getMembers(api: string): ng.IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + api + '/members');
  }

  addOrUpdateMember(api: string, membership: IMembership): ng.IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/${api}/members`, membership);
  }

  deleteMember(api: string, userId: string): ng.IHttpPromise<any> {
    return this.$http.delete(`${this.Constants.env.baseURL}/apis/` + api + '/members?user=' + userId);
  }

  transferOwnership(api: string, ownership: IMembership): ng.IHttpPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + api + '/members/transfer_ownership', ownership);
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
  getApiEvents(api, eventTypes): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + api + '/events?type=' + eventTypes);
  }

  searchApiEvents(type, api, from, to, page, size): ng.IPromise<any> {
    return this.$http.get(
      `${this.Constants.env.baseURL}/apis/` + api + `/events/search?type=${type}&from=${from}&to=${to}&page=${page}&size=${size}`,
    );
  }

  listPlans(apiId, type): string {
    let url = this.$http.get(`${this.Constants.env.baseURL}/apis/` + apiId + '/plans');
    if (type) {
      url += '?type=' + type;
    }
    return url;
  }

  /*
   * API plans
   */
  getApiPlans(apiId, status?, security?): ng.IPromise<any> {
    return this.$http.get(
      `${this.Constants.env.baseURL}/apis/` +
        apiId +
        '/plans?status=' +
        (status ? status : 'staging,published,closed,deprecated') +
        (security ? '&security=' + security : ''),
    );
  }

  getApiPlan(apiId, planId): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + apiId + '/plans/' + planId);
  }

  getPublishedApiPlans(apiId): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + apiId + '/plans?status=published');
  }

  savePlan(api, plan): ng.IPromise<any> {
    let promise = null;
    if (plan.id) {
      promise = this.$http.put(`${this.Constants.env.baseURL}/apis/` + api.id + '/plans/' + plan.id, {
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
      promise = this.$http.post(`${this.Constants.env.baseURL}/apis/` + api.id + '/plans', {
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

  closePlan(api, planId): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + api.id + '/plans/' + planId + '/_close').then(async (response) => {
      await this.syncV2Api(api);
      return response;
    });
  }

  publishPlan(api, planId): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + api.id + '/plans/' + planId + '/_publish').then(async (response) => {
      await this.syncV2Api(api);
      return response;
    });
  }

  deprecatePlan(api, planId) {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + api.id + '/plans/' + planId + '/_deprecate').then(async (response) => {
      await this.syncV2Api(api);
      return response;
    });
  }

  /*
   * API subscriptions
   */
  getSubscriptions(apiId: string, query?: string): ng.IHttpPromise<PagedResult> {
    let req = `${this.Constants.env.baseURL}/apis/` + apiId + '/subscriptions';
    if (query !== undefined) {
      req += query;
    }

    return this.$http.get(req);
  }

  exportSubscriptionsAsCSV(apiId: string, query?: string): ng.IPromise<any> {
    let req = `${this.Constants.env.baseURL}/apis/` + apiId + '/subscriptions/export';
    if (query !== undefined) {
      req += query;
    }

    return this.$http.get(req, { timeout: 30000 });
  }

  getSubscribers(apiId: string): ng.IHttpPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + apiId + '/subscribers');
  }

  getSubscription(apiId, subscriptionId): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + apiId + '/subscriptions/' + subscriptionId);
  }

  closeSubscription(apiId, subscriptionId): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + apiId + '/subscriptions/' + subscriptionId + '/status?status=CLOSED');
  }

  pauseSubscription(apiId, subscriptionId) {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + apiId + '/subscriptions/' + subscriptionId + '/status?status=PAUSED');
  }

  updateSubscription(apiId, subscription) {
    return this.$http.put(`${this.Constants.env.baseURL}/apis/` + apiId + '/subscriptions/' + subscription.id, {
      id: subscription.id,
      starting_at: subscription.starting_at,
      ending_at: subscription.ending_at,
    });
  }

  resumeSubscription(apiId, subscriptionId) {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + apiId + '/subscriptions/' + subscriptionId + '/status?status=RESUMED');
  }

  processSubscription(apiId, subscriptionId, processSubscription): ng.IPromise<any> {
    return this.$http.post(
      `${this.Constants.env.baseURL}/apis/` + apiId + '/subscriptions/' + subscriptionId + '/_process',
      processSubscription,
    );
  }

  transferSubscription(apiId, subscriptionId, transferSubscription): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + apiId + '/subscriptions/' + subscriptionId + '/_transfer', {
      id: transferSubscription.id,
      plan: transferSubscription.plan,
    });
  }

  getPlanSubscriptions(apiId, planId): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + apiId + '/subscriptions?plan=' + planId);
  }

  getAllPlanSubscriptions(apiId, planId): ng.IPromise<any> {
    return this.$http.get(
      `${this.Constants.env.baseURL}/apis/` + apiId + '/subscriptions?plan=' + planId + '&status=accepted,pending,rejected,closed,paused',
    );
  }

  subscribe(apiId: string, applicationId: string, planId: string, customApiKey: string): ng.IHttpPromise<any> {
    const params = {
      params: {
        plan: planId,
        application: applicationId,
        customApiKey: customApiKey,
      },
    };
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + apiId + '/subscriptions', null, params);
  }

  listApiKeys(apiId, subscriptionId): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + apiId + '/subscriptions/' + subscriptionId + '/keys');
  }

  revokeApiKey(apiId, subscriptionId, apiKey): ng.IPromise<any> {
    return this.$http.delete(`${this.Constants.env.baseURL}/apis/` + apiId + '/subscriptions/' + subscriptionId + '/keys/' + apiKey);
  }

  reactivateApiKey(apiId, subscriptionId, apiKey): ng.IPromise<any> {
    return this.$http.post(
      `${this.Constants.env.baseURL}/apis/` + apiId + '/subscriptions/' + subscriptionId + '/keys/' + apiKey + '/_reactivate',
      '',
    );
  }

  renewApiKey(apiId, subscriptionId, customApiKey): ng.IPromise<any> {
    const params = {
      params: {
        customApiKey: customApiKey,
      },
    };
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + apiId + '/subscriptions/' + subscriptionId, null, params);
  }

  updateApiKey(apiId, apiKey): ng.IPromise<any> {
    return this.$http.put(`${this.Constants.env.baseURL}/apis/` + apiId + '/keys/' + apiKey.key, apiKey);
  }

  listApiMetadata(apiId): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + apiId + '/metadata');
  }

  createMetadata(apiId, metadata): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + apiId + '/metadata', metadata);
  }

  updateMetadata(apiId, metadata): ng.IPromise<any> {
    return this.$http.put(`${this.Constants.env.baseURL}/apis/` + apiId + '/metadata/' + metadata.key, metadata);
  }

  deleteMetadata(apiId, metadataId): ng.IPromise<any> {
    return this.$http.delete(`${this.Constants.env.baseURL}/apis/` + apiId + '/metadata/' + metadataId);
  }

  getPermissions(api): ng.IPromise<IHttpResponse<any>> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + api + '/members/permissions');
  }

  /*
   * Health-check
   */
  apiHealth(api: string, type?: string, field?: string): ng.IPromise<any> {
    let req = `${this.Constants.env.baseURL}/apis/` + api + '/health';
    if (type !== undefined) {
      req += '?type=' + type;
    }
    if (field !== undefined) {
      req += '&field=' + field;
    }

    return this.$http.get(req, { timeout: 30000 });
  }

  apiHealthLogs(api: string, query: LogsQuery): ng.IPromise<any> {
    let url = `${this.Constants.env.baseURL}/apis/` + api + '/health/logs?';

    const keys = Object.keys(query);
    _.forEach(keys, (key) => {
      const val = query[key];
      if (val !== undefined && val !== '') {
        url += key + '=' + val + '&';
      }
    });

    return this.$http.get(url, { timeout: 30000 });
  }

  getHealthLog(api: string, log: string): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + api + '/health/logs/' + log);
  }

  apiHealthAverage(api, request): ng.IPromise<IHttpResponse<any>> {
    let url = `${this.Constants.env.baseURL}/apis/` + api + '/health/average?';

    const keys = Object.keys(request);
    _.forEach(keys, (key) => {
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

  getApiRatings(api, pageNumber): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + api + '/ratings?pageSize=10&pageNumber=' + pageNumber);
  }

  getApiRatingForConnectedUser(api): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + api + '/ratings/current');
  }

  getApiRatingSummaryByApi(api): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + api + '/ratings/summary');
  }

  createRating(api, rating): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + api + '/ratings', rating);
  }

  createRatingAnswer(api, ratingId, ratingAnswer): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + api + '/ratings/' + ratingId + '/answers', ratingAnswer);
  }

  updateRating(api, rating): ng.IPromise<any> {
    return this.$http.put(`${this.Constants.env.baseURL}/apis/` + api + '/ratings/' + rating.id, {
      rate: rating.rate,
      title: rating.title,
      comment: rating.comment,
    });
  }

  deleteRating(api, ratingId): ng.IPromise<any> {
    return this.$http.delete(`${this.Constants.env.baseURL}/apis/` + api + '/ratings/' + ratingId);
  }

  deleteRatingAnswer(api, ratingId, answerId): ng.IPromise<any> {
    return this.$http.delete(`${this.Constants.env.baseURL}/apis/` + api + '/ratings/' + ratingId + '/answers/' + answerId);
  }

  /*
   * Quality Metrics
   */
  getQualityMetrics(api): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + api + '/quality');
  }

  getQualityMetricCssClass(score): string {
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

  getPortalHeaders(api): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/` + api + '/headers');
  }

  isEndpointNameAlreadyUsed(api: any, name: string, onCreate: boolean) {
    const endpointsName: string[] = [];
    _.forEach(api.proxy.groups, (group) => {
      endpointsName.push(group.name);
      _.forEach(group.endpoints, (endpoint) => {
        endpointsName.push(endpoint.name);
      });
    });
    // in update mode, the api endpoint is updated when the form is filled.
    // that's why we have to count it twice to detect non uniqueness
    return _.filter(endpointsName, (endpointName) => name === endpointName).length > (onCreate ? 0 : 1);
  }

  askForReview(api, message?): ng.IPromise<any> {
    return this.$http.post(
      `${this.Constants.env.baseURL}/apis/` + api.id + '/reviews?action=ASK',
      { message: message },
      { headers: { 'If-Match': api.etag } },
    );
  }

  acceptReview(api, message): ng.IPromise<any> {
    return this.$http.post(
      `${this.Constants.env.baseURL}/apis/` + api.id + '/reviews?action=ACCEPT',
      { message: message },
      { headers: { 'If-Match': api.etag } },
    );
  }

  rejectReview(api, message): ng.IPromise<any> {
    return this.$http.post(
      `${this.Constants.env.baseURL}/apis/` + api.id + '/reviews?action=REJECT',
      { message: message },
      { headers: { 'If-Match': api.etag } },
    );
  }

  /*
   * Api Keys
   */
  verifyApiKey(apiId: string, apiKey: string): ng.IPromise<any> {
    return this.$http.post(`${this.Constants.env.baseURL}/apis/` + apiId + '/keys/_verify?apiKey=' + apiKey);
  }

  getFlowSchemaForm(): ng.IPromise<any> {
    return this.$http.get(`${this.Constants.env.baseURL}/apis/schema`);
  }

  private async syncV2Api(api) {
    if (isV2(api)) {
      const updatedApi = await this.get(api.id);
      this.$rootScope.$broadcast('apiChangeSuccess', { api: updatedApi.data });
      return true;
    }
    return false;
  }

  /*
   * Logs
   */
  private buildURLWithQuery(query: LogsQuery, url) {
    const keys = Object.keys(query);
    _.forEach(keys, (key) => {
      const val = query[key];
      if (val !== undefined && val !== '') {
        url += key + '=' + val + '&';
      }
    });
    return url;
  }

  private cloneQuery(query: LogsQuery) {
    const clonedQuery = _.clone(query);
    if (_.startsWith(clonedQuery.field, '-')) {
      clonedQuery.order = false;
      clonedQuery.field = clonedQuery.field.substring(1);
    } else {
      clonedQuery.order = true;
    }
    return clonedQuery;
  }
}

export default ApiService;

export function isV2(api) {
  return api && api.gravitee === '2.0.0';
}
