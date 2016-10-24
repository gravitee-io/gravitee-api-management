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
class ApplicationSubscriptionsController {
  constructor(resolvedApplication, resolvedSubscriptions, ApplicationService, NotificationService, $mdDialog, $scope,
  ApiService) {
    'ngInject';
    this.application = resolvedApplication.data;
    this.ApplicationService = ApplicationService;
    this.ApiService = ApiService;
    this.NotificationService = NotificationService;
    this.$mdDialog = $mdDialog;

    $scope.data = [];
    this.subscriptions = resolvedSubscriptions.data;
    $scope.showRevokedKeys = false;

    this.statusFilters = ['accepted', 'pending', 'rejected', 'closed'];
    this.selectedStatus = ['accepted', 'pending'];
    this.apiNameById = {};
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
    this.subscriptionsByApi = _.groupBy(_.filter(this.subscriptions, function (subscription) {
      return _.includes(that.selectedStatus, subscription.status);
    }), function (sub) {
      that.apiNameById[sub.plan.apis[0].id] = sub.plan.apis[0].name;
      return sub.plan.apis[0].id;
    });
  }

  toggleSubscription(scope, subscription) {
    scope.toggle();
    if (!subscription.apiKeys) {
      this.listApiKeys(subscription);
    }
  }

  listApiKeys(subscription) {
    this.ApplicationService.listApiKeys(this.application.id, subscription.id).then(response => {
      subscription.apiKeys = response.data;
    });
  }

  hasKeysDefined() {
    return this.subscriptions !== null && Object.keys(this.subscriptions).length > 0;
  }

  generateAPIKey(applicationId, subscription) {
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
        _this.ApplicationService.renewApiKey(applicationId, subscription.id).then(() => {
          _this.NotificationService.show('A new API Key has been generated');
          _this.listApiKeys(subscription);
        });
      });
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
        _this.ApplicationService.revokeApiKey(_this.application.id, subscription.id, apiKey).then(() => {
          _this.NotificationService.show('API Key ' + apiKey + ' has been revoked !');
          _this.listApiKeys(subscription);
        });
      });
  }

  onClipboardSuccess(e) {
    this.NotificationService.show('API Key has been copied to clipboard');
    e.clearSelection();
  }

  showSubscribeApiModal(ev) {
    var that = this;
    this.$mdDialog.show({
      controller: 'DialogSubscribeApiController',
      templateUrl: 'app/application/dialog/subscribeApi.dialog.html',
      parent: angular.element(document.body),
      targetEvent: ev,
      clickOutsideToClose: true,
      application: that.application,
      subscriptions: that.subscriptions
    }).then(function (application) {
      if (application) {
        that.getSubscriptions(application.id);
      }
    });
  }

  showExpirationModal(apiId, apiKey) {
    var _this = this;

    this.$mdDialog.show({
      controller: 'DialogApiKeyExpirationController',
      controllerAs: 'dialogApiKeyExpirationController',
      templateUrl: 'app/api/admin/subscriptions/apikey.expiration.dialog.html',
      clickOutsideToClose: true
    }).then(function (expirationDate) {
      apiKey.expire_at = expirationDate;

      _this.ApiService.updateApiKey(apiId, apiKey).then(() => {
        _this.NotificationService.show('An expiration date has been settled for API Key');
      });
    });
  }
}

export default ApplicationSubscriptionsController;
