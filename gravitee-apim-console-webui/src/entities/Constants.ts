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
import { InjectionToken } from '@angular/core';

import { ConsoleSettings } from './consoleSettings';
import { Environment } from './environment/environment';
import { SocialIdentityProvider } from './organization/socialIdentityProvider';
import { ConsoleCustomization } from './management-api-v2/consoleCustomization';

export interface Constants {
  production?: boolean;
  env?: {
    baseURL?: string;
    v2BaseURL?: string;
    /**
     * @deprecated Use EnvironmentSettingsService instead with `.get()` or `.getSnapshot()`
     */
    settings?: EnvSettings;
  };
  org?: {
    id: string;
    environments: Environment[];
    currentEnv: Environment;
    settings?: ConsoleSettings;
    identityProviders?: SocialIdentityProvider[];
    baseURL?: string;
    v2BaseURL?: string;
  };
  baseURL: string;
  v2BaseURL?: string;
  isOEM: boolean;
  customization?: ConsoleCustomization;
}

// eslint-disable-next-line no-redeclare
export const Constants = new InjectionToken<Constants>('Constants');

export interface EnvSettings {
  analytics: {
    clientTimeout: number;
  };
  api: {
    labelsDictionary: string[];
    primaryOwnerMode: string;
  };
  apiQualityMetrics: {
    enabled: boolean;
    functionalDocumentationWeight: number;
    technicalDocumentationWeight: number;
    descriptionWeight: number;
    descriptionMinLength: number;
    logoWeight: number;
    categoriesWeight: number;
    labelsWeight: number;
    healthcheckWeight: number;
  };
  apiReview: {
    enabled: boolean;
  };
  application: {
    registration: {
      enabled: boolean;
    };
    types: {
      simple: {
        enabled: boolean;
      };
      browser: {
        enabled: boolean;
      };
      web: {
        enabled: boolean;
      };
      native: {
        enabled: boolean;
      };
      backend_to_backend: {
        enabled: boolean;
      };
    };
  };
  authentication: {
    google: any;
    github: any;
    oauth2: any;
    forceLogin: {
      enabled: boolean;
    };
    localLogin: {
      enabled: boolean;
    };
  };
  dashboards: {
    apiStatus: {
      enabled: boolean;
    };
  };
  company: {
    name: string;
  };
  documentation: {
    url: string;
  };
  openAPIDocViewer: {
    openAPIDocType: {
      swagger: {
        enabled: boolean;
      };
      redoc: {
        enabled: boolean;
      };
      defaultType: string;
    };
  };
  plan: {
    security: {
      apikey: {
        enabled: boolean;
      };
      customApiKey: {
        enabled: boolean;
      };
      sharedApiKey: {
        enabled: boolean;
      };
      oauth2: {
        enabled: boolean;
      };
      keyless: {
        enabled: boolean;
      };
      jwt: {
        enabled: boolean;
      };
    };
  };
  portal: {
    url: string;
    entrypoint: string;
    apikeyHeader: string;
    support: {
      enabled: boolean;
    };
    apis: {
      tilesMode: {
        enabled: boolean;
      };
      categoryMode: {
        enabled: boolean;
      };
      apiHeaderShowTags: {
        enabled: boolean;
      };
      apiHeaderShowCategories: {
        enabled: boolean;
      };
    };
    analytics: {
      enabled: boolean;
    };
    rating: {
      enabled: boolean;
      comment: {
        mandatory: boolean;
      };
    };
    userCreation: {
      enabled: boolean;
      automaticValidation: {
        enabled: boolean;
      };
    };
    uploadMedia: {
      enabled: boolean;
      maxSizeInOctet: number;
    };
  };
  portalNext: {
    access: {
      enabled: boolean;
    };
  };
  reCaptcha: {
    enabled: boolean;
    siteKey: string;
  };
  scheduler: {
    tasks: number;
    notifications: number;
  };
}
