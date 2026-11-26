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
import { buildModuleNavPath } from '@gravitee/gamma-modules-sdk/routing';

import type { CredentialConsumer, CredentialConsumerWarning } from '../types/credential';

const TYPE_LABELS: Readonly<Record<string, string>> = {
    LLM_PROXY: 'LLM proxy',
    AI_WORKSPACE: 'AI workspace',
};

const CONSUMER_ROUTES: Readonly<Record<string, (id: string) => string>> = {
    'aim:LLM_PROXY': id => `llm-proxy/${id}`,
    'aim:AI_WORKSPACE': id => `ai-workspaces/${id}`,
};

export function credentialConsumerHref(consumer: CredentialConsumer, currentPathname: string): string | undefined {
    const route = CONSUMER_ROUTES[`${consumer.module}:${consumer.type}`];
    return route ? buildModuleNavPath(consumer.module, route(encodeURIComponent(consumer.id)), currentPathname) : undefined;
}

export function consumerWarningsMessage(warnings: readonly CredentialConsumerWarning[] | undefined): string | undefined {
    if (!warnings?.length) {
        return undefined;
    }
    return warnings.map(warning => `${warning.name} kept the former settings of the credential: ${warning.message}`).join(' ');
}

export function credentialConsumerTypeLabel(type: string): string {
    const label = TYPE_LABELS[type];
    if (label) {
        return label;
    }
    const words = type.toLowerCase().replaceAll('_', ' ');
    return words.charAt(0).toUpperCase() + words.slice(1);
}
