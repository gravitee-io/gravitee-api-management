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

import { EnvironmentSmtpSettingsPage } from './EnvironmentSmtpSettingsPage';
import { useResetEnvironmentBrandedSenders } from '../features/environment-settings/hooks/useResetEnvironmentBrandedSenders';
import { PASSWORD_SENTINEL, type ConsoleSettings } from '../features/organization-settings/types/consoleSettings';
import { usePortalSettings } from '../features/security-plan-types/hooks/usePortalSettings';
import { useSavePortalSettings } from '../features/security-plan-types/hooks/useSavePortalSettings';
import { ApimApiError } from '../shared/api/apimClient';
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

jest.mock('../features/environment-settings/hooks/useResetEnvironmentBrandedSenders', () => ({
    useResetEnvironmentBrandedSenders: jest.fn(),
}));

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUsePortalSettings = jest.mocked(usePortalSettings);
const mockUseSavePortalSettings = jest.mocked(useSavePortalSettings);
const mockUseResetEnvironmentBrandedSenders = jest.mocked(useResetEnvironmentBrandedSenders);

const SETTINGS: ConsoleSettings = {
    email: {
        enabled: true,
        host: 'smtp.example.com',
        port: 587,
        username: 'admin',
        password: PASSWORD_SENTINEL,
        protocol: 'smtp',
        subject: '[gravitee] %s',
        from: 'noreply@example.com',
        properties: { auth: true, startTlsEnable: true, sslTrust: '' },
        brandedSenders: [{ domains: ['partners.example.com'], from: 'partners@example.com', subject: '[Partners] %s' }],
        brandedSendersInherited: false,
    },
    trialInstance: { enabled: false },
};

function renderPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return render(<EnvironmentSmtpSettingsPage />, { wrapper: Wrapper });
}

describe('EnvironmentSmtpSettingsPage', () => {
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
        mockUseResetEnvironmentBrandedSenders.mockReturnValue({
            mutate: jest.fn(),
            isPending: false,
        } as unknown as ReturnType<typeof useResetEnvironmentBrandedSenders>);
    });

    it('renders the environment-scoped SMTP fields and description', () => {
        renderPage();
        expect(screen.getByRole('heading', { name: 'SMTP' })).not.toBeNull();
        expect(screen.getByText(/this environment uses/)).not.toBeNull();
        expect((screen.getByLabelText('Host') as HTMLInputElement).value).toBe('smtp.example.com');
        expect((screen.getByLabelText('Password') as HTMLInputElement).value).toBe(PASSWORD_SENTINEL);
    });

    it('does not show the organization architecture override banner', () => {
        renderPage();
        expect(
            screen.queryByText(/Depending on your architecture, this configuration may be overridden by a local configuration file/),
        ).toBeNull();
    });

    it('disables Enable Emailing only when email.enabled is system-provided', () => {
        mockUsePortalSettings.mockReturnValue({
            data: { ...SETTINGS, metadata: { readonly: ['email.enabled'] } },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof usePortalSettings>);
        renderPage();
        expect((screen.getByLabelText('Enable Emailing') as HTMLButtonElement).disabled).toBe(true);
        expect((screen.getByLabelText('Host') as HTMLInputElement).disabled).toBe(false);
    });

    it('disables the form when the user lacks environment-settings update permission', () => {
        mockUseHasPermission.mockReturnValue(false);
        renderPage();
        expect((screen.getByLabelText('Enable Emailing') as HTMLButtonElement).disabled).toBe(true);
        expect((screen.getByLabelText('Host') as HTMLInputElement).disabled).toBe(true);
        expect(screen.queryByRole('button', { name: /Save changes/i })).toBeNull();
    });

    it('hides branded senders reset when the user lacks environment-settings update permission', () => {
        mockUseHasPermission.mockReturnValue(false);
        renderPage();
        expect(screen.queryByRole('button', { name: /Reset to Org settings/i })).toBeNull();
    });

    it('hides SMTP on a trial instance', () => {
        mockUsePortalSettings.mockReturnValue({
            data: { ...SETTINGS, trialInstance: { enabled: true } },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof usePortalSettings>);
        renderPage();
        expect(screen.getByText('SMTP is not available on trial instances.')).not.toBeNull();
        expect(screen.queryByLabelText('Host')).toBeNull();
    });

    it('posts email settings to the shared portal settings save mutation', () => {
        const mutate = jest.fn();
        mockUseSavePortalSettings.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useSavePortalSettings>);
        renderPage();
        fireEvent.change(screen.getByLabelText('Host'), { target: { value: 'smtp.acme.com' } });
        fireEvent.click(screen.getByRole('button', { name: /Save changes/i }));
        expect(mutate).toHaveBeenCalledWith(
            expect.objectContaining({
                email: expect.objectContaining({
                    host: 'smtp.acme.com',
                    password: PASSWORD_SENTINEL,
                }),
            }),
            expect.any(Object),
        );
    });

    it('offers "Reset to Org settings" when there is an environment override to drop', () => {
        renderPage();
        expect(screen.getByRole('button', { name: /Reset to Org settings/i })).not.toBeNull();
    });

    it('hides the reset action when branded senders are inherited from the organization', () => {
        mockUsePortalSettings.mockReturnValue({
            data: { ...SETTINGS, email: { ...SETTINGS.email, brandedSendersInherited: true } },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof usePortalSettings>);
        renderPage();
        expect(screen.queryByRole('button', { name: /Reset to Org settings/i })).toBeNull();
    });

    it('resets immediately when the page has no unsaved changes', () => {
        const mutate = jest.fn();
        mockUseResetEnvironmentBrandedSenders.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useResetEnvironmentBrandedSenders>);
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /Reset to Org settings/i }));
        expect(mutate).toHaveBeenCalled();
        expect(screen.queryByRole('heading', { name: 'Reset branded senders' })).toBeNull();
    });

    it('confirms before resetting when there are unsaved changes', () => {
        const mutate = jest.fn();
        mockUseResetEnvironmentBrandedSenders.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useResetEnvironmentBrandedSenders>);
        renderPage();
        fireEvent.change(screen.getByLabelText('Host'), { target: { value: 'smtp.acme.com' } });
        fireEvent.click(screen.getByRole('button', { name: /Reset to Org settings/i }));
        expect(mutate).not.toHaveBeenCalled();
        expect(screen.getByRole('heading', { name: 'Reset branded senders' })).not.toBeNull();

        fireEvent.click(screen.getByRole('button', { name: 'Reset' }));
        expect(mutate).toHaveBeenCalled();
    });

    it('renders nothing while a forbidden response redirects away', () => {
        mockUsePortalSettings.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
            error: new ApimApiError(403, 'Forbidden'),
        } as ReturnType<typeof usePortalSettings>);
        renderPage();
        expect(screen.queryByRole('heading', { name: 'SMTP' })).toBeNull();
    });
});
