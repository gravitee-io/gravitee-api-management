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
var SubscriptionsController = (function () {
    function SubscriptionsController($mdDialog, $scope, ApiService, NotificationService, resolvedApi, resolvedSubscriptions, ApplicationService) {
        'ngInject';
        this.$mdDialog = $mdDialog;
        this.$scope = $scope;
        this.ApiService = ApiService;
        this.NotificationService = NotificationService;
        this.resolvedApi = resolvedApi;
        this.resolvedSubscriptions = resolvedSubscriptions;
        this.ApplicationService = ApplicationService;
        $scope.data = [];
        this.api = resolvedApi.data;
        this.subscriptions = resolvedSubscriptions.data;
        $scope.showRevokedKeys = false;
        this.statusFilters = ['accepted', 'pending', 'rejected', 'closed'];
        this.selectedStatus = ['accepted', 'pending'];
        this.applyFilters();
    }
    SubscriptionsController.prototype.changeFilter = function (statusFilter) {
        if (_.includes(this.selectedStatus, statusFilter)) {
            _.pull(this.selectedStatus, statusFilter);
        }
        else {
            this.selectedStatus.push(statusFilter);
        }
        this.applyFilters();
    };
    SubscriptionsController.prototype.applyFilters = function () {
        var that = this;
        this.subscriptionsByApplication =
            _(this.subscriptions)
                .filter(function (subscription) { return _.includes(that.selectedStatus, subscription.status); })
                .groupBy('application.id')
                .orderBy(function (subscriptions) { return subscriptions[0].application.name; })
                .value();
    };
    SubscriptionsController.prototype.hasKeysDefined = function () {
        return this.subscriptions !== null && Object.keys(this.subscriptions).length > 0;
    };
    SubscriptionsController.prototype.revoke = function (subscription, apiKey) {
        var that = this;
        this.$mdDialog.show({
            controller: 'DialogConfirmController',
            controllerAs: 'ctrl',
            template: require('../../../components/dialog/confirmWarning.dialog.html'),
            clickOutsideToClose: true,
            locals: {
                title: 'Are you sure you want to revoke API Key \'' + apiKey + '\' ?',
                confirmButton: 'Revoke'
            }
        }).then(function (response) {
            if (response) {
                that.ApiService.revokeApiKey(that.api.id, subscription.id, apiKey).then(function () {
                    that.NotificationService.show('API Key ' + apiKey + ' has been revoked !');
                    that.refresh();
                });
            }
        });
    };
    SubscriptionsController.prototype.onClipboardSuccess = function (e) {
        this.NotificationService.show('API Key has been copied to clipboard');
        e.clearSelection();
    };
    SubscriptionsController.prototype.showExpirationModal = function (apiKey) {
        var _this = this;
        this.$mdDialog.show({
            controller: 'DialogApiKeyExpirationController',
            controllerAs: 'dialogApiKeyExpirationController',
            template: require('./apikey.expiration.dialog.html'),
            clickOutsideToClose: true
        }).then(function (expirationDate) {
            apiKey.expire_at = expirationDate;
            _this.ApiService.updateApiKey(_this.api.id, apiKey).then(function () {
                _this.NotificationService.show('An expiration date has been settled for API Key');
            });
        });
    };
    SubscriptionsController.prototype.refresh = function () {
        var that = this;
        this.ApiService.getSubscriptions(this.api.id).then(function (response) {
            that.subscriptions = response.data;
            that.applyFilters();
        });
    };
    SubscriptionsController.prototype.process = function (subscription, accepted) {
        var that = this;
        if (accepted) {
            this.$mdDialog.show({
                controller: 'DialogSubscriptionAcceptController',
                controllerAs: 'dialogSubscriptionAcceptController',
                template: require('./subscription.accept.dialog.html'),
                clickOutsideToClose: true
            }).then(function (processSubscription) {
                processSubscription.accepted = accepted;
                that.doProcessSubscription(subscription, processSubscription);
            });
        }
        else {
            this.$mdDialog.show({
                controller: 'DialogSubscriptionRejectController',
                controllerAs: 'dialogSubscriptionRejectController',
                template: require('./subscription.reject.dialog.html'),
                clickOutsideToClose: true
            }).then(function (reason) {
                that.doProcessSubscription(subscription, { accepted: accepted, reason: reason });
            });
        }
    };
    SubscriptionsController.prototype.doProcessSubscription = function (subscription, processSubscription) {
        var that = this;
        this.ApiService.processPlanSubscription(this.api.id, subscription.plan.id, subscription.id, processSubscription).then(function () {
            that.refresh();
            that.NotificationService.show('The subscription has been ' + (processSubscription.accepted ? 'accepted' : 'rejected') + ' with success');
        });
    };
    SubscriptionsController.prototype.toggleSubscription = function (scope, subscription) {
        scope.toggle();
        if (!subscription.apiKeys) {
            this.listApiKeys(subscription);
        }
    };
    SubscriptionsController.prototype.listApiKeys = function (subscription) {
        this.ApiService.listApiKeys(this.api.id, subscription.id).then(function (response) {
            subscription.apiKeys = response.data;
        });
    };
    SubscriptionsController.prototype.generateAPIKey = function (apiId, subscription) {
        var _this = this;
        this.$mdDialog.show({
            controller: 'DialogConfirmController',
            controllerAs: 'ctrl',
            template: require('../../../components/dialog/confirmWarning.dialog.html'),
            clickOutsideToClose: true,
            locals: {
                title: 'Are you sure you want to renew your API Key ?',
                msg: 'Your previous API Key will be no longer valid in 1 hour !',
                confirmButton: 'Renew'
            }
        }).then(function (response) {
            if (response) {
                _this.ApiService.renewApiKey(apiId, subscription.id).then(function () {
                    _this.NotificationService.show('A new API Key has been generated');
                    _this.listApiKeys(subscription);
                });
            }
        });
    };
    SubscriptionsController.prototype.showAddSubscriptionModal = function () {
        var _this = this;
        this.ApiService.getPublishedApiPlans(this.api.id).then(function (response) {
            // Allow only subscribable plan
            var plans = _.filter(response.data, function (plan) { return plan.security !== 'key_less'; });
            _this.$mdDialog.show({
                controller: 'DialogSubscriptionCreateController',
                controllerAs: 'dialogSubscriptionCreateController',
                template: require('./subscription.create.dialog.html'),
                clickOutsideToClose: true,
                locals: {
                    plans: plans,
                }
            }).then(function (data) {
                if (data && data.applicationId && data.planId) {
                    _this.ApplicationService.subscribe(data.applicationId, data.planId).then(function (response) {
                        var newSub = response.data;
                        _this.NotificationService.show('A new subscription has been created.');
                        _this.subscriptions.push(newSub);
                        if (newSub.status === "pending") {
                            _this.doProcessSubscription(newSub, { accepted: true });
                        }
                        else {
                            _this.refresh();
                        }
                    });
                }
            });
        });
    };
    return SubscriptionsController;
}());
exports.default = SubscriptionsController;
