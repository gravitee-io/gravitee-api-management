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
import _ = require('lodash');
import ApiService from "../../../services/api.service";
import NotificationService from "../../../services/notification.service";

const ApiSubscriptionComponent: ng.IComponentOptions = {
  bindings: {
    api: '<',
    subscription: '<'
  },
  template: require('./subscription.html'),
  controller: class {

    private subscription:any;
    private keys:any[];
    private api: any;

    constructor(
      private $rootScope: ng.IRootScopeService,
      private $mdDialog: angular.material.IDialogService,
      private NotificationService: NotificationService,
      private ApiService: ApiService
    ) {
      'ngInject';
    }

    $onInit() {
      this.listApiKeys();
    }

    listApiKeys() {
      if (this.subscription.plan.security === 'api_key') {
        // Retrieve api_keys for current current subscription
        this.ApiService.listApiKeys(this.api.id, this.subscription.id).then((response) => {
          this.keys = response.data;
        });
      }
    }

    close() {
      let msg = 'The application will not be able to consume API anymore.';
      if (this.subscription.plan.security === 'api_key') {
        msg += '<br/>All Api-keys associated to this subscription will be closed and could no be used.'
      }

      this.$mdDialog.show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to close this subscription?',
          msg: msg,
          confirmButton: 'Close'
        }
      }).then( (response) => {
        if (response) {
          this.ApiService.closeSubscription(this.api.id, this.subscription.id).then((response) => {
            this.NotificationService.show('The subscription has been closed');
            this.subscription = response.data;
            this.listApiKeys();
          });
        }
      });
    }

    reject() {
      this.$mdDialog.show({
        controller: 'DialogSubscriptionRejectController',
        controllerAs: 'dialogSubscriptionRejectController',
        template: require('./subscription.reject.dialog.html'),
        clickOutsideToClose: true
      }).then( (reason) => {
        this.process({accepted: false, reason: reason});
      });
    }

    accept() {
      this.$mdDialog.show({
        controller: 'DialogSubscriptionAcceptController',
        controllerAs: 'dialogSubscriptionAcceptController',
        template: require('./subscription.accept.dialog.html'),
        clickOutsideToClose: true
      }).then( (subscription) => {
        subscription.accepted = true;
        this.process(subscription);
        this.listApiKeys();
      });
    }

    process(processSubscription) {
      this.ApiService.processSubscription(this.api.id, this.subscription.id, processSubscription)
        .then( (response) => {
          this.NotificationService.show('The subscription has been ' + (processSubscription.accepted ? 'accepted' : 'rejected'));
          this.subscription = response.data;
          this.$rootScope.$broadcast("graviteeUserTaskRefresh");
      });
    }

    renewApiKey() {
      this.$mdDialog.show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to renew your API Key?',
          msg: 'Your previous API Key will be no longer valid in 2 hours!',
          confirmButton: 'Renew'
        }
      }).then( (response) => {
        if (response) {
          this.ApiService.renewApiKey(this.api.id, this.subscription.id).then(() => {
            this.NotificationService.show('A new API Key has been generated');
            this.listApiKeys();
          });
        }
      });
    }

    revokeApiKey(apiKey) {
      this.$mdDialog.show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to revoke API Key \'' + apiKey + '\' ?',
          confirmButton: 'Revoke'
        }
      }).then( (response) => {
        if (response) {
          this.ApiService.revokeApiKey(this.api.id, this.subscription.id, apiKey).then(() => {
            this.NotificationService.show('API Key ' + apiKey + ' has been revoked!');
            this.listApiKeys();
          });
        }
      });
    }

    expireApiKey(apiKey) {
      this.$mdDialog.show({
        controller: 'DialogApiKeyExpirationController',
        controllerAs: 'dialogApiKeyExpirationController',
        template: require('./apikey.expiration.dialog.html'),
        clickOutsideToClose: true
      }).then(expirationDate => {
        apiKey.expire_at = expirationDate;

        this.ApiService.updateApiKey(this.api.id, apiKey).then(() => {
          this.NotificationService.show('An expiration date has been defined for API Key.');
        });
      });
    }

    onCopyApiKeySuccess(e) {
      this.NotificationService.show('API Key has been copied to clipboard');
      e.clearSelection();
    }
  }
};

export default ApiSubscriptionComponent;
