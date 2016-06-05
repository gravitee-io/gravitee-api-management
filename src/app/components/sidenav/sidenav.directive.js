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
  constructor($rootScope, $mdSidenav, $mdDialog, $scope, $state, UserService) {
    'ngInject';
    this.$rootScope = $rootScope;
    this.$mdSidenav = $mdSidenav;
    this.$mdDialog = $mdDialog;
    this.UserService = UserService;
    this.$state = $state;

    var _that = this;

    this.routeMenuItems = _.filter($state.get(), function (state) {
      return !state.abstract && state.menu;
    });

    _that.loadMenuItems($scope, UserService);

    $rootScope.$on('userLoginSuccessful', function () {
      _that.loadMenuItems($scope, UserService);
    });

    $scope.$on('$stateChangeSuccess', function (event, toState, toParams) {
      $scope.subMenuItems = _.filter(_that.routeMenuItems, function (routeMenuItem) {
        var firstParam = _.values(toParams)[0];
        // do not display title if id is an UUID (not human readable)
        if (firstParam && !_that.isUUID(firstParam)) {
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
      _that.$state.go('login');
    });
  }

  loadMenuItems($scope, UserService) {
    $scope.menuItems = _.filter(this.routeMenuItems, function (routeMenuItem) {
      return routeMenuItem.menu.firstLevel && (!routeMenuItem.roles || UserService.isUserInRoles(routeMenuItem.roles));
    });
  }

  isUUID(param) {
    return param.match('[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}');
  }

  close() {
    this.$mdSidenav('left').close();
  }

  logout() {
    var that = this;
    this.UserService.logout().then(function () {
      that.$rootScope.$broadcast('graviteeLogout');
    })
  }

  isDisplayed() {
    return 'login' !== this.$state.current.name;
  }

  goToUserPage() {
    this.$state.go(this.$rootScope.graviteeUser?'user':'home');
  }
}

export default SideNavDirective;
