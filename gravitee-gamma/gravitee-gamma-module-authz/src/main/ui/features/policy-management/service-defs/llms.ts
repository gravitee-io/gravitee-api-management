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
import { BrainIcon } from '@gravitee/graphene-core/icons';
import type { ServicePageConfig } from '../ServicePolicyPage';

const conditionSnippets: readonly { label: string; snippet: string }[] = [
    { label: 'Token budget', snippet: 'context.usage.tokens_per_day(principal) < 50000' },
    { label: 'Cost ceiling', snippet: 'context.usage.cost_per_day(principal) < 100' },
    { label: 'PII filter on', snippet: 'context.guardrails.pii == true' },
    { label: 'Model size small', snippet: 'resource.size in ["small", "medium"]' },
];

export const llmsServiceConfig: ServicePageConfig = {
    type: 'LLM',
    title: 'AI Model Policies',
    description: 'Control who can invoke each AI Provider or specific Model, and under what usage or cost ceilings.',
    createButtonLabel: 'Create Policy for AI Models',
    searchPlaceholder: 'Search AI Model policies…',
    icon: BrainIcon,
    hasTarget: true,
    serviceLabel: 'AI Model',
    resourceGroups: [
        { key: 'LLMProvider', label: 'AI Provider' },
        { key: 'LLMModel', label: 'Model' },
    ],
    conditionSnippets,
};
