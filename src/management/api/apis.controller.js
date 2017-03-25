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
var ApisController = (function () {
    function ApisController(ApiService, $mdDialog, $scope, $state, Constants, Build, resolvedApis, UserService, graviteeUser, $q, resolvedViews) {
        'ngInject';
        this.ApiService = ApiService;
        this.$mdDialog = $mdDialog;
        this.$scope = $scope;
        this.$state = $state;
        this.Constants = Constants;
        this.Build = Build;
        this.resolvedApis = resolvedApis;
        this.UserService = UserService;
        this.graviteeUser = graviteeUser;
        this.$q = $q;
        this.graviteeUser = graviteeUser;
        this.graviteeUIVersion = Build.version;
        this.portalTitle = Constants.portalTitle;
        this.apis = resolvedApis.data;
        this.apisScrollAreaHeight = this.$state.current.name === 'apis.list' ? 195 : 90;
        this.isAPIsHome = this.$state.includes('apis');
        this.createMode = !Constants.devMode; // && Object.keys($rootScope.graviteeUser).length > 0;
        this.views = resolvedViews;
        this.reloadSyncState();
        $scope.$on('$stateChangeStart', function () {
            $scope.hideApis = true;
        });
    }
    ApisController.prototype.reloadSyncState = function () {
        var _this = this;
        var promises = _.map(this.apis, function (api) {
            if (_this.isOwner(api) && !_this.devMode) {
                return _this.ApiService.isAPISynchronized(api.id)
                    .then(function (sync) { return sync; });
            }
        });
        this.$q.all(_.filter(promises, function (p) { return p !== undefined; }))
            .then(function (syncList) {
            _this.syncStatus = _.fromPairs(_.map(syncList, function (sync) {
                return [sync.data.api_id, sync.data.is_synchronized];
            }));
        });
    };
    ApisController.prototype.update = function (api) {
        var _this = this;
        this.ApiService.update(api).then(function () {
            _this.$scope.formApi.$setPristine();
            _this.NotificationService.show('Api updated with success');
        });
    };
    ApisController.prototype.getVisibilityIcon = function (api) {
        switch (api.visibility) {
            case 'public':
                return 'public';
            case 'restricted':
                return 'vpn_lock';
            case 'private':
                return 'lock';
        }
    };
    ApisController.prototype.getVisibility = function (api) {
        switch (api.visibility) {
            case 'public':
                return 'Public';
            case 'restricted':
                return 'Restricted';
            case 'private':
                return 'Private';
        }
    };
    ApisController.prototype.isOwner = function (api) {
        return api.permission && (api.permission === 'owner' || api.permission === 'primary_owner');
    };
    ApisController.prototype.showImportDialog = function () {
        var that = this;
        this.$mdDialog.show({
            controller: 'DialogApiImportController',
            controllerAs: 'dialogApiImportCtrl',
            template: require('./general/dialog/apiImport.dialog.html'),
            apiId: '',
            clickOutsideToClose: true
        }).then(function (response) {
            if (response) {
                that.$state.go('apis.admin.general', { apiId: response.data.id }, { reload: true });
            }
        });
    };
    ApisController.prototype.getSubMessage = function () {
        if (!this.graviteeUser.username) {
            return 'Login to get access to more APIs';
        }
        else if (this.UserService.isUserInRoles(['ADMIN', 'API_PUBLISHER'])) {
            return 'Start creating an API';
        }
        else {
            return '';
        }
    };
    return ApisController;
}());
exports.ApisController = ApisController;
exports.default = ApisController;
