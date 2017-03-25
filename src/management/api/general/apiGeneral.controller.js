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
var ApiAdminController = (function () {
    function ApiAdminController(ApiService, NotificationService, UserService, $scope, $mdDialog, $mdEditDialog, $rootScope, resolvedApi, base64, $state, ViewService, GroupService, TagService, SidenavService) {
        'ngInject';
        var _this = this;
        this.ApiService = ApiService;
        this.NotificationService = NotificationService;
        this.UserService = UserService;
        this.$scope = $scope;
        this.$mdDialog = $mdDialog;
        this.$mdEditDialog = $mdEditDialog;
        this.$rootScope = $rootScope;
        this.resolvedApi = resolvedApi;
        this.base64 = base64;
        this.$state = $state;
        this.ViewService = ViewService;
        this.GroupService = GroupService;
        this.TagService = TagService;
        this.SidenavService = SidenavService;
        if ('apis.admin.general' === $state.current.name) {
            $state.go('apis.admin.general.main');
        }
        this.ApiService = ApiService;
        this.NotificationService = NotificationService;
        this.UserService = UserService;
        this.GroupService = GroupService;
        this.$scope = $scope;
        this.$rootScope = $rootScope;
        this.$mdEditDialog = $mdEditDialog;
        this.$mdDialog = $mdDialog;
        this.initialApi = _.cloneDeep(resolvedApi.data);
        this.api = resolvedApi.data;
        this.$scope.selected = [];
        if (!this.api.group) {
            this.api.group = GroupService.getEmptyGroup();
        }
        this.groups = [this.api.group];
        this.$scope.lbs = [
            {
                name: 'Round-Robin',
                value: 'ROUND_ROBIN'
            }, {
                name: 'Random',
                value: 'RANDOM'
            }, {
                name: 'Weighted Round-Robin',
                value: 'WEIGHTED_ROUND_ROBIN'
            }, {
                name: 'Weighted Random',
                value: 'WEIGHTED_RANDOM'
            }
        ];
        this.initState();
        ViewService.list().then(function (response) {
            _this.views = response.data;
        });
        TagService.list().then(function (response) {
            _this.tags = response.data;
        });
    }
    ApiAdminController.prototype.toggleVisibility = function () {
        if (this.api.visibility === "public") {
            this.api.visibility = "private";
        }
        else {
            this.api.visibility = "public";
        }
        this.formApi.$setDirty();
    };
    ApiAdminController.prototype.initState = function () {
        this.$scope.apiEnabled = (this.$scope.$parent.apiCtrl.api.state === 'started');
        // Failover
        this.failoverEnabled = (this.api.proxy.failover !== undefined);
        // Context-path editable
        this.contextPathEditable = (this.api.permission === 'primary_owner') || this.UserService.isUserInRoles(['ADMIN']);
        var self = this;
        this.$scope.$on("apiChangeSucceed", function () {
            self.initialApi = _.cloneDeep(self.$scope.$parent.apiCtrl.api);
            self.api = self.$scope.$parent.apiCtrl.api;
        });
    };
    ApiAdminController.prototype.loadApplicationGroups = function () {
        var _this = this;
        this.GroupService.list("API").then(function (groups) {
            _this.groups = _.union([_this.GroupService.getEmptyGroup()], groups.data);
        });
    };
    ApiAdminController.prototype.changeLifecycle = function (id) {
        var started = this.api.state === 'started';
        var that = this;
        this.$mdDialog.show({
            controller: 'DialogConfirmController',
            controllerAs: 'ctrl',
            template: require('../../../components/dialog/confirmWarning.dialog.html'),
            clickOutsideToClose: true,
            locals: {
                title: "Are you sure you want to " + (started ? "stop" : "start") + " the API ?",
                msg: '',
                confirmButton: (started ? 'stop' : 'start')
            }
        }).then(function (response) {
            if (response) {
                if (started) {
                    that.ApiService.stop(id).then(function () {
                        that.api.state = 'stopped';
                        that.$scope.apiEnabled = false;
                        that.NotificationService.show("API " + that.initialApi.name + " has been stopped!");
                    });
                }
                else {
                    that.ApiService.start(id).then(function () {
                        that.api.state = 'started';
                        that.$scope.apiEnabled = true;
                        that.NotificationService.show("API " + that.initialApi.name + " has been started!");
                    });
                }
            }
        }).catch(function () {
            that.initState();
        });
    };
    ApiAdminController.prototype.editWeight = function (event, endpoint) {
        event.stopPropagation(); // in case autoselect is enabled
        var _that = this;
        var editDialog = {
            modelValue: endpoint.weight,
            placeholder: 'Weight',
            save: function (input) {
                endpoint.weight = input.$modelValue;
                _that.formApi.$setDirty();
            },
            targetEvent: event,
            title: 'Endpoint weight',
            type: 'number',
            validators: {
                'ng-required': 'true',
                'min': 1,
                'max': 99
            }
        };
        var promise = this.$mdEditDialog.large(editDialog);
        promise.then(function (ctrl) {
            var input = ctrl.getInput();
            input.$viewChangeListeners.push(function () {
                input.$setValidity('test', input.$modelValue !== 'test');
            });
        });
    };
    ApiAdminController.prototype.removeEndpoints = function () {
        var _that = this;
        var that = this;
        this.$mdDialog.show({
            controller: 'DialogConfirmController',
            controllerAs: 'ctrl',
            template: require('../../../components/dialog/confirmWarning.dialog.html'),
            clickOutsideToClose: true,
            locals: {
                title: 'Are you sure you want to delete endpoint(s) ?',
                msg: '',
                confirmButton: 'Delete'
            }
        }).then(function (response) {
            if (response) {
                _(_that.$scope.selected).forEach(function (endpoint) {
                    _(_that.api.proxy.endpoints).forEach(function (endpoint2, index, object) {
                        if (endpoint2 !== undefined && endpoint2.name === endpoint.name) {
                            object.splice(index, 1);
                        }
                    });
                });
                that.update(that.api);
            }
        });
    };
    ApiAdminController.prototype.reset = function () {
        this.api = _.cloneDeep(this.initialApi);
        this.$scope.$parent.apiCtrl.api = this.api;
        this.formApi.$setPristine();
        this.formApi.$setUntouched();
    };
    ApiAdminController.prototype.delete = function (id) {
        var that = this;
        this.$mdDialog.show({
            controller: 'DialogConfirmController',
            controllerAs: 'ctrl',
            template: require('../../../components/dialog/confirmWarning.dialog.html'),
            clickOutsideToClose: true,
            locals: {
                title: 'Are you sure you want to delete \'' + this.api.name + '\' API ?',
                msg: '',
                confirmButton: 'Delete'
            }
        }).then(function (response) {
            if (response) {
                that.ApiService.delete(id).then(function () {
                    that.NotificationService.show('API \'' + that.initialApi.name + '\' has been removed');
                    that.$state.go('apis.list', {}, { reload: true });
                });
            }
        });
    };
    ApiAdminController.prototype.onApiUpdate = function (updatedApi) {
        this.api = updatedApi;
        this.initState();
        this.formApi.$setPristine();
        this.$rootScope.$broadcast("apiChangeSuccess");
        this.NotificationService.show('API \'' + this.initialApi.name + '\' saved');
        this.SidenavService.setCurrentResource(this.api.name);
    };
    ApiAdminController.prototype.update = function (api) {
        var _this = this;
        if (!this.failoverEnabled) {
            delete api.proxy.failover;
        }
        this.ApiService.update(api).then(function (updatedApi) {
            _this.onApiUpdate(updatedApi.data);
        });
    };
    ApiAdminController.prototype.showImportDialog = function () {
        var that = this;
        this.$mdDialog.show({
            controller: 'DialogApiImportController',
            controllerAs: 'dialogApiImportCtrl',
            template: require('./dialog/apiImport.dialog.html'),
            clickOutsideToClose: true,
            locals: {
                apiId: this.$scope.$parent.apiCtrl.api.id
            }
        }).then(function (response) {
            if (response) {
                that.onApiUpdate(response.data);
            }
        });
    };
    ApiAdminController.prototype.showExportDialog = function () {
        this.$mdDialog.show({
            controller: 'DialogApiExportController',
            controllerAs: 'dialogApiExportCtrl',
            template: require('./dialog/apiExport.dialog.html'),
            clickOutsideToClose: true,
            locals: {
                apiId: this.$scope.$parent.apiCtrl.api.id
            }
        });
    };
    ApiAdminController.prototype.getTenant = function (tenantId) {
        return _.find(this.tenants, { 'id': tenantId });
    };
    return ApiAdminController;
}());
exports.default = ApiAdminController;
