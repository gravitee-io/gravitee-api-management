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

function runBlock($rootScope, $window, $http, $mdSidenav, $transitions, $state,
                  $timeout, UserService: UserService, Constants, PermissionStrategies) {
  'ngInject';

  $transitions.onStart({ to: (state) => state.name !== 'login' && state.name !== 'registration' && state.name !== 'confirm'}, function(trans) {
    let forceLogin = Constants.authentication.forceLogin.enabled;

    if (forceLogin && !UserService.isAuthenticated()) {
      return trans.router.stateService.target('login');
    }
  });

  $transitions.onFinish({}, function (trans) {
    let fromState = trans.from();
    let toState = trans.to();

    let notEligibleForDevMode = Constants.portal.devMode.enabled && toState.data && !toState.data.devMode && !UserService.currentUser.isAdmin();
    let notEligibleForUserCreation = !Constants.portal.userCreation.enabled && (fromState.name === 'registration' || fromState === 'confirm');

    if (notEligibleForDevMode || notEligibleForUserCreation) {
      return trans.router.stateService.target('login');
    } else if (toState.data && toState.data.perms && toState.data.perms.only && !UserService.isUserHasPermissions(toState.data.perms.only)) {
      return trans.router.stateService.target(UserService.isAuthenticated() ? 'management.apis.list' : 'login');
    }
  });

  $rootScope.$on('graviteeLogout', function () {
    $window.location.href = $window.location.pathname;
  });

  $rootScope.$watch(function () {
    return $http.pendingRequests.length > 0;
  }, function (hasPendingRequests) {
    $rootScope.isLoading = hasPendingRequests;
  });

  $rootScope.displayLoader = true;

  // force displayLoader value to change on a new digest cycle
  $timeout(function () {
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
