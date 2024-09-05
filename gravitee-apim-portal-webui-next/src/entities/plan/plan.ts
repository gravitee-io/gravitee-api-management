/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
export interface Plan {
  id: string;
  name: string;
  security: PlanSecurityEnum;
  description?: string;
  characteristics?: Array<string>;
  validation: PlanValidationEnum;
  order: number;
  comment_required?: boolean;
  comment_question?: string;
  general_conditions?: string;
  mode: PlanMode;
  usage_configuration?: PlanUsageConfiguration;
}
export type PlanSecurityEnum = 'API_KEY' | 'KEY_LESS' | 'JWT' | 'OAUTH2' | 'MTLS';
export const PlanSecurityEnum = {
  APIKEY: 'API_KEY' as PlanSecurityEnum,
  KEYLESS: 'KEY_LESS' as PlanSecurityEnum,
  JWT: 'JWT' as PlanSecurityEnum,
  OAUTH2: 'OAUTH2' as PlanSecurityEnum,
};
export type PlanValidationEnum = 'AUTO' | 'MANUAL';
export const PlanValidationEnum = {
  AUTO: 'AUTO' as PlanValidationEnum,
  MANUAL: 'MANUAL' as PlanValidationEnum,
};

export const getPlanSecurityTypeLabel = (securityType: PlanSecurityEnum | undefined) => {
  switch (securityType) {
    case 'OAUTH2':
      return 'OAuth2';
    case 'JWT':
      return 'JWT';
    case 'API_KEY':
      return 'API Key';
    case 'KEY_LESS':
      return 'Keyless';
    case 'MTLS':
      return 'mTLS';
    default:
      return '';
  }
};

export interface PlanUsageConfiguration {
  rate_limit?: TimePeriodConfiguration;
  quota?: TimePeriodConfiguration;
}

export type PlanMode = 'STANDARD' | 'PUSH';

export const PlanMode = {
  STANDARD: 'STANDARD' as PlanMode,
  PUSH: 'PUSH' as PlanMode,
};

export interface TimePeriodConfiguration {
  period_time?: number;
  period_time_unit?: PeriodTimeUnit;
  /**
   * Maximum number of requests.
   */
  limit?: number;
}

export type PeriodTimeUnit = 'SECONDS' | 'MINUTES' | 'HOURS' | 'DAYS' | 'WEEKS' | 'MONTHS';

export const PeriodTimeUnit = {
  SECONDS: 'SECONDS' as PeriodTimeUnit,
  MINUTES: 'MINUTES' as PeriodTimeUnit,
  HOURS: 'HOURS' as PeriodTimeUnit,
  DAYS: 'DAYS' as PeriodTimeUnit,
  WEEKS: 'WEEKS' as PeriodTimeUnit,
  MONTHS: 'MONTHS' as PeriodTimeUnit,
};
