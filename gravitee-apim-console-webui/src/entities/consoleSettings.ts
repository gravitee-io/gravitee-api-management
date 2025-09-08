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
  alert?: DisablableFeature;
  authentication?: ConsoleSettingsAuthentication;
  cors?: ConsoleSettingsCors;
  reCaptcha?: ConsoleSettingsReCaptcha;
  scheduler?: ConsoleSettingsScheduler;
  analyticsPendo?: ConsoleSettingsAnalyticsPendo;
  logging?: ConsoleSettingsLogging;
  maintenance?: DisablableFeature;
  management?: ConsoleSettingsManagement;
  newsletter?: DisablableFeature;
  theme?: ConsoleSettingsTheme;
  emulateV4Engine?: ConsoleSettingsV4EmulationEngine;
  alertEngine?: DisablableFeature;
  licenseExpirationNotification?: DisablableFeature;
  trialInstance?: DisablableFeature;
  federation?: DisablableFeature;
  cloudHosted?: DisablableFeature;
  userGroup?: ConsoleSettingsUserGroup;
  elGen?: DisablableFeature;
}

export interface ConsoleSettingsEmail extends DisablableFeature {
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
  localLogin?: DisablableFeature;
  externalAuth?: DisablableFeature;
  externalAuthAccountDeletion?: DisablableFeature;
}

export interface ConsoleSettingsCors {
  allowOrigin?: string[];
  allowHeaders?: string[];
  allowMethods?: string[];
  exposedHeaders?: string[];
  maxAge?: number;
}

export interface ConsoleSettingsReCaptcha extends DisablableFeature {
  siteKey?: string;
}

export interface ConsoleSettingsScheduler {
  tasks?: number;
  notifications?: number;
}

export interface ConsoleSettingsAnalyticsPendo extends DisablableFeature {
  apiKey?: string;
  accountType?: string;
  accountHrid?: string;
  accountId?: string;
}

export interface ConsoleSettingsLogging {
  maxDurationMillis?: number;
  audit?: DisablableFeature & {
    trail?: DisablableFeature;
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

interface DisablableFeature {
  enabled?: boolean;
}

export interface ConsoleSettingsManagement {
  support?: DisablableFeature;
  title?: string;
  url?: string;
  installationType?: 'standalone' | 'multi-tenant';
  userCreation?: DisablableFeature;
  automaticValidation?: DisablableFeature;
  systemRoleEdition?: DisablableFeature;
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

interface ConsoleSettingsUserGroup {
  required: DisablableFeature;
}
