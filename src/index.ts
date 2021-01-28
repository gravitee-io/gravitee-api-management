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
import _ = require('lodash');
import './index.scss';
import './management/management.module';
import { loadDefaultTranslations } from '@gravitee/ui-components/src/lib/i18n';

// fix angular-schema-form angular<1.7
Object.assign(angular, { lowercase: _.toLower, uppercase: _.toUpper });

let initInjector: ng.auto.IInjectorService = angular.injector(['ng']);
let $http: ng.IHttpService = initInjector.get('$http');
let $q: ng.IQService = initInjector.get('$q');
let $window: ng.IWindowService = initInjector.get('$window');
let configNoCache = { headers: { 'Cache-Control': 'no-cache', 'Pragma': 'no-cache' } };
let ConstantsJSON: any;

fetchData()
  .then((constants: any) => initLoader(constants))
  .then((constants: any) => initTheme(constants))
  .then(initComponents)
  .then(bootstrapApplication);

function fetchData() {

  return $q.all(
    [$http.get('constants.json', configNoCache),
      $http.get('build.json', configNoCache)])
    .then((responses: any) => {
      ConstantsJSON = responses[0].data;
      let build = responses[1].data;
      angular.module('gravitee-management').constant('Build', build);
      ConstantsJSON = computeBaseURLs(ConstantsJSON);
      return $http.get(`${ConstantsJSON.org.baseURL}/console`);
    })
    .then((responses: any) => {
      let constants = _.assign(ConstantsJSON);
      constants.org.settings = responses.data;

      angular.module('gravitee-management').constant('Constants', constants);

      if (constants.org.settings.theme.css) {
        const link = document.createElement('link');
        link.rel = 'stylesheet';
        link.type = 'text/css';
        link.href = constants.org.settings.theme.css;
        document.head.appendChild(link);
      }
      return constants;
    })
    .catch((error) => {
      document.getElementById('gravitee-error').innerText = 'Management API unreachable or error occurs, please check logs';
      throw error;
    });
}

function computeBaseURLs(constants: any): any {
  if (constants.baseURL.endsWith('/')) {
    constants.baseURL = constants.baseURL.slice(0, -1);
  }

  let basePath;
  let orgEnvIndex = constants.baseURL.indexOf('/organizations');
  if (orgEnvIndex >= 0) {
    constants.baseURL = constants.baseURL.substr(0, orgEnvIndex);
  }
  basePath = constants.baseURL;

  constants.org = {};
  preselectEnvironment();
  constants.org.baseURL = `${basePath}/organizations/DEFAULT`;
  constants.env = {};
  // we use a placeholder here ({:envId}) that will be replaced in management.interceptor
  constants.env.baseURL = `${constants.org.baseURL}/environments/{:envId}`;

  return constants;
}

function preselectEnvironment() {

  const environmentRegex = /environments\/([\w|\-]+)/;
  const environment = environmentRegex.exec(document.location.toString());
  if (environment && environment[1]) {
    $window.localStorage.setItem('gv-last-environment-loaded', environment[1]);
  }
}

function initLoader(constants: any) {
  const img = document.createElement('img');
  img.classList.add('gravitee-splash-screen');
  img.setAttribute('src', constants.org.settings.theme.loader);

  document.getElementById('loader').appendChild(img);

  return $q.resolve(constants);
}

function initTheme(constants: any) {
  return $http.get(`./themes/${constants.org.settings.theme.name}-theme.json`, configNoCache)
    .then((response: any) => {
      angular.module('gravitee-management').constant('Theme', response.data);
    }).catch(() => {
      return $http.get('./themes/default-theme.json', configNoCache).then((response: any) => {
        angular.module('gravitee-management').constant('Theme', response.data);
      });
    });
}

function initComponents() {
  loadDefaultTranslations();
}

function bootstrapApplication() {
  angular.element(document).ready(function () {
    angular.bootstrap(document, ['gravitee-management'], { strictDi: true });
  });
}
