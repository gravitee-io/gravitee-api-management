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
import 'zone.js';
import 'reflect-metadata';
import '@angular/compiler';

import * as angular from 'angular';
import * as _ from 'lodash';
import './index.scss';
import './management/management.module.ajs';
import { loadDefaultTranslations } from '@gravitee/ui-components/src/lib/i18n';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { UIRouter, UrlService } from '@uirouter/core';
import { NgZone } from '@angular/core';
import { LicenseConfiguration } from '@gravitee/ui-particles-angular';

import { AppModule } from './app.module';
import { Constants } from './entities/Constants';
import { FeatureInfoData } from './shared/components/gio-license/gio-license-data';

// fix angular-schema-form angular<1.7
Object.assign(angular, { lowercase: _.toLower, uppercase: _.toUpper });

const initInjector: ng.auto.IInjectorService = angular.injector(['ng']);
const $http: ng.IHttpService = initInjector.get('$http');
const $q: ng.IQService = initInjector.get('$q');
const configNoCache = { headers: { 'Cache-Control': 'no-cache', Pragma: 'no-cache' } };
let ConstantsJSON: any;

fetchData().then((constants: Constants) => {
  return initLoader(constants)
    .then(() => initTheme(constants))
    .then(() => initComponents())
    .then(() => bootstrapApplication(constants));
});

function fetchData() {
  return $q
    .all([$http.get('build.json', configNoCache), $http.get('constants.json', configNoCache)])
    .then((responses: any) => {
      // Store build information
      angular.module('gravitee-management').constant('Build', responses[0].data);
      // Store build information
      const baseURL = sanitizeBaseURLs(responses[1].data);
      return $http.get(`${baseURL}/v2/ui/bootstrap`);
    })
    .then((bootstrapResponse: any) => {
      ConstantsJSON = prepareConstants(bootstrapResponse.data);
      return $http.get(`${ConstantsJSON.org.baseURL}/console`);
    })
    .then((consoleResponse: any) => {
      const constants = _.assign(ConstantsJSON);
      constants.org.settings = consoleResponse.data;

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

function sanitizeBaseURLs(constants: any): any {
  let baseURL = constants.baseURL;
  if (constants.baseURL.endsWith('/')) {
    baseURL = constants.baseURL.slice(0, -1);
  }
  const orgIndex = baseURL.indexOf('/organizations');
  if (orgIndex >= 0) {
    baseURL = baseURL.substr(0, orgIndex);
  }
  return baseURL;
}

function prepareConstants(bootstrap: any): any {
  const constants: any = {};
  if (bootstrap.baseURL.endsWith('/')) {
    bootstrap.baseURL = bootstrap.baseURL.slice(0, -1);
  }

  constants.baseURL = bootstrap.baseURL;
  constants.org = {};
  const organizationId = bootstrap.organizationId;
  constants.org.baseURL = `${constants.baseURL}/organizations/${organizationId}`;
  constants.env = {};
  // we use a placeholder here ({:envId}) that will be replaced in management.interceptor
  constants.env.baseURL = `${constants.org.baseURL}/environments/{:envId}`;
  constants.v2BaseURL = `${constants.baseURL}/v2`;
  constants.env.v2BaseURL = `${constants.v2BaseURL}/environments/{:envId}`;

  return constants;
}

function initLoader(constants: Constants) {
  const img = document.createElement('img');
  img.classList.add('gravitee-splash-screen');
  img.setAttribute('src', constants.org.settings.theme.loader);

  document.getElementById('loader').appendChild(img);

  return $q.resolve(constants);
}

function initTheme(constants: Constants) {
  return $http
    .get(`./themes/${constants.org.settings.theme.name}-theme.json`, configNoCache)
    .then((response: any) => {
      angular.module('gravitee-management').constant('Theme', response.data);
    })
    .catch(() => {
      return $http.get('./themes/default-theme.json', configNoCache).then((response: any) => {
        angular.module('gravitee-management').constant('Theme', response.data);
      });
    });
}

function initComponents() {
  loadDefaultTranslations();
}

function bootstrapApplication(constants: Constants) {
  angular.module('gravitee-management').config(($urlServiceProvider: UrlService) => $urlServiceProvider.deferIntercept());
  const resourceURL = `${constants.v2BaseURL}/license`;
  const featureInfoData = FeatureInfoData;
  const licenseConfiguration: LicenseConfiguration = {
    resourceURL,
    featureInfoData,
    trialResourceURL: 'https://gravitee.io/self-hosted-trial',
    utmSource: 'oss_apim',
    utmCampaign: 'oss_apim_to_ee_apim',
  };
  platformBrowserDynamic([
    { provide: 'Constants', useValue: constants },
    { provide: 'LicenseConfiguration', useValue: licenseConfiguration },
  ])
    .bootstrapModule(AppModule)
    .then((platformRef) => {
      // Intialize the Angular Module
      // get() the UIRouter instance from DI to initialize the router
      const urlService: UrlService = platformRef.injector.get(UIRouter).urlService;

      // Instruct UIRouter to listen to URL changes
      setTimeout(() => {
        function startUIRouter() {
          urlService.listen();
          urlService.sync();
        }
        platformRef.injector.get<NgZone>(NgZone).run(startUIRouter);
      }, 1000);
    });
}
