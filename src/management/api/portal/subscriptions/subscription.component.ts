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

import ApiService from '../../../../services/api.service';
import NotificationService from '../../../../services/notification.service';
import { StateService } from '@uirouter/core';
import moment = require('moment');

const ApiSubscriptionComponent: ng.IComponentOptions = {
  bindings: {
    api: '<',
    subscription: '<'
  },
  template: require('./subscription.html'),
  controller: class {

    private subscription: any;
    private keys: any[];
    private api: any;
    private plans: any[];
    private backStateParams: any;

    constructor(
      private $rootScope: ng.IRootScopeService,
      private $mdDialog: angular.material.IDialogService,
      private NotificationService: NotificationService,
      private ApiService: ApiService,
      private $state: StateService
    ) {
      'ngInject';

      this.backStateParams = {
        application: $state.params.application,
        plan: $state.params.plan,
        status: $state.params.status,
        page: $state.params.page,
        size: $state.params.size,
        api_key: $state.params.api_key
      };
    }

    $onInit() {
      this.listApiKeys();
      this.getApiPlans();
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
      let msg = '<code>'
        + this.subscription.application.name
        + '</code> will not be able to consume <code>'
        + this.api.name
        + '</code> anymore.';
      if (this.subscription.plan.security === 'api_key') {
        msg += '<br/>All Api-keys associated to this subscription will be closed and could not be used.';
      }

      this.$mdDialog.show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to close this subscription to ' + this.subscription.plan.name + '?',
          msg: msg,
          confirmButton: 'Close the subscription'
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

    pause() {
      let msg = 'The application will not be able to consume this API anymore.';
      if (this.subscription.plan.security === 'api_key') {
        msg += '<br/>All Api-keys associated to this subscription will be paused and could not be used.';
      }

      this.$mdDialog.show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to pause this subscription?',
          msg: msg,
          confirmButton: 'Pause'
        }
      }).then( (response) => {
        if (response) {
          this.ApiService.pauseSubscription(this.api.id, this.subscription.id).then((response) => {
            this.NotificationService.show('The subscription has been paused');
            this.subscription = response.data;
            this.listApiKeys();
          });
        }
      });
    }

    resume() {
      let msg = 'The application will again be able to consume your API.';

      this.$mdDialog.show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to resume this subscription?',
          msg: msg,
          confirmButton: 'Resume'
        }
      }).then( (response) => {
        if (response) {
          this.ApiService.resumeSubscription(this.api.id, this.subscription.id).then((response) => {
            this.NotificationService.show('The subscription has been resumed');
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
      });
    }

    process(processSubscription) {
      this.ApiService.processSubscription(this.api.id, this.subscription.id, processSubscription)
        .then( (response) => {
          this.NotificationService.show('The subscription has been ' + (processSubscription.accepted ? 'accepted' : 'rejected'));
          this.subscription = response.data;
          this.$rootScope.$broadcast('graviteeUserTaskRefresh');
          if (processSubscription.accepted) {
            this.listApiKeys();
          }
        });
    }

    transferSubscription(transferSubscription) {
      this.ApiService.transferSubscription(this.api.id, this.subscription.id, transferSubscription)
        .then( (response) => {
          this.NotificationService.show('The subscription has been successfully transferred');
          this.subscription = response.data;
          this.getApiPlans();
        });
    }

    renewApiKey() {
      this.$mdDialog.show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../../components/dialog/confirmWarning.dialog.html'),
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
        template: require('../../../../components/dialog/confirmWarning.dialog.html'),
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
        clickOutsideToClose: true,
        locals: {
          maxEndDate: this.subscription.ending_at
        }
      }).then(expirationDate => {
        apiKey.expire_at = expirationDate;

        this.ApiService.updateApiKey(this.api.id, apiKey).then(() => {
          this.NotificationService.show('An expiration date has been defined for API Key.');
          this.listApiKeys();
        });
      });
    }

    onCopyApiKeySuccess(e) {
      this.NotificationService.show('API Key has been copied to clipboard');
      e.clearSelection();
    }

    transfer() {
      this.$mdDialog.show({
        controller: 'DialogSubscriptionTransferController',
        controllerAs: '$ctrl',
        template: require('./subscription.transfer.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          plans: this.plans
        }
      }).then(plan => {
        this.subscription.plan = plan;
        this.transferSubscription(this.subscription);
      });
    }

    changeEndDate() {
      this.$mdDialog.show({
        controller: 'DialogSubscriptionChangeEndDateController',
        controllerAs: '$ctrl',
        template: require('./subscription.change.end.date.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          subscription: this.subscription

        }
      }).then(endDate => {
        this.subscription.ending_at = endDate;
        this.ApiService.updateSubscription(this.api.id, this.subscription).then(() => {
          this.NotificationService.show('The end date has been modified.');
          this.listApiKeys();
        });
      });

    }

    isValid(key) {
      return !key.revoked && !key.expired ;
    }

    private getApiPlans() {
      this.ApiService.getApiPlans(this.api.id, 'published', this.subscription.plan.security).then(response => {
        this.plans = _.filter(response.data, plan => plan.id !== this.subscription.plan.id);
      });
    }
  }
};

export default ApiSubscriptionComponent;
