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
  alert?: DisableableFeature;
  authentication?: ConsoleSettingsAuthentication;
  cors?: ConsoleSettingsCors;
  reCaptcha?: ConsoleSettingsReCaptcha;
  scheduler?: ConsoleSettingsScheduler;
  analyticsPendo?: ConsoleSettingsAnalyticsPendo;
  logging?: ConsoleSettingsLogging;
  maintenance?: DisableableFeature;
  management?: ConsoleSettingsManagement;
  newsletter?: DisableableFeature;
  theme?: ConsoleSettingsTheme;
  emulateV4Engine?: ConsoleSettingsV4EmulationEngine;
  alertEngine?: DisableableFeature;
  licenseExpirationNotification?: DisableableFeature;
  trialInstance?: DisableableFeature;
  federation?: DisableableFeature;
  cloudHosted?: DisableableFeature;
  userGroup?: ConsoleSettingsUserGroup;
  elGen?: DisableableFeature;
  kafkaConsole?: DisableableFeature;
}

export interface ConsoleSettingsEmail extends DisableableFeature {
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
  localLogin?: DisableableFeature;
  externalAuth?: DisableableFeature;
  externalAuthAccountDeletion?: DisableableFeature;
}

export interface ConsoleSettingsCors {
  allowOrigin?: string[];
  allowHeaders?: string[];
  allowMethods?: string[];
  exposedHeaders?: string[];
  maxAge?: number;
}

export interface ConsoleSettingsReCaptcha extends DisableableFeature {
  siteKey?: string;
}

export interface ConsoleSettingsScheduler {
  tasks?: number;
  notifications?: number;
}

export interface ConsoleSettingsAnalyticsPendo extends DisableableFeature {
  apiKey?: string;
  accountType?: string;
  accountHrid?: string;
  accountId?: string;
}

export interface ConsoleSettingsLogging {
  maxDurationMillis?: number;
  audit?: DisableableFeature & {
    trail?: DisableableFeature;
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

interface DisableableFeature {
  enabled?: boolean;
}

export interface ConsoleSettingsManagement {
  support?: DisableableFeature;
  title?: string;
  url?: string;
  installationType?: 'standalone' | 'multi-tenant';
  userCreation?: DisableableFeature;
  automaticValidation?: DisableableFeature;
  systemRoleEdition?: DisableableFeature;
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
  required: DisableableFeature;
}
