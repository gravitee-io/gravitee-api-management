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
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var _ = require("lodash");
var ApplicationService = (function () {
    function ApplicationService($http, Constants) {
        'ngInject';
        this.$http = $http;
        this.applicationsURL = Constants.baseURL + "applications/";
    }
    ApplicationService.prototype.subscriptionsURL = function (applicationId) {
        return "" + this.applicationsURL + applicationId + "/subscriptions/";
    };
    ApplicationService.prototype.get = function (applicationId) {
        return this.$http.get(this.applicationsURL + applicationId);
    };
    ApplicationService.prototype.getMembers = function (applicationId) {
        return this.$http.get(this.applicationsURL + applicationId + '/members');
    };
    ApplicationService.prototype.addOrUpdateMember = function (applicationId, member) {
        var url = "" + this.applicationsURL + applicationId + "/members?user=" + member.username + "&type=" + member.type;
        return this.$http.post(url, '');
    };
    ApplicationService.prototype.deleteMember = function (applicationId, memberUsername) {
        return this.$http.delete(this.applicationsURL + applicationId + '/members?user=' + memberUsername);
    };
    ApplicationService.prototype.list = function () {
        return this.$http.get(this.applicationsURL);
    };
    ApplicationService.prototype.listByGroup = function (group) {
        return this.$http.get(this.applicationsURL + "?group=" + group);
    };
    ApplicationService.prototype.create = function (application) {
        return this.$http.post(this.applicationsURL, application);
    };
    ApplicationService.prototype.update = function (application) {
        return this.$http.put(this.applicationsURL + application.id, {
            'name': application.name,
            'description': application.description,
            'type': application.type,
            'group': application.group ? application.group.id : ''
        });
    };
    ApplicationService.prototype.delete = function (applicationId) {
        return this.$http.delete(this.applicationsURL + applicationId);
    };
    ApplicationService.prototype.search = function (query) {
        return this.$http.get(this.applicationsURL + "?query=" + query);
    };
    // Plans
    ApplicationService.prototype.subscribe = function (applicationId, planId) {
        return this.$http.post(this.subscriptionsURL(applicationId) + '?plan=' + planId, '');
    };
    ApplicationService.prototype.listSubscriptions = function (applicationId, planId) {
        var url = this.subscriptionsURL(applicationId);
        if (planId) {
            url = url + '?plan=' + planId;
        }
        return this.$http.get(url);
    };
    ApplicationService.prototype.getSubscription = function (applicationId, subscriptionId) {
        return this.$http.get(this.subscriptionsURL(applicationId) + subscriptionId);
    };
    ApplicationService.prototype.listApiKeys = function (applicationId, subscriptionId) {
        return this.$http.get(this.subscriptionsURL(applicationId) + subscriptionId + '/keys');
    };
    ApplicationService.prototype.renewApiKey = function (applicationId, subscriptionId) {
        return this.$http.post(this.subscriptionsURL(applicationId) + subscriptionId, '');
    };
    ApplicationService.prototype.revokeApiKey = function (applicationId, subscriptionId, apiKey) {
        return this.$http.delete(this.subscriptionsURL(applicationId) + subscriptionId + '/keys/' + apiKey);
    };
    /*
     * Analytics
     */
    ApplicationService.prototype.analytics = function (application, request) {
        var url = this.applicationsURL + application + '/analytics?';
        var keys = Object.keys(request);
        _.forEach(keys, function (key) {
            var val = request[key];
            if (val !== undefined && val !== '') {
                url += key + '=' + val + '&';
            }
        });
        return this.$http.get(url);
    };
    return ApplicationService;
}());
exports.default = ApplicationService;
