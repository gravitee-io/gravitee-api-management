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
/* global malarkey:false, toastr:false, moment:false */
import config from './index.config';

import routerConfig from './index.route';

import runBlock from './index.run';
import MainController from './main/main.controller';
import ApiService from './api/api.service';
import ApiController from './api/api.controller';
import GraviteeContributorService from './components/graviteeContributor/graviteeContributor.service';
import NavbarDirective from './components/navbar/navbar.directive';
import GraviteeContributorDirective from './components/graviteeContributor/graviteeContributor.directive';

angular.module('gravitee', ['ui.router', 'ngMaterial'])
  .constant('malarkey', malarkey)
  .constant('toastr', toastr)
  .constant('moment', moment)
  .constant('baseURL', 'http://localhost:8082/api/')
  .config(config)

  .config(routerConfig)

  .run(runBlock)
  .service('graviteeContributorService', GraviteeContributorService)
  .controller('MainController', MainController)
  .service('ApiService', ApiService)
  .controller('ApiController', ApiController)
  .directive('graviteeNavbar', () => new NavbarDirective())
  .directive('graviteeContributor', () => new GraviteeContributorDirective(malarkey));
