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
class SubscriptionsController {
  constructor($window, $mdDialog, $scope, $state, ApiService, NotificationService, resolvedApi, resolvedSubscriptions) {
    'ngInject';
    this.$window = $window;
    this.$mdDialog = $mdDialog;
    this.$state = $state;
    this.ApiService = ApiService;
    this.NotificationService = NotificationService;

    $scope.data = [];

    this.api = resolvedApi.data;
    this.subscriptions = resolvedSubscriptions.data;
    $scope.showRevokedKeys = false;

    this.statusFilters = ['accepted', 'pending', 'rejected', 'closed'];
    this.selectedStatus = ['accepted', 'pending'];
    this.applyFilters();
  }

  changeFilter(statusFilter) {
    if (_.includes(this.selectedStatus, statusFilter)) {
      _.pull(this.selectedStatus, statusFilter);
    } else {
      this.selectedStatus.push(statusFilter);
    }
    this.applyFilters();
  }

  applyFilters() {
    var that = this;
    this.subscriptionsByApplication = _.groupBy(_.filter(this.subscriptions, function (subscription) {
      return _.includes(that.selectedStatus, subscription.status);
    }), 'application.name');
  }

  hasKeysDefined() {
    return this.subscriptions !== null && Object.keys(this.subscriptions).length > 0;
  }

  revoke(subscription, apiKey) {
    var alert = this.$mdDialog.confirm({
      title: 'Warning',
      content: 'Are you sure you want to revoke API Key \'' + apiKey + '\'?',
      ok: 'Revoke',
      cancel: 'Cancel'
    });

    var _this = this;

    this.$mdDialog
      .show(alert)
      .then(function () {
        _this.ApiService.revokeApiKey(_this.api.id, subscription.id, apiKey).then(() => {
          _this.NotificationService.show('API Key ' + apiKey + ' has been revoked !');

          _this.refresh();
        });
      });
  }

  onClipboardSuccess(e) {
    this.NotificationService.show('API Key has been copied to clipboard');
    e.clearSelection();
  }

  showExpirationModal(apiKey) {
    var _this = this;

    this.$mdDialog.show({
      controller: 'DialogApiKeyExpirationController',
      controllerAs: 'dialogApiKeyExpirationController',
      templateUrl: 'app/api/admin/subscriptions/apikey.expiration.dialog.html',
      clickOutsideToClose: true
    }).then(function (expirationDate) {
      apiKey.expire_at = expirationDate;

      _this.ApiService.updateApiKey(_this.api.id, apiKey).then(() => {
        _this.NotificationService.show('An expiration date has been settled for API Key');
      });
    });
  }

  refresh() {
    var that = this;
    this.ApiService.getSubscriptions(this.api.id).then(function (response) {
      that.subscriptions = response.data;
      that.applyFilters();
    });
  }

  process(subscription, accepted) {
    var that = this;
    if (accepted) {
      this.$mdDialog.show({
        controller: 'DialogSubscriptionAcceptController',
        controllerAs: 'dialogSubscriptionAcceptController',
        templateUrl: 'app/api/admin/subscriptions/subscription.accept.dialog.html',
        clickOutsideToClose: true
      }).then(function (processSubscription) {
        processSubscription.accepted = accepted;
        that.doProcessSubscription(subscription, processSubscription);
      });
    } else {
      this.$mdDialog.show({
        controller: 'DialogSubscriptionRejectController',
        controllerAs: 'dialogSubscriptionRejectController',
        templateUrl: 'app/api/admin/subscriptions/subscription.reject.dialog.html',
        clickOutsideToClose: true
      }).then(function (reason) {
        that.doProcessSubscription(subscription, {accepted: accepted, reason: reason});
      });
    }
  }

  doProcessSubscription(subscription, processSubscription) {
    var that = this;
    this.ApiService.processPlanSubscription(this.api.id, subscription.plan.id, subscription.id, processSubscription).then(function () {
      that.refresh();
      that.NotificationService.show('The subscription has been ' + (processSubscription.accepted ? 'accepted' : 'rejected') + ' with success');
    });
  }

  toggleSubscription(scope, subscription) {
    scope.toggle();
    if (!subscription.apiKeys) {
      this.listApiKeys(subscription);
    }
  }

  listApiKeys(subscription) {
    this.ApiService.listApiKeys(this.api.id, subscription.id).then(response => {
      subscription.apiKeys = response.data;
    });
  }

  generateAPIKey(apiId, subscription) {
    var alert = this.$mdDialog.confirm({
      title: 'Warning',
      content: 'Are you sure you want to renew your API Key ? Your previous API Key will be no longer valid in 1 hour !',
      ok: 'Renew',
      cancel: 'Cancel'
    });

    var _this = this;

    this.$mdDialog
      .show(alert)
      .then(function () {
        _this.ApiService.renewApiKey(apiId, subscription.id).then(() => {
          _this.NotificationService.show('A new API Key has been generated');
          _this.listApiKeys(subscription);
        });
      });
  }
}

export default SubscriptionsController;
