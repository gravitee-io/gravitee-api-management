"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * Created by david on 27/11/2015.
 */
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
var angular = require("angular");
var _ = require("lodash");
var ApiMembersController = (function () {
    function ApiMembersController(ApiService, resolvedApi, resolvedMembers, $state, $mdDialog, NotificationService, $scope, UserService, GroupService) {
        'ngInject';
        var _this = this;
        this.ApiService = ApiService;
        this.resolvedApi = resolvedApi;
        this.resolvedMembers = resolvedMembers;
        this.$state = $state;
        this.$mdDialog = $mdDialog;
        this.NotificationService = NotificationService;
        this.$scope = $scope;
        this.UserService = UserService;
        this.GroupService = GroupService;
        this.api = resolvedApi.data;
        this.members = resolvedMembers.data;
        this.membershipTypes = ['owner', 'user'];
        this.newPrimaryOwner = null;
        this.$scope.searchText = "";
        if (this.api.group && this.api.group.id != null) {
            GroupService.getMembers(this.api.group.id).then(function (members) {
                _this.groupMembers = members.data;
            });
        }
    }
    ApiMembersController.prototype.updateMember = function (member) {
        var _this = this;
        this.ApiService.addOrUpdateMember(this.api.id, member).then(function () {
            _this.NotificationService.show('Member ' + member.username + " has been updated with role " + member.type);
        });
    };
    ApiMembersController.prototype.deleteMember = function (member) {
        var _this = this;
        var index = this.members.indexOf(member);
        this.ApiService.deleteMember(this.api.id, member.username).then(function () {
            _this.members.splice(index, 1);
            _this.NotificationService.show("Member " + member.username + " has been removed");
        });
    };
    ApiMembersController.prototype.isOwner = function () {
        return this.api.permission && (this.api.permission === 'owner' || this.api.permission === 'primary_owner');
    };
    ApiMembersController.prototype.isPrimaryOwner = function () {
        return this.api.permission && (this.api.permission === 'primary_owner');
    };
    ApiMembersController.prototype.showAddMemberModal = function (ev) {
        var _this = this;
        this.$mdDialog.show({
            controller: 'DialogAddMemberApiController',
            template: require('./addMember.dialog.html'),
            parent: angular.element(document.body),
            targetEvent: ev,
            clickOutsideToClose: true,
            locals: {
                api: this.api,
                apiMembers: this.members
            }
        }).then(function (api) {
            if (api) {
                _this.ApiService.getMembers(api.id).then(function (response) {
                    _this.members = response.data;
                });
            }
        }, function () {
            // You cancelled the dialog
        });
    };
    ApiMembersController.prototype.showPermissionsInformation = function () {
        this.$mdDialog.show({
            controller: 'DialogApiPermissionsHelpController',
            controllerAs: 'ctrl',
            template: require('./permissions.dialog.html'),
            parent: angular.element(document.body),
            clickOutsideToClose: true
        });
    };
    ApiMembersController.prototype.showDeleteMemberConfirm = function (ev, member) {
        ev.stopPropagation();
        var self = this;
        this.$mdDialog.show({
            controller: 'DialogConfirmController',
            controllerAs: 'ctrl',
            template: require('../../../components/dialog/confirm.dialog.html'),
            clickOutsideToClose: true,
            locals: {
                title: 'Would you like to remove the member ?',
                confirmButton: 'Remove'
            }
        }).then(function (response) {
            if (response) {
                self.deleteMember(member);
            }
        });
    };
    ApiMembersController.prototype.searchUser = function (query) {
        var _this = this;
        if (query) {
            return this.UserService.search(query).then(function (response) {
                var usersFound = response.data;
                var filterUsers = _.filter(usersFound, function (user) {
                    return _.findIndex(_this.members, function (apiMember) {
                        return apiMember.username === user.id && apiMember.type === 'primary_owner';
                    }) === -1;
                });
                return filterUsers;
            });
        }
        else {
            var filterMembers = _.filter(this.members, function (member) { return member.type !== 'primary_owner'; });
            var members = _.flatMap(filterMembers, function (member) { return { 'id': member.username }; });
            return members;
        }
    };
    ApiMembersController.prototype.selectedItemChange = function (item) {
        if (item) {
            this.newPrimaryOwner = item;
        }
        else {
            if (this.newPrimaryOwner !== null) {
                this.newPrimaryOwner = null;
            }
        }
    };
    ApiMembersController.prototype.showTransferOwnershipConfirm = function (ev) {
        var _this = this;
        this.$mdDialog.show({
            controller: 'DialogTransferApiController',
            template: require('./transferAPI.dialog.html'),
            parent: angular.element(document.body),
            targetEvent: ev,
            clickOutsideToClose: true
        }).then(function (transferAPI) {
            if (transferAPI) {
                _this.transferOwnership();
            }
        }, function () {
            // You cancelled the dialog
        });
    };
    ApiMembersController.prototype.transferOwnership = function () {
        var _this = this;
        this.ApiService.transferOwnership(this.api.id, this.newPrimaryOwner.id).then(function () {
            _this.NotificationService.show("API ownership changed !");
            _this.$state.go(_this.$state.current, {}, { reload: true });
        });
    };
    return ApiMembersController;
}());
exports.default = ApiMembersController;
