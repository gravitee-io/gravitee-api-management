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
import * as angular from 'angular';

import './app.module.ajs';
import { enableProdMode } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { loadDefaultTranslations } from '@gravitee/ui-components/src/lib/i18n';
import { computeStyles, LicenseConfiguration } from '@gravitee/ui-particles-angular';
import { toLower, toUpper } from 'lodash';

import { AppModule } from './app.module';
import { Constants } from './entities/Constants';
import { getFeatureInfoData } from './shared/components/gio-license/gio-license-data';
import { ConsoleCustomization } from './entities/management-api-v2/consoleCustomization';
import { environment } from './environments/environment';

const requestConfig: RequestInit = {
  headers: { 'Cache-Control': 'no-cache', Pragma: 'no-cache' },
};

// fix angular-schema-form angular<1.7
Object.assign(angular, { lowercase: toLower, uppercase: toUpper });

fetchData().then(({ constants, build }) => {
  initComponents();
  bootstrapApplication(constants);

  angular.module('gravitee-management').constant('Build', build);

  angular.module('gravitee-management').constant('Constants', constants);

  const initAppTitle = ($rootScope) => {
    $rootScope.consoleTitle = constants.org.settings.management.title;
  };
  initAppTitle.$inject = ['$rootScope'];
  angular.module('gravitee-management').run(initAppTitle);
});

function fetchData(): Promise<{ constants: Constants; build: any }> {
  return Promise.all([
    fetch('build.json', requestConfig).then((r) => r.json()),
    fetch('constants.json', requestConfig).then((r) => r.json()),
  ])
    .then(([buildResponse, constantsResponse]) => {
      const baseURL = sanitizeBaseURLs(constantsResponse.baseURL);
      const enforcedOrganizationId = getEnforcedOrganizationId({
        baseURL,
        organizationId: constantsResponse.organizationId,
      });
      return fetch(
        enforcedOrganizationId ? `${baseURL}/v2/ui/bootstrap?organizationId=${enforcedOrganizationId}` : `${baseURL}/v2/ui/bootstrap`,
        requestConfig,
      )
        .then((r) => r.json())
        .then((bootstrapResponse: { baseURL: string; organizationId: string }) => ({
          bootstrapResponse,
          build: buildResponse,
          production: environment.production,
        }));
    })
    .then(({ bootstrapResponse, build, production }) => {
      const constants = prepareConstants(bootstrapResponse);

      constants.production = production ?? true;

      return Promise.all([
        fetch(`${constants.org.baseURL}/console`, requestConfig).then((r) => r.json()),
        fetch(`${constants.org.v2BaseURL}/ui/customization`, requestConfig).then((r) => (r.status === 200 ? r.json() : null)),
        fetch(`${constants.org.baseURL}/social-identities`, requestConfig).then((r) => r.json()),
      ]).then(([consoleResponse, uiCustomizationResponse, identityProvidersResponse]) => {
        constants.org.settings = consoleResponse;
        constants.org.identityProviders = identityProvidersResponse;

        if (uiCustomizationResponse) {
          customizeUI(uiCustomizationResponse);
          constants.isOEM = true;
          constants.customization = uiCustomizationResponse;
          if (uiCustomizationResponse.title) {
            constants.org.settings.management.title = uiCustomizationResponse.title;
          }
        }
        return { constants, build };
      });
    })
    .catch((error) => {
      document.getElementById('gravitee-error').innerText = 'Management API unreachable or error occurs, please check logs';
      throw error;
    });
}

function sanitizeBaseURLs(baseURLToSanitize: string): string {
  let baseURL = baseURLToSanitize;
  if (baseURLToSanitize.endsWith('/')) {
    baseURL = baseURLToSanitize.slice(0, -1);
  }
  const orgIndex = baseURL.indexOf('/organizations');
  if (orgIndex >= 0) {
    baseURL = baseURL.substr(0, orgIndex);
  }
  return baseURL;
}

function prepareConstants(bootstrap: { baseURL: string; organizationId: string }): Constants {
  if (bootstrap.baseURL.endsWith('/')) {
    bootstrap.baseURL = bootstrap.baseURL.slice(0, -1);
  }

  return {
    baseURL: bootstrap.baseURL,
    v2BaseURL: `${bootstrap.baseURL}/v2`,
    org: {
      id: bootstrap.organizationId,
      baseURL: `${bootstrap.baseURL}/organizations/${bootstrap.organizationId}`,
      v2BaseURL: `${bootstrap.baseURL}/v2/organizations/${bootstrap.organizationId}`,
      currentEnv: null,
      settings: {},
      environments: null,
    },
    env: {
      baseURL: `${bootstrap.baseURL}/organizations/${bootstrap.organizationId}/environments/{:envId}`,
      v2BaseURL: `${bootstrap.baseURL}/v2/environments/{:envId}`,
    },
    isOEM: false,
  };
}

function getEnforcedOrganizationId(constants: { baseURL: string; organizationId?: string }): string | undefined {
  let organizationId: string;
  if (constants.organizationId) {
    organizationId = constants.organizationId;
  } else {
    const baseURL = constants.baseURL;
    const orgIndex = baseURL.indexOf('/organizations/');
    if (orgIndex >= 0) {
      const subPathWithOrga = baseURL.substr(orgIndex, baseURL.length);
      const splitArr = subPathWithOrga.split('/');
      if (splitArr.length >= 3) {
        organizationId = splitArr[2];
      }
    }
  }
  return organizationId;
}

function initComponents() {
  loadDefaultTranslations();
}

function customizeUI(uiCustomization: ConsoleCustomization) {
  if (uiCustomization !== null) {
    const styles = computeStyles({
      menuBackground: uiCustomization.theme.menuBackground,
      menuActive: uiCustomization.theme.menuActive,
    });
    styles.forEach((style) => {
      document.documentElement.style.setProperty(style.key, style.value);
    });
    document.getElementById('favicon').setAttribute('href', uiCustomization.favicon);
  }
}

function bootstrapApplication(constants: Constants) {
  const resourceURL = `${constants.org.v2BaseURL}/license`;
  const featureInfoData = getFeatureInfoData(constants.customization?.ctaConfiguration);
  const trialURLConfiguration = {
    trialResourceURL: constants.customization?.ctaConfiguration?.trialURL || 'https://gravitee.io/self-hosted-trial',
    ...(constants.customization?.ctaConfiguration?.trialURL ? {} : { utmSource: 'oss_apim', utmCampaign: 'oss_apim_to_ee_apim' }),
  };
  const licenseConfiguration: LicenseConfiguration = {
    resourceURL,
    featureInfoData,
    ...trialURLConfiguration,
  };

  if (constants.production) {
    enableProdMode();
  }

  platformBrowserDynamic([
    { provide: Constants, useValue: constants },
    { provide: 'LicenseConfiguration', useValue: licenseConfiguration },
  ])
    .bootstrapModule(AppModule)
    .catch((err) => {
      // eslint-disable-next-line
      console.error(err);
    });
}
