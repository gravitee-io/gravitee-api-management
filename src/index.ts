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

import 'angular-ui-router';

import './index.scss';

import './portal/portal.module';
import './management/management.module';

const constants = require('../constants.json');
const build = require('../build.json');

initLoader().then(bootstrapApplication);

function initLoader() {
  let initInjector: ng.auto.IInjectorService = angular.injector(['ng']);
  let $q: ng.IQService = initInjector.get('$q');

  const img = document.createElement('img');
  img.classList.add('gravitee-splash-screen');
  img.setAttribute('src', constants.loaderLogo);

  document.getElementById('loader').appendChild(img);

  return $q.resolve();
}

function bootstrapApplication() {
  angular.module('gravitee-management').constant('Constants', constants);
  angular.module('gravitee-management').constant('Build', build);

  angular.module('gravitee-portal').constant('Build', build);

  angular.element(document).ready(function() {
    angular.bootstrap(document, ['gravitee-portal', 'gravitee-management']);
  });
}
