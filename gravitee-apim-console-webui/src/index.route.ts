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
import { StateProvider, UrlService } from '@uirouter/angularjs';

import OrganizationService from './services/organization.service';
import UserService from './services/user.service';

/* @ngInject */
function routerConfig($stateProvider: StateProvider, $urlServiceProvider: UrlService) {
  $stateProvider
    .state('root', {
      resolve: {
        /* @ngInject */
        graviteeUser: (UserService: UserService) => UserService.current(),
      },
    })
    .state('withoutSidenav', {
      parent: 'root',
      abstract: true,
      template:
        "<div class='gio-root'>" +
        '<gio-top-nav></gio-top-nav>' +
        "<div class='gio-main-page'>" +
        "<div ui-view class='gio-main-page__content'></div>" +
        '<gv-contextual-doc></gv-contextual-doc>' +
        '</div>' +
        '</div>',
    })
    .state('withSidenav', {
      parent: 'root',
      abstract: true,
      url: '/environments/:environmentId',
      template:
        "<div class='gio-root'>" +
        '<gio-top-nav></gio-top-nav>' +
        "<div class='gio-main-page'>" +
        '<gio-side-nav></gio-side-nav>' +
        "<div ui-view class='gio-main-page__content'></div>" +
        '<gv-contextual-doc></gv-contextual-doc>' +
        '</div>' +
        '</div>',
    })
    .state('user', {
      url: '/user',
      component: 'user',
      parent: 'withSidenav',
      resolve: {
        /* @ngInject */
        user: (UserService: UserService) => UserService.refreshCurrent(),
      },
    })
    .state('login', {
      url: '/login?redirectUri',
      template: require('./user/login/login.html'),
      controller: 'LoginController',
      controllerAs: '$ctrl',
      redirectTo: (transition) => {
        const UserService = transition.injector().get('UserService');
        if (UserService.currentUser && UserService.currentUser.id) {
          return 'management';
        }
      },
      resolve: {
        /* @ngInject */
        identityProviders: (OrganizationService: OrganizationService) =>
          OrganizationService.listSocialIdentityProviders().then((response) => response.data),
      },
      params: {
        redirectUri: {
          type: 'string',
        },
      },
    })
    .state('registration', {
      url: '/registration',
      template: require('./user/registration/registration.html'),
      controller: 'RegistrationController',
      controllerAs: '$ctrl',
      redirectTo: (transition) => {
        const UserService = transition.injector().get('UserService');
        if (UserService.currentUser && UserService.currentUser.id) {
          return 'management';
        }
      },
    })
    .state('confirm', {
      url: '/registration/confirm/:token',
      template: require('./user/registration/confirm/confirm.html'),
      controller: 'ConfirmController',
      controllerAs: 'confirmCtrl',
    })
    .state('resetPassword', {
      url: '/resetPassword/:token',
      template: require('./user/resetPassword/resetPassword.html'),
      controller: 'ResetPasswordController',
      controllerAs: 'resetPasswordCtrl',
    })
    .state('logout', {
      component: 'logout',
    })
    .state('newsletter', {
      url: '/newsletter',
      template: require('./user/newsletter/newsletter-subscription.html'),
      controller: 'NewsletterSubscriptionController',
      controllerAs: '$ctrl',
      resolve: {
        /* @ngInject */
        taglines: (UserService: UserService) => UserService.getNewsletterTaglines().then((response) => response.data),
      },
    });

  $urlServiceProvider.rules.otherwise('/login');
}

export default routerConfig;
