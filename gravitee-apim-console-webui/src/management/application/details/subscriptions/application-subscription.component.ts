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
import { StateService } from '@uirouter/core';

import ApplicationService from '../../../../services/application.service';
import NotificationService from '../../../../services/notification.service';
import { PlanSecurityType } from '../../../../entities/plan/plan';

const ApplicationSubscriptionComponent: ng.IComponentOptions = {
  bindings: {
    application: '<',
    subscription: '<',
  },
  template: require('./application-subscription.html'),
  controller: class {
    private subscription: any;
    private keys: any[];
    private application: any;
    private backStateParams: any;

    constructor(
      private $mdDialog: angular.material.IDialogService,
      private NotificationService: NotificationService,
      private ApplicationService: ApplicationService,
      private $state: StateService,
    ) {
      'ngInject';

      this.backStateParams = {
        api: $state.params.api,
        status: $state.params.status,
        page: $state.params.page,
        size: $state.params.size,
        api_key: $state.params.api_key,
      };
    }

    $onInit() {
      this.listApiKeys();
    }

    listApiKeys() {
      if (this.subscription.plan.security === PlanSecurityType.API_KEY) {
        // Retrieve api_keys for current current subscription
        this.ApplicationService.listApiKeys(this.application.id, this.subscription.id).then((response) => {
          this.keys = response.data;
        });
      }
    }

    renewApiKey() {
      this.$mdDialog
        .show({
          controller: 'DialogConfirmController',
          controllerAs: 'ctrl',
          template: require('../../../../components/dialog/confirmWarning.dialog.html'),
          clickOutsideToClose: true,
          locals: {
            title: 'Are you sure you want to renew your API Key?',
            msg: 'Your previous API Key will be no longer valid in 2 hours!',
            confirmButton: 'Renew',
          },
        })
        .then((response) => {
          if (response) {
            this.ApplicationService.renewApiKey(this.application.id, this.subscription.id).then(() => {
              this.NotificationService.show('A new API Key has been generated');
              this.listApiKeys();
            });
          }
        });
    }

    revokeApiKey(apiKey) {
      this.$mdDialog
        .show({
          controller: 'DialogConfirmController',
          controllerAs: 'ctrl',
          template: require('../../../../components/dialog/confirmWarning.dialog.html'),
          clickOutsideToClose: true,
          locals: {
            title: "Are you sure you want to revoke API Key '" + apiKey.key + "'?",
            confirmButton: 'Revoke',
          },
        })
        .then((response) => {
          if (response) {
            this.ApplicationService.revokeApiKey(this.application.id, this.subscription.id, apiKey.id).then(() => {
              this.NotificationService.show('API Key ' + apiKey.key + ' has been revoked!');
              this.listApiKeys();
            });
          }
        });
    }

    onCopyApiKeySuccess(e) {
      this.NotificationService.show('API Key has been copied to clipboard');
      e.clearSelection();
    }

    close() {
      let msg = 'The application will not be able to consume this API anymore.';
      if (this.subscription.plan.security === PlanSecurityType.API_KEY) {
        msg += '<br/>All Api-keys associated to this subscription will be closed and could not be used.';
      }

      this.$mdDialog
        .show({
          controller: 'DialogConfirmController',
          controllerAs: 'ctrl',
          template: require('../../../../components/dialog/confirmWarning.dialog.html'),
          clickOutsideToClose: true,
          locals: {
            title: 'Are you sure you want to close this subscription?',
            msg: msg,
            confirmButton: 'Close',
          },
        })
        .then((response) => {
          if (response) {
            this.ApplicationService.closeSubscription(this.application.id, this.subscription.id).then((response) => {
              this.NotificationService.show('The subscription has been closed');
              this.subscription = response.data;
              this.listApiKeys();
            });
          }
        });
    }
  },
};

export default ApplicationSubscriptionComponent;
