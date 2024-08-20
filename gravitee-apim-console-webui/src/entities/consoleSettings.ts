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
export interface ConsoleSettings {
  email?: ConsoleSettingsEmail;
  metadata?: ConsoleSettingsMetadata;
  alert?: ConsoleSettingsAlert;
  authentication?: ConsoleSettingsAuthentication;
  cors?: ConsoleSettingsCors;
  reCaptcha?: ConsoleSettingsReCaptcha;
  scheduler?: ConsoleSettingsScheduler;
  analyticsPendo?: ConsoleSettingsAnalyticsPendo;
  logging?: ConsoleSettingsLogging;
  maintenance?: ConsoleSettingsMaintenance;
  management?: ConsoleSettingsManagement;
  newsletter?: ConsoleSettingsNewsletter;
  theme?: ConsoleSettingsTheme;
  emulateV4Engine?: ConsoleSettingsV4EmulationEngine;
  alertEngine?: ConsoleSettingsAlertEngine;
  licenseExpirationNotification?: ConsoleSettingsLicenseExpirationNotification;
  trialInstance?: ConsoleSettingsTrialInstance;
  federation?: ConsoleSettingsFederation;
  scoring?: ConsoleSettingsScoring;
}

export interface ConsoleSettingsEmail {
  enabled?: boolean;
  host?: string;
  port?: number;
  username?: string;
  password?: string;
  protocol?: string;
  subject?: string;
  from?: string;
  properties?: {
    auth?: boolean;
    startTlsEnable?: boolean;
    sslTrust?: string;
  };
}

export type ConsoleSettingsMetadata = Record<string, string[]>;

export interface ConsoleSettingsAlert {
  enabled?: boolean;
}

export interface ConsoleSettingsAuthentication {
  google?: {
    clientId?: string;
  };
  github?: {
    clientId?: string;
  };
  oauth2?: {
    clientId?: string;
  };
  localLogin?: {
    enabled?: boolean;
  };
  externalAuth?: {
    enabled?: boolean;
  };
  externalAuthAccountDeletion?: {
    enabled?: boolean;
  };
}

export interface ConsoleSettingsCors {
  allowOrigin?: string[];
  allowHeaders?: string[];
  allowMethods?: string[];
  exposedHeaders?: string[];
  maxAge?: number;
}

export interface ConsoleSettingsReCaptcha {
  enabled?: boolean;
  siteKey?: string;
}

export interface ConsoleSettingsScheduler {
  tasks?: number;
  notifications?: number;
}

export interface ConsoleSettingsAnalyticsPendo {
  enabled?: boolean;
  apiKey?: string;
  accountType?: string;
  accountHrid?: string;
  accountId?: string;
}

export interface ConsoleSettingsLogging {
  maxDurationMillis?: number;
  audit?: {
    enabled?: boolean;
    trail?: {
      enabled: boolean;
    };
  };
  user?: {
    displayed?: boolean;
  };
  messageSampling?: {
    probabilistic?: {
      default: number;
      limit: number;
    };
    count?: {
      default: number;
      limit: number;
    };
    temporal?: {
      default: string;
      limit: string;
    };
  };
}

export interface ConsoleSettingsMaintenance {
  enabled?: boolean;
}

export interface ConsoleSettingsManagement {
  support?: {
    enabled?: boolean;
  };
  title?: string;
  url?: string;
  installationType?: 'standalone' | 'multi-tenant';
  userCreation?: {
    enabled?: boolean;
  };
  automaticValidation?: {
    enabled?: boolean;
  };
  systemRoleEdition?: {
    enabled?: boolean;
  };
}

export interface ConsoleSettingsNewsletter {
  enabled?: boolean;
}

export interface ConsoleSettingsTheme {
  name?: string;
  logo?: string;
  loader?: string;
  css?: string;
}

export interface ConsoleSettingsV4EmulationEngine {
  defaultValue?: string;
}

export interface ConsoleSettingsAlertEngine {
  enabled?: boolean;
}

export interface ConsoleSettingsLicenseExpirationNotification {
  enabled?: boolean;
}

export interface ConsoleSettingsTrialInstance {
  enabled?: boolean;
}

export interface ConsoleSettingsFederation {
  enabled?: boolean;
}

export interface ConsoleSettingsScoring {
  enabled?: boolean;
}
