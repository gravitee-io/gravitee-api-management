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
import * as _ from "lodash";
import { PagedResult } from "../entities/pagedResult";

export class LogsQuery {
  from: number;
  to: number;
  query?: string;
  page: number;
  size: number;
}

interface IMembership {
  id?: string;
  reference?: string;
  role: string;
}

class ApiService {
  private apisURL: string;
  private Constants: any;

  constructor(private $http, Constants) {
    'ngInject';
    this.apisURL = `${Constants.baseURL}apis/`;
    this.Constants = Constants;
  }

  defaultHttpHeaders() {
    return [
      'Accept','Accept-Charset','Accept-Encoding','Accept-Language','Accept-Ranges','Access-Control-Allow-Credentials',
      'Access-Control-Allow-Headers','Access-Control-Allow-Methods','Access-Control-Allow-Origin',
      'Access-Control-Expose-Headers','Access-Control-Max-Age','Access-Control-Request-Headers',
      'Access-Control-Request-Method','Age','Allow','Authorization','Cache-Control','Connection','Content-Disposition',
      'Content-Encoding','Content-ID','Content-Language','Content-Length','Content-Location','Content-MD5','Content-Range',
      'Content-Type','Cookie','Date','ETag','Expires','Expect','Forwarded','From','Host','If-Match','If-Modified-Since',
      'If-None-Match','If-Unmodified-Since','Keep-Alive','Last-Modified','Location','Link','Max-Forwards','MIME-Version',
      'Origin','Pragma','Proxy-Authenticate','Proxy-Authorization','Proxy-Connection','Range','Referer','Retry-After',
      'Server','Set-Cookie','Set-Cookie2','TE','Trailer','Transfer-Encoding','Upgrade','User-Agent','Vary','Via',
      'Warning','WWW-Authenticate','X-Forwarded-For','X-Forwarded-Proto','X-Forwarded-Server','X-Forwarded-Host'
    ];
  }

  get(name) {
    return this.$http.get(this.apisURL + name);
  }

  list(view?: string) {
    return this.$http.get(this.apisURL + (view ? '?view=' + view : ''));
  }

  searchApis(query?: string) {
    return this.$http.post(this.apisURL + '_search?q=' + query);
  }

  listTopAPIs() {
    return this.$http.get(this.apisURL + '?top=true');
  }

  listByGroup(group) {
    return this.$http.get(this.apisURL + '?group=' + group);
  }

  start(api) {
    return this.$http.post(this.apisURL + api.id + '?action=START', {}, {headers: {'If-Match': api.etag}});
  }

  stop(api) {
    return this.$http.post(this.apisURL + api.id + '?action=STOP', {}, {headers: {'If-Match': api.etag}});
  }

  reload(name) {
    return this.$http.post(this.apisURL + 'reload/' + name);
  }

  create(api) {
    return this.$http.post(this.apisURL, api);
  }

  update(api) {
    //clean endpoint http proxy
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
        'labels': api.labels, 'path_mappings': api.path_mappings
      }, {headers: {'If-Match': api.etag}}
    );
  }

  delete(name) {
    return this.$http.delete(this.apisURL + name);
  }

  listPolicies(apiName) {
    return this.$http.get(this.apisURL + apiName + '/policies');
  }

  isAPISynchronized(apiId) {
    return this.$http.get(this.apisURL + apiId + '/state');
  }

  deploy(apiId) {
    return this.$http.post(this.apisURL + apiId + '/deploy');
  }

  rollback(apiId, apiDescriptor) {
    return this.$http.post(this.apisURL + apiId + '/rollback', apiDescriptor);
  }

  import(apiId, apiDefinition) {
    return this.$http.post(this.apisURL + (apiId ? apiId + '/' : '') + 'import', apiDefinition);
  }

  importSwagger(swaggerDescriptor) {
    return this.$http.post(this.apisURL + 'import/swagger', swaggerDescriptor);
  }

  export(apiId, exclude, exportVersion) {
    return this.$http.get(this.apisURL + apiId + '/export?exclude=' + exclude.join(",") + (exportVersion ? '&version=' + exportVersion : ''));
  }

  verify(criteria) {
    return this.$http.post(this.apisURL + 'verify', criteria);
  }

  importPathMappings(apiId, page) {
    return this.$http.post(this.apisURL + apiId + '/import-path-mappings?page=' + page);
  }

  /*
   * Analytics
   */
  analytics(api, request) {
    var url = this.apisURL + api + '/analytics?';

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
  findLogs(api: string, query: LogsQuery) {
    var url = this.apisURL + api + '/logs?';

    var keys = Object.keys(query);
    _.forEach(keys, function (key) {
      var val = query[key];
      if (val !== undefined && val !== '') {
        url += key + '=' + val + '&';
      }
    });

    return this.$http.get(url, {timeout: 30000});
  }

  getLog(api, logId, timestamp) {
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
  getApiEvents(api, eventTypes) {
    return this.$http.get(this.apisURL + api + '/events?type=' + eventTypes);
  }

  listPlans(apiId, type) {
    let url = this.$http.get(this.apisURL + apiId + '/plans');
    if (type) {
      url += '?type=' + type;
    }
    return url;
  }

  /*
   * API plans
   */
  getApiPlans(apiId) {
    return this.$http.get(this.apisURL + apiId + '/plans?status=staging,published,closed');
  }

  getApiPlan(apiId, planId) {
    return this.$http.get(this.apisURL + apiId + '/plans/' + planId);
  }

  getPublishedApiPlans(apiId) {
    return this.$http.get(this.apisURL + apiId + '/plans?status=published');
  }

  savePlan(apiId, plan) {
    if (plan.id) {
      return this.$http.put(this.apisURL + apiId + '/plans/' + plan.id,
        {
          id: plan.id, name: plan.name, description: plan.description,
          validation: plan.validation, policies: plan.policies,
          securityDefinition: plan.securityDefinition,
          characteristics: plan.characteristics, order: plan.order, paths: plan.paths,
          excluded_groups: plan.excludedGroups
        });
    } else {
      return this.$http.post(this.apisURL + apiId + '/plans',
        {
          name: plan.name, description: plan.description, api: plan.api,
          validation: plan.validation, policies: plan.policies,
          characteristics: plan.characteristics, type: plan.type, paths: plan.paths,
          security: plan.security, securityDefinition: plan.securityDefinition, excluded_groups: plan.excludedGroups
        });
    }
  }

  closePlan(apiId, planId) {
    return this.$http.post(this.apisURL + apiId + '/plans/' + planId + '/_close');
  }

  deletePlan(apiId, planId) {
    return this.$http.delete(this.apisURL + apiId + '/plans/' + planId);
  }

  publishPlan(apiId, planId) {
    return this.$http.post(this.apisURL + apiId + '/plans/' + planId + '/_publish');
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

  getSubscribers(apiId: string): ng.IHttpPromise<any> {
    return this.$http.get(this.apisURL + apiId + '/subscribers');
  }

  getSubscription(apiId, subscriptionId) {
    return this.$http.get(this.apisURL + apiId + '/subscriptions/' + subscriptionId);
  }

  closeSubscription(apiId, subscriptionId) {
    return this.$http.post(this.apisURL + apiId + '/subscriptions/' + subscriptionId + '/status?status=CLOSED');
  }

  processSubscription(apiId, subscriptionId, processSubscription) {
    return this.$http.post(this.apisURL + apiId + '/subscriptions/' + subscriptionId + '/_process', processSubscription);
  }

  getPlanSubscriptions(apiId, planId) {
    return this.$http.get(this.apisURL + apiId + '/subscriptions?plan='+planId);
  }

  getAllPlanSubscriptions(apiId, planId) {
    return this.$http.get(this.apisURL + apiId + '/subscriptions?plan='+planId+'&status=accepted,pending,rejected,closed');
  }

  subscribe(apiId: string, applicationId: string, planId: string): ng.IHttpPromise<any> {
    return this.$http.post(this.apisURL + apiId + '/subscriptions?plan=' + planId + '&application=' + applicationId, '');
  }

  /*
  revokeSubscription(apiId, subscriptionId) {
    return this.$http.delete(this.apisURL + apiId + '/subscriptions/' + subscriptionId);
  }
  */

  listApiKeys(apiId, subscriptionId) {
    return this.$http.get(this.apisURL + apiId + '/subscriptions/' + subscriptionId + '/keys');
  }

  revokeApiKey(apiId, subscriptionId, apiKey) {
    return this.$http.delete(this.apisURL + apiId + '/subscriptions/' + subscriptionId + '/keys/' + apiKey);
  }

  renewApiKey(apiId, subscriptionId) {
    return this.$http.post(this.apisURL + apiId + '/subscriptions/' + subscriptionId);
  }

  updateApiKey(apiId, apiKey) {
    return this.$http.put(this.apisURL + apiId + '/keys/' + apiKey.key, apiKey);
  }

  listApiMetadata(apiId) {
    return this.$http.get(this.apisURL + apiId + '/metadata');
  }

  getApiMetadata(apiId, metadataId) {
    return this.$http.get(this.apisURL + apiId + '/metadata/' + metadataId);
  }

  createMetadata(apiId, metadata) {
    return this.$http.post(this.apisURL + apiId + '/metadata', metadata);
  }

  updateMetadata(apiId, metadata) {
    return this.$http.put(this.apisURL + apiId + '/metadata/' + metadata.key, metadata);
  }

  deleteMetadata(apiId, metadataId) {
    return this.$http.delete(this.apisURL + apiId + '/metadata/' + metadataId);
  }

  getPermissions(api) {
    return this.$http.get(this.apisURL + api + '/members/permissions');
  }

  /*
   * Health-check
   */
  apiHealth(api: string, type?: string, field?: string) {
    let req = this.apisURL + api + '/health';
    if (type !== undefined) {
      req += '?type=' + type;
    }
    if (field !== undefined) {
      req += '&field=' + field;
    }

    return this.$http.get(req, {timeout: 30000});
  }

  apiHealthLogs(api: string, query: LogsQuery) {
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

  getHealthLog(api: string, log: string) {
    return this.$http.get(this.apisURL + api + '/health/logs/' + log);
  }

  apiHealthAverage(api, request) {
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

  isRatingEnabled() {
    return this.Constants.portal.rating.enabled;
  }

  getApiRatings(api, pageNumber) {
    return this.$http.get(this.apisURL + api + '/ratings?pageSize=10&pageNumber=' + pageNumber);
  }

  getApiRatingForConnectedUser(api) {
    return this.$http.get(this.apisURL + api + '/ratings/current');
  }

  getApiRatingSummaryByApi(api) {
    return this.$http.get(this.apisURL + api + '/ratings/summary');
  }

  createRating(api, rating) {
    return this.$http.post(this.apisURL + api + '/ratings', rating);
  }

  createRatingAnswer(api, ratingId, ratingAnswer) {
    return this.$http.post(this.apisURL + api + '/ratings/' + ratingId + '/answers', ratingAnswer);
  }

  updateRating(api, rating) {
    return this.$http.put(this.apisURL + api + '/ratings/' + rating.id,
      {'rate': rating.rate, 'title': rating.title, 'comment': rating.comment});
  }

  deleteRating(api, ratingId) {
    return this.$http.delete(this.apisURL + api + '/ratings/' + ratingId);
  }

  deleteRatingAnswer(api, ratingId, answerId) {
    return this.$http.delete(this.apisURL + api + '/ratings/' + ratingId + '/answers/' + answerId);
  }

  /*
   * Quality Metrics
   */
  getQualityMetrics(api) {
    return this.$http.get(this.apisURL + api + '/quality');
  }

  getQualityMetricCssClass(score) {
    if (score !== undefined) {
      if ( score < 50 ) {
        return 'gravitee-qm-score-bad';
      } else if (score >= 50 && score < 80) {
        return 'gravitee-qm-score-medium'
      } else {
        return  'gravitee-qm-score-good';
      }
    }
    return;
  }

  getPortalHeaders(api) {
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
    //in update mode, the api endpoint is updated when the form is filled.
    // that's why we have to count it twice to detect non uniqueness
    return _.filter(endpointsName, (endpointName) => name === endpointName).length > (onCreate ? 0 : 1);
  }
}

export default ApiService;
