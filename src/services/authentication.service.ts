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

import { AuthProvider, SatellizerConfig } from "satellizer";
import { IdentityProvider } from "../entities/identityProvider";
import * as _ from "lodash";
import UserService from "./user.service";
import RouterService from "./router.service";
import { IScope } from "angular";
import { StateService } from '@uirouter/core';

class AuthenticationService {

  constructor(
    private $rootScope: IScope,
    private Constants,
    private $window,
    private $state: StateService,
    private $auth: AuthProvider,
    private UserService: UserService,
    private RouterService: RouterService,
    private SatellizerConfig: SatellizerConfig) {
    'ngInject';
  }

  authenticate(provider: IdentityProvider) {
    provider.type = (provider.type === 'oidc') ? 'oauth2' : provider.type;

    let satellizerProvider = this.SatellizerConfig.providers[provider.type];
    if (!satellizerProvider) {
      satellizerProvider = _.merge(provider, {
        oauthType: '2.0',
        requiredUrlParams: ['scope'],
        scopeDelimiter: ' ',
        scope: provider.scopes
      });
    } else {
      _.merge(satellizerProvider, provider);
    }

    this.SatellizerConfig.providers[provider.type] = _.merge(satellizerProvider, {
      url: this.Constants.baseURL + 'auth/oauth2/' + provider.id,
      redirectUri: window.location.origin + (window.location.pathname == '/' ? '' : window.location.pathname),
    });

    this.$auth.authenticate(provider.type)
      .then( () => {
        this.UserService.current().then( (user) => {
          if (provider.userLogoutEndpoint) {
            this.$window.localStorage.setItem("user-logout-url", provider.userLogoutEndpoint);
          }
          this.$rootScope.$broadcast('graviteeUserRefresh', {'user' : user});

          let route = this.RouterService.getLastRoute();
          if (route.from && route.from.name !== '' && route.from.name !== 'logout' && route.from.name !== 'confirm') {
            this.$state.go(route.from.name, route.fromParams);
          } else {
            this.$state.go('portal.home');
          }
        });
      })
      .catch( () => {});
  }
}

export default AuthenticationService;
