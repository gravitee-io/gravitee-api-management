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
import { IdentityProvider } from "../../../entities/identityProvider";
import IdentityProviderService from "../../../services/identityProvider.service";
import NotificationService from "../../../services/notification.service";
import PortalConfigService from "../../../services/portalConfig.service";

const IdentityProvidersComponent: ng.IComponentOptions = {
  bindings: {
    identityProviders: '<'
  },
  template: require('./identity-providers.html'),
  controller: function(
    $mdDialog: angular.material.IDialogService,
    IdentityProviderService: IdentityProviderService,
    PortalConfigService: PortalConfigService,
    NotificationService: NotificationService,
    $state: StateService,
    Constants
  ) {
    'ngInject';

    this.Constants = Constants;

    this.availableProviders = [
      {'name': 'Gravitee.io AM', 'icon': 'perm_identity', 'type': 'graviteeio_am'},
      {'name': 'Google', 'icon': 'google-plus', 'type': 'google'},
      {'name': 'GitHub', 'icon': 'github-circle', 'type': 'github'},
      {'name': 'OpenID Connect', 'icon': 'perm_identity', 'type': 'oidc'}
    ];

    this.select = (provider: IdentityProvider) => {
      $state.go('management.settings.identityproviders.identityprovider', {id: provider.id});
    };

    this.create = (type) => {
      $state.go('management.settings.identityproviders.new', {type: type});
    };

    this.delete = (provider: IdentityProvider) => {
      let that = this;
      $mdDialog.show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to delete this identity provider ?',
          msg: '',
          confirmButton: 'Delete'
        }
      }).then(function (response) {
        if (response) {
          IdentityProviderService.delete(provider).then(response => {
            NotificationService.show("Identity provider '" + provider.name + "' has been deleted");
            $state.go('management.settings.identityproviders.list', {}, {reload: true});
          });
        }
      });
    };

    this.saveForceLogin = () => {
      PortalConfigService.save({
        authentication: {
          forceLogin: {
            enabled: this.Constants.authentication.forceLogin.enabled
          }
        }
      }).then( response => {
        NotificationService.show("Authentication is now " + (this.Constants.authentication.forceLogin.enabled?"mandatory":"optional") );
      });
    };

    this.saveShowLoginForm = () => {
      PortalConfigService.save({
        authentication: {
          localLogin: {
            enabled: this.Constants.authentication.localLogin.enabled
          }
        }
      }).then( response => {
        NotificationService.show("Login form is now " + (this.Constants.authentication.localLogin.enabled?"enabled":"disabled"));
      });
    };
  }
};

export default IdentityProvidersComponent;
