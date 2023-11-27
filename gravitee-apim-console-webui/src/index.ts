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
import { enableProdMode } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { loadDefaultTranslations } from '@gravitee/ui-components/src/lib/i18n';
import { computeStyles, LicenseConfiguration } from '@gravitee/ui-particles-angular';

import { AppModule } from './app.module';
import { Constants } from './entities/Constants';
import { getFeatureInfoData } from './shared/components/gio-license/gio-license-data';
import { ConsoleCustomization } from './entities/management-api-v2/consoleCustomization';

const configNoCache = { headers: { 'Cache-Control': 'no-cache', Pragma: 'no-cache' } };

// fix angular-schema-form angular<1.7
Object.assign(angular, { lowercase: _.toLower, uppercase: _.toUpper });

fetchData().then(({ constants, build }) => {
  initComponents();
  bootstrapApplication(constants);

  angular.module('gravitee-management').constant('Build', build);

  angular.module('gravitee-management').constant('Constants', constants);
});

function fetchData(): Promise<{ constants: Constants; build: any }> {
  return Promise.all([
    fetch('build.json', configNoCache).then((r) => r.json()),
    fetch('constants.json', configNoCache).then((r) => r.json()),
  ])
    .then(([buildResponse, constantsResponse]) => {
      const baseURL = sanitizeBaseURLs(constantsResponse);
      const enforcedOrganizationId = getEnforcedOrganizationId(constantsResponse);
      return fetch(
        enforcedOrganizationId ? `${baseURL}/v2/ui/bootstrap?organizationId=${enforcedOrganizationId}` : `${baseURL}/v2/ui/bootstrap`,
      )
        .then((r) => r.json())
        .then((bootstrapResponse: { baseURL: string; organizationId: string }) => ({
          bootstrapResponse,
          build: buildResponse,
          production: constantsResponse.production,
        }));
    })
    .then(({ bootstrapResponse, build, production }) => {
      const constants = prepareConstants(bootstrapResponse);

      constants.production = production ?? true;

      return Promise.all([
        fetch(`${constants.org.baseURL}/console`).then((r) => r.json()),
        fetch(`${constants.v2BaseURL}/ui/customization`).then((r) => (r.status === 200 ? r.json() : null)),
        fetch(`${constants.org.baseURL}/social-identities`).then((r) => r.json()),
      ]).then(([consoleResponse, uiCustomizationResponse, identityProvidersResponse]) => {
        if (uiCustomizationResponse) {
          customizeUI(uiCustomizationResponse);
          constants.isOEM = true;
          constants.customization = uiCustomizationResponse;
          if (uiCustomizationResponse.data.title) {
            constants.org.settings.management.title = uiCustomizationResponse.data.title;
          }
        }

        constants.org.settings = consoleResponse;
        constants.org.identityProviders = identityProvidersResponse;
        return { constants, build };
      });
    })
    .catch((error) => {
      document.getElementById('gravitee-error').innerText = 'Management API unreachable or error occurs, please check logs';
      throw error;
    });
}

function sanitizeBaseURLs(constants): string {
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

function prepareConstants(bootstrap: { baseURL: string; organizationId: string }): Constants {
  if (bootstrap.baseURL.endsWith('/')) {
    bootstrap.baseURL = bootstrap.baseURL.slice(0, -1);
  }

  return {
    baseURL: bootstrap.baseURL,
    v2BaseURL: `${bootstrap.baseURL}/v2`,
    org: {
      baseURL: `${bootstrap.baseURL}/organizations/${bootstrap.organizationId}`,
      v2BaseURL: `${bootstrap.baseURL}/v2/organizations/${bootstrap.organizationId}`,
      currentEnv: null,
      settings: null,
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
  const resourceURL = `${constants.v2BaseURL}/license`;
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
    { provide: 'Constants', useValue: constants },
    { provide: 'LicenseConfiguration', useValue: licenseConfiguration },
  ])
    .bootstrapModule(AppModule)
    .catch((err) => {
      // eslint-disable-next-line
      console.error(err);
    });
}
