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
import { SlidersHorizontalIcon } from '@gravitee/graphene-core/icons';
import type { ServicePageConfig } from '../ServicePolicyPage';

const conditionSnippets: readonly { label: string; snippet: string }[] = [
    { label: 'Corporate IP range', snippet: 'context.source.ip.in_cidr("10.0.0.0/8")' },
    { label: 'MFA required', snippet: 'context.auth.mfa == true' },
    { label: 'Business hours', snippet: 'context.time.hour >= 9 && context.time.hour < 17' },
    { label: 'Owner only', snippet: 'resource.owner == principal' },
];

export const customServiceConfig: ServicePageConfig = {
    type: 'CUSTOM',
    title: 'Custom Policies',
    description:
        'Write policies against anything that is not already routed as an MCP, API, Agent, LLM or Event — internal applications, data assets, and bespoke resources.',
    createButtonLabel: 'Create Custom Policy',
    searchPlaceholder: 'Search custom policies…',
    icon: SlidersHorizontalIcon,
    hasTarget: false,
    serviceLabel: 'Custom',
    conditionSnippets,
};
