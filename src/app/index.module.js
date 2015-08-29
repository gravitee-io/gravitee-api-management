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

import runBlock from './index.run';
import MainController from './main/main.controller';
import ApiService from './api/api.service';
import ApiController from './api/api.controller';
import PolicyService from './policy/policy.service.js';
import TeamService from './user/team.service';
import UserService from './user/user.service';
import UserController from './user/user.controller';
import LoginService from './login/login.service';
import LoginController from './login/login.controller';
import GraviteeContributorService from './components/graviteeContributor/graviteeContributor.service';
import NavbarDirective from './components/navbar/navbar.directive';
import GraviteeContributorDirective from './components/graviteeContributor/graviteeContributor.directive';
import NotificationService from './components/notification/notification.service';

angular.module('gravitee', ['ui.router', 'ngMaterial', 'dndLists'])
  .constant('malarkey', malarkey)
  .constant('moment', moment)
  .constant('baseURL', 'http://localhost:8082/')
  .config(config)

  .config(routerConfig)

  .config(interceptorConfig)

  .run(runBlock)
  .service('graviteeContributorService', GraviteeContributorService)
  .controller('MainController', MainController)
  .service('ApiService', ApiService)
  .controller('ApiController', ApiController)
  .service('PolicyService', PolicyService)
  .service('TeamService', TeamService)
  .service('UserService', UserService)
  .controller('UserController', UserController)
  .service('LoginService', LoginService)
  .service('NotificationService', NotificationService)
  .controller('LoginController', LoginController)
  .directive('graviteeNavbar', () => new NavbarDirective())
  .directive('graviteeContributor', () => new GraviteeContributorDirective(malarkey));
