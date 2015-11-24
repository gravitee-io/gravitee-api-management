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
/* global btoa:false */
class NavbarDirective {
  constructor () {
    let directive = {
      restrict: 'E',
      templateUrl: 'app/components/navbar/navbar.html',
      controller: NavbarController,
      controllerAs: 'navbarCtrl',
      bindToController: true
    };

    return directive;
  }
}

class NavbarController {
  constructor ($rootScope, $mdSidenav, $location, $cookieStore, LoginService) {
    'ngInject';
    this.$rootScope = $rootScope;
    this.$mdSidenav = $mdSidenav;
		this.$location = $location;
		this.$cookieStore = $cookieStore;
		this.LoginService = LoginService;
		this.user = {};
  }

  toggleSidenav(menuId) {
    this.$mdSidenav(menuId).toggle();
  }

  openMenu($mdOpenMenu, ev) {
    $mdOpenMenu(ev);
  }

	login() {
    var that = this;
    this.LoginService.login(this.user).then(function() {
			that.$cookieStore.put('GraviteeAuthentication', btoa(that.user.username + ":" + that.user.password));
			that.user = {};
			that.$rootScope.$broadcast('authenticationSuccess');
      that.$location.path('/');
    });
  }

  logout() {
    this.$rootScope.$broadcast('graviteeLogout');
  }
}

export default NavbarDirective;
