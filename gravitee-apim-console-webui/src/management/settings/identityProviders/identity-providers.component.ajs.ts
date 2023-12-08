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
import { Router } from '@angular/router';
import { filter } from 'lodash';

import { IdentityProviderActivation } from '../../../entities/identity-provider';
import EnvironmentService from '../../../services/environment.service';
import IdentityProviderService from '../../../services/identityProvider.service';
import NotificationService from '../../../services/notification.service';
import PortalSettingsService from '../../../services/portalSettings.service';
import UserService from '../../../services/user.service';

const IdentityProvidersComponentAjs: ng.IComponentOptions = {
  bindings: {
    identityProviders: '<',
    identities: '<',
    settings: '<',
    activatedRoute: '<',
    ngRouter: '<',
  },
  template: require('html-loader!./identity-providers.html'),
  controller: [
    'IdentityProviderService',
    'EnvironmentService',
    'PortalSettingsService',
    'NotificationService',
    'UserService',
    'Constants',
    'ngRouter',
    function (
      IdentityProviderService: IdentityProviderService,
      EnvironmentService: EnvironmentService,
      PortalSettingsService: PortalSettingsService,
      NotificationService: NotificationService,
      UserService: UserService,
      Constants,
      ngRouter: Router,
    ) {
      this.ngRouter = ngRouter;
      this.activatedIdps = {};

      this.identityProviders = [];
      this.identities = [];

      this.providedConfigurationMessage = 'Configuration provided by the system';

      this.$onInit = () => {
        this.envId = this.activatedRoute.snapshot.params.envId;
        this.canUpdatePortalSettings = UserService.isUserHasPermissions([
          'environment-settings-c',
          'environment-settings-u',
          'environment-settings-d',
        ]);

        Promise.all([
          IdentityProviderService.list(),
          EnvironmentService.listEnvironmentIdentities(Constants.org.currentEnv.id),
          PortalSettingsService.get(),
        ]).then(([identityProviderResponse, environmentIdentitiesResponse, portalSettingsResponse]) => {
          this.identityProviders = identityProviderResponse || [];
          this.identities = environmentIdentitiesResponse.data || [];
          this.settings = portalSettingsResponse.data || {};

          this.identities.forEach((ipa: IdentityProviderActivation) => (this.activatedIdps[ipa.identityProvider] = true));
          this.hasEnabledIdp = this.identityProviders.filter((idp) => idp.enabled).length > 0;
        });
      };

      this.hasActivatedIdp = () => {
        // activated IDP must also be enabled
        const enabledIdpIds = this.identityProviders.filter((idp) => idp.enabled === true).map((idp) => idp.id);
        for (const idpId of enabledIdpIds) {
          if (this.activatedIdps[idpId]) {
            return true;
          }
        }
        return false;
      };

      this.saveForceLogin = () => {
        PortalSettingsService.save(this.settings).then((response) => {
          NotificationService.show('Authentication is now ' + (this.settings.authentication.forceLogin.enabled ? 'mandatory' : 'optional'));
          this.settings = response.data;
          this.$onInit();
        });
      };

      this.saveShowLoginForm = () => {
        PortalSettingsService.save(this.settings).then((response) => {
          NotificationService.show('Login form is now ' + (this.settings.authentication.localLogin.enabled ? 'enabled' : 'disabled'));
          this.settings = response.data;
          this.$onInit();
        });
      };

      this.toggleActivatedIdp = (identityProviderId: string) => {
        const updatedIPA: Partial<IdentityProviderActivation>[] = filter(
          Object.keys(this.activatedIdps),
          (idpId) => this.activatedIdps[idpId] === true,
        ).map((idpId) => ({ identityProvider: idpId }));

        EnvironmentService.updateEnvironmentIdentities(this.envId, updatedIPA).then(this._updateHandler(identityProviderId));
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
        return PortalSettingsService.isReadonly(this.settings, property);
      };
    },
  ],
};

export default IdentityProvidersComponentAjs;
