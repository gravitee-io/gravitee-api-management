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
import { Environment } from './environment/environment';

export interface Constants {
  env?: {
    baseURL?: string;
    settings?: EnvSettings;
  };
  org?: {
    environments: Environment[];
    currentEnv: Environment;
    settings?: OrgSettings;
    baseURL?: string;
  };
  baseURL: string;
}

interface OrgSettings {
  alert?: {
    enabled?: boolean;
  };
  authentication?: {
    google?: unknown;
    github?: unknown;
    oauth2?: unknown;
    localLogin?: {
      enabled?: boolean;
    };
  };
  reCaptcha?: {
    enabled?: boolean;
    siteKey?: string;
  };
  scheduler?: {
    tasks?: number;
    notifications?: number;
  };
  logging?: {
    maxDurationMillis?: number;
    audit?: {
      enabled?: boolean;
      trail?: {
        enabled?: boolean;
      };
    };
    user?: {
      displayed?: boolean;
    };
  };
  maintenance?: {
    enabled?: boolean;
  };
  management?: {
    support?: {
      enabled?: boolean;
    };
    title?: string;
    userCreation?: {
      enabled?: boolean;
    };
    automaticValidation?: {
      enabled?: boolean;
    };
  };
  newsletter?: {
    enabled?: boolean;
  };
  theme?: {
    css?: string;
    name?: string;
    logo?: string;
    loader?: string;
  };
}

interface EnvSettings {
  analytics: {
    clientTimeout: number;
  };
  api: {
    labelsDictionary: any[];
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
  reCaptcha: {
    enabled: boolean;
    siteKey: string;
  };
  scheduler: {
    tasks: number;
    notifications: number;
  };
}
