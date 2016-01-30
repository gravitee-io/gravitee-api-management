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
/* global setInterval:false, clearInterval:false */
function runBlock($rootScope, $window, $http, $cookieStore, $mdSidenav) {
  'ngInject';
  var graviteeAuthenticationKey = 'GraviteeAuthentication';

  function setAuthorization() {
    var graviteeAuthentication = $cookieStore.get(graviteeAuthenticationKey);
    if (graviteeAuthentication) {
      $http.defaults.headers.common.Authorization = 'Basic ' + graviteeAuthentication;
      $rootScope.authenticated = true;
    } else {
      $http.defaults.headers.common.Authorization = '';
      $rootScope.authenticated = false;
    }
  }

  setAuthorization();

  $rootScope.$on('graviteeLogout', function () {
    $cookieStore.remove(graviteeAuthenticationKey);
    $cookieStore.remove('authenticatedUser');
    $rootScope.authenticated = false;
    setAuthorization();
    $mdSidenav('left').close();
    $window.location.reload();
  });

  $rootScope.$on('$stateChangeStart', function (event, toState) {
    $rootScope.contentIsActive = (toState.name.indexOf("application.") === 0 ||
    toState.name.indexOf("apis.admin") === 0 ||
    toState.name.indexOf("apis.portal") === 0);
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

  if ($window.innerWidth < 500) {
    $rootScope.percentWidth = 100;
  } else if ($window.innerWidth < 700) {
    $rootScope.percentWidth = 50;
  } else {
    $rootScope.percentWidth = 33;
  }

  $rootScope.reducedMode = $rootScope.percentWidth > 33 || !$rootScope.authenticated;
}

export default runBlock;
