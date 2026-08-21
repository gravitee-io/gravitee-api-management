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

import { buttonHarness, renderWithGraphene } from '@gravitee/graphene-core/testing';
import { screen } from '@testing-library/react';

import { IdentityProviderRoleMappings } from './IdentityProviderRoleMappings';

describe('IdentityProviderRoleMappings', () => {
    beforeAll(() => {
        Object.defineProperty(window, 'matchMedia', {
            writable: true,
            value: jest.fn().mockImplementation((query: string) => ({
                matches: false,
                media: query,
                onchange: null,
                addListener: jest.fn(),
                removeListener: jest.fn(),
                addEventListener: jest.fn(),
                removeEventListener: jest.fn(),
                dispatchEvent: jest.fn(),
            })),
        });
        global.ResizeObserver = class ResizeObserver {
            observe() {}
            unobserve() {}
            disconnect() {}
        } as typeof ResizeObserver;
    });

    it('lets the user add an organization-only role mapping when there are no environments', async () => {
        const onChange = jest.fn();
        renderWithGraphene(
            <IdentityProviderRoleMappings
                mappings={[]}
                environments={[]}
                organizationRoles={[{ id: 'ADMIN', name: 'ADMIN' }]}
                environmentRoles={[]}
                showErrors={false}
                errors={{}}
                disabled={false}
                onChange={onChange}
            />,
        );

        expect(screen.getByRole('button', { name: 'Add role mapping' })).toHaveProperty('disabled', false);
        await buttonHarness({ name: 'Add role mapping' }).click();
        expect(onChange).toHaveBeenCalledWith([{ condition: '', organizations: [], environments: {} }]);
    });
});
