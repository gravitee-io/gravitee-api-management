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
import { federatedProviderLabel } from './federatedProviderLabels';

describe('federatedProviderLabel', () => {
    it.each<[string, string]>([
        ['has no map entry', 'mycompany-gateway'],
        // A bare object index would resolve this key on the prototype and hand back a function instead.
        ['collides with an Object.prototype member', 'toString'],
        // Both spellings occur in the data, so the alias is a second key — not a case-insensitive lookup.
        ['differs from the AWS alias only in case', 'Aws'],
        ['drops a hyphen from the aws-api-gateway key', 'aws-apigateway'],
        ['spells the solace key as its display name', 'Solace'],
        ['extends the apigee key with a suffix', 'apigee-x'],
        ['shortens the azure-api-management key', 'azure'],
        ['shortens the ibm-api-connect key', 'ibm'],
        ['shortens the confluent-platform key', 'confluent'],
        ['misspells the MuleSoft display name', 'Mulesoft'],
        ['drops the hyphen from the edge-stack key', 'edgestack'],
    ])('returns a provider code that %s verbatim', (_scenario, provider) => {
        expect(federatedProviderLabel(provider)).toBe(provider);
    });

    it('maps the uppercase AWS alias to the same label as aws-api-gateway', () => {
        expect(federatedProviderLabel('AWS')).toBe(federatedProviderLabel('aws-api-gateway'));
        expect(federatedProviderLabel('AWS')).toBe('AWS API Gateway');
    });
});
