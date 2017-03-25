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
var ApiPlansController = (function () {
    function ApiPlansController(resolvedPlans, $mdSidenav, $mdDialog, $scope, ApiService, $state, $stateParams, NotificationService, dragularService) {
        'ngInject';
        this.resolvedPlans = resolvedPlans;
        this.$mdSidenav = $mdSidenav;
        this.$mdDialog = $mdDialog;
        this.$scope = $scope;
        this.ApiService = ApiService;
        this.$state = $state;
        this.$stateParams = $stateParams;
        this.NotificationService = NotificationService;
        this.dragularService = dragularService;
        this.plans = resolvedPlans.data;
        this.dndEnabled = true;
        this.statusFilters = ['staging', 'published', 'closed'];
        this.selectedStatus = ['published'];
        this.securityTypes = [
            {
                'id': 'api_key',
                'name': 'API Key'
            }, {
                'id': 'key_less',
                'name': 'Keyless (public)'
            }
        ];
        $scope.planEdit = true;
        this.countByStatus = {};
        this.resetPlan();
        if ($stateParams.state) {
            if (_.includes(this.statusFilters, $stateParams.state)) {
                this.changeFilter($stateParams.state);
            }
            else {
                this.applyFilters();
            }
        }
        else {
            this.applyFilters();
        }
        var that = this;
        $scope.configure = function (plan) {
            that.resetPlan();
            if (!$mdSidenav('plan-edit').isOpen()) {
                $scope.plan = plan;
                if ($scope.plan.paths['/']) {
                    _.forEach($scope.plan.paths['/'], function (path) {
                        if (path['rate-limit']) {
                            $scope.rateLimit = path['rate-limit'].rate;
                        }
                        if (path.quota) {
                            $scope.quota = path.quota.quota;
                        }
                        if (path['resource-filtering']) {
                            $scope.resourceFiltering.whitelist = path['resource-filtering'].whitelist;
                        }
                    });
                }
                $mdSidenav('live-preview').toggle();
                $mdSidenav('plan-edit').toggle();
            }
        };
        $scope.timeUnits = ['SECONDS', 'MINUTES', 'HOURS', 'DAYS'];
        $scope.methods = ['GET', 'POST', 'PUT', 'DELETE', 'HEAD', 'PATCH', 'OPTIONS', 'TRACE', 'CONNECT'];
        this.resetResourceFiltering();
        $scope.$watch('livePreviewIsOpen', function (livePreviewIsOpen) {
            if (livePreviewIsOpen === false && $mdSidenav('plan-edit').isOpen()) {
                $mdSidenav('plan-edit').toggle();
            }
        });
        $scope.$on('planChangeSuccess', function (event, args) {
            that.changeFilter(args.state);
        });
    }
    ApiPlansController.prototype.init = function () {
        var that = this;
        var d = document.querySelector('.plans');
        this.dragularService([d], {
            moves: function () {
                return that.dndEnabled;
            },
            scope: this.$scope,
            containersModel: _.cloneDeep(this.plans),
            nameSpace: 'plan'
        });
        this.$scope.$on('dragulardrop', function (e, el, target, source, dragularList, index) {
            var movedPlan = that.plans[index];
            for (var idx = 0; idx < dragularList.length; idx++) {
                if (movedPlan.id === dragularList[idx].id) {
                    movedPlan.order = idx + 1;
                    break;
                }
            }
            that.plans = dragularList;
            that.ApiService.savePlan(that.$stateParams.apiId, movedPlan).then(function () {
                // sync list from server because orders has been changed
                that.list();
                that.NotificationService.show('Plans have been reordered successfully');
            });
        });
    };
    ApiPlansController.prototype.list = function () {
        var _this = this;
        return this.ApiService.getApiPlans(this.$stateParams.apiId).then(function (response) {
            _this.plans = response.data;
            _this.applyFilters();
            return { plans: _this.plans };
        });
    };
    ApiPlansController.prototype.changeFilter = function (statusFilter) {
        this.selectedStatus = statusFilter;
        this.dndEnabled = (statusFilter === 'published');
        if (_.includes(this.selectedStatus, statusFilter)) {
            _.pull(this.selectedStatus, statusFilter);
        }
        else {
            this.selectedStatus.push(statusFilter);
        }
        this.$state.transitionTo(this.$state.current, _.merge(this.$state.params, {
            state: statusFilter
        }));
        this.applyFilters();
    };
    ApiPlansController.prototype.applyFilters = function () {
        this.countPlansByStatus();
        var that = this;
        this.filteredPlans = _.filter(this.plans, function (plan) {
            return _.includes(that.selectedStatus, plan.status);
        });
    };
    ApiPlansController.prototype.resetResourceFiltering = function () {
        this.$scope.resourceFiltering = {
            whitelist: []
        };
    };
    ApiPlansController.prototype.resetPlan = function () {
        this.$scope.plan = { characteristics: [] };
        this.resetRateLimit();
        this.resetQuota();
        this.resetResourceFiltering();
        if (this.$scope.formPlan) {
            this.$scope.formPlan.$setPristine();
            this.$scope.formPlan.$setUntouched();
        }
    };
    ApiPlansController.prototype.resetRateLimit = function () {
        delete this.$scope.rateLimit;
    };
    ApiPlansController.prototype.resetQuota = function () {
        delete this.$scope.quota;
    };
    ApiPlansController.prototype.addPlan = function () {
        this.resetPlan();
        this.$mdSidenav('plan-edit').toggle();
        this.$mdSidenav('live-preview').toggle();
    };
    ApiPlansController.prototype.save = function () {
        var that = this;
        this.$scope.plan.paths = {
            '/': []
        };
        // set resource filtering whitelist
        _.remove(this.$scope.resourceFiltering.whitelist, function (whitelistItem) {
            return !whitelistItem.pattern;
        });
        if (this.$scope.resourceFiltering.whitelist.length) {
            that.$scope.plan.paths['/'].push({
                'methods': that.$scope.methods,
                'resource-filtering': {
                    'whitelist': this.$scope.resourceFiltering.whitelist
                }
            });
        }
        // set rate limit policy
        if (this.$scope.rateLimit && this.$scope.rateLimit.limit) {
            this.$scope.plan.paths['/'].push({
                'methods': this.$scope.methods,
                'rate-limit': {
                    'rate': this.$scope.rateLimit
                }
            });
        }
        // set quota policy
        if (this.$scope.quota && this.$scope.quota.limit) {
            this.$scope.plan.paths['/'].push({
                'methods': this.$scope.methods,
                'quota': {
                    'quota': this.$scope.quota,
                    'addHeaders': true
                }
            });
        }
        that.ApiService.savePlan(that.$stateParams.apiId, that.$scope.plan).then(function (response) {
            var createMode = !that.planAlreadyCreated(response.data.id);
            that.$scope.$parent.apiCtrl.checkAPISynchronization({ id: that.$stateParams.apiId });
            that.NotificationService.show('The plan ' + that.$scope.plan.name + ' has been saved with success');
            that.ApiService.getApiPlans(that.$stateParams.apiId).then(function (response) {
                that.plans = response.data;
                that.$mdSidenav('plan-edit').toggle();
                that.$mdSidenav('live-preview').toggle();
                if (createMode) {
                    that.changeFilter('staging');
                }
                else {
                    that.applyFilters();
                }
            });
        });
    };
    ApiPlansController.prototype.planAlreadyCreated = function (planId) {
        return _.some(this.plans, function (plan) {
            return plan.id === planId;
        });
    };
    ApiPlansController.prototype.close = function (plan, ev) {
        var _this = this;
        this.ApiService.getPlanSubscriptions(this.$stateParams.apiId, plan.id).then(function (response) {
            _this.$mdDialog.show({
                controller: 'DialogClosePlanController',
                template: require('./closePlan.dialog.html'),
                parent: angular.element(document.body),
                targetEvent: ev,
                clickOutsideToClose: true,
                apiId: _this.$stateParams.apiId,
                plan: plan,
                subscriptions: response.data.length
            }).then(function (plan) {
                if (plan) {
                    _this.list();
                }
            }, function () {
                // You cancelled the dialog
            });
        });
    };
    ApiPlansController.prototype.publish = function (plan, ev) {
        var _this = this;
        this.$mdDialog.show({
            controller: 'DialogPublishPlanController',
            template: require('./publishPlan.dialog.html'),
            parent: angular.element(document.body),
            targetEvent: ev,
            clickOutsideToClose: true,
            apiId: this.$stateParams.apiId,
            plan: plan
        }).then(function (plan) {
            if (plan) {
                _this.list();
            }
        }, function () {
            // You cancelled the dialog
        });
    };
    ApiPlansController.prototype.countPlansByStatus = function () {
        this.countByStatus = _.countBy(this.plans, 'status');
    };
    return ApiPlansController;
}());
exports.default = ApiPlansController;
