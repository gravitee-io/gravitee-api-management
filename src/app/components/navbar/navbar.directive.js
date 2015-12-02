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
  constructor ($rootScope, $mdSidenav, $cookieStore, $mdDialog, LoginService, $window) {
    'ngInject';
    this.$rootScope = $rootScope;
    this.$mdSidenav = $mdSidenav;
		this.$cookieStore = $cookieStore;
		this.$mdDialog = $mdDialog;
    this.LoginService = LoginService;
    this.$window = $window;
		this.user = {};
  }

  toggleSidenav(menuId) {
    this.$mdSidenav(menuId).toggle();
  }

	showLoginModal(ev) {
    var _that = this;
    this.$mdDialog.show({
      controller: 'DialogLoginController',
      templateUrl: 'app/login/dialog/login.dialog.html',
      parent: angular.element(document.body),
			targetEvent: ev,
      clickOutsideToClose: true
    });
  }
}

export default NavbarDirective;
