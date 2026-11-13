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
import { primaryEnvironmentSegment } from '../../users/utils/crossModuleResourcePath';
import type { CredentialConsumer } from '../types/credential';

const TYPE_LABELS: Readonly<Record<string, string>> = {
    LLM_PROXY: 'LLM proxy',
    AI_WORKSPACE: 'AI workspace',
};

// Console paths of the consumer pages in other modules, keyed by `<module>:<type>`. A consumer
// without an entry shows as plain text.
const CONSUMER_PATHS: Readonly<Record<string, (id: string) => string>> = {
    'aim:LLM_PROXY': id => `aim/llm-proxy/${id}`,
    'aim:AI_WORKSPACE': id => `aim/ai-workspaces/${id}`,
};

export function credentialConsumerHref(
    environment: { id: string; hrids?: readonly string[] },
    consumer: CredentialConsumer,
): string | undefined {
    const path = CONSUMER_PATHS[`${consumer.module}:${consumer.type}`];
    const segment = encodeURIComponent(primaryEnvironmentSegment(environment));
    return path ? `/environments/${segment}/${path(encodeURIComponent(consumer.id))}` : undefined;
}

export function credentialConsumerTypeLabel(type: string): string {
    const label = TYPE_LABELS[type];
    if (label) {
        return label;
    }
    const words = type.toLowerCase().replaceAll('_', ' ');
    return words.charAt(0).toUpperCase() + words.slice(1);
}
