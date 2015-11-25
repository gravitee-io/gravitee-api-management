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
/* global malarkey:false, moment:false */
import config from './index.config';

import routerConfig from './index.route';

import interceptorConfig from './index.interceptor';

import delegatorConfig from './index.delegator';

import runBlock from './index.run';
import MainController from './main/main.controller';
import ChartDirective from './components/chart/chart.directive';
import ApiService from './api/api.service';
import ApisController from './api/apis.controller';
import ApiPortalController from './api/portal/apiPortal.controller';
import ApiAdminController from './api/admin/apiAdmin.controller';
import DialogApiController from './api/dialog/apiDialog.controller';
import TeamService from './user/team.service';
import UserService from './user/user.service';
import UserController from './user/user.controller';
import LoginService from './login/login.service';
import LoginController from './login/login.controller';
import NavbarDirective from './components/navbar/navbar.directive';
import NotificationService from './components/notification/notification.service';
import DocumentationDirective from './documentation/documentation.directive';
import DocumentationController from './documentation/documentation.controller';
import DocumentationService from './documentation/documentation.service';
import ProfileController from './profile/profile.controller';
import ApplicationsController from './application/applications.controller';
import ApplicationController from './application/details/application.controller';
import DialogApplicationController from './application/dialog/applicationDialog.controller';
import DialogSubscribeApiController from './application/dialog/subscribeApiDialog.controller';
import DialogAddMemberController from './application/dialog/addMemberDialog.controller';
import ApplicationService from './application/details/application.service';

angular.module('gravitee', ['ui.router', 'ngMaterial', 'dndLists', 'ramlConsoleApp', 'btford.markdown', 'swaggerUi',
    'ngMdIcons', 'ui.codemirror', 'md.data.table', 'highcharts-ng', 'ngCookies'])
  .constant('malarkey', malarkey)
  .constant('moment', moment)
  .constant('baseURL', 'http://localhost:8083/management/')
  .config(config)
  .config(routerConfig)
  .config(interceptorConfig)
  .config(delegatorConfig)
  .config(function ($mdThemingProvider) {
    $mdThemingProvider.theme('default')
      .primaryPalette('blue-grey')
      .accentPalette('blue');
  })
  .run(runBlock)
  .controller('MainController', MainController)
  .service('ApiService', ApiService)
  .controller('ApisController', ApisController)
  .controller('ApiAdminController', ApiAdminController)
  .controller('ApiPortalController', ApiPortalController)
  .controller('DialogApiController', DialogApiController)
  .service('TeamService', TeamService)
  .service('UserService', UserService)
  .controller('UserController', UserController)
  .service('LoginService', LoginService)
  .service('NotificationService', NotificationService)
  .controller('LoginController', LoginController)
  .controller('DocumentationController', DocumentationController)
  .service('DocumentationService', DocumentationService)
  .controller('ProfileController', ProfileController)
	.controller('ApplicationsController', ApplicationsController)
  .controller('ApplicationController', ApplicationController)
	.controller('DialogApplicationController', DialogApplicationController)
	.controller('DialogSubscribeApiController', DialogSubscribeApiController)
	.controller('DialogAddMemberController', DialogAddMemberController)
  .service('ApplicationService', ApplicationService)
  .directive('graviteeNavbar', () => new NavbarDirective())
  .directive('chart', () => new ChartDirective())
  .directive('filecontent', () => new DocumentationDirective());
