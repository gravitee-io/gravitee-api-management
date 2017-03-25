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
var GroupService = (function () {
    function GroupService($http, Constants) {
        'ngInject';
        this.$http = $http;
        this.groupsURL = Constants.baseURL + "configuration/groups";
    }
    GroupService.prototype.getEmptyGroup = function () {
        return {
            "id": null,
            "name": "NONE"
        };
    };
    GroupService.prototype.list = function (type) {
        if (type) {
            return this.$http.get(this.groupsURL + "?type=" + type);
        }
        return this.$http.get(this.groupsURL);
    };
    GroupService.prototype.getMembers = function (groupId) {
        return this.$http.get([this.groupsURL, groupId, "members"].join("/"));
    };
    GroupService.prototype.create = function (newGroup) {
        if (newGroup) {
            return this.$http.post(this.groupsURL, newGroup);
        }
    };
    GroupService.prototype.update = function (groupId, name) {
        if (groupId && name) {
            return this.$http.put([this.groupsURL, groupId].join("/"), { 'name': name });
        }
    };
    GroupService.prototype.remove = function (groupId) {
        if (groupId) {
            return this.$http.delete([this.groupsURL, groupId].join("/"));
        }
    };
    GroupService.prototype.addOrUpdateMember = function (group, member) {
        return this.$http.post([this.groupsURL, group, 'members?user=' + member.username + '&type=' + member.type].join("/"));
    };
    GroupService.prototype.deleteMember = function (group, memberUsername) {
        return this.$http.delete([this.groupsURL, group, 'members?user=' + memberUsername].join("/"));
    };
    return GroupService;
}());
exports.default = GroupService;
