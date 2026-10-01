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

/**
 * Placeholder account profile until Cloud MAPI is wired into Gamma.
 * Mirrors Cockpit `Account.customer` flag driving trial vs gateways sections.
 */
export const CLOUD_ACCOUNT_PROFILE = {
    customer: false,
    trialDaysRemaining: 30,
    /** Mock permission until Cloud MAPI is connected. */
    canCreateEnvironment: true,
    canDeployGateway: true,
} as const;

export const CLOUD_ENVIRONMENT_NAME_MIN_LENGTH = 2;
export const CLOUD_ENVIRONMENT_NAME_MAX_LENGTH = 128;
export const CLOUD_ENVIRONMENT_HRID_MIN_LENGTH = 2;
export const CLOUD_ENVIRONMENT_HRID_MAX_LENGTH = 16;

export type CloudProduct = 'APIM' | 'AM';

export const CLOUD_PRODUCT_OPTIONS: readonly CloudProduct[] = ['APIM', 'AM'];

/** Mock account for Cloud general settings UI until Cockpit/Cloud API is connected. */
export const CLOUD_ACCOUNT_MOCK = {
    id: '95cb6306-405c-4c87-8b63-06405c9c87eb',
    name: 'Gravitee',
    description: '',
    hrid: 'gravitee-1773323333030',
    /** Cloud accounts cannot change HRID in Cockpit (self-hosted only). */
    canChangeHrid: false,
} as const;

export type CustomReporterStatus = 'active' | 'not-linked';

export interface CustomReporter {
    readonly id: string;
    readonly name: string;
    readonly type: string;
    readonly configuration: string;
    readonly outputFormat: string;
    readonly status: CustomReporterStatus;
    readonly enabled: boolean;
}

/** Mock custom reporters until Cloud MAPI is connected. */
export const CLOUD_CUSTOM_REPORTERS_MOCK: readonly CustomReporter[] = [
    {
        id: 'reporter-1',
        name: 'Config 1',
        type: 'TCP Reporter',
        configuration: 'cbgzt-2a09-bac6-406c-277d--3ef-cf.run.pinggy-free.link:44799',
        outputFormat: 'JSON',
        status: 'active',
        enabled: true,
    },
    {
        id: 'reporter-2',
        name: 'Config 2',
        type: 'TCP Reporter',
        configuration: '1.2.3.4:1234',
        outputFormat: 'JSON',
        status: 'not-linked',
        enabled: true,
    },
] as const;

export const CUSTOM_REPORTERS_DOCS_URL = 'https://documentation.gravitee.io/';

export const MAX_ACCOUNT_TOKENS = 10;

export interface AccountToken {
    readonly id: string;
    readonly tokenName: string;
    readonly createdAt: string;
    readonly creatorUserName: string;
}

/** Empty by default to match Cockpit empty state until Cloud API is connected. */
export const CLOUD_ACCOUNT_TOKENS_MOCK: readonly AccountToken[] = [];

export interface CloudToken {
    readonly id: string;
    readonly name: string;
    readonly use: string;
    readonly environmentName: string;
    readonly createdAt: string;
    readonly creatorName: string;
}

/** Empty by default to match Cockpit empty state until Cloud API is connected. */
export const CLOUD_CLOUD_TOKENS_MOCK: readonly CloudToken[] = [];

/** SSO not configured by default (Cockpit creation mode). */
export const CLOUD_SSO_MOCK = {
    configured: false,
    enabled: false,
} as const;

export interface AccountMember {
    readonly id: string;
    readonly name: string;
    readonly email: string;
    readonly role: string;
    /** When true, member cannot be edited or removed (e.g. primary owner). */
    readonly readonly?: boolean;
}

/** Mock members until Cloud API is connected. */
export const CLOUD_ACCOUNT_MEMBERS_MOCK: readonly AccountMember[] = [
    {
        id: 'member-1',
        name: 'Karen Rai',
        email: 'karen.rai@graviteesource.com',
        role: 'CLOUD_ACCOUNT_OWNER',
    },
    {
        id: 'member-2',
        name: 'varun goel',
        email: 'aayaangoel10+6@gmail.com',
        role: 'CLOUD_ACCOUNT_OWNER',
    },
    {
        id: 'member-3',
        name: 'varun goel',
        email: 'aayaangoel10+1@gmail.com',
        role: 'ACCOUNT_PRIMARY_OWNER',
        readonly: true,
    },
    {
        id: 'member-4',
        name: 'Varun Goel',
        email: 'varun.goel@graviteesource.com',
        role: 'CLOUD_ACCOUNT_OWNER',
        readonly: true,
    },
];

export interface AccountMemberRole {
    readonly id: string;
    readonly name: string;
}

/** Assignable account member roles until Cloud API is connected. */
export const CLOUD_ACCOUNT_MEMBER_ROLES_MOCK: readonly AccountMemberRole[] = [
    { id: 'role-cloud-account-owner', name: 'CLOUD_ACCOUNT_OWNER' },
];

export type CloudGatewayConnectionStatus = 'connected' | 'disconnected' | 'unknown';

export interface CloudGateway {
    readonly id: string;
    readonly name: string;
    readonly configuration: string;
    readonly provider?: string;
    readonly region?: string;
    readonly version?: string;
    readonly environmentName: string;
    readonly connectionStatus: CloudGatewayConnectionStatus;
}

/** Mock gateways until Cloud MAPI is connected. */
export const CLOUD_GATEWAYS_MOCK: readonly CloudGateway[] = [
    {
        id: 'gateway-1',
        name: 'Development AM',
        configuration: 'Gravitee Hosted',
        provider: 'Azure',
        region: 'westeurope',
        version: '4.11.13',
        environmentName: 'Development AM',
        connectionStatus: 'connected',
    },
    {
        id: 'gateway-2',
        name: 'Staging Gateway',
        configuration: 'Gravitee Hosted',
        provider: 'Azure',
        region: 'eastus',
        version: '4.11.12',
        environmentName: 'Test',
        connectionStatus: 'disconnected',
    },
];
