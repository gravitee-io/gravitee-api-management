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
import angular = require('angular');

import 'angular-material';
import 'angular-translate';
import 'angular-translate-loader-static-files';

import * as moment from 'moment';

import portalRouterConfig from './portal.route';
import authenticationConfig from '../authentication/authentication.config';
import portalI18nConfig from './portal.i18n';

// i18n
import i18nCustomLoader from './i18n/loader';

import { HomeController } from './home/home.controller';
import { PortalApisController } from './api/apis.controller';

// API
import ApiComponent from './api/api.component';
import ApiHeaderComponent from './api/header/api-header.component';
import ApiHomepageComponent from './api/home/api-homepage.component';
import ApiPlansComponent from './api/plan/api-plans.component';
import ApiSubscribeComponent from './api/subscribe/api-subscribe.component';
import ApiRatingsComponent from './api/rating/api-ratings.component';

// API documentation
import ApiPagesComponent from './api/documentation/api-pages.component';
import ApiPageComponent from './api/documentation/api-page.component';

// Portal Pages
import PagesComponent from './pages/pages.component';
import PageComponent from './pages/page.component';

import { NavbarComponent } from './components/navbar/navbar.component';
import ThemeElementDirective from './components/theme/theme-element.directive';

import uiRouter from 'angular-ui-router';
import {permission, uiPermission} from 'angular-permission';
import StarRatingDirective from "./components/starRating/star.rating.directive";

require('angulartics');

angular.module('gravitee-portal', [uiRouter, permission, uiPermission, 'ngMaterial', 'pascalprecht.translate',
  'duScroll', 'satellizer', 'angulartics', require('angulartics-google-analytics')])
  .value('duScrollOffset', 54)
  .config(portalRouterConfig)
  .config(portalI18nConfig)
  .config(authenticationConfig)
  .factory('i18nCustomLoader', i18nCustomLoader)
  .controller('HomeController', HomeController)
  .controller('PortalApisController', PortalApisController)
  .component('api', ApiComponent)
  .component('apiPortalHeader', ApiHeaderComponent) // apiHeader already used in management.... :(
  .component('apiHomepage', ApiHomepageComponent)
  .component('apiPlans', ApiPlansComponent)
  .component('apiPages', ApiPagesComponent)
  .component('apiPage', ApiPageComponent)
  .component('pages', PagesComponent)
  .component('page', PageComponent)
  .component('apiSubscribe', ApiSubscribeComponent)
  .component('graviteeNavbar', NavbarComponent)
  .component('apiRatings', ApiRatingsComponent)
  .directive('gvThemeElement', () => ThemeElementDirective)
  .directive('gvStarRating', () => StarRatingDirective)
  .filter('humanDateFilter', function () {
    return function(input) {
      if (!moment().subtract(1, 'weeks').isAfter(input)) {
        return moment(input).fromNow();
      } else {
        return moment(input).format('D MMM. YYYY');
      }
    };
  })
  .config(function ($mdThemingProvider: ng.material.IThemingProvider) {
    $mdThemingProvider.theme('default')
      .primaryPalette('blue-grey')
      .accentPalette('blue');

    $mdThemingProvider.theme('sidenav')
      .backgroundPalette('grey', {
        'default': '50'
      });

    $mdThemingProvider.theme('toast-success');
    $mdThemingProvider.theme('toast-error');
  }).run(function(Constants, $window, $rootScope, $location) {
    if ((Constants.portal.analytics.enabled)) {
      const script = document.createElement('script');
      script.async = true;
      script.text = '(function(i,s,o,g,r,a,m){i[\'GoogleAnalyticsObject\']=r;i[r]=i[r]||function(){\n' +
        '          (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),\n' +
        '          m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)\n' +
        '        })(window,document,\'script\',\'//www.google-analytics.com/analytics.js\',\'ga\');';
      document.head.appendChild(script);

      $window.ga('create', Constants.portal.analytics.trackingId, { 'cookieDomain': 'none' });

      $rootScope.$on('$stateChangeSuccess', function () {
        $window.ga('send', 'pageview', $location.path());
      });
    }
  });
