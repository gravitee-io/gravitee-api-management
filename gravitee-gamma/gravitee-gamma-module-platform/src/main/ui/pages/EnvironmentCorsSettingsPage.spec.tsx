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

import { useEnvironment, useHasPermission } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react';
import type { ReactNode } from 'react';

import { EnvironmentCorsSettingsPage } from './EnvironmentCorsSettingsPage';
import { useSaveEnvironmentPortalSettings } from '../features/environment-settings/hooks/useSaveEnvironmentPortalSettings';
import { usePortalSettings } from '../features/security-plan-types/hooks/usePortalSettings';
import type { PortalSettings } from '../features/security-plan-types/services/portalSettings';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
    useEnvironment: jest.fn(),
}));

jest.mock('../features/security-plan-types/hooks/usePortalSettings', () => ({
    usePortalSettings: jest.fn(),
}));

jest.mock('../features/environment-settings/hooks/useSaveEnvironmentPortalSettings', () => ({
    useSaveEnvironmentPortalSettings: jest.fn(),
}));

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseEnvironment = jest.mocked(useEnvironment);
const mockUsePortalSettings = jest.mocked(usePortalSettings);
const mockUseSaveEnvironmentPortalSettings = jest.mocked(useSaveEnvironmentPortalSettings);

const SETTINGS: PortalSettings = {
    cors: {
        allowOrigin: ['https://portal.example.com'],
        allowMethods: ['GET', 'POST'],
        allowHeaders: ['Authorization'],
        exposedHeaders: ['ETag'],
        maxAge: 1728000,
    },
    plan: { security: { keyless: { enabled: true } } },
    email: { host: 'smtp.example.com' },
};

function renderPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return render(<EnvironmentCorsSettingsPage />, { wrapper: Wrapper });
}

describe('EnvironmentCorsSettingsPage', () => {
    beforeEach(() => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseEnvironment.mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
        mockUsePortalSettings.mockReturnValue({
            data: SETTINGS,
            isLoading: false,
            isError: false,
        } as ReturnType<typeof usePortalSettings>);
        mockUseSaveEnvironmentPortalSettings.mockReturnValue({
            mutate: jest.fn(),
            isPending: false,
        } as unknown as ReturnType<typeof useSaveEnvironmentPortalSettings>);
    });

    it('renders portal CORS fields and the environment description', () => {
        renderPage();
        expect(screen.getByRole('heading', { name: 'CORS' })).not.toBeNull();
        expect(screen.getByText(/developer portal API from this environment/)).not.toBeNull();
        expect(screen.getByText('https://portal.example.com')).not.toBeNull();
        expect((screen.getByLabelText('Max age') as HTMLInputElement).value).toBe('1728000');
    });

    it('saves only the cors overlay and keeps plan and email', () => {
        const mutate = jest.fn();
        mockUseSaveEnvironmentPortalSettings.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useSaveEnvironmentPortalSettings>);
        renderPage();
        fireEvent.change(screen.getByLabelText('Max age'), { target: { value: '60' } });
        fireEvent.click(screen.getByRole('button', { name: /Save changes/i }));
        expect(mutate).toHaveBeenCalledWith(
            expect.objectContaining({
                cors: expect.objectContaining({ maxAge: 60, allowOrigin: ['https://portal.example.com'] }),
                plan: SETTINGS.plan,
                email: SETTINGS.email,
            }),
            expect.any(Object),
        );
    });

    it('does not save when max age is invalid', () => {
        const mutate = jest.fn();
        mockUseSaveEnvironmentPortalSettings.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useSaveEnvironmentPortalSettings>);
        renderPage();
        fireEvent.change(screen.getByLabelText('Max age'), { target: { value: '-1' } });
        expect(screen.getByRole('button', { name: /Save changes/i })).toHaveProperty('disabled', true);
        fireEvent.click(screen.getByRole('button', { name: /Save changes/i }));
        expect(mutate).not.toHaveBeenCalled();
    });

    it('does not save when an Allow-Origin regex is invalid', () => {
        const mutate = jest.fn();
        mockUseSaveEnvironmentPortalSettings.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useSaveEnvironmentPortalSettings>);
        renderPage();
        fireEvent.change(screen.getByLabelText('Allow-Origin'), { target: { value: '(http' } });
        fireEvent.blur(screen.getByLabelText('Allow-Origin'));
        expect(screen.getByRole('button', { name: /Save changes/i })).toHaveProperty('disabled', true);
        fireEvent.click(screen.getByRole('button', { name: /Save changes/i }));
        expect(mutate).not.toHaveBeenCalled();
    });

    it('discards in-progress edits back to the last saved state', () => {
        renderPage();
        fireEvent.change(screen.getByLabelText('Max age'), { target: { value: '60' } });
        fireEvent.click(screen.getByRole('button', { name: /Discard/i }));
        expect((screen.getByLabelText('Max age') as HTMLInputElement).value).toBe('1728000');
        expect(screen.queryByRole('button', { name: /Save changes/i })).toBeNull();
    });

    it('shows a loading skeleton while portal settings are fetching', () => {
        mockUsePortalSettings.mockReturnValue({
            data: undefined,
            isLoading: true,
            isError: false,
        } as ReturnType<typeof usePortalSettings>);
        renderPage();
        expect(screen.queryByRole('heading', { name: 'CORS' })).toBeNull();
        expect(screen.queryByLabelText('Max age')).toBeNull();
    });

    it('shows an error when portal settings fail to load', () => {
        mockUsePortalSettings.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
        } as ReturnType<typeof usePortalSettings>);
        renderPage();
        expect(screen.getByText(/Failed to load settings/)).not.toBeNull();
        expect(screen.queryByLabelText('Max age')).toBeNull();
    });

    it('keeps other CORS fields editable when only Allow-Origin is system-provided', () => {
        mockUsePortalSettings.mockReturnValue({
            data: {
                ...SETTINGS,
                metadata: { readonly: ['http.api.portal.cors.allow-origin'] },
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof usePortalSettings>);

        renderPage();
        expect((screen.getByPlaceholderText(/https:\/\/mydomain.com/) as HTMLInputElement).disabled).toBe(true);
        expect((screen.getByLabelText('Max age') as HTMLInputElement).disabled).toBe(false);
        expect((screen.getByLabelText('GET') as HTMLButtonElement).disabled).toBe(false);
    });

    it('resets form state when the environment changes', () => {
        const { rerender } = renderPage();
        fireEvent.change(screen.getByLabelText('Max age'), { target: { value: '60' } });
        expect((screen.getByLabelText('Max age') as HTMLInputElement).value).toBe('60');

        mockUseEnvironment.mockReturnValue({ id: 'env-2' } as ReturnType<typeof useEnvironment>);
        mockUsePortalSettings.mockReturnValue({
            data: {
                ...SETTINGS,
                cors: { ...SETTINGS.cors, maxAge: 90 },
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof usePortalSettings>);

        rerender(<EnvironmentCorsSettingsPage />);
        expect((screen.getByLabelText('Max age') as HTMLInputElement).value).toBe('90');
    });

    it('does not clobber in-progress edits when the same environment refetches', () => {
        const { rerender } = renderPage();
        fireEvent.change(screen.getByLabelText('Max age'), { target: { value: '60' } });

        mockUsePortalSettings.mockReturnValue({
            data: {
                ...SETTINGS,
                cors: { ...SETTINGS.cors, maxAge: 1728000 },
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof usePortalSettings>);

        rerender(<EnvironmentCorsSettingsPage />);
        expect((screen.getByLabelText('Max age') as HTMLInputElement).value).toBe('60');
    });

    it('disables CORS fields when the user cannot update environment settings', () => {
        mockUseHasPermission.mockReturnValue(false);
        renderPage();
        expect((screen.getByLabelText('Max age') as HTMLInputElement).disabled).toBe(true);
        expect(screen.getByText(/You do not have permission to modify these settings/)).not.toBeNull();
    });
});
