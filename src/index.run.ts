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

import UserService from "./services/user.service";

function runBlock($rootScope, $window, $http, $mdSidenav, $transitions, SidenavService, swaggerModules, PageSwaggerConfigurationService,
                  PageSwaggerHttpClientService, $timeout, UserService: UserService, Constants) {
  'ngInject';

  $transitions.onEnter({ }, function(trans) {
    let fromState = trans.from();
    let toState = trans.to();

    var notEligibleForDevMode = Constants.devMode && !toState.data.devMode;
    var notEligibleForUserCreation = !Constants.userCreationEnabled && (fromState.is('registration') || fromState.is('confirm'));

    if (notEligibleForDevMode || notEligibleForUserCreation) {
      return trans.router.stateService.target('login');
      //this.$state.go(redirectionState && redirectionState.name ? redirectionState.name : 'home');
    } else if (toState.data && toState.data.roles && !UserService.isUserInRoles(toState.data.roles)) {
      return trans.router.stateService.target(UserService.isAuthenticated() ? 'home' : 'login');
    }
  });

  function configureScreenSize(user) {
    /*
    if (screen.width < 500) {
      $rootScope.percentWidth = 100;
    } else if (screen.width < 770) {
      $rootScope.percentWidth = 50;
    } else {
      $rootScope.percentWidth = 33;
    }
    $rootScope.reducedMode = $rootScope.percentWidth > 33 || !user;
    */
  }

  /*
  UserService.current().then(function (user) {
    $rootScope.graviteeUser = user.data;
    $rootScope.$broadcast('userLoginSuccessful');

  //  configureScreenSize($rootScope.graviteeUser);

    if ($rootScope.graviteeUser) {
      UserService.currentUserPicture().then(function (response) {
        $rootScope.graviteeUser.picture = response.data;
      });
    }
  });
  */

  $rootScope.$on('graviteeLogout', function () {
    $mdSidenav('left').close();
    $window.location.href = $window.location.pathname;
  });

  // Progress Bar
  var interval, intervalTimeInMs = 500;
  $rootScope.$watch(function () {
    return $http.pendingRequests.length > 0;
  }, function (hasPendingRequests) {
    if (hasPendingRequests) {
      $rootScope.isLoading = true;
      $rootScope.progressValue = 0;
      interval = setInterval(function () {
        $rootScope.$apply(function () {
          if ($rootScope.progressValue === 100) {
            $rootScope.progressValue = 0;
          } else {
            $rootScope.progressValue += 10;
          }
        });
      }, intervalTimeInMs);
    } else {
      clearInterval(interval);
      $rootScope.progressValue = 100;
      $rootScope.isLoading = false;
    }
  });

  //swagger-ui
  swaggerModules.add(swaggerModules.BEFORE_PARSE, PageSwaggerConfigurationService);
  swaggerModules.add(swaggerModules.BEFORE_EXPLORER_LOAD, PageSwaggerHttpClientService);

  $rootScope.displayLoader = true;

  // force displayLoader value to change on a new digest cycle
  $timeout(function () {
    $rootScope.displayLoader = false;
  });
}

export default runBlock;
