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

class ApiService {
  private apisURL: string;
  private Constants: any;

  constructor(private $http, Constants) {
    'ngInject';
    this.apisURL = `${Constants.baseURL}apis/`;
    this.Constants = Constants;
  }

  get(name) {
    return this.$http.get(this.apisURL + name);
  }

  list(view?: string) {
    return this.$http.get(this.apisURL + (view ? '?view=' + view : ''));
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
        'labels': api.labels
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

  export(apiId, exclude) {
    return this.$http.get(this.apisURL + apiId + '/export?exclude=' + exclude.join(","));
  }

  verify(criteria) {
    return this.$http.post(this.apisURL + 'verify', criteria);
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

  getLog(api, logId) {
    return this.$http.get(this.apisURL + api + '/logs/' + logId);
  }

  /*
   * Members
   */
  getMembers(api) {
    return this.$http.get(this.apisURL + api + '/members');
  }

  addOrUpdateMember(api, member) {
    return this.$http.post(this.apisURL + api + '/members?user=' + member.username + '&type=' + member.type + '&rolename=' + member.role, '');
  }

  deleteMember(api, memberUsername) {
    return this.$http.delete(this.apisURL + api + '/members?user=' + memberUsername);
  }

  transferOwnership(api, memberUsername, newRole: string) {
    return this.$http.post(this.apisURL + api + '/members/transfer_ownership?user=' + memberUsername, {
      role: newRole
    });
  }

  /*
   * API events
   */
  getApiEvents(api, eventTypes) {
    return this.$http.get(this.apisURL + api + '/events?type=' + eventTypes);
  }

  listPlans(apiId, type) {
    var url = this.$http.get(this.apisURL + apiId + '/plans');
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
    return this.Constants.rating && this.Constants.rating.enabled;
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
}

export default ApiService;
