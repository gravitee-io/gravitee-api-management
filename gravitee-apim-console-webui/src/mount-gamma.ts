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
import { enableProdMode } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { loadDefaultTranslations } from '@gravitee/ui-components/src/lib/i18n';
import { computeStyles, LicenseConfiguration } from '@gravitee/ui-particles-angular';

import { Build, Constants, DefaultPortal } from './entities/Constants';
import { getFeatureInfoData } from './shared/components/gio-license/gio-license-data';
import { ConsoleCustomization } from './entities/management-api-v2/consoleCustomization';
import { environment } from './environments/environment';
import { GammaPortalModule, GAMMA_MOUNT_ELEMENT } from './app/gamma-portal/gamma-portal.module';

const requestConfig: RequestInit = {
  headers: { 'Cache-Control': 'no-cache', Pragma: 'no-cache' },
};

export interface MountConfig {
  baseURL?: string;
  envHrid?: string;
}

export interface MountResult {
  unmount: () => void;
}

/**
 * Mounts the Gamma Portal (homepage only) into the given DOM element.
 * Fetches bootstrap data and Constants from the Management API, then bootstraps
 * the Angular gamma portal. Returns an unmount callback to destroy the app (e.g. on route change).
 */
export function mount(mountElement: HTMLElement, _config?: MountConfig): Promise<MountResult> {
  return fetchDataGamma()
    .then(({ constants }) => {
      loadDefaultTranslations();

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

      const platformRef = platformBrowserDynamic([
        { provide: Constants, useValue: constants },
        { provide: 'LicenseConfiguration', useValue: licenseConfiguration },
        { provide: GAMMA_MOUNT_ELEMENT, useValue: mountElement },
      ]);

      return platformRef.bootstrapModule(GammaPortalModule).then(ngModuleRef => ({
        unmount: () => {
          ngModuleRef.destroy();
        },
      }));
    })
    .catch(error => {
      console.error('[Gamma Portal] Bootstrap failed:', error);
      throw error;
    });
}

function fetchDataGamma(): Promise<{ constants: Constants; build: Build }> {
  return Promise.all([
    fetch('build.json', requestConfig).then(r => r.json()),
    fetch('constants.json', requestConfig).then(r => r.json()),
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
        .then(r => getSuccessJsonDataOrThrowError(r))
        .then((bootstrapResponse: { baseURL: string; organizationId: string }) => ({
          bootstrapResponse,
          build: buildResponse,
          production: environment.production,
          defaultPortal: constantsResponse.defaultPortal,
        }));
    })
    .then(({ bootstrapResponse, build, production, defaultPortal }) => {
      const constants = prepareConstants(bootstrapResponse, defaultPortal, build);
      constants.production = production ?? true;

      return Promise.all([
        fetch(`${constants.org.baseURL}/console`, requestConfig).then(r => getSuccessJsonDataOrThrowError(r)),
        fetch(`${constants.org.v2BaseURL}/ui/customization`, requestConfig).then(r => (r.status === 200 ? r.json() : null)),
        fetch(`${constants.org.baseURL}/social-identities`, requestConfig).then(r => getSuccessJsonDataOrThrowError(r)),
      ]).then(([consoleResponse, uiCustomizationResponse, identityProvidersResponse]) => {
        constants.org.settings = consoleResponse;
        constants.org.identityProviders = identityProvidersResponse;

        if (uiCustomizationResponse) {
          customizeUIGamma(uiCustomizationResponse);
          constants.isOEM = true;
          constants.customization = uiCustomizationResponse;
          if (uiCustomizationResponse.title) {
            constants.org.settings.management.title = uiCustomizationResponse.title;
          }
        }
        return { constants, build };
      });
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

function prepareConstants(
  bootstrap: { baseURL: string; organizationId: string },
  defaultPortal: DefaultPortal,
  build: Build,
): Constants {
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
    defaultPortal,
    build: build,
  };
}

function getEnforcedOrganizationId(constants: { baseURL: string; organizationId?: string }): string | undefined {
  if (constants.organizationId) {
    return constants.organizationId;
  }
  const baseURL = constants.baseURL;
  const orgIndex = baseURL.indexOf('/organizations/');
  if (orgIndex >= 0) {
    const subPathWithOrga = baseURL.substr(orgIndex, baseURL.length);
    const splitArr = subPathWithOrga.split('/');
    if (splitArr.length >= 3) {
      return splitArr[2];
    }
  }
  return undefined;
}

function customizeUIGamma(uiCustomization: ConsoleCustomization) {
  if (uiCustomization !== null) {
    const styles = computeStyles({
      menuBackground: uiCustomization.theme.menuBackground,
      menuActive: uiCustomization.theme.menuActive,
    });
    styles.forEach((style: { key: string; value: string }) => {
      document.documentElement.style.setProperty(style.key, style.value);
    });
    const favicon = document.getElementById('favicon');
    if (favicon && uiCustomization.favicon) {
      favicon.setAttribute('href', uiCustomization.favicon);
    }
  }
}

function getSuccessJsonDataOrThrowError(response: Response): Promise<any> {
  if (!response.ok) {
    return response
      .json()
      .catch(() => {
        throw new Error('Management API unreachable or error occurs, please check logs');
      })
      .then(json => {
        throw new Error(json.error?.message ?? json.message ?? 'Management API unreachable or error occurs');
      });
  }
  return response.json();
}
