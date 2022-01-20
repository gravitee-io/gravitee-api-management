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
import { IScope } from 'angular';
import * as _ from 'lodash';

import { IdentityProvider, IdentityProviderActivation } from '../../entities/identity-provider';
import ConsoleSettingsService from '../../services/consoleSettings.service';
import EnvironmentService from '../../services/environment.service';
import IdentityProviderService from '../../services/identityProvider.service';
import NotificationService from '../../services/notification.service';
import OrganizationService from '../../services/organization.service';
import PortalSettingsService from '../../services/portalSettings.service';

const IdentityProvidersComponent: ng.IComponentOptions = {
  bindings: {
    identityProviders: '<',
    identities: '<',
    target: '<',
    targetId: '<',
    settings: '<',
  },
  template: require('./identity-providers.html'),
  controller: function (
    $mdDialog: angular.material.IDialogService,
    IdentityProviderService: IdentityProviderService,
    EnvironmentService: EnvironmentService,
    OrganizationService: OrganizationService,
    ConsoleSettingsService: ConsoleSettingsService,
    PortalSettingsService: PortalSettingsService,
    NotificationService: NotificationService,
    $state: StateService,
    Constants,
    $rootScope: IScope,
  ) {
    'ngInject';
    this.$rootScope = $rootScope;
    this.activatedIdps = {};

    this.providedConfigurationMessage = 'Configuration provided by the system';

    this.$onInit = () => {
      this.identities.forEach((ipa: IdentityProviderActivation) => (this.activatedIdps[ipa.identityProvider] = true));
      this.hasEnabledIdp = this.identityProviders.filter((idp) => idp.enabled).length > 0;
    };

    this.availableProviders = [
      { name: 'Gravitee.io AM', icon: 'perm_identity', type: 'GRAVITEEIO_AM' },
      { name: 'Google', icon: 'google-plus', type: 'GOOGLE' },
      { name: 'GitHub', icon: 'github-circle', type: 'GITHUB' },
      { name: 'OpenID Connect', icon: 'perm_identity', type: 'OIDC' },
    ];

    this.create = (type) => {
      $state.go('organization.settings.ajs-identityproviders.new', { type: type });
    };

    this.delete = (provider: IdentityProvider) => {
      $mdDialog
        .show({
          controller: 'DialogConfirmController',
          controllerAs: 'ctrl',
          template: require('../dialog/confirmWarning.dialog.html'),
          clickOutsideToClose: true,
          locals: {
            title: 'Are you sure you want to delete this identity provider?',
            msg: '',
            confirmButton: 'Delete',
          },
        })
        .then((response) => {
          if (response) {
            IdentityProviderService.delete(provider).then(() => {
              NotificationService.show("Identity provider '" + provider.name + "' has been deleted");
              $state.go('organization.settings.ajs-identityproviders.list', {}, { reload: true });
            });
          }
        });
    };

    this.hasActivatedIdp = () => {
      if (this.target === 'ENVIRONMENT') {
        // activated IDP must also be enabled
        const enabledIdpIds = this.identityProviders.filter((idp) => idp.enabled === true).map((idp) => idp.id);
        for (const idpId of enabledIdpIds) {
          if (this.activatedIdps[idpId]) {
            return true;
          }
        }
        return false;
      } else {
        return Object.keys(this.activatedIdps).length > 0;
      }
    };

    this.saveForceLogin = () => {
      PortalSettingsService.save(this.settings).then((response) => {
        NotificationService.show('Authentication is now ' + (this.settings.authentication.forceLogin.enabled ? 'mandatory' : 'optional'));
        this.settings = response.data;
        $state.reload();
      });
    };

    this.saveShowLoginForm = () => {
      if (this.target === 'ENVIRONMENT') {
        PortalSettingsService.save(this.settings).then((response) => {
          NotificationService.show('Login form is now ' + (this.settings.authentication.localLogin.enabled ? 'enabled' : 'disabled'));
          this.settings = response.data;
          $state.reload();
        });
      } else {
        ConsoleSettingsService.save(this.settings).then((response) => {
          NotificationService.show('Login form is now ' + (this.settings.authentication.localLogin.enabled ? 'enabled' : 'disabled'));
          this.consoleSettings = response.data;
          $state.reload();
        });
      }
    };

    this.toggleActivatedIdp = (identityProviderId: string) => {
      const updatedIPA: Partial<IdentityProviderActivation>[] = _.filter(
        Object.keys(this.activatedIdps),
        (idpId) => this.activatedIdps[idpId] === true,
      ).map((idpId) => ({ identityProvider: idpId }));

      if (this.target === 'ENVIRONMENT') {
        EnvironmentService.updateEnvironmentIdentities(this.targetId, updatedIPA).then(this._updateHandler(identityProviderId));
      } else {
        OrganizationService.updateOrganizationIdentities(updatedIPA).then(this._updateHandler(identityProviderId));
      }
    };

    this._updateHandler = (identityProviderId) => {
      return () => {
        NotificationService.show(identityProviderId + ' is now ' + (this.activatedIdps[identityProviderId] ? 'enabled' : 'disabled'));
        if (!this.activatedIdps[identityProviderId]) {
          delete this.activatedIdps[identityProviderId];
          this.updateLocalLoginState();
        }
      };
    };

    this.updateLocalLoginState = () => {
      if (!this.hasActivatedIdp() && !this.settings.authentication.localLogin.enabled) {
        this.settings.authentication.localLogin.enabled = true;
        this.saveShowLoginForm();
      }
    };

    this.isReadonlySetting = (property: string): boolean => {
      if (this.target === 'ENVIRONMENT') {
        return PortalSettingsService.isReadonly(this.settings, property);
      } else {
        return ConsoleSettingsService.isReadonly(this.settings, property);
      }
    };
  },
};

export default IdentityProvidersComponent;
