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
import UserService from './services/user.service';
import { User } from './entities/user';
import {IScope} from 'angular';
import { StateService } from '@uirouter/core';
import {StateProvider, UrlService} from "@uirouter/angularjs";
import PortalService from "./services/portal.service";
import InstancesService from "./services/instances.service";

function routerConfig($stateProvider: StateProvider, $urlServiceProvider: UrlService) {
  'ngInject';
  $stateProvider
    .state(
      'root',
      {
        abstract: true,
        template: "<div layout='row'>" +
        "<div ui-view='sidenav' class='gravitee-sidenav'></div>" +
        "<md-content ui-view layout='column' flex style='height: 100vh' class='md-content'></md-content>" +
        "</div>",
        resolve: {
          graviteeUser: (UserService: UserService) => UserService.current()
        }
      }
    )
    .state(
      'withSidenav',
      {
        parent: 'root',
        abstract: true,
        views: {
          'sidenav': {
            component: 'gvSidenav'
          },
          '': {
            template: '<div flex layout="row">' +
            '<div class="gv-main-container" ui-view layout="column" flex></div>' +
            '<gv-contextual-doc></gv-contextual-doc>' +
            '</div>'
          }
        },
        resolve: {
          allMenuItems: ($state: StateService) => $state.get(),
          menuItems: ($state: StateService, graviteeUser: User, Constants: any) => {
            'ngInject';
            return $state.get()
                  .filter((state: any) => !state.abstract && state.data && state.data.menu)
                  .filter(routeMenuItem => {
                    let isMenuItem = routeMenuItem.data.menu.firstLevel;
                    let isMenuAllowed = !routeMenuItem.data.perms || !routeMenuItem.data.perms.only
                      || graviteeUser.allowedTo(routeMenuItem.data.perms.only);
                    if (Constants.portal.devMode.enabled && !graviteeUser.isAdmin()) {
                      return isMenuItem && isMenuAllowed && routeMenuItem.data.devMode;
                    }  else {
                      return isMenuItem && isMenuAllowed;
                    }
                  });
          }
        }
      }
    )
    .state('user', {
      url: '/user',
      component: 'user',
      parent: 'withSidenav',
      resolve: {
        user: ( graviteeUser: User) => graviteeUser
      }
    })
    .state('login', {
      url: '/login',
      template: require('./user/login/login.html'),
      controller: 'LoginController',
      controllerAs: '$ctrl',
      data: {
        devMode: true
      },
      resolve: {
        checkUser : function (UserService, $state) {
          if (UserService.currentUser && UserService.currentUser.id) {
            $state.go('portal.home');
          }
        },
        identityProviders: (PortalService: PortalService) => PortalService.listSocialIdentityProviders().then(response => response.data)
      }
    })
    .state('registration', {
      url: '/registration',
      template: require('./user/registration/registration.html'),
      controller: 'RegistrationController',
      controllerAs: 'registrationCtrl',
      data: {
        devMode: true
      },
      resolve: {
        checkUser : function (UserService, $state) {
          if (UserService.currentUser && UserService.currentUser.id) {
            $state.go('portal.home');
          }
        }
      }
    })
    .state('confirm', {
      url: '/registration/confirm/:token',
      template: require('./user/registration/confirm/confirm.html'),
      controller: 'ConfirmController',
      controllerAs: 'confirmCtrl',
      data: {
        devMode: true
      }
    })
    .state('resetPassword', {
      url: '/resetPassword/:token',
      template: require('./user/resetPassword/resetPassword.html'),
      controller: 'ResetPasswordController',
      controllerAs: 'resetPasswordCtrl',
      data: {
        devMode: true
      }
    })
    .state('logout', {
      template: '<div class="gravitee-no-sidenav-container"></div>',
      controller: (UserService: UserService, $state: StateService, $rootScope: IScope, $window: ng.IWindowService) => {
        UserService.logout().then(
          () => {
            $state.go('portal.home');
            $rootScope.$broadcast('graviteeUserRefresh', {});
            $rootScope.$broadcast('graviteeUserCancelScheduledServices');
            let userLogoutEndpoint = $window.localStorage.getItem("user-logout-url");
            $window.localStorage.removeItem("user-logout-url");
            if (userLogoutEndpoint != undefined) {
              var redirectUri = encodeURIComponent(window.location.origin + (window.location.pathname == '/' ? '' : window.location.pathname));
              $window.location.href= userLogoutEndpoint + redirectUri;
            }
          }
        );
      }
    })
    .state('support', {
      url: '/support',
      template: require('./support/ticket.html'),
      controller: 'SupportTicketController',
      controllerAs: 'supportTicketCtrl'
    });

  $urlServiceProvider.rules.otherwise('/');
}

export default routerConfig;
