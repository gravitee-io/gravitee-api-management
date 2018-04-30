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

import './index.scss';

import './portal/portal.module';
import './management/management.module';

let configNoCache = {headers: {'Cache-Control': 'no-cache', 'Pragma': 'no-cache'}};
let baseURL: string;

fetchData()
  .then((constants:any) => initLoader(constants))
  .then((constants:any) => initTheme(constants))
  .then(bootstrapApplication);

function fetchData() {
  let initInjector: ng.auto.IInjectorService = angular.injector(['ng']);
  let $http: ng.IHttpService = initInjector.get('$http');
  let $q: ng.IQService = initInjector.get('$q');

  return $q.all(
    [$http.get('constants.json', configNoCache),
      $http.get('build.json', configNoCache)])
    .then((responses: any) => {
      baseURL = responses[0].data.baseURL;
      let build = responses[1].data;
      angular.module('gravitee-management').constant('Build', build);
      angular.module('gravitee-portal').constant('Build', build);
      return $http.get(`${baseURL}portal`);
    })
    .then( (response: any) => {
      let constants = response.data;
      constants.baseURL = baseURL;
      angular.module('gravitee-management').constant('Constants', constants);
      angular.module('gravitee-portal').constant('Constants', constants);

      if (constants.theme.css) {
        const link = document.createElement("link");
        link.rel = "stylesheet";
        link.type = "text/css";
        link.href = constants.theme.css;
        document.head.appendChild(link);
      }
      return constants;
    });
}

function initLoader(constants:any) {
  let initInjector: ng.auto.IInjectorService = angular.injector(['ng']);
  let $q: ng.IQService = initInjector.get('$q');

  const img = document.createElement('img');
  img.classList.add('gravitee-splash-screen');
  img.setAttribute('src', constants.theme.loader);

  document.getElementById('loader').appendChild(img);

  return $q.resolve(constants);
}

function initTheme(constants:any) {
  let initInjector: ng.auto.IInjectorService = angular.injector(['ng']);
  let $http: ng.IHttpService = initInjector.get('$http');

  return $http.get(`./themes/${constants.theme.name}-theme.json`, configNoCache)
    .then((response: any) => {
      angular.module('gravitee-portal').constant('Theme', response.data);
    });
}

function bootstrapApplication() {
  angular.element(document).ready(function () {
    angular.bootstrap(document, ['gravitee-portal', 'gravitee-management']);
  });
}
