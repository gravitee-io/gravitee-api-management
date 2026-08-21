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

import { filterIdentityProviders, sortIdentityProviderRows, toIdentityProviderRows } from './identityProviderTableUtils';
import type { IdentityProviderListItem, IdentityProviderRow } from '../types/identityProvider';

const GOOGLE: IdentityProviderListItem = {
    id: 'google-idp',
    name: 'Google',
    description: 'Google SSO',
    enabled: true,
    sync: false,
    type: 'GOOGLE',
    created_at: 1,
    updated_at: 1,
};

const AM: IdentityProviderListItem = {
    id: 'gravitee-am',
    name: 'Gravitee.io AM',
    description: 'AM',
    enabled: true,
    sync: true,
    type: 'GRAVITEEIO_AM',
    created_at: 1,
    updated_at: 1,
};

describe('identityProviderTableUtils', () => {
    it('sorts Gravitee AM first then by id', () => {
        expect(sortIdentityProviderRows(toIdentityProviderRows([GOOGLE, AM], [])).map(row => row.id)).toEqual([
            'gravitee-am',
            'google-idp',
        ]);
    });

    it('marks activated providers from the activation list', () => {
        const rows = toIdentityProviderRows([GOOGLE, AM], [{ identityProvider: 'google-idp' }]);
        expect(rows.find(row => row.id === 'google-idp')?.activated).toBe(true);
        expect(rows.find(row => row.id === 'gravitee-am')?.activated).toBe(false);
    });

    it('does not mark activation state when activations have not loaded', () => {
        const rows = toIdentityProviderRows([GOOGLE, AM], undefined);
        expect(rows.find(row => row.id === 'google-idp')?.activated).toBeUndefined();
        expect(filterIdentityProviders(rows, 'activated')).toEqual([]);
        expect(filterIdentityProviders(rows, 'deactivated')).toEqual([]);
    });

    it('filters by id, name, type, description, or status', () => {
        const rows: IdentityProviderRow[] = toIdentityProviderRows([GOOGLE, AM], [{ identityProvider: 'gravitee-am' }]);
        expect(filterIdentityProviders(rows, 'google').map(row => row.id)).toEqual(['google-idp']);
        expect(filterIdentityProviders(rows, 'activated').map(row => row.id)).toEqual(['gravitee-am']);
        expect(filterIdentityProviders(rows, 'deactivated').map(row => row.id)).toEqual(['google-idp']);
        expect(filterIdentityProviders(rows, 'openid')).toEqual([]);
        expect(filterIdentityProviders(rows, '  ').map(row => row.id)).toEqual(['gravitee-am', 'google-idp']);
    });
});
