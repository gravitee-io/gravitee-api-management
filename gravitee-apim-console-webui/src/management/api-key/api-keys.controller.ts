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

import { StateParams, StateService } from '@uirouter/core';
import { Observable } from 'rxjs';

import NotificationService from '../../services/notification.service';
import ApplicationService from '../../services/application.service';

class ApiKeysController {
  private subscription: any;
  private keys: any[];
  private application: any;
  private listEvent: Observable<void>;
  private backStateParams: StateParams;

  constructor(
    private $mdDialog: angular.material.IDialogService,
    private NotificationService: NotificationService,
    private ApplicationService: ApplicationService,
    private $state: StateService,
  ) {
    'ngInject';
    this.backStateParams = $state.params;
  }

  $onInit() {
    this.listApiKeys();
    if (this.listEvent) {
      this.listEvent.subscribe(() => this.listApiKeys());
    }
  }

  listApiKeys(): void {
    this.ApplicationService.listApiKeys(this.application, this.subscription).then((response) => {
      this.keys = response.data;
    });
  }

  renewApiKey(): void {
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to renew your API Key?',
          msg: 'Your previous API Key will be no longer valid in 2 hours!',
          confirmButton: 'Renew',
        },
      })
      .then((response) => {
        if (response) {
          this.ApplicationService.renewApiKey(this.application, this.subscription).then(() => {
            this.NotificationService.show('A new API Key has been generated');
            this.listApiKeys();
          });
        }
      });
  }

  isValid(key): boolean {
    return !key.revoked && !key.expired;
  }

  areKeysEditable(): boolean {
    // shared api keys are editable if this is not a subscription related screen
    if (this.isSharedApiKey()) {
      return !this.subscription;
    }
    // other keys are editable if subscription is accepted
    return this.subscription && this.subscription.status === 'ACCEPTED';
  }

  isSharedApiKey(): boolean {
    return this.application.api_key_mode === 'SHARED';
  }

  revokeApiKey(apiKey): void {
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: "Are you sure you want to revoke API Key '" + apiKey.key + "'?",
          confirmButton: 'Revoke',
        },
      })
      .then((response) => {
        if (response) {
          this.ApplicationService.revokeApiKey(this.application, this.subscription, apiKey.id).then(() => {
            this.NotificationService.show('API Key ' + apiKey.key + ' has been revoked!');
            this.listApiKeys();
          });
        }
      });
  }

  onCopyApiKeySuccess(e): void {
    this.NotificationService.show('API Key has been copied to clipboard');
    e.clearSelection();
  }

  getTitle(): string {
    return this.isSharedApiKey() ? 'Shared API Key' : 'API Keys';
  }
}

export default ApiKeysController;
