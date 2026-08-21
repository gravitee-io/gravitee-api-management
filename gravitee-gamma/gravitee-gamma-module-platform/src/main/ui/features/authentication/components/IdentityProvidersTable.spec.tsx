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

import { buttonHarness, dataTableHarness, inputHarness, renderWithGraphene } from '@gravitee/graphene-core/testing';
import { fireEvent, screen } from '@testing-library/react';

import { IdentityProvidersTable } from './IdentityProvidersTable';
import type { IdentityProviderRow } from '../types/identityProvider';

const ROWS: IdentityProviderRow[] = [
    {
        id: 'gravitee-am',
        name: 'Gravitee.io AM',
        description: 'Organization AM',
        enabled: true,
        sync: true,
        type: 'GRAVITEEIO_AM',
        activated: true,
        created_at: Date.parse('2026-07-27T14:30:00Z'),
        updated_at: Date.parse('2026-07-27T14:30:00Z'),
    },
    {
        id: 'google-idp',
        name: 'Google',
        description: 'Google SSO',
        enabled: true,
        sync: false,
        type: 'GOOGLE',
        activated: false,
        created_at: Date.parse('2026-07-27T14:30:00Z'),
        updated_at: Date.parse('2026-07-27T14:30:00Z'),
    },
];

function identityProvidersTable() {
    return dataTableHarness({ within: screen.getByRole('region', { name: 'Identity Providers' }) });
}

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

describe('IdentityProvidersTable', () => {
    it('renders name, callback id, status, and type', () => {
        renderWithGraphene(<IdentityProvidersTable rows={ROWS} />);
        const table = identityProvidersTable();
        expect(table.getHeaders().slice(0, 4)).toEqual(['Name', 'Id', 'Status', 'Type']);
        expect(table.getRow('Gravitee.io AM').getCellText('Name')).toBe('Gravitee.io AM');
        expect(table.getRow('Google SSO').getCellText('Name')).toBe('Google');
        expect(table.getRow('Google SSO').getCellText('Id')).toBe('google-idp');
        expect(table.getRow('Gravitee.io AM').getCellText('Id')).toBe('gravitee-am');
        expect(screen.queryByRole('link', { name: 'Google' })).toBeNull();
        expect(table.getRow('Gravitee.io AM').getCellText('Type')).toBe('Gravitee.io AM');
        expect(table.getRow('Google SSO').getCellText('Type')).toBe('Google');
        expect(table.getRow('Google SSO').getCellElement('Type').querySelector('svg')).toBeNull();
        expect(table.getRow('Google SSO').getCellText('Status')).toBe('Deactivated');
        expect(table.getRow('Gravitee.io AM').getCellText('Status')).toBe('Activated');
        expect(screen.getByText('Synced')).not.toBeNull();
        expect(screen.getAllByText('Available on developer portal')).toHaveLength(2);
    });

    it('filters rows by search query', async () => {
        renderWithGraphene(<IdentityProvidersTable rows={ROWS} />);
        await inputHarness({ name: 'Search identity providers' }).type('google');
        const table = identityProvidersTable();
        expect(table.queryRow('Organization AM')).toBeNull();
        expect(table.getRow('Google SSO').getCellText('Name')).toBe('Google');
    });

    it('filters rows by callback id', async () => {
        renderWithGraphene(<IdentityProvidersTable rows={ROWS} />);
        await inputHarness({ name: 'Search identity providers' }).type('google-idp');
        const table = identityProvidersTable();
        expect(table.queryRow('Organization AM')).toBeNull();
        expect(table.getRow('Google SSO').getCellText('Id')).toBe('google-idp');
    });

    it('clears the search from the no-results empty state', async () => {
        renderWithGraphene(<IdentityProvidersTable rows={ROWS} />);
        await inputHarness({ name: 'Search identity providers' }).type('nobody');
        expect(identityProvidersTable().isEmpty()).toBe(true);
        expect(screen.getByText('No identity providers found')).not.toBeNull();
        await buttonHarness({ name: 'Clear search' }).click();
        expect(identityProvidersTable().getRow('Gravitee.io AM').getCellText('Name')).toBe('Gravitee.io AM');
    });

    it('calls onToggle when Activate is selected', async () => {
        const onToggle = jest.fn();
        renderWithGraphene(<IdentityProvidersTable rows={ROWS} canActivate onToggle={onToggle} />);
        await buttonHarness({ name: /Actions for Google/i }).click();
        (await screen.findByRole('menuitem', { name: /^Activate$/ })).click();
        expect(onToggle).toHaveBeenCalledWith(ROWS[1]);
    });

    it('calls onDelete when Delete is selected', async () => {
        const onDelete = jest.fn();
        renderWithGraphene(<IdentityProvidersTable rows={ROWS} canDelete onDelete={onDelete} />);
        await buttonHarness({ name: /Actions for Gravitee.io AM/i }).click();
        (await screen.findByRole('menuitem', { name: /^Delete$/ })).click();
        expect(onDelete).toHaveBeenCalledWith(ROWS[0]);
    });

    it('hides the actions menu when the user cannot activate or delete', () => {
        renderWithGraphene(<IdentityProvidersTable rows={ROWS} />);
        expect(screen.queryByRole('button', { name: /Actions for Google/i })).toBeNull();
    });

    it('shows an unknown status instead of deactivated when activation state is missing', () => {
        renderWithGraphene(
            <IdentityProvidersTable
                rows={ROWS.map(row => {
                    const { activated: _activated, ...rest } = row;
                    return rest;
                })}
            />,
        );
        expect(identityProvidersTable().getRow('Google SSO').getCellText('Status')).toBe('—');
    });

    it('omits Activate when activation state is unknown', async () => {
        renderWithGraphene(
            <IdentityProvidersTable
                rows={ROWS.map(row => {
                    const { activated: _activated, ...rest } = row;
                    return rest;
                })}
                canActivate
                canDelete
                onToggle={jest.fn()}
                onDelete={jest.fn()}
            />,
        );
        await buttonHarness({ name: /Actions for Google/i }).click();
        expect(screen.queryByRole('menuitem', { name: /^Activate$/ })).toBeNull();
        expect(screen.queryByRole('menuitem', { name: /^Deactivate$/ })).toBeNull();
        expect(await screen.findByRole('menuitem', { name: /^Delete$/ })).not.toBeNull();
    });

    it('keeps page 2 rows after a client-side page change', () => {
        const many: IdentityProviderRow[] = Array.from({ length: 11 }, (_, index) => ({
            id: `idp-${index}`,
            name: `Provider ${index}`,
            description: `Description ${index}`,
            enabled: true,
            sync: false,
            type: 'GOOGLE',
            activated: false,
            created_at: 1,
            updated_at: 1,
        }));
        renderWithGraphene(<IdentityProvidersTable rows={many} />);
        fireEvent.click(screen.getByRole('button', { name: 'Next page' }));
        const table = identityProvidersTable();
        expect(table.queryRow('Description 0')).toBeNull();
        expect(table.getRow('Description 10').getCellText('Name')).toBe('Provider 10');
    });
});
