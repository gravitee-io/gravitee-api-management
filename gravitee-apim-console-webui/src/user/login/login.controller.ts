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
import { GioPendoService } from '@gravitee/ui-analytics';
import { StateParams, StateService } from '@uirouter/core';
import { IScope } from 'angular';
import * as _ from 'lodash';

import { IdentityProvider } from '../../entities/identity-provider/identityProvider';
import { User } from '../../entities/user/user';
import AuthenticationService from '../../services/authentication.service';
import ReCaptchaService from '../../services/reCaptcha.service';
import RouterService from '../../services/router.service';
import UserService from '../../services/user.service';

class LoginController {
  user: any = {};
  userCreationEnabled: boolean;
  localLoginDisabled: boolean;

  constructor(
    private AuthenticationService: AuthenticationService,
    private UserService: UserService,
    private $state: StateService,
    private Constants,
    private $rootScope: IScope,
    private RouterService: RouterService,
    private identityProviders,
    private $window,
    private $stateParams: StateParams,
    private $scope,
    private ReCaptchaService: ReCaptchaService,
    private ngGioPendoService: GioPendoService,
  ) {
    'ngInject';
    this.userCreationEnabled = Constants.org.settings.management.userCreation.enabled;
    this.localLoginDisabled = !Constants.org.settings.authentication.localLogin.enabled || false;
    this.$state = $state;
    this.$rootScope = $rootScope;
    this.$scope = $scope;
    $scope.canBeDisabled = false;
  }

  $onInit() {
    this.ReCaptchaService.displayBadge();
    document.addEventListener('click', this._toDisabledMode);
  }

  $onDestroy() {
    document.removeEventListener('click', this._toDisabledMode);
  }

  authenticate(identityProvider: string) {
    const nonce = this.AuthenticationService.nonce(32);

    const redirectUri = this.getRedirectUri();

    this.$window.localStorage[nonce] = JSON.stringify({ redirectUri });

    const provider = _.find(this.identityProviders, { id: identityProvider }) as IdentityProvider;
    this.AuthenticationService.authenticate(provider, nonce);
  }

  _toDisabledMode = () => {
    this.$scope.canBeDisabled = true;
    this.$scope.$apply();
    document.removeEventListener('click', this._toDisabledMode);
  };

  login() {
    this.ReCaptchaService.execute('login').then(() =>
      this.UserService.login(this.user)
        .then(() => {
          this.UserService.current().then((user) => {
            this.loginSuccess(user);
          });
        })
        .catch(() => {
          this.user.username = '';
          this.user.password = '';
        }),
    );
  }

  loginSuccess(user: User) {
    this.ngGioPendoService.initialize(
      {
        id: `${user.sourceId}`,
        email: `${user.email}`,
      },
      {
        id: `${user.sourceId}`,
        userSource: user.source,
      },
    );
    this.$rootScope.$broadcast('graviteeUserRefresh', { user: user });
    const redirectUri = this.getRedirectUri();
    if (redirectUri && !redirectUri.includes('/newsletter')) {
      this.$window.location.href = redirectUri;
    } else {
      const route = this.RouterService.getLastRoute();
      if (
        route.from &&
        route.from.name !== '' &&
        route.from.name !== 'logout' &&
        route.from.name !== 'confirm' &&
        route.from.name !== 'resetPassword'
      ) {
        this.$state.go(route.from.name, route.fromParams);
      } else {
        this.$state.go('management');
      }
    }
  }

  getProviderBackGroundColor(provider: any) {
    if (provider.color) {
      return provider.color;
    }
    if (provider.type === 'OIDC') {
      return 'black';
    }
    if (provider.type === 'GRAVITEEIO_AM') {
      return '#86c3d0';
    }
    return '';
  }

  getProviderColor(provider: any) {
    if (provider.type === 'GRAVITEEIO_AM') {
      return '#383E3F';
    }
    return 'white';
  }

  getProviderStyle(provider: any) {
    return {
      'background-color': this.getProviderBackGroundColor(provider),
      color: this.getProviderColor(provider),
    };
  }

  getRedirectUri() {
    let redirectUri = '';
    if (this.$state.params.redirectUri) {
      if (this.$state.params.redirectUri.toLowerCase().startsWith('http')) {
        redirectUri = this.$state.params.redirectUri;
        if (this.$state.params['#']) {
          redirectUri += '#' + this.$state.params['#'];
        }
      } else {
        redirectUri = '#!' + this.$state.params.redirectUri;
      }
    }
    return redirectUri;
  }
}

export default LoginController;
