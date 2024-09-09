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
/**
 * TODO: to complete, contains only one part used in the Ui console
 */
export interface PortalSettings {
  portal?: PortalSettingsPortal;
  metadata?: PortalSettingsMetadata;
  application?: PortalSettingsApplication;
  apiQualityMetrics?: PortalSettingsApiQualityMetrics;
  apiReview?: PortalSettingsApiReview;
  authentication?: PortalSettingsAuthentication;
  company?: PortalSettingsCompany;
  plan?: PortalSettingsPlan;
  api?: PortalSettingsApi;
  dashboards?: PortalSettingsDashboards;
  scheduler?: PortalSettingsScheduler;
  documentation?: PortalSettingsDocumentation;
  openAPIDocViewer?: PortalSettingsOpenAPIDocViewer;
  cors?: PortalSettingsCors;
  email?: PortalSettingsEmail;
  portalNext?: PortalSettingsPortalNext;
}

export type PortalSettingsMetadata = Record<string, string[]>;

export interface PortalSettingsApplication {
  registration: {
    enabled: boolean;
  };
  types?: {
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
}

export interface PortalSettingsPortal {
  analytics?: {
    enabled: boolean;
    trackingId?: string;
  };
  apikeyHeader?: string;
  apis?: {
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
    promotedApiMode?: {
      enabled: boolean;
    };
  };
  entrypoint?: string;
  rating?: {
    enabled: boolean;
    comment: {
      mandatory: boolean;
    };
  };
  support?: {
    enabled: boolean;
  };
  tcpPort?: number;
  uploadMedia?: {
    enabled: boolean;
    maxSizeInOctet: number;
  };
  userCreation?: {
    enabled: boolean;
    automaticValidation: {
      enabled: boolean;
    };
  };
  url?: string;
  homepageTitle?: string;
}

export interface PortalSettingsApiQualityMetrics {
  enabled: boolean;
  functionalDocumentationWeight: number;
  technicalDocumentationWeight: number;
  descriptionWeight: number;
  descriptionMinLength: number;
  logoWeight: number;
  categoriesWeight: number;
  labelsWeight: number;
  healthcheckWeight: number;
}

export interface PortalSettingsApiReview {
  enabled: boolean;
}

export interface PortalSettingsAuthentication {
  forceLogin?: {
    enabled: boolean;
  };
  localLogin?: {
    enabled: boolean;
  };
}

export interface PortalSettingsCompany {
  name: string;
}

export interface PortalSettingsPlan {
  security: {
    keyless: {
      enabled: boolean;
    };
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
    jwt: {
      enabled: boolean;
    };
    push: {
      enabled: boolean;
    };
    mtls: {
      enabled: boolean;
    };
  };
}

export interface PortalSettingsApi {
  labelsDictionary: string[];
  primaryOwnerMode: string;
}

export interface PortalSettingsDashboards {
  apiStatus: {
    enabled: boolean;
  };
}

export interface PortalSettingsScheduler {
  tasks: number;
  notifications: string;
}

export interface PortalSettingsDocumentation {
  url: string;
}

export interface PortalSettingsEmail {
  enabled: boolean;
  host: string;
  port: number;
  username: string;
  password: string;
  protocol: string;
  subject: string;
  from: string;
  properties: {
    auth: boolean;
    startTlsEnable: boolean;
    sslTrust: string;
  };
}

export interface PortalSettingsCors {
  allowOrigin: string[];
  allowMethods: string[];
  allowHeaders: string[];
  exposedHeaders: string[];
  maxAge: number;
}

export interface PortalSettingsOpenAPIDocViewer {
  openAPIDocType: {
    swagger: {
      enabled: boolean;
    };
    defaultType: any;
    redoc: {
      enabled: boolean;
    };
  };
}
export interface PortalSettingsPortalNext {
  access: {
    enabled: boolean;
  };
  banner?: {
    title?: string;
    subtitle?: string;
    enabled?: boolean;
    primaryButton?: BannerButton;
    secondaryButton?: BannerButton;
  };
}

export interface BannerButton {
  enabled?: boolean;
  label?: string;
  target?: string;
  type?: 'EXTERNAL';
  visibility?: 'PUBLIC' | 'PRIVATE';
}
