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

import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react';
import type { ReactNode } from 'react';

import { EnvironmentCorsSettingsPage } from './EnvironmentCorsSettingsPage';
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
    cors: {
        allowOrigin: ['https://portal.example.com'],
        allowMethods: ['GET', 'POST'],
        allowHeaders: ['Authorization'],
        exposedHeaders: ['ETag'],
        maxAge: 1728000,
    },
    plan: { security: { apikey: { enabled: true } } },
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
        jest.mocked(useForbiddenResourceRedirect).mockImplementation(() => undefined);
        mockUseHasPermission.mockReturnValue(true);
        mockUsePortalSettings.mockReturnValue({
            data: SETTINGS,
            isLoading: false,
            isError: false,
        } as ReturnType<typeof usePortalSettings>);
        mockUseSavePortalSettings.mockReturnValue({
            mutate: jest.fn(),
            isPending: false,
        } as unknown as ReturnType<typeof useSavePortalSettings>);
    });

    it('renders environment-scoped CORS fields and description', () => {
        renderPage();
        expect(screen.getByRole('heading', { name: 'CORS' })).not.toBeNull();
        expect(screen.getByText(/developer portal API from this environment/)).not.toBeNull();
        expect(screen.getByText('https://portal.example.com')).not.toBeNull();
        expect(screen.getByText(/Exact origins, \* , or a regular expression\. Press Enter to add\./)).not.toBeNull();
        expect((screen.getByLabelText(/Max age \(seconds\)/) as HTMLInputElement).value).toBe('1728000');
        expect(screen.getByText(/cached by clients$/)).not.toBeNull();
        expect(screen.getByText(/preflight request\./)).not.toBeNull();
    });

    it('does not show the organization architecture override banner', () => {
        renderPage();
        expect(
            screen.queryByText(/Depending on your architecture, this configuration may be overridden by a local configuration file/),
        ).toBeNull();
    });

    it('saves only the cors overlay and keeps other portal settings', () => {
        const mutate = jest.fn();
        mockUseSavePortalSettings.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useSavePortalSettings>);
        renderPage();
        fireEvent.change(screen.getByLabelText(/Max age \(seconds\)/), { target: { value: '60' } });
        fireEvent.click(screen.getByRole('button', { name: /Save changes/i }));
        expect(mutate).toHaveBeenCalledWith(
            expect.objectContaining({
                cors: expect.objectContaining({ maxAge: 60 }),
                plan: SETTINGS.plan,
            }),
            expect.any(Object),
        );
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
        expect((screen.getByLabelText('Allow-Origin') as HTMLInputElement).disabled).toBe(true);
        expect(screen.getByLabelText('Allow-Origin').closest('[data-system-readonly="true"]')).not.toBeNull();
        expect((screen.getByLabelText(/Max age \(seconds\)/) as HTMLInputElement).disabled).toBe(false);
        expect((screen.getByLabelText('GET') as HTMLButtonElement).disabled).toBe(false);
    });

    it('disables the form when the user lacks environment-settings update permission', () => {
        mockUseHasPermission.mockReturnValue(false);
        renderPage();
        expect((screen.getByLabelText('Allow-Origin') as HTMLInputElement).disabled).toBe(true);
        expect(screen.queryByRole('button', { name: /Save changes/i })).toBeNull();
    });
});
