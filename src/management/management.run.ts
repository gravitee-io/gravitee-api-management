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
/* global setInterval:false, clearInterval:false, screen:false */
import UserService from '../services/user.service';
import EnvironmentService from '../services/environment.service';
import PortalConfigService from '../services/portalConfig.service';

function runBlock($rootScope, $window, $http, $mdSidenav, $transitions, $state,
                  $timeout, UserService: UserService, Constants, PermissionStrategies, ReCaptchaService, ApiService, EnvironmentService: EnvironmentService, PortalConfigService: PortalConfigService) {
  'ngInject';

  $transitions.onStart({
    to: (state) => state.name !== 'login' && state.name !== 'registration'
      && state.name !== 'confirm' && state.name !== 'confirmProfile' && state.name !== 'resetPassword'
  }, (trans) => {
    if (!UserService.isAuthenticated()) {
      return trans.router.stateService.target('login');
    }
    if (UserService.isAuthenticated() && UserService.currentUser.firstLogin && !$window.localStorage.getItem('profileConfirmed')) {
      return trans.router.stateService.target('confirmProfile');
    }
  });

  $transitions.onBefore({}, trans => {
    const params = Object.assign({}, trans.params());
    const stateService = trans.router.stateService;
    let toState = trans.to();

    if (toState.name.startsWith('management')) {

      if (!EnvironmentService.isSameEnvironment(Constants.org.currentEnv, params.environmentId)) {
        if (!params.environmentId) {
          params.environmentId = EnvironmentService.getFirstHridOrElseId(Constants.org.currentEnv);
        }
        let targetEnv = EnvironmentService.getEnvironmentFromHridOrId(Constants.org.environments, params.environmentId);
        if (targetEnv) {
          Constants.org.currentEnv = targetEnv;
          return UserService.refreshEnvironmentPermissions().then(() => {
            return stateService.target(toState, params, { reload: true });
          });
        } else {
          params.environmentId = EnvironmentService.getFirstHridOrElseId(Constants.org.currentEnv);
          return stateService.target(toState, params, { reload: true });
        }
      }
    }
  });

  $transitions.onBefore({}, trans => {

    const params = Object.assign({}, trans.params());
    const stateService = trans.router.stateService;
    let toState = trans.to();

    if (UserService.currentUser && UserService.currentUser.id && !Constants.org.currentEnv) {
      return EnvironmentService.list()
        .then(response => {
          Constants.org.environments = response.data;

          const lastEnvironmentLoaded = 'gv-last-environment-loaded';
          const lastEnv = $window.localStorage.getItem(lastEnvironmentLoaded);
          if (lastEnv !== null) {
            const foundEnv = Constants.org.environments.find(env => env.id === lastEnv);
            if (foundEnv) {
              Constants.org.currentEnv = Constants.org.environments.find(env => env.id === lastEnv);
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
        .then(response => {
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
  }, { priority: 10 });

  $transitions.onFinish({}, function(trans) {

    // Hide recaptcha badge by default (let each component decide whether it should display the recaptcha badge or not).
    ReCaptchaService.hideBadge();

    let fromState = trans.from();
    let toState = trans.to();

    let notEligibleForUserCreation = !Constants.org.settings.management.userCreation.enabled && (fromState.name === 'registration' || fromState === 'confirm');

    if (notEligibleForUserCreation) {
      return trans.router.stateService.target('login');
    } else if (toState.data && toState.data.perms && toState.data.perms.only && !UserService.isUserHasPermissions(toState.data.perms.only)) {
      return trans.router.stateService.target(UserService.isAuthenticated() ? 'management.home' : 'login');
    }
  });

  $rootScope.$on('graviteeLogout', function(event, params) {
    $state.go('login', {redirectUri: params.redirectUri});
  });

  $rootScope.$watch(function () {
    return $http.pendingRequests.length > 0;
  }, function(hasPendingRequests) {
    $rootScope.isLoading = hasPendingRequests;
  });

  $rootScope.displayLoader = true;

  // force displayLoader value to change on a new digest cycle
  $timeout(function() {
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

}

export default runBlock;
