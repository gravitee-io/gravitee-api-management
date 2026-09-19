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

import { buttonHarness, radioGroupHarness, renderWithGraphene } from '@gravitee/graphene-core/testing';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { screen } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useHasPermission } from '@gravitee/gamma-modules-sdk';

import { PrimaryOwnerModePage } from './PrimaryOwnerModePage';
import { usePortalSettings } from '../features/security-plan-types/hooks/usePortalSettings';
import { useSavePortalSettings } from '../features/security-plan-types/hooks/useSavePortalSettings';
import type { PortalSettings } from '../features/security-plan-types/services/portalSettings';
import { useForbiddenResourceRedirect } from '../shared/hooks/useForbiddenResourceRedirect';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
}));

jest.mock('../shared/hooks/useForbiddenResourceRedirect');

jest.mock('../features/security-plan-types/hooks/usePortalSettings', () => ({
    usePortalSettings: jest.fn(),
}));

jest.mock('../features/security-plan-types/hooks/useSavePortalSettings', () => ({
    useSavePortalSettings: jest.fn(),
}));

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUsePortalSettings = jest.mocked(usePortalSettings);
const mockUseSavePortalSettings = jest.mocked(useSavePortalSettings);

const SETTINGS: PortalSettings = {
    company: { name: 'Acme' },
    plan: { security: { apikey: { enabled: true } } },
    api: { labelsDictionary: ['internal'], primaryOwnerMode: 'USER' },
    apiProduct: { primaryOwnerMode: 'HYBRID' },
};

function renderPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return renderWithGraphene(<PrimaryOwnerModePage />, { wrapper: Wrapper });
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

describe('PrimaryOwnerModePage', () => {
    let mutate: jest.Mock;

    beforeEach(() => {
        Element.prototype.hasPointerCapture = jest.fn();
        Element.prototype.setPointerCapture = jest.fn();
        jest.mocked(useForbiddenResourceRedirect).mockImplementation(() => undefined);
        mutate = jest.fn();
        mockUseHasPermission.mockReturnValue(true);
        mockUsePortalSettings.mockReturnValue({
            data: SETTINGS,
            isLoading: false,
            isError: false,
        } as ReturnType<typeof usePortalSettings>);
        mockUseSavePortalSettings.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useSavePortalSettings>);
    });

    it('renders the page title, current modes, and help text', () => {
        renderPage();

        expect(screen.getByRole('heading', { name: 'Primary Owner Mode' })).not.toBeNull();
        expect(screen.getByText(/when someone creates an API or an API product in this environment/)).not.toBeNull();
        expect(radioGroupHarness({ name: 'API Primary Owner mode' }).getValue()).toBe('USER');
        expect(radioGroupHarness({ name: 'API Product Primary Owner mode' }).getValue()).toBe('HYBRID');
        expect(screen.getAllByText('A user or a group can be the primary owner. This is the default.').length).toBeGreaterThan(0);
    });

    it('does not show the organization architecture override banner', () => {
        renderPage();
        expect(
            screen.queryByText(/Depending on your architecture, this configuration may be overridden by a local configuration file/),
        ).toBeNull();
    });

    it('saves both modes and keeps other portal settings', async () => {
        renderPage();

        await radioGroupHarness({ name: 'API Primary Owner mode' }).select('GROUP');
        await radioGroupHarness({ name: 'API Product Primary Owner mode' }).select('USER');
        await buttonHarness({ name: /Save changes/i }).click();

        expect(mutate).toHaveBeenCalledWith(
            expect.objectContaining({
                api: expect.objectContaining({ labelsDictionary: ['internal'], primaryOwnerMode: 'GROUP' }),
                apiProduct: expect.objectContaining({ primaryOwnerMode: 'USER' }),
                plan: SETTINGS.plan,
                company: SETTINGS.company,
            }),
            expect.any(Object),
        );
    });

    it('restores the last saved state on discard', async () => {
        renderPage();

        await radioGroupHarness({ name: 'API Primary Owner mode' }).select('HYBRID');
        expect(radioGroupHarness({ name: 'API Primary Owner mode' }).getValue()).toBe('HYBRID');

        await buttonHarness({ name: 'Discard' }).click();

        expect(radioGroupHarness({ name: 'API Primary Owner mode' }).getValue()).toBe('USER');
        expect(screen.queryByRole('button', { name: /Save changes/i })).toBeNull();
    });

    it('lets viewers see the setting and cannot change it', () => {
        mockUseHasPermission.mockReturnValue(false);
        renderPage();

        expect(screen.getByRole('heading', { name: 'Primary Owner Mode' })).not.toBeNull();
        expect(screen.getByText(/You do not have permission to modify these settings/)).not.toBeNull();
        expect(radioGroupHarness({ name: 'API Primary Owner mode' }).isDisabled()).toBe(true);
        expect(radioGroupHarness({ name: 'API Product Primary Owner mode' }).isDisabled()).toBe(true);
        expect(screen.queryByRole('button', { name: /Save changes/i })).toBeNull();
    });

    it('shows a loading state', () => {
        mockUsePortalSettings.mockReturnValue({
            data: undefined,
            isLoading: true,
            isError: false,
        } as ReturnType<typeof usePortalSettings>);

        renderPage();

        expect(screen.queryByRole('heading', { name: 'Primary Owner Mode' })).toBeNull();
        expect(screen.queryByRole('radiogroup')).toBeNull();
    });

    it('shows an error state', () => {
        mockUsePortalSettings.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
        } as ReturnType<typeof usePortalSettings>);

        renderPage();

        expect(screen.getByRole('heading', { name: 'Primary Owner Mode' })).not.toBeNull();
        expect(screen.getByText('Failed to load settings. Please refresh and try again.')).not.toBeNull();
    });

    it('does not save a readonly API mode overlay', async () => {
        mockUsePortalSettings.mockReturnValue({
            data: {
                ...SETTINGS,
                metadata: { readonly: ['api.primaryOwnerMode'] },
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof usePortalSettings>);

        renderPage();

        await radioGroupHarness({ name: 'API Product Primary Owner mode' }).select('GROUP');
        await buttonHarness({ name: /Save changes/i }).click();

        expect(mutate).toHaveBeenCalledWith(
            expect.objectContaining({
                api: expect.objectContaining({ primaryOwnerMode: 'USER' }),
                apiProduct: expect.objectContaining({ primaryOwnerMode: 'GROUP' }),
            }),
            expect.any(Object),
        );
    });
});
