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

import { buttonHarness, inputHarness, renderWithGraphene } from '@gravitee/graphene-core/testing';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import { IdentityProviderEditForm } from './IdentityProviderEditForm';
import { notify } from '../../../shared/notify';
import { useUpdateIdentityProvider } from '../hooks/useIdentityProviderMutations';
import type { IdentityProvider } from '../types/identityProvider';

jest.mock('../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

jest.mock('../hooks/useIdentityProviderMutations');

const mockUseUpdateIdentityProvider = jest.mocked(useUpdateIdentityProvider);
const mockOnCancel = jest.fn();

const PROVIDER: IdentityProvider = {
    id: 'google-idp',
    name: 'Google',
    description: 'Google SSO',
    type: 'GOOGLE',
    enabled: true,
    emailRequired: true,
    syncMappings: false,
    configuration: { clientId: 'id', clientSecret: 'secret' },
    groupMappings: [],
    roleMappings: [],
};

const ROLE_MAPPED_PROVIDER: IdentityProvider = {
    ...PROVIDER,
    roleMappings: [
        {
            condition: "{#jsonPath(#profile, '$.job')}",
            organizations: ['ADMIN'],
            environments: { DEFAULT: ['USER'] },
        },
    ],
};

function renderForm({
    canUpdate = true,
    mappingsDisabled = false,
    provider = PROVIDER,
    environments = [{ id: 'DEFAULT', name: 'Default', description: 'Default environment' }],
}: {
    canUpdate?: boolean;
    mappingsDisabled?: boolean;
    provider?: IdentityProvider;
    environments?: { id: string; name: string; description?: string }[];
} = {}) {
    return renderWithGraphene(
        <MemoryRouter initialEntries={['/authentication/google-idp']}>
            <IdentityProviderEditForm
                provider={provider}
                groups={[{ id: 'group-a', name: 'Group A' }]}
                environments={environments}
                organizationRoles={[{ id: 'ADMIN', name: 'ADMIN' }]}
                environmentRoles={[{ id: 'USER', name: 'USER' }]}
                canUpdate={canUpdate}
                mappingsDisabled={mappingsDisabled}
                onCancel={mockOnCancel}
            />
        </MemoryRouter>,
    );
}

describe('IdentityProviderEditForm', () => {
    let mutateAsync: jest.Mock;

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

    beforeEach(() => {
        mutateAsync = jest.fn().mockResolvedValue(PROVIDER);
        mockOnCancel.mockClear();
        mockUseUpdateIdentityProvider.mockReturnValue({ mutateAsync, isPending: false } as ReturnType<typeof useUpdateIdentityProvider>);
        Element.prototype.scrollIntoView = jest.fn();
    });

    it('saves an updated provider', async () => {
        mutateAsync.mockResolvedValue({ ...PROVIDER, name: 'Google SSO' });
        renderForm();
        await inputHarness({ name: /^Name/ }).type(' SSO');
        await buttonHarness({ name: 'Update' }).click();
        await waitFor(() => {
            expect(mutateAsync).toHaveBeenCalledWith(
                expect.objectContaining({
                    name: 'Google SSO',
                    configuration: { clientId: 'id', clientSecret: 'secret' },
                    groupMappings: [],
                    roleMappings: [],
                }),
            );
            expect(mutateAsync.mock.calls[0]?.[0]).not.toHaveProperty('userProfileMapping');
            expect(notify.success).toHaveBeenCalledWith('Identity provider successfully saved!');
        });
        expect(screen.getByRole('button', { name: 'Update' })).toHaveProperty('disabled', true);
        expect(screen.getByLabelText(/^Name/)).toHaveProperty('value', 'Google SSO');
    });

    it('shows configuration, groups mapping, and roles mapping on the same page', () => {
        renderForm();
        expect(screen.getByRole('heading', { name: 'General' })).not.toBeNull();
        expect(screen.getByRole('heading', { name: 'Configuration' })).not.toBeNull();
        expect(screen.getByLabelText(/^Client Id/)).not.toBeNull();
        expect(screen.getByRole('heading', { name: 'Groups Mapping' })).not.toBeNull();
        expect(screen.getByRole('button', { name: 'Add group mapping' })).not.toBeNull();
        expect(screen.getByRole('heading', { name: 'Roles Mapping' })).not.toBeNull();
        expect(screen.getByRole('button', { name: 'Add role mapping' })).not.toBeNull();
        expect(screen.queryByRole('heading', { name: 'User profile mapping' })).toBeNull();
    });

    it('shows user profile mapping on the same page for OpenID Connect', () => {
        renderForm({
            provider: {
                ...PROVIDER,
                id: 'oidc-idp',
                name: 'OIDC',
                type: 'OIDC',
                configuration: {
                    clientId: 'id',
                    clientSecret: 'secret',
                    tokenEndpoint: 'https://idp.example.com/token',
                    authorizeEndpoint: 'https://idp.example.com/authorize',
                    userInfoEndpoint: 'https://idp.example.com/userinfo',
                    scopes: ['openid'],
                },
                userProfileMapping: { id: 'sub' },
            },
        });
        expect(screen.getByRole('heading', { name: 'Configuration' })).not.toBeNull();
        expect(screen.getByRole('heading', { name: 'User profile mapping' })).not.toBeNull();
        expect(screen.getByRole('heading', { name: 'Groups Mapping' })).not.toBeNull();
        expect(screen.getByRole('heading', { name: 'Roles Mapping' })).not.toBeNull();
    });

    it('keeps Update disabled until the form is dirty', () => {
        renderForm();
        expect(screen.getByRole('button', { name: 'Update' })).toHaveProperty('disabled', true);
    });

    it('hides save actions without update permission', () => {
        renderForm({ canUpdate: false });
        expect(screen.queryByRole('button', { name: 'Update' })).toBeNull();
        expect(screen.getByLabelText(/^Name/)).toHaveProperty('disabled', true);
    });

    it('disables configuration fields without update permission', () => {
        renderForm({ canUpdate: false });
        expect(screen.getByLabelText(/^Client Id/)).toHaveProperty('disabled', true);
    });

    it('keeps general and configuration editable when mapping catalogs failed', async () => {
        mutateAsync.mockResolvedValue({ ...PROVIDER, name: 'Google SSO' });
        renderForm({ mappingsDisabled: true });
        expect(screen.getByLabelText(/^Name/)).toHaveProperty('disabled', false);
        expect(screen.getByLabelText(/^Client Secret/)).toHaveProperty('disabled', false);
        expect(screen.getByRole('button', { name: 'Add group mapping' })).toHaveProperty('disabled', true);
        expect(screen.getByRole('button', { name: 'Add role mapping' })).toHaveProperty('disabled', true);
        await inputHarness({ name: /^Name/ }).type(' SSO');
        await buttonHarness({ name: 'Update' }).click();
        await waitFor(() => {
            expect(mutateAsync).toHaveBeenCalledWith(expect.objectContaining({ name: 'Google SSO' }));
        });
    });

    it('requires a condition after adding a group mapping', async () => {
        renderForm();
        await buttonHarness({ name: 'Add group mapping' }).click();
        await buttonHarness({ name: 'Update' }).click();
        expect(await screen.findByText('Condition is required.')).not.toBeNull();
        expect(mutateAsync).not.toHaveBeenCalled();
    });

    it('keeps in-progress edits when the provider object is replaced with the same data', async () => {
        const { rerender } = renderForm();
        await inputHarness({ name: /^Name/ }).type(' SSO');
        rerender(
            <MemoryRouter initialEntries={['/authentication/google-idp']}>
                <IdentityProviderEditForm
                    provider={{ ...PROVIDER }}
                    groups={[{ id: 'group-a', name: 'Group A' }]}
                    environments={[{ id: 'DEFAULT', name: 'Default', description: 'Default environment' }]}
                    organizationRoles={[{ id: 'ADMIN', name: 'ADMIN' }]}
                    environmentRoles={[{ id: 'USER', name: 'USER' }]}
                    canUpdate
                    mappingsDisabled={false}
                    onCancel={mockOnCancel}
                />
            </MemoryRouter>,
        );
        expect(screen.getByLabelText(/^Name/)).toHaveProperty('value', 'Google SSO');
    });

    it('restores the loaded provider when Discard is clicked', async () => {
        renderForm();
        await inputHarness({ name: /^Name/ }).type(' SSO');
        await buttonHarness({ name: 'Discard' }).click();
        expect(screen.getByLabelText(/^Name/)).toHaveProperty('value', 'Google');
        expect(screen.getByRole('button', { name: 'Update' })).toHaveProperty('disabled', true);
    });

    it('saves existing role mappings on update', async () => {
        renderForm({ provider: ROLE_MAPPED_PROVIDER });
        await inputHarness({ name: /^Condition/ }).type('X');
        await buttonHarness({ name: 'Update' }).click();
        await waitFor(() => {
            expect(mutateAsync).toHaveBeenCalledWith(
                expect.objectContaining({
                    roleMappings: [
                        {
                            condition: "{#jsonPath(#profile, '$.job')}X",
                            organizations: ['ADMIN'],
                            environments: { DEFAULT: ['USER'] },
                        },
                    ],
                }),
            );
        });
    });

    it('saves a newly added role mapping with organization and environment roles', async () => {
        const user = userEvent.setup();
        renderForm();
        await buttonHarness({ name: 'Add role mapping' }).click();
        await inputHarness({ name: /^Condition/ }).type('has-admin-job');
        await user.click(screen.getByLabelText(/^Organization roles/));
        await user.click(screen.getByRole('button', { name: 'ADMIN' }));
        await user.keyboard('{Escape}');
        await user.click(screen.getByRole('button', { name: 'Roles' }));
        await user.click(screen.getByRole('button', { name: 'USER' }));
        await user.keyboard('{Escape}');
        await buttonHarness({ name: 'Update' }).click();
        await waitFor(() => {
            expect(mutateAsync).toHaveBeenCalledWith(
                expect.objectContaining({
                    roleMappings: [
                        {
                            condition: 'has-admin-job',
                            organizations: ['ADMIN'],
                            environments: { DEFAULT: ['USER'] },
                        },
                    ],
                }),
            );
        });
    });

    it('allows adding an organization-only role mapping when there are no environments', async () => {
        const user = userEvent.setup();
        renderForm({ environments: [] });
        expect(screen.getByRole('button', { name: 'Add role mapping' })).toHaveProperty('disabled', false);
        await buttonHarness({ name: 'Add role mapping' }).click();
        await inputHarness({ name: /^Condition/ }).type('is-admin');
        await user.click(screen.getByLabelText(/^Organization roles/));
        await user.click(screen.getByRole('button', { name: 'ADMIN' }));
        await user.keyboard('{Escape}');
        await buttonHarness({ name: 'Update' }).click();
        await waitFor(() => {
            expect(mutateAsync).toHaveBeenCalledWith(
                expect.objectContaining({
                    roleMappings: [{ condition: 'is-admin', organizations: ['ADMIN'], environments: {} }],
                }),
            );
        });
    });

    it('asks the page to handle Cancel on a dirty form', async () => {
        renderForm();
        await inputHarness({ name: /^Name/ }).type(' SSO');
        await buttonHarness({ name: 'Cancel' }).click();
        expect(mockOnCancel).toHaveBeenCalled();
    });
});
