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
// eslint-disable-next-line
/* global setInterval:false, clearInterval:false, screen:false */
import angular from 'angular';
import { StateDeclaration, TransitionService } from '@uirouter/angularjs';

import EnvironmentService from '../services/environment.service';
import PortalConfigService from '../services/portalConfig.service';
import UserService from '../services/user.service';
import ConsoleSettingsService from '../services/consoleSettings.service';

function runBlock(
  $rootScope,
  $window,
  $http,
  $mdSidenav,
  $transitions: TransitionService,
  $state,
  $timeout,
  UserService: UserService,
  Constants,
  PermissionStrategies,
  ReCaptchaService,
  ApiService,
  EnvironmentService: EnvironmentService,
  PortalConfigService: PortalConfigService,
  ConsoleSettingsService: ConsoleSettingsService,
) {
  'ngInject';

  $transitions.onStart(
    {
      to: (state) =>
        state.name !== 'login' &&
        state.name !== 'registration' &&
        state.name !== 'confirm' &&
        state.name !== 'newsletter' &&
        state.name !== 'resetPassword',
    },
    (trans) => {
      if (!UserService.isAuthenticated()) {
        return trans.router.stateService.target('login');
      }
      if (
        UserService.isAuthenticated() &&
        UserService.currentUser.firstLogin &&
        Constants.org.settings.newsletter.enabled &&
        !$window.localStorage.getItem('newsletterProposed')
      ) {
        return trans.router.stateService.target('newsletter');
      } else {
        if (!Constants.org.settings.newsletter.enabled) {
          $rootScope.$broadcast('graviteeUserRefresh', { user: UserService.currentUser, refresh: true });
        }
      }
    },
  );

  $transitions.onStart(
    {
      to: (state) => state.name === 'management.apis.create',
    },
    async (trans) => {
      const { definitionVersion } = trans.params();
      if (definitionVersion === '2.0.0') {
        return;
      }

      const settings = await ConsoleSettingsService.getConsole();
      const hasPathBasedApiCreation = settings?.data?.management?.pathBasedApiCreation?.enabled;
      if (!hasPathBasedApiCreation) {
        return trans.router.stateService.target('management.apis.new');
      }
    },
  );

  $transitions.onBefore(
    {
      to: (state, transition) => {
        return (
          state.name.startsWith('management') &&
          !EnvironmentService.isSameEnvironment(Constants.org.currentEnv, transition.params().environmentId)
        );
      },
    },
    async (trans) => {
      const params = Object.assign({}, trans.params());
      const stateService = trans.router.stateService;
      const toState = trans.to();

      let shouldReload = true;
      if (!params.environmentId) {
        shouldReload = false;
        params.environmentId = EnvironmentService.getFirstHridOrElseId(Constants.org.currentEnv);
      }
      const targetEnv = EnvironmentService.getEnvironmentFromHridOrId(Constants.org.environments, params.environmentId);
      if (targetEnv) {
        Constants.org.currentEnv = targetEnv;
        return UserService.refreshEnvironmentPermissions().then(() => {
          return stateService.target(toState, params, { reload: shouldReload });
        });
      } else {
        params.environmentId = EnvironmentService.getFirstHridOrElseId(Constants.org.currentEnv);
        return stateService.target(toState, params, { reload: shouldReload });
      }
    },
  );

  $transitions.onBefore(
    {},
    (trans) => {
      const params = Object.assign({}, trans.params());
      const stateService = trans.router.stateService;
      const toState = trans.to() as StateDeclaration & { apiDefinition: any };

      if (UserService.currentUser && UserService.currentUser.id && !Constants.org.currentEnv) {
        return EnvironmentService.list()
          .then((response) => {
            Constants.org.environments = response.data;

            const lastEnvironmentLoaded = 'gv-last-environment-loaded';
            const lastEnv = $window.localStorage.getItem(lastEnvironmentLoaded);
            if (lastEnv !== null) {
              const foundEnv = Constants.org.environments.find((env) => env.id === lastEnv);
              if (foundEnv) {
                Constants.org.currentEnv = Constants.org.environments.find((env) => env.id === lastEnv);
              } else {
                Constants.org.currentEnv = Constants.org.environments[0];
                $window.localStorage.removeItem(lastEnvironmentLoaded);
              }
            } else {
              Constants.org.currentEnv = Constants.org.environments[0];
            }

            return response.data;
          })
          .then((environments) => {
            if (environments && environments.length >= 1) {
              return PortalConfigService.get();
            }
          })
          .then((response) => {
            if (response) {
              Constants.env.settings = response.data;
            }
          });
      } else if (toState.apiDefinition) {
        return ApiService.get(params.apiId).then((response) => {
          if (response.data.gravitee != null && toState.apiDefinition.version !== response.data.gravitee) {
            return stateService.target(toState.apiDefinition.redirect, params);
          }
          return {};
        });
      }
    },
    { priority: 10 },
  );

  $transitions.onFinish({}, (trans) => {
    // Hide recaptcha badge by default (let each component decide whether it should display the recaptcha badge or not).
    ReCaptchaService.hideBadge();

    const fromState = trans.from();
    const toState = trans.to();

    const notEligibleForUserCreation =
      !Constants.org.settings.management.userCreation.enabled && (fromState.name === 'registration' || fromState === 'confirm');

    if (notEligibleForUserCreation) {
      return trans.router.stateService.target('login');
    } else if (
      toState.data &&
      toState.data.perms &&
      toState.data.perms.only &&
      !UserService.isUserHasPermissions(toState.data.perms.only)
    ) {
      return trans.router.stateService.target(UserService.isAuthenticated() ? 'management.home' : 'login');
    }
  });

  // eslint-disable-next-line angular/on-watch
  $rootScope.$on('graviteeLogout', (event, params) => {
    $state.go('login', { redirectUri: params.redirectUri });
  });

  // eslint-disable-next-line angular/on-watch
  $rootScope.$watch(
    () => {
      return $http.pendingRequests.length > 0;
    },
    (hasPendingRequests) => {
      $rootScope.isLoading = hasPendingRequests;
    },
  );

  $rootScope.displayLoader = true;

  // force displayLoader value to change on a new digest cycle
  $timeout(() => {
    $rootScope.displayLoader = false;
  });

  $rootScope.PermissionStrategies = PermissionStrategies;

  // set status of the window
  $rootScope.isWindowFocused = true;
  $window.onblur = () => {
    $rootScope.isWindowFocused = false;
  };
  $window.onfocus = () => {
    $rootScope.isWindowFocused = true;
  };

  // Add or remove `bootstrap, mat` class wrapper to disable it on some routes with `useAngularMaterial` flag
  $transitions.onFinish({}, function (trans) {
    const toState = trans.to();

    const useAngularMaterial = toState.data && toState.data.useAngularMaterial;

    const htmlClass = {
      bootstrap: !useAngularMaterial,
      mat: useAngularMaterial,
      'mat-typography': useAngularMaterial,
      'mat-app-background': useAngularMaterial,
    };

    Object.entries(htmlClass).forEach(([key, toAdd]) => {
      if (toAdd) {
        angular.element(document.querySelector('html')).addClass(key);
        angular.element(document.querySelector('body')).addClass(key);
        return;
      }
      angular.element(document.querySelector('html')).removeClass(key);
      angular.element(document.querySelector('body')).removeClass(key);
    });

    return true;
  });
}

export default runBlock;
