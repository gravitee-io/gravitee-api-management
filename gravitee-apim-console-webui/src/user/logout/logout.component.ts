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
import { IComponentOptions, IOnInit, IScope, IWindowService } from 'angular';
import { StateService } from '@uirouter/core';

import { Constants } from '../../entities/Constants';
import UserService from '../../services/user.service';

class LogoutComponentController implements IOnInit {
  constructor(
    private readonly UserService: UserService,
    private readonly $state: StateService,
    private readonly $rootScope: IScope,
    private readonly $window: IWindowService,
    private readonly Constants: Constants,
  ) {
    'ngInject';
  }

  $onInit(): void {
    delete this.Constants.org.currentEnv;
    delete this.Constants.org.environments;

    this.UserService.logout().then(() => {
      this.$state.go('login');
      this.$rootScope.$broadcast('graviteeUserRefresh', {});
      this.$rootScope.$broadcast('graviteeUserCancelScheduledServices');
      const userLogoutEndpoint = this.$window.localStorage.getItem('user-logout-url');
      this.$window.localStorage.removeItem('user-logout-url');
      this.reinitToDefaultOrganization();
      if (userLogoutEndpoint != null) {
        const redirectUri = window.location.origin + (window.location.pathname === '/' ? '' : window.location.pathname);
        if (userLogoutEndpoint.endsWith('target_url=')) {
          // If we use a Gravitee AM IDP, the logoutEndpoint will end with `target_url=` (See AMIdentityProviderEntity.java)
          // We must fill this query param so older versions of AM still work.
          this.$window.location.href =
            userLogoutEndpoint + encodeURIComponent(redirectUri) + '&post_logout_redirect_uri=' + encodeURIComponent(redirectUri);
        } else if (userLogoutEndpoint.endsWith('post_logout_redirect_uri=')) {
          // Otherwise we use an OIDC IDP, and the logout endpoint may already contain the `post_logout_redirect_uri`
          this.$window.location.href = userLogoutEndpoint + encodeURIComponent(redirectUri);
        } else {
          const separator = userLogoutEndpoint.indexOf('?') > -1 ? '&' : '?';
          this.$window.location.href = userLogoutEndpoint + separator + 'post_logout_redirect_uri=' + encodeURIComponent(redirectUri);
        }
      }
    });
  }

  private reinitToDefaultOrganization() {
    this.$window.localStorage.setItem('gv-last-organization-loaded', 'DEFAULT');
    if (this.Constants.baseURL.endsWith('/')) {
      this.Constants.baseURL = this.Constants.baseURL.slice(0, -1);
    }

    const orgEnvIndex = this.Constants.baseURL.indexOf('/organizations');
    if (orgEnvIndex >= 0) {
      this.Constants.baseURL = this.Constants.baseURL.substr(0, orgEnvIndex);
    }

    this.Constants.org.baseURL = `${this.Constants.baseURL}/organizations/DEFAULT`;
    this.Constants.env.baseURL = `${this.Constants.org.baseURL}/environments/{:envId}`;
  }
}

export const LogoutComponent: IComponentOptions = {
  controller: LogoutComponentController,
};
