/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
export type PlanSecurity = 'API_KEY' | 'KEY_LESS' | 'JWT' | 'OAUTH2' | 'MTLS';
export type PlanValidation = 'AUTO' | 'MANUAL';
export type PlanMode = 'STANDARD' | 'PUSH';
export type PeriodTimeUnit = 'SECONDS' | 'MINUTES' | 'HOURS' | 'DAYS' | 'WEEKS' | 'MONTHS';

export interface TimePeriodConfiguration {
    period_time?: number;
    period_time_unit?: PeriodTimeUnit;
    limit?: number;
}

export interface PlanUsageConfiguration {
    rate_limit?: TimePeriodConfiguration;
    quota?: TimePeriodConfiguration;
}

export interface Plan {
    id: string;
    apiId: string;
    name: string;
    security: PlanSecurity;
    description?: string;
    characteristics?: string[];
    validation: PlanValidation;
    order: number;
    comment_required?: boolean;
    comment_question?: string;
    general_conditions?: string;
    mode: PlanMode;
    usage_configuration?: PlanUsageConfiguration;
}

export function getPlanSecurityLabel(security: PlanSecurity | undefined): string {
    switch (security) {
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
}
