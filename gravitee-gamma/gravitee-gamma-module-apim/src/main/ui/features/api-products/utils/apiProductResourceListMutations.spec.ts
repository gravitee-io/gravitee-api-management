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
import {
    removeProductResourceApply,
    toggleProductResourceApply,
    upsertProductResourceApply,
} from './apiProductResourceListMutations';
import { ApimApiError } from '../../../shared/api/apimClient';
import type { ApiResource } from '../../apis/types/resource';

const RESOURCES: ApiResource[] = [
    { name: 'cache-a', type: 'cache', enabled: true, configuration: {} },
    { name: 'oauth', type: 'oauth2', enabled: false, configuration: { clientId: 'x' } },
];

describe('apiProductResourceListMutations', () => {
    describe('toggleProductResourceApply', () => {
        it('toggles enabled on the named resource', () => {
            const apply = toggleProductResourceApply('cache-a');
            expect(apply(RESOURCES)).toEqual([
                { name: 'cache-a', type: 'cache', enabled: false, configuration: {} },
                { name: 'oauth', type: 'oauth2', enabled: false, configuration: { clientId: 'x' } },
            ]);
        });

        it('throws 409 when the resource was removed', () => {
            expect(() => toggleProductResourceApply('missing')(RESOURCES)).toThrow(ApimApiError);
            try {
                toggleProductResourceApply('missing')(RESOURCES);
            } catch (error) {
                expect(error).toMatchObject({ status: 409 });
            }
        });
    });

    describe('removeProductResourceApply', () => {
        it('removes the named resource', () => {
            const apply = removeProductResourceApply('oauth');
            expect(apply(RESOURCES)).toEqual([{ name: 'cache-a', type: 'cache', enabled: true, configuration: {} }]);
        });

        it('throws 409 when the resource was removed', () => {
            expect(() => removeProductResourceApply('missing')(RESOURCES)).toThrow(ApimApiError);
        });
    });

    describe('upsertProductResourceApply', () => {
        it('appends a resource when creating', () => {
            const next: ApiResource = { name: 'new-cache', type: 'cache', enabled: true, configuration: {} };
            expect(upsertProductResourceApply(next)(RESOURCES)).toEqual([...RESOURCES, next]);
        });

        it('rejects duplicate names when creating', () => {
            const next: ApiResource = { name: 'cache-a', type: 'cache', enabled: true, configuration: {} };
            expect(() => upsertProductResourceApply(next)(RESOURCES)).toThrow(ApimApiError);
            try {
                upsertProductResourceApply(next)(RESOURCES);
            } catch (error) {
                expect(error).toMatchObject({ status: 400, message: 'A resource with this name already exists.' });
            }
        });

        it('replaces a resource when editing', () => {
            const updated: ApiResource = {
                name: 'cache-renamed',
                type: 'cache',
                enabled: true,
                configuration: { ttl: 60 },
            };
            expect(upsertProductResourceApply(updated, 'cache-a')(RESOURCES)).toEqual([updated, RESOURCES[1]!]);
        });

        it('rejects renaming to an existing name', () => {
            const updated: ApiResource = { name: 'oauth', type: 'cache', enabled: true, configuration: {} };
            expect(() => upsertProductResourceApply(updated, 'cache-a')(RESOURCES)).toThrow(ApimApiError);
        });

        it('throws 409 when the edited resource was removed', () => {
            const updated: ApiResource = { name: 'cache-a', type: 'cache', enabled: true, configuration: {} };
            expect(() => upsertProductResourceApply(updated, 'missing')(RESOURCES)).toThrow(ApimApiError);
        });
    });
});
