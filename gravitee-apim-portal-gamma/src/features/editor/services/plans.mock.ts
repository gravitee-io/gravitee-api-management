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
import type { Plan } from '../entities/plan';

const MOCK_PLANS: Plan[] = [
    {
        id: 'plan-payments-key',
        apiId: 'api-payments',
        name: 'Standard API Key',
        security: 'API_KEY',
        description: 'Access with a dedicated API key. Auto-approved.',
        validation: 'AUTO',
        order: 1,
        mode: 'STANDARD',
        usage_configuration: {
            rate_limit: { limit: 100, period_time: 1, period_time_unit: 'SECONDS' },
        },
    },
    {
        id: 'plan-payments-oauth',
        apiId: 'api-payments',
        name: 'OAuth2 Partner',
        security: 'OAUTH2',
        description: 'OAuth2 client credentials for partner integrations.',
        validation: 'MANUAL',
        order: 2,
        mode: 'STANDARD',
    },
    {
        id: 'plan-payments-keyless',
        apiId: 'api-payments',
        name: 'Public Sandbox',
        security: 'KEY_LESS',
        description: 'Open access for sandbox testing — no subscription required.',
        validation: 'AUTO',
        order: 3,
        mode: 'STANDARD',
    },
    {
        id: 'plan-accounts-key',
        apiId: 'api-accounts',
        name: 'Developer API Key',
        security: 'API_KEY',
        description: 'Standard developer access with API key authentication.',
        validation: 'AUTO',
        order: 1,
        mode: 'STANDARD',
        usage_configuration: {
            quota: { limit: 10000, period_time: 1, period_time_unit: 'DAYS' },
        },
    },
    {
        id: 'plan-accounts-push',
        apiId: 'api-accounts',
        name: 'Account Events Webhook',
        security: 'API_KEY',
        description: 'Push account balance changes to your webhook endpoint.',
        validation: 'AUTO',
        order: 2,
        mode: 'PUSH',
    },
    {
        id: 'plan-notifications-key',
        apiId: 'api-notifications',
        name: 'Messaging API Key',
        security: 'API_KEY',
        description: 'Send notifications via API key.',
        validation: 'AUTO',
        order: 1,
        mode: 'STANDARD',
    },
    {
        id: 'plan-identity-oauth',
        apiId: 'api-identity',
        name: 'Identity OAuth2',
        security: 'OAUTH2',
        description: 'OAuth2 access to identity services.',
        validation: 'MANUAL',
        order: 1,
        mode: 'STANDARD',
    },
];

export async function getPlansForApi(apiId: string): Promise<Plan[]> {
    return MOCK_PLANS.filter(plan => plan.apiId === apiId).sort((a, b) => a.order - b.order);
}

export async function getPlanById(planId: string): Promise<Plan | undefined> {
    return MOCK_PLANS.find(plan => plan.id === planId);
}
