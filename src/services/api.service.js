"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
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
var _ = require("lodash");
var ApiService = (function () {
    function ApiService($http, Constants, $q) {
        'ngInject';
        this.$http = $http;
        this.$q = $q;
        this.apisURL = Constants.baseURL + "apis/";
    }
    ApiService.prototype.get = function (name) {
        return this.$http.get(this.apisURL + name);
    };
    ApiService.prototype.list = function (view) {
        return this.$http.get(this.apisURL + (view ? '?view=' + view : ''));
    };
    ApiService.prototype.listByGroup = function (group) {
        return this.$http.get(this.apisURL + '?group=' + group);
    };
    ApiService.prototype.start = function (name) {
        return this.$http.post(this.apisURL + name + '?action=START');
    };
    ApiService.prototype.stop = function (name) {
        return this.$http.post(this.apisURL + name + '?action=STOP');
    };
    ApiService.prototype.reload = function (name) {
        return this.$http.post(this.apisURL + 'reload/' + name);
    };
    ApiService.prototype.create = function (api) {
        return this.$http.post(this.apisURL, api);
    };
    ApiService.prototype.update = function (api) {
        return this.$http.put(this.apisURL + api.id, { 'version': api.version, 'description': api.description, 'proxy': api.proxy, 'paths': api.paths, 'private': api.private,
            'visibility': api.visibility, 'name': api.name, 'services': api.services, 'properties': api.properties, 'tags': api.tags,
            'picture': api.picture, 'resources': api.resources, 'views': api.views,
            'group': api.group ? api.group.id : ''
        });
    };
    ApiService.prototype.delete = function (name) {
        return this.$http.delete(this.apisURL + name);
    };
    ApiService.prototype.listPolicies = function (apiName) {
        return this.$http.get(this.apisURL + apiName + '/policies');
    };
    ApiService.prototype.isAPISynchronized = function (apiId) {
        return this.$http.get(this.apisURL + apiId + '/state');
    };
    ApiService.prototype.deploy = function (apiId) {
        return this.$http.post(this.apisURL + apiId + '/deploy');
    };
    ApiService.prototype.rollback = function (apiId, apiDescriptor) {
        return this.$http.post(this.apisURL + apiId + '/rollback', apiDescriptor);
    };
    ApiService.prototype.import = function (apiId, apiDefinition) {
        return this.$http.post(this.apisURL + (apiId ? apiId + '/' : '') + 'import', apiDefinition);
    };
    ApiService.prototype.importSwagger = function (swaggerDescriptor) {
        return this.$http.post(this.apisURL + 'import/swagger', swaggerDescriptor);
    };
    ApiService.prototype.export = function (apiId, exclude) {
        return this.$http.get(this.apisURL + apiId + '/export?exclude=' + exclude.join(","));
    };
    ApiService.prototype.verify = function (criteria) {
        return this.$http.post(this.apisURL + 'verify', criteria);
    };
    /*
     * Analytics
     */
    ApiService.prototype.analytics = function (api, request) {
        var url = this.apisURL + api + '/analytics?';
        var keys = Object.keys(request);
        _.forEach(keys, function (key) {
            var val = request[key];
            if (val !== undefined && val !== '') {
                url += key + '=' + val + '&';
            }
        });
        return this.$http.get(url);
    };
    /*
     * Health
     */
    ApiService.prototype.apiHealth = function (api, interval, from, to) {
        return this.$http.get(this.apisURL + api + '/health?interval=' + interval + '&from=' + from + '&to=' + to);
    };
    /*
     * Members
     */
    ApiService.prototype.getMembers = function (api) {
        return this.$http.get(this.apisURL + api + '/members');
    };
    ApiService.prototype.addOrUpdateMember = function (api, member) {
        return this.$http.post(this.apisURL + api + '/members?user=' + member.username + '&type=' + member.type, '');
    };
    ApiService.prototype.deleteMember = function (api, memberUsername) {
        return this.$http.delete(this.apisURL + api + '/members?user=' + memberUsername);
    };
    ApiService.prototype.transferOwnership = function (api, memberUsername) {
        return this.$http.post(this.apisURL + api + '/members/transfer_ownership?user=' + memberUsername);
    };
    /*
     * API events
     */
    ApiService.prototype.getApiEvents = function (api, eventTypes) {
        return this.$http.get(this.apisURL + api + '/events?type=' + eventTypes);
    };
    ApiService.prototype.listPlans = function (apiId, type) {
        var url = this.$http.get(this.apisURL + apiId + '/plans');
        if (type) {
            url += '?type=' + type;
        }
        return url;
    };
    /*
     * API plans
     */
    ApiService.prototype.getApiPlans = function (apiId) {
        return this.$http.get(this.apisURL + apiId + '/plans?status=staging,published,closed');
    };
    ApiService.prototype.getPublishedApiPlans = function (apiId) {
        return this.$http.get(this.apisURL + apiId + '/plans?status=published');
    };
    ApiService.prototype.savePlan = function (apiId, plan) {
        if (plan.id) {
            return this.$http.put(this.apisURL + apiId + '/plans/' + plan.id, {
                id: plan.id, name: plan.name, description: plan.description,
                validation: plan.validation, policies: plan.policies,
                characteristics: plan.characteristics, order: plan.order, paths: plan.paths
            });
        }
        else {
            return this.$http.post(this.apisURL + apiId + '/plans', {
                name: plan.name, description: plan.description, api: plan.api,
                validation: plan.validation, policies: plan.policies,
                characteristics: plan.characteristics, type: plan.type, paths: plan.paths,
                security: plan.security
            });
        }
    };
    ApiService.prototype.closePlan = function (apiId, planId) {
        return this.$http.post(this.apisURL + apiId + '/plans/' + planId + '/_close');
    };
    ApiService.prototype.deletePlan = function (apiId, planId) {
        return this.$http.delete(this.apisURL + apiId + '/plans/' + planId);
    };
    ApiService.prototype.publishPlan = function (apiId, planId) {
        return this.$http.post(this.apisURL + apiId + '/plans/' + planId + '/_publish');
    };
    ApiService.prototype.updatePlanSubscription = function (apiId, subscription) {
        return this.$http.put(this.apisURL + apiId + '/plans/subscriptions/' + subscription.id, subscription);
    };
    ApiService.prototype.processPlanSubscription = function (apiId, planId, subscriptionId, processSubscription) {
        return this.$http.post(this.apisURL + apiId + '/plans/' + planId + '/subscriptions/' + subscriptionId + '/process', processSubscription);
    };
    ApiService.prototype.getPlanSubscriptions = function (apiId, planId) {
        return this.$http.get(this.apisURL + apiId + '/plans/' + planId + '/subscriptions/');
    };
    /*
     * API subscriptions
     */
    ApiService.prototype.getSubscriptions = function (apiId) {
        return this.$http.get(this.apisURL + apiId + '/subscriptions');
    };
    ApiService.prototype.revokeSubscription = function (apiId, subscriptionId) {
        return this.$http.delete(this.apisURL + apiId + '/subscriptions/' + subscriptionId);
    };
    ApiService.prototype.listApiKeys = function (apiId, subscriptionId) {
        return this.$http.get(this.apisURL + apiId + '/subscriptions/' + subscriptionId + '/keys');
    };
    ApiService.prototype.revokeApiKey = function (apiId, subscriptionId, apiKey) {
        return this.$http.delete(this.apisURL + apiId + '/subscriptions/' + subscriptionId + '/keys/' + apiKey);
    };
    ApiService.prototype.renewApiKey = function (apiId, subscriptionId) {
        return this.$http.post(this.apisURL + apiId + '/subscriptions/' + subscriptionId);
    };
    ApiService.prototype.updateApiKey = function (apiId, apiKey) {
        return this.$http.put(this.apisURL + apiId + '/keys/' + apiKey.key, apiKey);
    };
    return ApiService;
}());
exports.default = ApiService;
