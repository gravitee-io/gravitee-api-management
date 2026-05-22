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
import { GlobeIcon } from '@gravitee/graphene-core/icons';
import type { ServicePageConfig } from '../ServicePolicyPage';

const conditionSnippets: readonly { label: string; snippet: string }[] = [
    { label: 'Corporate IP range', snippet: 'context.source.ip.in_cidr("10.0.0.0/8")' },
    { label: 'Scope present', snippet: 'context.auth.scopes.contains("orders:read")' },
    { label: 'Rate < 100/min', snippet: 'context.rate.per_minute(principal) < 100' },
    { label: 'Tenant match', snippet: 'context.request.header.x_tenant == principal.tenant' },
];

export const apisServiceConfig: ServicePageConfig = {
    type: 'API',
    title: 'API Policies',
    description: 'Control which principals can reach each API and its endpoints, and what data fields they may see.',
    createButtonLabel: 'Create Policy for API',
    searchPlaceholder: 'Search API policies…',
    icon: GlobeIcon,
    hasTarget: true,
    serviceLabel: 'API',
    resourceGroups: [
        { key: 'API', label: 'API' },
        { key: 'Endpoint', label: 'Endpoints' },
        { key: 'DataField', label: 'Data Fields' },
    ],
    conditionSnippets,
};
