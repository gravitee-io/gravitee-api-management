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
import LoginController from './user/login/login.controller';
import { User } from './entities/user';
import {IScope} from 'angular';

function routerConfig($stateProvider: ng.ui.IStateProvider, $urlRouterProvider: ng.ui.IUrlRouterProvider) {
  'ngInject';
  $stateProvider
    .state(
      'root',
      {
        abstract: true,
        template: "<div layout='row'>" +
        "<div ui-view='sidenav' class='gravitee-sidenav'></div>" +
        "<md-content ui-view layout='column' flex></md-content>" +
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
            template: '<div class="gv-main-container" ui-view layout="column" flex></div>'
          }
        },
        resolve: {
          allMenuItems: ($state: ng.ui.IStateService) => $state.get(),
          menuItems: ($state: ng.ui.IStateService, graviteeUser: User, Constants: any) => {
            'ngInject';
            return $state.get()
                  .filter((state: any) => !state.abstract && state.data && state.data.menu)
                  .filter(routeMenuItem => {
                    let isMenuItem = routeMenuItem.data.menu.firstLevel && (!routeMenuItem.data.roles || graviteeUser.allowedTo(routeMenuItem.data.roles));
                    if (Constants.devMode) {
                      return isMenuItem && routeMenuItem.data.devMode;
                    }  else {
                      return isMenuItem;
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
      controller: LoginController,
      controllerAs: '$ctrl',
      data: {
        devMode: true
      }
    })
    .state('registration', {
      url: '/registration',
      template: require('./user/registration/registration.html'),
      controller: 'RegistrationController',
      controllerAs: 'registrationCtrl',
      data: {
        devMode: true
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
    .state('logout', {
      controller: (UserService: UserService, $state: ng.ui.IStateService, $rootScope: IScope) => {
        UserService.logout().then(
          () => { $rootScope.$broadcast('graviteeUserRefresh');$state.go('portal.home'); }
        );
      }
    });

  $urlRouterProvider.otherwise('/');
}

export default routerConfig;
