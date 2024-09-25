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
require('jest');

const angular = require('angular');
const _ = require('lodash');

require('@uirouter/angularjs');
require('angular-mocks');
require('@angular/compiler');

// Magic things for angular-schema-form
Object.assign(angular, { lowercase: _.toLower, uppercase: _.toUpper });

const mock = () => {
  let storage = {};
  return {
    getItem: (key) => (key in storage ? storage[key] : null),
    setItem: (key, value) => (storage[key] = value || ''),
    removeItem: (key) => delete storage[key],
    clear: () => (storage = {}),
  };
};
Object.defineProperty(window, 'localStorage', { value: mock() });
Object.defineProperty(window, 'sessionStorage', { value: mock() });

require('jest-preset-angular/setup-jest');

export function setupAngularJsTesting() {
  require('./src/index');
  beforeEach(() => {
    angular.mock.module('gravitee-management');
    // `downgradeInjectable` Does not seem to work with the current test configuration. Waiting for the angular migration to have a less hybrid config than now.
    angular.module('gravitee-management').factory('ngGioPendoService', [() => ({})]);
    angular.module('gravitee-management').factory('ngGioPermissionService', [() => ({})]);
    angular.module('gravitee-management').factory('ngIfMatchEtagInterceptor', [() => ({})]);

    angular.module('gravitee-management').constant('Constants', {
      org: {
        baseURL: 'https://url.test:3000/management/organizations/DEFAULT',
        settings: {
          reCaptcha: false,
        },
      },
      env: {
        baseURL: 'https://url.test:3000/management/organizations/DEFAULT/environments/DEFAULT',
      },
    });
  });
}
window.HTMLElement.prototype.scrollIntoView = jest.fn();
