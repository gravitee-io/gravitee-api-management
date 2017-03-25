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
import * as _ from 'lodash';
import * as angular from 'angular';

import ApplicationService from '../../../../services/applications.service';
import NotificationService from '../../../../services/notification.service';
import ApiService from "../../../../services/api.service";

class ApplicationSubscriptionsController {
  private application: any;
  private subscriptions: any;
  private statusFilters: string[];
  private selectedStatus: string[];
  private apiNameById: any;
  private subscriptionsByApi: any;
  private showRevokedKeys: boolean;
  private data: any[];

  constructor(
    private ApplicationService: ApplicationService,
    private NotificationService: NotificationService,
    private $mdDialog: angular.material.IDialogService,
    private ApiService: ApiService
  ) {
    'ngInject';

    this.showRevokedKeys = false;
    this.data = [];
    this.statusFilters = ['accepted', 'pending', 'rejected', 'closed'];
    this.selectedStatus = ['accepted', 'pending'];
    this.apiNameById = {};
  }

  $onInit() {
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
    let that = this;
    this.subscriptionsByApi = _.groupBy(_.filter(that.subscriptions, (subscription: any) => _.includes(that.selectedStatus, subscription.status)), function (sub) {
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
        _this.ApplicationService.renewApiKey(applicationId, subscription.id).then(() => {
          _this.NotificationService.show('A new API Key has been generated');
          _this.listApiKeys(subscription);
        });
      }
    });
  }

  revoke(subscription, apiKey) {
    var _this = this;
    this.$mdDialog.show({
      controller: 'DialogConfirmController',
      controllerAs: 'ctrl',
      template: require('../../../components/dialog/confirmWarning.dialog.html'),
      clickOutsideToClose: true,
      locals: {
        title: 'Are you sure you want to revoke API Key \'' + apiKey + '\'?',
        confirmButton: 'Revoke'
      }
    }).then(function (response) {
      if (response) {
        _this.ApplicationService.revokeApiKey(_this.application.id, subscription.id, apiKey).then(() => {
          _this.NotificationService.show('API Key ' + apiKey + ' has been revoked !');
          _this.listApiKeys(subscription);
        });
      }
    });
  }

  onClipboardSuccess(e) {
    this.NotificationService.show('API Key has been copied to clipboard');
    e.clearSelection();
  }

  /*
  showSubscribeApiModal(ev) {
    this.$mdDialog.show({
      controller: 'DialogSubscribeApiController',
      templateUrl: 'application/dialog/subscribeApi.dialog.html',
      parent: angular.element(document.body),
      targetEvent: ev,
      clickOutsideToClose: true,
      locals: {
        application: this.application,
        subscriptions: this.subscriptions
      }
    }).then(application =>{
      if (application) {
        // TODO : check it ! There was no ApiService...
        this.ApiService.getSubscriptions(application.id);
      }
    });
  }
  */

  showExpirationModal(apiId, apiKey) {
    this.$mdDialog.show({
      controller: 'DialogApiKeyExpirationController',
      controllerAs: 'dialogApiKeyExpirationController',
      template: require('../../../api/admin/subscriptions/apikey.expiration.dialog.html'),
      clickOutsideToClose: true
    }).then(expirationDate =>{
      apiKey.expire_at = expirationDate;

      this.ApiService.updateApiKey(apiId, apiKey).then(() => {
        this.NotificationService.show('An expiration date has been settled for API Key');
      });
    });
  }
}

export default ApplicationSubscriptionsController;
