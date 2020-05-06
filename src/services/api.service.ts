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
  private apisURL: string;
  private Constants: any;
  private analyticsHttpTimeout: number;

  constructor(private $http, Constants) {
    'ngInject';
    this.apisURL = `${Constants.baseURL}apis/`;
    this.Constants = Constants;
    this.analyticsHttpTimeout = Constants.analytics.clientTimeout as number;
  }

  defaultHttpHeaders(): string[] {
    return [
      'Accept', 'Accept-Charset', 'Accept-Encoding', 'Accept-Language', 'Accept-Ranges', 'Access-Control-Allow-Credentials',
      'Access-Control-Allow-Headers', 'Access-Control-Allow-Methods', 'Access-Control-Allow-Origin',
      'Access-Control-Expose-Headers', 'Access-Control-Max-Age', 'Access-Control-Request-Headers',
      'Access-Control-Request-Method', 'Age', 'Allow', 'Authorization', 'Cache-Control', 'Connection', 'Content-Disposition',
      'Content-Encoding', 'Content-ID', 'Content-Language', 'Content-Length', 'Content-Location', 'Content-MD5', 'Content-Range',
      'Content-Type', 'Cookie', 'Date', 'ETag', 'Expires', 'Expect', 'Forwarded', 'From', 'Host', 'If-Match', 'If-Modified-Since',
      'If-None-Match', 'If-Unmodified-Since', 'Keep-Alive', 'Last-Modified', 'Location', 'Link', 'Max-Forwards', 'MIME-Version',
      'Origin', 'Pragma', 'Proxy-Authenticate', 'Proxy-Authorization', 'Proxy-Connection', 'Range', 'Referer', 'Retry-After',
      'Server', 'Set-Cookie', 'Set-Cookie2', 'TE', 'Trailer', 'Transfer-Encoding', 'Upgrade', 'User-Agent', 'Vary', 'Via',
      'Warning', 'WWW-Authenticate', 'X-Forwarded-For', 'X-Forwarded-Proto', 'X-Forwarded-Server', 'X-Forwarded-Host'
    ];
  }

  get(name): ng.IPromise<any> {
    return this.$http.get(this.apisURL + name);
  }

  list(view?: string, portal?: boolean, opts?: any): ng.IPromise<any> {
    let params = '';
    if (view !== undefined && view !== null) {
      params += '?view=' + view;
    }
    if (portal !== undefined) {
      if (params === '') {
        params += '?';
      } else {
        params += '&';
      }
      params += 'portal=' + portal;
    }
    return this.$http.get(this.apisURL + params, {}, opts);
  }

  searchApis(query?: string, opts?: any): ng.IPromise<any> {
    return this.$http.post(this.apisURL + '_search?q=' + query, {}, opts);
  }

  listTopAPIs(): ng.IPromise<any> {
    return this.$http.get(this.apisURL + '?top=true');
  }

  listByGroup(group): ng.IPromise<any> {
    return this.$http.get(this.apisURL + '?group=' + group);
  }

  start(api): ng.IPromise<any> {
    return this.$http.post(this.apisURL + api.id + '?action=START', {}, {headers: {'If-Match': api.etag}});
  }

  stop(api): ng.IPromise<any> {
    return this.$http.post(this.apisURL + api.id + '?action=STOP', {}, {headers: {'If-Match': api.etag}});
  }

  reload(name): ng.IPromise<any> {
    return this.$http.post(this.apisURL + 'reload/' + name);
  }

  update(api): ng.IPromise<any> {
    // clean endpoint http proxy
    if (api.proxy && api.proxy.endpoints) {
      _.forEach(api.proxy.endpoints, (endpoint) => {
        if (endpoint.proxy && (!endpoint.proxy.host || !endpoint.proxy.port)) {
          delete endpoint.proxy;
        }
      });
    }
    return this.$http.put(this.apisURL + api.id,
      {'version': api.version, 'description': api.description, 'proxy': api.proxy, 'paths': api.paths, 'private': api.private,
        'visibility': api.visibility, 'name': api.name, 'services': api.services, 'properties': api.properties, 'tags': api.tags,
        'picture': api.picture, 'resources': api.resources, 'views': api.views, 'groups': api.groups,
        'labels': api.labels, 'path_mappings': api.path_mappings, 'response_templates': api.response_templates, 'lifecycle_state': api.lifecycle_state
      }, {headers: {'If-Match': api.etag}}
    );
  }

  delete(name): ng.IPromise<any> {
    return this.$http.delete(this.apisURL + name);
  }

  listPolicies(apiName): ng.IPromise<any> {
    return this.$http.get(this.apisURL + apiName + '/policies');
  }

  isAPISynchronized(apiId): ng.IPromise<any> {
    return this.$http.get(this.apisURL + apiId + '/state');
  }

  deploy(apiId): ng.IPromise<any> {
    return this.$http.post(this.apisURL + apiId + '/deploy');
  }

  rollback(apiId, apiDescriptor): ng.IPromise<any> {
    return this.$http.post(this.apisURL + apiId + '/rollback', apiDescriptor);
  }

  import(apiId: string, apiDefinition: string): ng.IPromise<any> {
    return this.$http.post(this.apisURL + (apiId ? apiId + '/' : '') + 'import', apiDefinition);
  }

  importSwagger(apiId: string, swaggerDescriptor: string): ng.IPromise<any> {
    return this.$http.post(this.apisURL + (apiId ? apiId + '/' : '') + 'import/swagger', swaggerDescriptor);
  }

  export(apiId, exclude, exportVersion): ng.IPromise<any> {
    return this.$http.get(this.apisURL + apiId + '/export?exclude=' + exclude.join(',') + (exportVersion ? '&version=' + exportVersion : ''));
  }

  verify(criteria, config?): ng.IPromise<any> {
    return this.$http.post(this.apisURL + 'verify', criteria, config);
  }

  importPathMappings(apiId, page): ng.IPromise<any> {
    return this.$http.post(this.apisURL + apiId + '/import-path-mappings?page=' + page);
  }

  duplicate(apiId, config): ng.IPromise<any> {
    return this.$http.post(this.apisURL + apiId + '/duplicate', config);
  }

  /*
   * Analytics
   */
  analytics(api, request): ng.IPromise<any> {
    var url = this.apisURL + api + '/analytics?';

    var keys = Object.keys(request);
    _.forEach(keys, function (key) {
      var val = request[key];
      if (val !== undefined && val !== '') {
        url += key + '=' + val + '&';
      }
    });

    return this.$http.get(url, {timeout: this.analyticsHttpTimeout});
  }

  findLogs(api: string, query: LogsQuery): ng.IPromise<any> {
    return this.$http.get(this.buildURLWithQuery(this.cloneQuery(query), this.apisURL + api + '/logs?'), {timeout: 30000});
  }

  exportLogsAsCSV(api: string, query: LogsQuery): ng.IPromise<any> {
    return this.$http.get(this.buildURLWithQuery(this.cloneQuery(query), this.apisURL + api + '/logs/export?'), {timeout: 30000});
  }

  getLog(api, logId, timestamp): ng.IPromise<any> {
    return this.$http.get(this.apisURL + api + '/logs/' + logId + ((timestamp) ? '?timestamp=' + timestamp : ''));
  }

  /*
   * Members
   */
  getMembers(api: string): ng.IHttpPromise<any> {
    return this.$http.get(this.apisURL + api + '/members');
  }

  addOrUpdateMember(api: string, membership: IMembership): ng.IHttpPromise<any> {
    return this.$http.post(`${this.apisURL}${api}/members`, membership);

  }

  deleteMember(api: string, userId: string): ng.IHttpPromise<any> {
    return this.$http.delete(this.apisURL + api + '/members?user=' + userId);
  }

  transferOwnership(api: string, ownership: IMembership): ng.IHttpPromise<any> {
    return this.$http.post(this.apisURL + api + '/members/transfer_ownership', ownership);
  }

  /*
   * API events
   */
  getApiEvents(api, eventTypes): ng.IPromise<any> {
    return this.$http.get(this.apisURL + api + '/events?type=' + eventTypes);
  }

  listPlans(apiId, type): string {
    let url = this.$http.get(this.apisURL + apiId + '/plans');
    if (type) {
      url += '?type=' + type;
    }
    return url;
  }

  /*
   * API plans
   */
  getApiPlans(apiId, status?, security?): ng.IPromise<any> {
    return this.$http.get(this.apisURL + apiId + '/plans?status=' + (status ? status : 'staging,published,closed,deprecated') + (security ? '&security=' + security : ''));
  }

  getApiPlan(apiId, planId): ng.IPromise<any> {
    return this.$http.get(this.apisURL + apiId + '/plans/' + planId);
  }

  getPublishedApiPlans(apiId): ng.IPromise<any> {
    return this.$http.get(this.apisURL + apiId + '/plans?status=published');
  }

  savePlan(apiId, plan): ng.IPromise<any> {
    if (plan.id) {
      return this.$http.put(this.apisURL + apiId + '/plans/' + plan.id,
        {
          id: plan.id, name: plan.name, description: plan.description,
          validation: plan.validation, policies: plan.policies,
          securityDefinition: plan.securityDefinition,
          characteristics: plan.characteristics, order: plan.order, paths: plan.paths,
          excluded_groups: plan.excluded_groups,
          comment_required: plan.comment_required,
          comment_message: plan.comment_message,
          tags: plan.tags,
          selection_rule: plan.selection_rule
        });
    } else {
      return this.$http.post(this.apisURL + apiId + '/plans',
        {
          name: plan.name, description: plan.description, api: plan.api,
          validation: plan.validation, policies: plan.policies,
          characteristics: plan.characteristics, type: plan.type, paths: plan.paths,
          security: plan.security, securityDefinition: plan.securityDefinition, excluded_groups: plan.excluded_groups,
          comment_required: plan.comment_required,
          comment_message: plan.comment_message,
          tags: plan.tags,
          selection_rule: plan.selection_rule
        });
    }
  }

  closePlan(apiId, planId): ng.IPromise<any> {
    return this.$http.post(this.apisURL + apiId + '/plans/' + planId + '/_close');
  }

  deletePlan(apiId, planId): ng.IPromise<any> {
    return this.$http.delete(this.apisURL + apiId + '/plans/' + planId);
  }

  publishPlan(apiId, planId): ng.IPromise<any> {
    return this.$http.post(this.apisURL + apiId + '/plans/' + planId + '/_publish');
  }

  depreciatePlan(apiId, planId) {
    return this.$http.post(this.apisURL + apiId + '/plans/' + planId + '/_depreciate');
  }

  /*
   * API subscriptions
   */
  getSubscriptions(apiId: string, query?: string): ng.IHttpPromise<PagedResult> {
    let req = this.apisURL + apiId + '/subscriptions';
    if (query !== undefined) {
      req += query;
    }

    return this.$http.get(req);
  }

  exportSubscriptionsAsCSV(apiId: string, query?: string): ng.IPromise<any> {
    let req = this.apisURL + apiId + '/subscriptions/export';
    if (query !== undefined) {
      req += query;
    }

    return this.$http.get(req, {timeout: 30000});
  }

  getSubscribers(apiId: string): ng.IHttpPromise<any> {
    return this.$http.get(this.apisURL + apiId + '/subscribers');
  }

  getSubscription(apiId, subscriptionId): ng.IPromise<any> {
    return this.$http.get(this.apisURL + apiId + '/subscriptions/' + subscriptionId);
  }

  closeSubscription(apiId, subscriptionId): ng.IPromise<any> {
    return this.$http.post(this.apisURL + apiId + '/subscriptions/' + subscriptionId + '/status?status=CLOSED');
  }

  pauseSubscription(apiId, subscriptionId) {
    return this.$http.post(this.apisURL + apiId + '/subscriptions/' + subscriptionId + '/status?status=PAUSED');
  }

  updateSubscription(apiId, subscription) {
    return this.$http.put(this.apisURL + apiId + '/subscriptions/' + subscription.id,
      {
        id: subscription.id,
        starting_at: subscription.starting_at,
        ending_at: subscription.ending_at
      });
  }

  resumeSubscription(apiId, subscriptionId) {
    return this.$http.post(this.apisURL + apiId + '/subscriptions/' + subscriptionId + '/status?status=RESUMED');
  }

  processSubscription(apiId, subscriptionId, processSubscription): ng.IPromise<any> {
    return this.$http.post(this.apisURL + apiId + '/subscriptions/' + subscriptionId + '/_process', processSubscription);
  }

  transferSubscription(apiId, subscriptionId, transferSubscription): ng.IPromise<any> {
    return this.$http.post(this.apisURL + apiId + '/subscriptions/' + subscriptionId + '/_transfer', {
      id: transferSubscription.id,
      plan: transferSubscription.plan
    });
  }

  getPlanSubscriptions(apiId, planId): ng.IPromise<any> {
    return this.$http.get(this.apisURL + apiId + '/subscriptions?plan=' + planId);
  }

  getAllPlanSubscriptions(apiId, planId): ng.IPromise<any> {
    return this.$http.get(this.apisURL + apiId + '/subscriptions?plan=' + planId + '&status=accepted,pending,rejected,closed');
  }

  subscribe(apiId: string, applicationId: string, planId: string): ng.IHttpPromise<any> {
    return this.$http.post(this.apisURL + apiId + '/subscriptions?plan=' + planId + '&application=' + applicationId, '');
  }

  listApiKeys(apiId, subscriptionId): ng.IPromise<any> {
    return this.$http.get(this.apisURL + apiId + '/subscriptions/' + subscriptionId + '/keys');
  }

  revokeApiKey(apiId, subscriptionId, apiKey): ng.IPromise<any> {
    return this.$http.delete(this.apisURL + apiId + '/subscriptions/' + subscriptionId + '/keys/' + apiKey);
  }

  renewApiKey(apiId, subscriptionId): ng.IPromise<any> {
    return this.$http.post(this.apisURL + apiId + '/subscriptions/' + subscriptionId);
  }

  updateApiKey(apiId, apiKey): ng.IPromise<any> {
    return this.$http.put(this.apisURL + apiId + '/keys/' + apiKey.key, apiKey);
  }

  listApiMetadata(apiId): ng.IPromise<any> {
    return this.$http.get(this.apisURL + apiId + '/metadata');
  }

  getApiMetadata(apiId, metadataId): ng.IPromise<any> {
    return this.$http.get(this.apisURL + apiId + '/metadata/' + metadataId);
  }

  createMetadata(apiId, metadata): ng.IPromise<any> {
    return this.$http.post(this.apisURL + apiId + '/metadata', metadata);
  }

  updateMetadata(apiId, metadata): ng.IPromise<any> {
    return this.$http.put(this.apisURL + apiId + '/metadata/' + metadata.key, metadata);
  }

  deleteMetadata(apiId, metadataId): ng.IPromise<any> {
    return this.$http.delete(this.apisURL + apiId + '/metadata/' + metadataId);
  }

  getPermissions(api): ng.IPromise<any> {
    return this.$http.get(this.apisURL + api + '/members/permissions');
  }

  /*
   * Health-check
   */
  apiHealth(api: string, type?: string, field?: string): ng.IPromise<any> {
    let req = this.apisURL + api + '/health';
    if (type !== undefined) {
      req += '?type=' + type;
    }
    if (field !== undefined) {
      req += '&field=' + field;
    }

    return this.$http.get(req, {timeout: 30000});
  }

  apiHealthLogs(api: string, query: LogsQuery): ng.IPromise<any> {
    let url = this.apisURL + api + '/health/logs?';

    let keys = Object.keys(query);
    _.forEach(keys, function (key) {
      let val = query[key];
      if (val !== undefined && val !== '') {
        url += key + '=' + val + '&';
      }
    });

    return this.$http.get(url, {timeout: 30000});
  }

  getHealthLog(api: string, log: string): ng.IPromise<any> {
    return this.$http.get(this.apisURL + api + '/health/logs/' + log);
  }

  apiHealthAverage(api, request): ng.IPromise<any> {
    var url = this.apisURL + api + '/health/average?';

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
   * API ratings
   */

  isRatingEnabled(): boolean {
    return this.Constants.portal.rating.enabled;
  }

  getApiRatings(api, pageNumber): ng.IPromise<any> {
    return this.$http.get(this.apisURL + api + '/ratings?pageSize=10&pageNumber=' + pageNumber);
  }

  getApiRatingForConnectedUser(api): ng.IPromise<any> {
    return this.$http.get(this.apisURL + api + '/ratings/current');
  }

  getApiRatingSummaryByApi(api): ng.IPromise<any> {
    return this.$http.get(this.apisURL + api + '/ratings/summary');
  }

  createRating(api, rating): ng.IPromise<any> {
    return this.$http.post(this.apisURL + api + '/ratings', rating);
  }

  createRatingAnswer(api, ratingId, ratingAnswer): ng.IPromise<any> {
    return this.$http.post(this.apisURL + api + '/ratings/' + ratingId + '/answers', ratingAnswer);
  }

  updateRating(api, rating): ng.IPromise<any> {
    return this.$http.put(this.apisURL + api + '/ratings/' + rating.id,
      {'rate': rating.rate, 'title': rating.title, 'comment': rating.comment});
  }

  deleteRating(api, ratingId): ng.IPromise<any> {
    return this.$http.delete(this.apisURL + api + '/ratings/' + ratingId);
  }

  deleteRatingAnswer(api, ratingId, answerId): ng.IPromise<any> {
    return this.$http.delete(this.apisURL + api + '/ratings/' + ratingId + '/answers/' + answerId);
  }

  /*
   * Quality Metrics
   */
  getQualityMetrics(api): ng.IPromise<any> {
    return this.$http.get(this.apisURL + api + '/quality');
  }

  getQualityMetricCssClass(score): string {
    if (score !== undefined) {
      if ( score < 50 ) {
        return 'gravitee-qm-score-bad';
      } else if (score >= 50 && score < 80) {
        return 'gravitee-qm-score-medium';
      } else {
        return  'gravitee-qm-score-good';
      }
    }
    return;
  }

  getPortalHeaders(api): ng.IPromise<any> {
    return this.$http.get(this.apisURL + api + '/headers');
  }

  isEndpointNameAlreadyUsed(api: any, name: string, onCreate: boolean) {
    let endpointsName: String[] = [];
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
    return this.$http.post(this.apisURL + api.id + '/reviews?action=ASK', {message: message}, {headers: {'If-Match': api.etag}});
  }

  acceptReview(api, message): ng.IPromise<any> {
    return this.$http.post(this.apisURL + api.id + '/reviews?action=ACCEPT', {message: message}, {headers: {'If-Match': api.etag}});
  }

  rejectReview(api, message): ng.IPromise<any> {
    return this.$http.post(this.apisURL + api.id + '/reviews?action=REJECT', {message: message}, {headers: {'If-Match': api.etag}});
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

export default ApiService;
