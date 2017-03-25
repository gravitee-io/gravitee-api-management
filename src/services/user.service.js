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
var user_1 = require("../entities/user");
var UserService = (function () {
    function UserService($http, $q, $rootScope, Constants) {
        'ngInject';
        this.$http = $http;
        this.$q = $q;
        this.$rootScope = $rootScope;
        this.baseURL = Constants.baseURL;
        this.usersURL = Constants.baseURL + "users/";
        this.userURL = Constants.baseURL + "user/";
    }
    UserService.prototype.list = function () {
        return this.$http.get(this.usersURL);
    };
    UserService.prototype.get = function (code) {
        return this.$http.get(this.usersURL + code).then(function (response) { return Object.assign(new user_1.User(), response.data); });
    };
    UserService.prototype.create = function (user) {
        return this.$http.post(this.baseURL + "users", user);
    };
    UserService.prototype.register = function (user) {
        return this.$http.post(this.usersURL + "register", user);
    };
    UserService.prototype.search = function (query) {
        return this.$http.get(this.usersURL + "?query=" + query);
    };
    UserService.prototype.isUserInRoles = function (roles) {
        return this.currentUser && this.currentUser.allowedTo(roles);
        /*
          if (!this.currentUser) {
            return false;
          }
        if (this.currentUser && (!roles || roles.length === 0)) {
            return false;
          }
        var rolesAllowed = false, that = this;
          _.forEach(roles, function(role) {
            _.forEach(that.currentUser.authorities, function(authority) {
            if (authority.authority === role) {
                rolesAllowed = true;
                return;
              }
            });
          });
        return rolesAllowed;
        */
    };
    UserService.prototype.current = function () {
        if (!this.currentUser || !this.currentUser.username) {
            var that_1 = this;
            return this.$http.get(this.userURL)
                .then(function (response) { return Object.assign(new user_1.User(), response.data); })
                .then(function (response) {
                that_1.currentUser = response;
                return that_1.currentUser;
            });
        }
        else {
            return this.$q.resolve(this.currentUser);
        }
    };
    UserService.prototype.isAuthenticated = function () {
        return (this.currentUser !== undefined && this.currentUser.username !== undefined);
    };
    UserService.prototype.login = function (user) {
        return this.$http.post(this.userURL + "login", {}, {
            headers: {
                Authorization: "Basic " + btoa(user.username + ":" + user.password)
            }
        });
    };
    UserService.prototype.logout = function () {
        var that = this;
        return this.$http.post(this.userURL + "logout", {})
            .then(function () { that.currentUser = new user_1.User(); });
    };
    UserService.prototype.currentUserPicture = function () {
        return this.$http.get(this.userURL + this.$rootScope.graviteeUser.username + "/picture");
    };
    UserService.prototype.save = function (user) {
        return this.$http.put(this.userURL + user.username + "/", { username: user.username, picture: user.picture });
    };
    UserService.prototype.listPlanSubscription = function () {
        return this.$http.get(this.userURL + "subscriptions");
    };
    return UserService;
}());
exports.default = UserService;
