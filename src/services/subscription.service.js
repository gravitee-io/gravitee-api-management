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
var SubscriptionService = (function () {
    function SubscriptionService($http, Constants) {
        'ngInject';
        this.$http = $http;
        this.subscriptionsURL = Constants.baseURL + "subscriptions/";
    }
    SubscriptionService.prototype.list = function (plan, application) {
        var url = this.subscriptionsURL;
        if (plan) {
            url += '?plan=' + plan;
        }
        if (application) {
            url += (plan ? '&' : '?') + 'application=' + application;
        }
        return this.$http.get(url);
    };
    return SubscriptionService;
}());
exports.default = SubscriptionService;
