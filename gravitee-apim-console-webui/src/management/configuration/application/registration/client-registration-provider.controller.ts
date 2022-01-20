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
import angular from 'angular';
import * as _ from 'lodash';

import { ClientRegistrationProvider } from '../../../../entities/clientRegistrationProvider';
import ClientRegistrationProviderService from '../../../../services/clientRegistrationProvider.service';
import NotificationService from '../../../../services/notification.service';

interface IClientRegistrationProviderScope extends ng.IScope {
  formClientRegistrationProvider: any;
}

class ClientRegistrationProviderController {
  private clientRegistrationProvider: ClientRegistrationProvider;
  private initialClientRegistrationProvider: ClientRegistrationProvider;
  private updateMode: boolean;
  private initialAccessTokenTypes: any[];
  private renewClientSecretMethods: string[];

  constructor(
    private $scope: IClientRegistrationProviderScope,
    private $state: StateService,
    private $mdEditDialog,
    private Constants,
    private $mdDialog: angular.material.IDialogService,
    private NotificationService: NotificationService,
    private ClientRegistrationProviderService: ClientRegistrationProviderService,
  ) {
    'ngInject';
  }

  $onInit() {
    this.renewClientSecretMethods = ['POST', 'PATCH', 'PUT'];

    this.initialAccessTokenTypes = [];
    this.initialAccessTokenTypes.push({
      name: 'Client Credentials',
      value: 'CLIENT_CREDENTIALS',
    });
    this.initialAccessTokenTypes.push({
      name: 'Initial Access Token',
      value: 'INITIAL_ACCESS_TOKEN',
    });

    this.updateMode = this.clientRegistrationProvider !== undefined && this.clientRegistrationProvider.id !== undefined;
    if (!this.updateMode) {
      // Initialize the client registration provider
      this.clientRegistrationProvider = new ClientRegistrationProvider();
      this.clientRegistrationProvider.scopes = [];
    }

    this.initialClientRegistrationProvider = _.cloneDeep(this.clientRegistrationProvider);
  }

  reset() {
    this.clientRegistrationProvider = _.cloneDeep(this.initialClientRegistrationProvider);
    this.$scope.formClientRegistrationProvider.$setPristine();
  }

  update() {
    if (!this.updateMode) {
      this.ClientRegistrationProviderService.create(this.clientRegistrationProvider).then((response: any) => {
        this.NotificationService.show('Client registration provider ' + this.clientRegistrationProvider.name + ' has been created');
        this.$state.go(
          'management.settings.clientregistrationproviders.clientregistrationprovider',
          { id: response.data.id },
          { reload: true },
        );
      });
    } else {
      this.ClientRegistrationProviderService.update(this.clientRegistrationProvider).then((response) => {
        this.NotificationService.show('Client registration provider ' + this.clientRegistrationProvider.name + ' has been updated');
        this.clientRegistrationProvider = response;
        this.$scope.formClientRegistrationProvider.$setPristine();
      });
    }
  }
}

export default ClientRegistrationProviderController;
