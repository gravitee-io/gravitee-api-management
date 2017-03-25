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
var angular = require("angular");
var GroupsController = (function () {
    function GroupsController(GroupService, ApplicationService, ApiService, NotificationService, $q, $mdDialog, $mdSidenav) {
        'ngInject';
        this.GroupService = GroupService;
        this.ApplicationService = ApplicationService;
        this.ApiService = ApiService;
        this.NotificationService = NotificationService;
        this.$q = $q;
        this.$mdDialog = $mdDialog;
        this.$mdSidenav = $mdSidenav;
        this.groupType = "APPLICATION";
        this.applicationGroups = [];
        this.apiGroups = [];
        this.listGroups();
    }
    GroupsController.prototype.listGroups = function () {
        var _this = this;
        this.GroupService.list().then(function (listResponse) {
            var promises = _.map(listResponse.data, function (group) {
                return _this.GroupService.getMembers(group.id).then(function (getMembersResponse) {
                    return {
                        group: group,
                        members: getMembersResponse.data
                    };
                }).then(function (groupWithMembers) {
                    if (groupWithMembers.group.type === "application") {
                        return _this.ApplicationService.listByGroup(groupWithMembers.group.id).then(function (applicationsResponse) {
                            return {
                                group: groupWithMembers.group,
                                members: groupWithMembers.members,
                                applications: applicationsResponse.data
                            };
                        });
                    }
                    else {
                        return _this.ApiService.listByGroup(groupWithMembers.group.id).then(function (apiResponse) {
                            return {
                                group: groupWithMembers.group,
                                members: groupWithMembers.members,
                                apis: apiResponse.data
                            };
                        });
                    }
                });
            });
            _this.$q.all(promises).then(function (responses) {
                var partition = _.partition(responses, function (item) {
                    return item.group.type === "application";
                });
                _this.applicationGroups = partition[0];
                _this.apiGroups = partition[1];
            });
        });
    };
    GroupsController.prototype.showMembers = function (item) {
        this.selectedGroup = item;
        this.$mdSidenav('group-members').toggle();
    };
    GroupsController.prototype.showApis = function (item) {
        this.selectedGroup = item;
        this.$mdSidenav('group-apis').toggle();
    };
    GroupsController.prototype.showApplications = function (item) {
        this.selectedGroup = item;
        this.$mdSidenav('group-applications').toggle();
    };
    GroupsController.prototype.selectGroupType = function (type) {
        this.groupType = type;
    };
    GroupsController.prototype.showAddGroupModal = function () {
        var _this = this;
        this.$mdDialog.show({
            controller: 'DialogAddGroupController',
            controllerAs: 'dialogAddGroupCtrl',
            template: require('./dialog/add-group.dialog.html'),
            currentName: "",
            action: "Add",
            clickOutsideToClose: true
        }).then(function (name) {
            if (name) {
                _this.GroupService.create({
                    name: name,
                    type: _this.groupType
                }).then(function () {
                    _this.listGroups();
                });
            }
        });
    };
    GroupsController.prototype.showRenameGroupModal = function (ev, groupId, name) {
        ev.stopPropagation();
        var _this = this;
        this.$mdDialog.show({
            controller: 'DialogAddGroupController',
            controllerAs: 'dialogAddGroupCtrl',
            template: require('./dialog/add-group.dialog.html'),
            currentName: name,
            action: "Edit",
            clickOutsideToClose: true
        }).then(function (name) {
            if (name) {
                _this.GroupService.update(groupId, name)
                    .then(function () {
                    _this.listGroups();
                });
            }
        });
    };
    GroupsController.prototype.showAddMemberModal = function (ev) {
        var _this = this;
        this.$mdDialog.show({
            controller: 'DialogAddGroupMemberController',
            template: require('./dialog/addMember.dialog.html'),
            parent: angular.element(document.body),
            targetEvent: ev,
            clickOutsideToClose: true,
            group: _this.selectedGroup
        }).then(function (members) {
            if (members) {
                _this.selectedGroup.members = _.unionWith(members, _this.selectedGroup.members, _.isEqual);
            }
        }, function () {
            // you cancelled the dialog
        });
    };
    GroupsController.prototype.removeGroup = function (ev, groupId, groupName) {
        ev.stopPropagation();
        var _this = this;
        this.$mdDialog.show({
            controller: 'DialogConfirmController',
            controllerAs: 'ctrl',
            template: require('../../../components/dialog/confirmWarning.dialog.html'),
            clickOutsideToClose: true,
            locals: {
                title: 'Would you like to remove the group "' + groupName + '" ?',
                confirmButton: 'Remove'
            }
        }).then(function (response) {
            if (response) {
                _this.GroupService.remove(groupId).then(function () {
                    _this.listGroups();
                });
            }
        });
    };
    GroupsController.prototype.removeMember = function (ev, username) {
        ev.stopPropagation();
        var _this = this;
        this.$mdDialog.show({
            controller: 'DialogConfirmController',
            controllerAs: 'ctrl',
            template: require('../../../components/dialog/confirmWarning.dialog.html'),
            clickOutsideToClose: true,
            locals: {
                msg: '',
                title: 'Would you like to remove the user "' + username + '" ?',
                confirmButton: 'Remove'
            }
        }).then(function (response) {
            if (response) {
                _this.GroupService.deleteMember(_this.selectedGroup.group.id, username).then(function () {
                    _this.NotificationService.show('Member ' + username + ' has been removed from the group');
                    _.remove(_this.selectedGroup.members, function (m) {
                        return m.username === username;
                    });
                });
            }
        });
    };
    GroupsController.prototype.updateMember = function (member) {
        var _this = this;
        this.GroupService.addOrUpdateMember(this.selectedGroup.group.id, member).then(function () {
            _this.NotificationService.show('Member ' + member.username + ' has been updated');
        });
    };
    return GroupsController;
}());
exports.default = GroupsController;
