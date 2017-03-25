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
var ApiResourcesController = (function () {
    function ApiResourcesController(ApiService, resolvedApi, $mdSidenav, $mdDialog, ResourceService, NotificationService, $scope, $rootScope) {
        'ngInject';
        var _this = this;
        this.ApiService = ApiService;
        this.resolvedApi = resolvedApi;
        this.$mdSidenav = $mdSidenav;
        this.$mdDialog = $mdDialog;
        this.ResourceService = ResourceService;
        this.NotificationService = NotificationService;
        this.$scope = $scope;
        this.$rootScope = $rootScope;
        this.api = resolvedApi.data;
        this.creation = true;
        this.resourceJsonSchemaForm = ["*"];
        this.types = [];
        this.ResourceService.list().then(function (_a) {
            var data = _a.data;
            _this.types = data;
        });
    }
    ApiResourcesController.prototype.initState = function () {
        if (this.resource !== undefined) {
            this.$scope.resourceEnabled = this.resource.enabled;
        }
        else {
            this.$scope.resourceEnabled = false;
        }
    };
    ApiResourcesController.prototype.switchEnabled = function () {
        if (this.resource === undefined) {
            this.resource = {};
        }
        this.resource.enabled = this.$scope.resourceEnabled;
        this.updateApi();
    };
    ApiResourcesController.prototype.showResourcePanel = function (resource) {
        var _this = this;
        this.$mdSidenav('resource-config').toggle();
        if (resource) {
            // Update resource
            this.creation = false;
            this.resource = resource;
            if (!this.resource.configuration) {
                this.resource.configuration = {};
            }
            this.ResourceService.getSchema(this.resource.type).then(function (_a) {
                var data = _a.data;
                _this.resourceJsonSchema = data;
                return {
                    schema: data
                };
            }, function (response) {
                if (response.status === 404) {
                    return {
                        schema: {}
                    };
                }
                else {
                    _this.NotificationService.showError('Unexpected error while loading resource schema for ' + _this.resource.type);
                }
            });
        }
        else {
            // Create new resource
            this.resourceJsonSchema = {};
            this.creation = true;
            this.resource = {};
            this.resource.configuration = {};
            this.resource.enabled = true;
        }
        this.initState();
    };
    ApiResourcesController.prototype.closeResourcePanel = function () {
        this.$mdSidenav('resource-config').close();
    };
    ApiResourcesController.prototype.onTypeChange = function () {
        var _this = this;
        this.resource.configuration = {};
        this.ResourceService.getSchema(this.resource.type).then(function (_a) {
            var data = _a.data;
            _this.resourceJsonSchema = data;
            return {
                schema: data
            };
        }, function (response) {
            if (response.status === 404) {
                _this.resourceJsonSchema = {};
                return {
                    schema: {}
                };
            }
            else {
                //todo manage errors
                _this.NotificationService.showError('Unexpected error while loading resource schema for ' + _this.resource.type);
            }
        });
    };
    ApiResourcesController.prototype.deleteResource = function (resourceIdx) {
        var that = this;
        this.$mdDialog.show({
            controller: 'DialogConfirmController',
            controllerAs: 'ctrl',
            template: require('../../../components/dialog/confirmWarning.dialog.html'),
            clickOutsideToClose: true,
            locals: {
                title: 'Are you sure you want to remove this resource ?',
                msg: '',
                confirmButton: 'Remove'
            }
        }).then(function (response) {
            if (response) {
                that.api.resources.splice(resourceIdx, 1);
                that.updateApi();
            }
        });
    };
    ApiResourcesController.prototype.saveResource = function () {
        delete this.resource.$$hashKey;
        if (this.creation) {
            this.api.resources.push(this.resource);
        }
        this.updateApi();
    };
    ApiResourcesController.prototype.updateApi = function () {
        var that = this;
        return this.ApiService.update(this.api).then(function (_a) {
            var data = _a.data;
            that.closeResourcePanel();
            that.api = data;
            that.$rootScope.$broadcast('apiChangeSuccess');
            that.NotificationService.show('API \'' + that.$scope.$parent.apiCtrl.api.name + '\' saved');
        });
    };
    return ApiResourcesController;
}());
exports.default = ApiResourcesController;
