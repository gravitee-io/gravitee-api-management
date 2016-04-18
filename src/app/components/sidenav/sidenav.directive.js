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
class SideNavDirective {
  constructor() {
    let directive = {
      restrict: 'E',
      templateUrl: 'app/components/sidenav/sidenav.html',
      controller: SideNavController,
      controllerAs: 'sidenavCtrl',
      bindToController: true
    };

    return directive;
  }
}

class SideNavController {
  constructor($rootScope, $cookieStore, $mdSidenav, $mdDialog, $scope, $state) {
    'ngInject';
    this.$rootScope = $rootScope;
    this.$cookieStore = $cookieStore;
    this.$mdSidenav = $mdSidenav;
    this.$mdDialog = $mdDialog;
    this.getUser();
    var self = this;
    this.$rootScope.$watch('authenticated', function () {
      self.getUser();
    });

    var routeMenuItems = _.filter($state.get(), function (state) {
      return !state.abstract && state.menu;
    });

    $scope.menuItems = _.filter(routeMenuItems, function (routeMenuItem) {
      return routeMenuItem.menu.firstLevel;
    });

    $scope.$on('$stateChangeSuccess', function (event, toState, toParams) {
      $scope.subMenuItems = _.filter(routeMenuItems, function (routeMenuItem) {
        var firstParam = _.values(toParams)[0];
        if (firstParam && !self.isUUID(firstParam)) {
          $scope.currentResource = firstParam;
        } else {
          delete $scope.currentResource;
        }
        var routeMenuItemSplitted = routeMenuItem.name.split('.'), toStateSplitted = toState.name.split('.');
        return !routeMenuItem.menu.firstLevel &&
          routeMenuItemSplitted[0] === toStateSplitted[0] && routeMenuItemSplitted[1] === toStateSplitted[1];
      });
    });

    $scope.$on('authenticationRequired', function () {
      self.showLoginModal();
    });
  }

  isUUID(param) {
    return param.match('[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}');
  }

  getUser() {
    this.user = this.$cookieStore.get('authenticatedUser');
  }

  close() {
    this.$mdSidenav('left').close();
  }

  logout() {
    this.$rootScope.$broadcast('graviteeLogout');
  }

  showLoginModal(ev) {
    this.$mdDialog.show({
      controller: 'DialogLoginController',
      templateUrl: 'app/login/dialog/login.dialog.html',
      parent: angular.element(document.body),
      targetEvent: ev,
      clickOutsideToClose: true
    });
  }
}

export default SideNavDirective;
