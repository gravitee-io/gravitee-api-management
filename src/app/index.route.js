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
function routerConfig ($stateProvider, $urlRouterProvider) {
  'ngInject';
  $stateProvider
    .state('home', {
      url: '/',
      templateUrl: 'app/main/main.html',
      controller: 'MainController',
      controllerAs: 'mainCtrl'
    })
    .state('login', {
      url: '/login',
      templateUrl: 'app/login/login.html',
      controller: 'LoginController',
      controllerAs: 'loginCtrl'
    })
    .state('apis', {
      url: '/apis',
      templateUrl: 'app/api/apis.html',
      controller: 'ApiController',
      controllerAs: 'apiCtrl'
    })
		.state('apisStub', {
      url: '/apis_stub',
      templateUrl: 'app/api/apis_stub.html',
      controller: 'ApiController',
      controllerAs: 'apiCtrl'
    })
		.state('api', {
      url: '/apis/:apiName',
      templateUrl: 'app/api/api.html',
      controller: 'ApiController',
      controllerAs: 'apiCtrl'
    })
    .state('users', {
      url: '/users',
      templateUrl: 'app/user/users_stub.html',
      controller: 'UserController',
      controllerAs: 'userCtrl'
    })
    .state('teams', {
      url: '/teams',
      templateUrl: 'app/user/users.html',
      controller: 'UserController',
      controllerAs: 'userCtrl'
    })
    .state('documentation', {
      url: '/documentation',
      templateUrl: 'app/documentation/documentation.html',
      controller: 'DocumentationController',
      controllerAs: 'documentationCtrl'
    })
    .state('profile', {
      url: '/profile',
      templateUrl: 'app/profile/profile_stub.html',
      controller: 'ProfileController',
      controllerAs: 'profileCtrl'
    })
    .state('applications', {
      url: '/applications',
      templateUrl: 'app/application/applications.html',
      controller: 'ApplicationController',
      controllerAs: 'applicationCtrl'
    })
		.state('application', {
      url: '/applications/:applicationName',
      templateUrl: 'app/application/application.html',
      controller: 'ApplicationController',
      controllerAs: 'applicationCtrl'
    });

  $urlRouterProvider.otherwise('/');
}

export default routerConfig;
