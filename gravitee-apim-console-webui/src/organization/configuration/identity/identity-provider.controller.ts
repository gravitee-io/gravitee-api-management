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

import { IdentityProvider } from '../../../entities/identity-provider/identityProvider';
import IdentityProviderService from '../../../services/identityProvider.service';
import NotificationService from '../../../services/notification.service';

interface IIdentityProviderScope extends ng.IScope {
  formIdentityProvider: any;
}

class IdentityProviderController {
  private identityProvider: IdentityProvider;
  private initialIdentityProvider: IdentityProvider;
  private tokenExchangeEndpoint: string;
  private updateMode: boolean;

  constructor(
    private $scope: IIdentityProviderScope,
    private $state: StateService,
    private $mdEditDialog,
    private Constants,
    private $mdDialog: angular.material.IDialogService,
    private NotificationService: NotificationService,
    private IdentityProviderService: IdentityProviderService,
  ) {
    'ngInject';
  }

  $onInit() {
    this.updateMode = this.identityProvider !== undefined && this.identityProvider.id !== undefined;
    if (!this.updateMode) {
      // Initialize the identity provider
      this.identityProvider = {
        enabled: true,
        configuration: { scopes: [] },
        type: this.$state.params.type,
        emailRequired: true,
        syncMappings: false,
      };
      // Default user mapping configuration for OIDC or Gravitee.io AM providers

      if (this.identityProvider.type === 'OIDC' || this.identityProvider.type === 'GRAVITEEIO_AM') {
        this.identityProvider.userProfileMapping = {
          id: 'sub',
          firstname: 'given_name',
          lastname: 'family_name',
          email: 'email',
          picture: 'picture',
        };
      }
    } else {
      this.tokenExchangeEndpoint = this.Constants.org.baseURL + '/auth/oauth2/' + this.identityProvider.id;
    }
    this.initialIdentityProvider = _.cloneDeep(this.identityProvider);
  }

  addGroupMapping() {
    this.identityProvider.groupMappings.push({});
    this.$scope.formIdentityProvider.$setDirty();
  }

  addRoleMapping() {
    this.identityProvider.roleMappings.push({});
    this.$scope.formIdentityProvider.$setDirty();
  }

  deleteGroupMapping(idx: number) {
    this.identityProvider.groupMappings.splice(idx, 1);
    this.$scope.formIdentityProvider.$setDirty();
  }

  deleteRoleMapping(idx: number) {
    this.identityProvider.roleMappings.splice(idx, 1);
    this.$scope.formIdentityProvider.$setDirty();
  }

  reset() {
    this.identityProvider = _.cloneDeep(this.initialIdentityProvider);
    this.$scope.formIdentityProvider.$setPristine();
  }

  update() {
    if (!this.updateMode) {
      this.IdentityProviderService.create(this.identityProvider).then((response: any) => {
        this.NotificationService.show('Identity provider ' + this.identityProvider.name + ' has been created');
        this.$state.go('organization.settings.ajs-identityproviders.identityprovider', { id: response.data.id }, { reload: true });
      });
    } else {
      this.IdentityProviderService.update(this.identityProvider).then((response) => {
        this.NotificationService.show('Identity provider ' + this.identityProvider.name + ' has been updated');
        this.identityProvider = response;
        this.$scope.formIdentityProvider.$setPristine();
      });
    }
  }
}

export default IdentityProviderController;
