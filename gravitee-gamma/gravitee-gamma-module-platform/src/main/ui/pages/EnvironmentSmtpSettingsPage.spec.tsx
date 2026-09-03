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
import { fireEvent, render, screen, within } from '@testing-library/react';
import type { ReactNode } from 'react';

import { EnvironmentSmtpSettingsPage } from './EnvironmentSmtpSettingsPage';
import { useResetPortalBrandedSenders } from '../features/environment-settings/hooks/useResetPortalBrandedSenders';
import { useSaveEnvironmentPortalSettings } from '../features/environment-settings/hooks/useSaveEnvironmentPortalSettings';
import { PASSWORD_SENTINEL } from '../features/organization-settings/types/consoleSettings';
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

jest.mock('../features/environment-settings/hooks/useResetPortalBrandedSenders', () => ({
    useResetPortalBrandedSenders: jest.fn(),
}));

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseEnvironment = jest.mocked(useEnvironment);
const mockUsePortalSettings = jest.mocked(usePortalSettings);
const mockUseSaveEnvironmentPortalSettings = jest.mocked(useSaveEnvironmentPortalSettings);
const mockUseResetPortalBrandedSenders = jest.mocked(useResetPortalBrandedSenders);

const SETTINGS: PortalSettings = {
    cors: { allowOrigin: ['https://portal.example.com'] },
    email: {
        enabled: true,
        host: 'smtp.example.com',
        port: 587,
        username: 'apim',
        password: PASSWORD_SENTINEL,
        protocol: 'smtp',
        subject: '[gravitee] %s',
        from: 'noreply@example.com',
        brandedSendersInherited: false,
        properties: { auth: true, startTlsEnable: true, sslTrust: '' },
        brandedSenders: [{ domains: ['partners.example.com'], from: 'Partners <partners@example.com>', subject: '[Partners] %s' }],
    },
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
        mockUseResetPortalBrandedSenders.mockReturnValue({
            mutate: jest.fn(),
            isPending: false,
        } as unknown as ReturnType<typeof useResetPortalBrandedSenders>);
    });

    it('renders SMTP fields and the environment description', () => {
        renderPage();
        expect(screen.getByRole('heading', { name: 'SMTP' })).not.toBeNull();
        expect(screen.getByText(/mail server this environment uses/)).not.toBeNull();
        expect((screen.getByLabelText('Host') as HTMLInputElement).value).toBe('smtp.example.com');
        expect((screen.getByLabelText('Password') as HTMLInputElement).value).toBe(PASSWORD_SENTINEL);
        expect(screen.getByRole('button', { name: 'Reset to Org settings' })).not.toBeNull();
    });

    it('posts email without wiping cors, keeping the password sentinel', () => {
        const mutate = jest.fn();
        mockUseSaveEnvironmentPortalSettings.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useSaveEnvironmentPortalSettings>);
        renderPage();
        fireEvent.change(screen.getByLabelText('Host'), { target: { value: 'smtp.acme.com' } });
        fireEvent.click(screen.getByRole('button', { name: /Save changes/i }));
        expect(mutate).toHaveBeenCalledWith(
            expect.objectContaining({
                cors: SETTINGS.cors,
                email: expect.objectContaining({
                    host: 'smtp.acme.com',
                    password: PASSWORD_SENTINEL,
                }),
            }),
            expect.any(Object),
        );
    });

    it('lets the user enable emailing when only other SMTP fields are system-provided', () => {
        const mutate = jest.fn();
        mockUseSaveEnvironmentPortalSettings.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useSaveEnvironmentPortalSettings>);
        mockUsePortalSettings.mockReturnValue({
            data: {
                ...SETTINGS,
                email: { ...SETTINGS.email, enabled: false },
                metadata: {
                    readonly: [
                        'email.host',
                        'email.port',
                        'email.username',
                        'email.password',
                        'email.protocol',
                        'email.subject',
                        'email.from',
                        'email.properties.auth',
                        'email.properties.starttls.enable',
                        'email.properties.ssl.trust',
                        'email.branded_senders',
                    ],
                },
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof usePortalSettings>);

        renderPage();

        const enableSwitch = screen.getByLabelText('Enable Emailing') as HTMLButtonElement;
        expect(enableSwitch.disabled).toBe(false);
        fireEvent.click(enableSwitch);
        expect((screen.getByLabelText('Host') as HTMLInputElement).disabled).toBe(true);
        fireEvent.click(screen.getByRole('button', { name: /Save changes/i }));
        expect(mutate).toHaveBeenCalledWith(
            expect.objectContaining({
                email: expect.objectContaining({ enabled: true }),
            }),
            expect.any(Object),
        );
    });

    it('disables reset when branded senders are already inherited from the organization', () => {
        mockUsePortalSettings.mockReturnValue({
            data: {
                ...SETTINGS,
                email: { ...SETTINGS.email, brandedSendersInherited: true },
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof usePortalSettings>);

        renderPage();
        expect(screen.getByRole('button', { name: 'Reset to Org settings' })).toHaveProperty('disabled', true);
    });

    it('disables reset when brandedSendersInherited is absent', () => {
        const email = { ...SETTINGS.email };
        delete email.brandedSendersInherited;
        mockUsePortalSettings.mockReturnValue({
            data: {
                ...SETTINGS,
                email,
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof usePortalSettings>);

        renderPage();
        expect(screen.getByRole('button', { name: 'Reset to Org settings' })).toHaveProperty('disabled', true);
    });

    it('does not flip reset lock from a same-env refetch while the form is dirty', () => {
        const { rerender } = renderPage();
        expect(screen.getByRole('button', { name: 'Reset to Org settings' })).toHaveProperty('disabled', false);
        fireEvent.change(screen.getByLabelText('Host'), { target: { value: 'smtp.draft.com' } });

        mockUsePortalSettings.mockReturnValue({
            data: {
                ...SETTINGS,
                email: { ...SETTINGS.email, brandedSendersInherited: true },
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof usePortalSettings>);

        rerender(<EnvironmentSmtpSettingsPage />);
        expect(screen.getByRole('button', { name: 'Reset to Org settings' })).toHaveProperty('disabled', false);
        expect((screen.getByLabelText('Host') as HTMLInputElement).value).toBe('smtp.draft.com');
    });

    it('confirms then resets branded senders to organization settings', () => {
        const mutate = jest.fn((_payload: unknown, options?: { onSuccess?: (settings: PortalSettings) => void }) => {
            options?.onSuccess?.({
                ...SETTINGS,
                email: { ...SETTINGS.email, brandedSenders: [], brandedSendersInherited: true, host: 'smtp.org.com' },
            });
        });
        mockUseResetPortalBrandedSenders.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useResetPortalBrandedSenders>);
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: 'Reset to Org settings' }));
        expect(screen.getByText('Reset branded senders?')).not.toBeNull();
        fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Reset to Org settings' }));
        expect(mutate).toHaveBeenCalled();
        expect((screen.getByLabelText('Host') as HTMLInputElement).value).toBe('smtp.org.com');
        expect(screen.queryByText('partners.example.com')).toBeNull();
        expect(screen.getByRole('button', { name: 'Reset to Org settings' })).toHaveProperty('disabled', true);
    });

    it('resets form state when the environment changes', () => {
        const { rerender } = renderPage();
        fireEvent.change(screen.getByLabelText('Host'), { target: { value: 'smtp.other.com' } });
        expect((screen.getByLabelText('Host') as HTMLInputElement).value).toBe('smtp.other.com');

        mockUseEnvironment.mockReturnValue({ id: 'env-2' } as ReturnType<typeof useEnvironment>);
        mockUsePortalSettings.mockReturnValue({
            data: {
                ...SETTINGS,
                email: { ...SETTINGS.email, host: 'smtp.env2.com' },
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof usePortalSettings>);

        rerender(<EnvironmentSmtpSettingsPage />);
        expect((screen.getByLabelText('Host') as HTMLInputElement).value).toBe('smtp.env2.com');
    });

    it('does not clobber in-progress edits when the same environment refetches', () => {
        const { rerender } = renderPage();
        fireEvent.change(screen.getByLabelText('Host'), { target: { value: 'smtp.draft.com' } });

        mockUsePortalSettings.mockReturnValue({
            data: SETTINGS,
            isLoading: false,
            isError: false,
        } as ReturnType<typeof usePortalSettings>);

        rerender(<EnvironmentSmtpSettingsPage />);
        expect((screen.getByLabelText('Host') as HTMLInputElement).value).toBe('smtp.draft.com');
    });

    it('disables SMTP fields when the user cannot update environment settings', () => {
        mockUseHasPermission.mockReturnValue(false);
        renderPage();
        expect((screen.getByLabelText('Host') as HTMLInputElement).disabled).toBe(true);
        expect(screen.getByText(/You do not have permission to modify these settings/)).not.toBeNull();
        expect(screen.queryByRole('button', { name: 'Reset to Org settings' })).toBeNull();
    });

    it('discards in-progress edits back to the last saved state', () => {
        renderPage();
        fireEvent.change(screen.getByLabelText('Host'), { target: { value: 'smtp.draft.com' } });
        fireEvent.click(screen.getByRole('button', { name: /Discard/i }));
        expect((screen.getByLabelText('Host') as HTMLInputElement).value).toBe('smtp.example.com');
        expect(screen.queryByRole('button', { name: /Save changes/i })).toBeNull();
    });

    it('shows a loading skeleton while portal settings are fetching', () => {
        mockUsePortalSettings.mockReturnValue({
            data: undefined,
            isLoading: true,
            isError: false,
        } as ReturnType<typeof usePortalSettings>);
        renderPage();
        expect(screen.queryByRole('heading', { name: 'SMTP' })).toBeNull();
        expect(screen.queryByLabelText('Host')).toBeNull();
    });

    it('shows an error when portal settings fail to load', () => {
        mockUsePortalSettings.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
        } as ReturnType<typeof usePortalSettings>);
        renderPage();
        expect(screen.getByText(/Failed to load settings/)).not.toBeNull();
        expect(screen.queryByLabelText('Host')).toBeNull();
    });

    it('disables reset when branded senders are system-provided', () => {
        mockUsePortalSettings.mockReturnValue({
            data: {
                ...SETTINGS,
                metadata: { readonly: ['email.branded_senders'] },
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof usePortalSettings>);

        renderPage();
        expect(screen.getByRole('button', { name: 'Reset to Org settings' })).toHaveProperty('disabled', true);
    });
});
