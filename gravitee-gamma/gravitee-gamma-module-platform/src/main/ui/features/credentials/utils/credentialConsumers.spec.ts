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
import { credentialConsumerHref, credentialConsumerTypeLabel } from './credentialConsumers';

const ENV = { id: 'env-1-id', hrids: ['env-1'] };

describe('credential consumers', () => {
    it.each([
        ['LLM_PROXY', '/environments/env-1/aim/llm-proxy/proxy%2F1'],
        ['AI_WORKSPACE', '/environments/env-1/aim/ai-workspaces/proxy%2F1'],
    ])('links an AIM %s to its page', (type, href) => {
        expect(credentialConsumerHref(ENV, { module: 'aim', type, id: 'proxy/1', name: 'Proxy' })).toBe(href);
    });

    it('falls back to the environment id when it has no hrid', () => {
        expect(credentialConsumerHref({ id: 'env-1-id' }, { module: 'aim', type: 'LLM_PROXY', id: 'p1', name: 'Proxy' })).toBe(
            '/environments/env-1-id/aim/llm-proxy/p1',
        );
    });

    it('gives no link to a consumer it does not know', () => {
        expect(credentialConsumerHref(ENV, { module: 'apim', type: 'API', id: 'api-1', name: 'API' })).toBeUndefined();
    });

    it('labels known types and humanizes the others', () => {
        expect(credentialConsumerTypeLabel('LLM_PROXY')).toBe('LLM proxy');
        expect(credentialConsumerTypeLabel('AI_WORKSPACE')).toBe('AI workspace');
        expect(credentialConsumerTypeLabel('WORKFLOW_TOOL')).toBe('Workflow tool');
    });
});
