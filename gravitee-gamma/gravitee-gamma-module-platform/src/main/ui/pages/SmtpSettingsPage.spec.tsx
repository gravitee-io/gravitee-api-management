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

import { SmtpSettingsPage } from './SmtpSettingsPage';
import { useOrgConsoleSettings } from '../features/organization-settings/hooks/useOrgConsoleSettings';
import { useSaveOrgConsoleSettings } from '../features/organization-settings/hooks/useSaveOrgConsoleSettings';
import { PASSWORD_SENTINEL, type ConsoleSettings } from '../features/organization-settings/types/consoleSettings';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
}));

jest.mock('../features/organization-settings/hooks/useOrgConsoleSettings', () => ({
    useOrgConsoleSettings: jest.fn(),
}));

jest.mock('../features/organization-settings/hooks/useSaveOrgConsoleSettings', () => ({
    useSaveOrgConsoleSettings: jest.fn(),
}));

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseOrgConsoleSettings = jest.mocked(useOrgConsoleSettings);
const mockUseSaveOrgConsoleSettings = jest.mocked(useSaveOrgConsoleSettings);

const SETTINGS: ConsoleSettings = {
    cors: { allowOrigin: ['https://console.example.com'] },
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
        brandedSenders: [],
    },
    trialInstance: { enabled: false },
};

function renderPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return render(<SmtpSettingsPage />, { wrapper: Wrapper });
}

describe('SmtpSettingsPage', () => {
    beforeEach(() => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseOrgConsoleSettings.mockReturnValue({
            data: SETTINGS,
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useOrgConsoleSettings>);
        mockUseSaveOrgConsoleSettings.mockReturnValue({
            mutate: jest.fn(),
            isPending: false,
        } as unknown as ReturnType<typeof useSaveOrgConsoleSettings>);
    });

    it('renders SMTP fields and keeps the password sentinel', () => {
        renderPage();
        expect(screen.getByRole('heading', { name: 'SMTP' })).not.toBeNull();
        expect((screen.getByLabelText('Host') as HTMLInputElement).value).toBe('smtp.example.com');
        expect((screen.getByLabelText('Password') as HTMLInputElement).value).toBe(PASSWORD_SENTINEL);
    });

    it('hides SMTP on a trial instance', () => {
        mockUseOrgConsoleSettings.mockReturnValue({
            data: { ...SETTINGS, trialInstance: { enabled: true } },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useOrgConsoleSettings>);
        renderPage();
        expect(screen.getByText('SMTP is not available on trial instances.')).not.toBeNull();
        expect(screen.queryByLabelText('Host')).toBeNull();
    });

    it('posts email without wiping cors, keeping the password sentinel', () => {
        const mutate = jest.fn();
        mockUseSaveOrgConsoleSettings.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useSaveOrgConsoleSettings>);
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
        mockUseSaveOrgConsoleSettings.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useSaveOrgConsoleSettings>);
        mockUseOrgConsoleSettings.mockReturnValue({
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
        } as ReturnType<typeof useOrgConsoleSettings>);

        renderPage();

        const enableSwitch = screen.getByLabelText('Enable Emailing') as HTMLButtonElement;
        expect(enableSwitch.disabled).toBe(false);

        fireEvent.click(enableSwitch);

        expect((screen.getByLabelText('Host') as HTMLInputElement).disabled).toBe(true);
        expect((screen.getByLabelText('Username') as HTMLInputElement).disabled).toBe(true);

        fireEvent.click(screen.getByRole('button', { name: /Save changes/i }));
        expect(mutate).toHaveBeenCalledWith(
            expect.objectContaining({
                email: expect.objectContaining({ enabled: true }),
            }),
            expect.any(Object),
        );
    });

    it('disables Enable Emailing only when email.enabled is system-provided', () => {
        mockUseOrgConsoleSettings.mockReturnValue({
            data: {
                ...SETTINGS,
                metadata: { readonly: ['email.enabled'] },
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useOrgConsoleSettings>);

        renderPage();

        expect((screen.getByLabelText('Enable Emailing') as HTMLButtonElement).disabled).toBe(true);
        expect((screen.getByLabelText('Host') as HTMLInputElement).disabled).toBe(false);
    });
});
