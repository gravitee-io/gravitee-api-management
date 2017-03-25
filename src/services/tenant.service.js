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
var TenantService = (function () {
    function TenantService($http, Constants) {
        'ngInject';
        this.$http = $http;
        this.tenantsURL = Constants.baseURL + "configuration/tenants/";
    }
    TenantService.prototype.list = function () {
        return this.$http.get(this.tenantsURL);
    };
    TenantService.prototype.create = function (tenants) {
        if (tenants && tenants.length) {
            return this.$http.post(this.tenantsURL, tenants);
        }
    };
    TenantService.prototype.update = function (tenants) {
        if (tenants && tenants.length) {
            return this.$http.put(this.tenantsURL, tenants);
        }
    };
    TenantService.prototype.delete = function (tenant) {
        if (tenant) {
            return this.$http.delete(this.tenantsURL + tenant.id);
        }
    };
    return TenantService;
}());
exports.default = TenantService;
