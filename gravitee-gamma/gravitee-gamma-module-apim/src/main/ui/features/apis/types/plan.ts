/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import type { EntityContext } from '../../../shared/types/entityContext';

export type PlanContext = EntityContext;

export type PlanSecurityType = 'API_KEY' | 'JWT' | 'OAUTH2' | 'MTLS' | 'KEY_LESS';
export type PlanStatus = 'STAGING' | 'PUBLISHED' | 'DEPRECATED' | 'CLOSED';
export type PlanValidation = 'AUTO' | 'MANUAL';
export type PlanTransitionAction = 'publish' | 'deprecate' | 'close';

export interface PolicyStep {
    policy: string;
    enabled: boolean;
    configuration: Record<string, unknown>;
}

export interface PolicyFlow {
    enabled: boolean;
    selectors?: Record<string, unknown>[];
    request?: PolicyStep[];
    response?: PolicyStep[];
}

export interface PlanSecurity {
    type: PlanSecurityType;
    configuration?: Record<string, unknown>;
}

export interface ManagedPlan {
    id: string;
    name: string;
    description?: string;
    status: PlanStatus;
    security: PlanSecurity;
    selectionRule?: string;
    order: number;
    validation: PlanValidation;
    characteristics?: string[];
    generalConditions?: string;
    commentRequired?: boolean;
    commentMessage?: string;
    excludedGroups?: string[];
    tags?: string[];
    flows?: PolicyFlow[];
    definitionVersion?: string;
    mode?: 'STANDARD' | 'PUSH';
}

export interface ManagedPlanPage {
    data: ManagedPlan[];
    pagination: { totalCount: number; pageIndex: number; pageSize: number };
}

// ─── Form shape used in the wizard ────────────────────────────────────────────

export interface ResourceFilteringRule {
    whitelist: boolean;
    pattern: string;
    methods: string[];
}

export interface GeneralFormData {
    name: string;
    description: string;
    characteristics: string[];
    generalConditions: string;
    autoValidation: boolean;
    commentRequired: boolean;
    commentMessage: string;
    excludedGroups: string[];
    tags: string[];
}

export interface SecurityFormData {
    configuration: Record<string, unknown>;
    selectionRule: string;
}

export type RateLimitErrorStrategy = 'BLOCK_ON_INTERNAL_ERROR' | 'FALLBACK_PASS_TROUGH';

export interface RateLimitFormData {
    errorStrategy: RateLimitErrorStrategy;
    async: boolean;
    addHeaders: boolean;
    key: string;
    useKeyOnly: boolean;
    max: number;
    dynamicLimit: string;
    period: number;
    unit: 'SECONDS' | 'MINUTES';
    dynamicPeriodTime: string;
}

export interface QuotaFormData {
    errorStrategy: RateLimitErrorStrategy;
    async: boolean;
    addHeaders: boolean;
    key: string;
    useKeyOnly: boolean;
    max: number;
    dynamicLimit: string;
    period: number;
    unit: 'HOURS' | 'DAYS' | 'WEEKS' | 'MONTHS';
    dynamicPeriodTime: string;
}

/**
 * Token budget for AI/LLM product plans, enforced by the `token-ratelimit` policy.
 * UI units are user-friendly; the transformer converts HOURS/DAYS to MINUTES
 * (the policy only supports SECONDS/MINUTES).
 */
export interface TokenBudgetFormData {
    limit: number;
    period: number;
    unit: 'MINUTES' | 'HOURS' | 'DAYS';
}

export interface RestrictionsFormData {
    rateLimitEnabled: boolean;
    rateLimit: RateLimitFormData;
    quotaEnabled: boolean;
    quota: QuotaFormData;
    tokenBudgetEnabled: boolean;
    tokenBudget: TokenBudgetFormData;
    resourceFilteringEnabled: boolean;
    resourceFiltering: ResourceFilteringRule[];
    normalizeRequestPath: boolean;
    decodeEncodedSlash: boolean;
}

export interface PlanFormValue {
    securityType: PlanSecurityType;
    general: GeneralFormData;
    security: SecurityFormData;
    restrictions: RestrictionsFormData;
}

export const PLAN_TYPES_BY_CTX: Record<PlanContext['type'], PlanSecurityType[]> = {
    api: ['API_KEY', 'JWT', 'OAUTH2', 'MTLS', 'KEY_LESS'],
    'api-product': ['API_KEY', 'JWT', 'MTLS'],
};

export const PLAN_SECURITY_LABELS: Record<PlanSecurityType, string> = {
    API_KEY: 'API Key',
    JWT: 'JWT',
    OAUTH2: 'OAuth2',
    MTLS: 'mTLS',
    KEY_LESS: 'Keyless',
};

export const EMPTY_GENERAL: GeneralFormData = {
    name: '',
    description: '',
    characteristics: [],
    generalConditions: '',
    autoValidation: false,
    commentRequired: false,
    commentMessage: '',
    excludedGroups: [],
    tags: [],
};

export const EMPTY_SECURITY: SecurityFormData = {
    configuration: {},
    selectionRule: '',
};

export const EMPTY_RESTRICTIONS: RestrictionsFormData = {
    rateLimitEnabled: false,
    rateLimit: {
        errorStrategy: 'FALLBACK_PASS_TROUGH',
        async: false,
        addHeaders: false,
        key: '',
        useKeyOnly: false,
        max: 10,
        dynamicLimit: '',
        period: 1,
        unit: 'SECONDS',
        dynamicPeriodTime: '',
    },
    quotaEnabled: false,
    quota: {
        errorStrategy: 'FALLBACK_PASS_TROUGH',
        async: false,
        addHeaders: true,
        key: '',
        useKeyOnly: false,
        max: 100,
        dynamicLimit: '',
        period: 1,
        unit: 'HOURS',
        dynamicPeriodTime: '',
    },
    tokenBudgetEnabled: false,
    tokenBudget: {
        limit: 100000,
        period: 1,
        unit: 'DAYS',
    },
    resourceFilteringEnabled: false,
    resourceFiltering: [],
    normalizeRequestPath: false,
    decodeEncodedSlash: false,
};

export const ALL_HTTP_METHODS = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS', 'HEAD', 'TRACE', 'CONNECT'] as const;
export type HttpMethod = (typeof ALL_HTTP_METHODS)[number];
