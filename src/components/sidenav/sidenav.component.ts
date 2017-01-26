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
// import '!style-loader!css-loader!sass-loader!./_sidenav.scss'

import UserService from '../../services/user.service';

export const SidenavComponent: ng.IComponentOptions = {
  template: require('./sidenav.html'),
  bindings: {
    graviteeUser: '<',
    menuItems: '<',
    allMenuItems: '<'
  },
  controller: function() {
    this.reducedMode = false;
  }
};

class SideNavController {
  private routeMenuItems: any;
  private userCreationEnabled: boolean;
  // private subMenuItems: any;
  // private menuItems: any;
  // private currentResource: any;
  private graviteeUser: any;
  // private reducedMode: boolean;

  constructor(
    // private $rootScope,
    // private $mdSidenav: angular.material.ISidenavService,
    // private $scope: ng.IScope,
    private $state: ng.ui.IStateService,
    private Constants: any
  ) {
    'ngInject';
    // $rootScope.devMode = Constants.devMode;
    // $rootScope.portalTitle = Constants.portalTitle;

    this.userCreationEnabled = Constants.userCreationEnabled;

    this.routeMenuItems = $state.get().filter(function (state: any) {
      return !state.abstract && state.data && state.data.menu;
    });

    // console.log(this.routeMenuItems);

    // this.loadMenuItems();

    // $rootScope.$on('userLoginSuccessful', () => {
    //   this.loadMenuItems();
    // });
    //
    // var that = this;
    // $rootScope.$on('$stateChangeStart', function (event, toState, toParams, fromState) {
    //   // init current resource name to delegate its initialization to specific modules
    //   var fromStates = fromState.name.split('.');
    //   var toStates = toState.name.split('.');
    //
    //   if ((fromStates[0] + '.' + fromStates[1]) !== (toStates[0] + '.' + toStates[1])) {
    //     delete that.subMenuItems;
    //     delete $rootScope.currentResource;
    //   }
    // });
    //
    // $rootScope.$on('$stateChangeSuccess', (event, toState, toParams, fromState) => {
    //   this.checkRedirectIfNotAllowed(toState, fromState, event);
    //   this.subMenuItems = this.routeMenuItems.filter(function (routeMenuItem: any) {
    //     let routeMenuItemSplitted = routeMenuItem.name.split('.'),
    //         toStateSplitted = toState.name.split('.');
    //
    //     return !routeMenuItem.data.menu.firstLevel &&
    //       routeMenuItemSplitted[0] === toStateSplitted[0] && routeMenuItemSplitted[1] === toStateSplitted[1];
    //   });
    // });
    //
    // $scope.$on('authenticationRequired', function () {
    //   $state.go('login');
    // });
  }

  // checkRedirectIfNotAllowed(targetState, redirectionState, event) {
  //   // if dev mode, check if the target state is authorized
  //   var notEligibleForDevMode = this.$rootScope.devMode && !targetState.devMode;
  //   var notEligibleForUserCreation = !this.userCreationEnabled && (this.$state.is('registration') || this.$state.is('confirm'));
  //   if (notEligibleForDevMode || notEligibleForUserCreation) {
  //     if (event) {
  //       event.preventDefault();
  //     }
  //     this.$state.go(redirectionState && redirectionState.name ? redirectionState.name : 'home');
  //   }
  // }

  // loadMenuItems() {
  //   this.menuItems = this.$state.get().filter(routeMenuItem =>{
  //     let isMenuItem = routeMenuItem.data.menu.firstLevel && (!routeMenuItem.data.roles || this.UserService.isUserInRoles(routeMenuItem.data.roles));
  //     // if (this.$rootScope.devMode) {
  //     //   return isMenuItem && routeMenuItem.devMode;
  //     // } else {
  //       return isMenuItem;
  //     // });
  //   });
  // }

  // menuItems() {
  //   console.log(this.$state);
  //   return this.$state.get()
  //              .filter(function (state: any) {
  //                return !state.abstract && state.data && state.data.menu;
  //              })
  //              .filter(routeMenuItem => {
  //                let isMenuItem = routeMenuItem.data.menu.firstLevel && (!routeMenuItem.data.roles || this.UserService.isUserInRoles(routeMenuItem.data.roles));
  //                // if (this.$rootScope.devMode) {
  //                //   return isMenuItem && routeMenuItem.devMode;
  //                // } else {
  //                  return isMenuItem;
  //                // }
  //              });
  // }

  // close() {
  //   this.$mdSidenav('left').close();
  // }

  isDisplayed() {
    return !(this.$state.is('login') || this.$state.is('registration') || this.$state.is('confirm'));
  }

  // goToUserPage() {
  //   this.$state.go(this.graviteeUser ? 'user' : 'home');
  // }
}
