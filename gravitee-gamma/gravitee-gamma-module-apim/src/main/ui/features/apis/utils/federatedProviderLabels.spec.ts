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
    it('returns a provider code with no map entry verbatim', () => {
        expect(federatedProviderLabel('mycompany-gateway')).toBe('mycompany-gateway');
    });

    it('returns a provider code colliding with an Object.prototype member verbatim', () => {
        // A bare object index would resolve this key on the prototype and hand back a function instead.
        expect(federatedProviderLabel('toString')).toBe('toString');
    });

    it('maps the uppercase AWS alias to the same label as aws-api-gateway', () => {
        expect(federatedProviderLabel('AWS')).toBe(federatedProviderLabel('aws-api-gateway'));
        expect(federatedProviderLabel('AWS')).toBe('AWS API Gateway');
    });

    it('resolves the AWS alias case-sensitively rather than normalizing case', () => {
        // Both spellings occur in the data, so the alias is a second key — not a case-insensitive lookup.
        expect(federatedProviderLabel('Aws')).toBe('Aws');
    });
});
