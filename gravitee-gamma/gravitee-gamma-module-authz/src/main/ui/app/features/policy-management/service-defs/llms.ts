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
import { Brain } from 'lucide-react';
import type { CatalogEntry } from '../../../../lib/api/authz-api.types';
import type { AiProviderEntry } from '../AiModelTargetPickerDialog';
import type { ServicePageConfig } from '../ServicePolicyPage';

/**
 * Build the two-step AI provider/model picker entries from the flat catalog.
 *
 * Convention: the backend catalog for LLM type returns entries where each entry is a provider,
 * and its sub-resources are models (kind = 'LLMModel').
 *
 * If the catalog instead has both providers and individual models as top-level entries,
 * we treat entries with sub-resources as providers and entries without as models of
 * the entry whose id is a prefix of theirs.
 */
export function buildLlmProviders(entries: readonly CatalogEntry[]): readonly AiProviderEntry[] {
    return entries.map(entry => ({
        id: entry.id,
        name: entry.name,
        description: entry.description,
        models: entry.subResources
            .filter(s => s.kind === 'LLMModel' || s.kind === 'model')
            .map(s => ({
                id: s.id,
                name: s.name,
                description: s.description,
            })),
    }));
}

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
    icon: Brain,
    hasTarget: true,
    targetPickerVariant: 'ai-model',
    targetPickerTitle: 'Create AI Model policy',
    targetPickerDescription: 'Pick an AI Provider to cover every model it exposes, or drill down to a specific Model.',
    serviceLabel: 'AI Model',
    resourceGroups: [
        { key: 'LLMProvider', label: 'AI Provider' },
        { key: 'LLMModel', label: 'Model' },
    ],
    conditionSnippets,
    buildProviders: buildLlmProviders,
};
