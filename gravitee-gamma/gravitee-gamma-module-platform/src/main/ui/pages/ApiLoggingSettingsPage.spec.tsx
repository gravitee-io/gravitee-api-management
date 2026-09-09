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

import { ApiLoggingSettingsPage } from './ApiLoggingSettingsPage';
import { useOrgConsoleSettings } from '../features/organization-settings/hooks/useOrgConsoleSettings';
import { useSaveOrgConsoleSettings } from '../features/organization-settings/hooks/useSaveOrgConsoleSettings';
import type { ConsoleSettings } from '../features/organization-settings/types/consoleSettings';

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
    email: { enabled: false },
    logging: {
        maxDurationMillis: 15000,
        audit: { enabled: true, trail: { enabled: true } },
        user: { displayed: true },
        messageSampling: {
            probabilistic: { default: 0.25, limit: 0.5 },
            count: { default: 200, limit: 20 },
            temporal: { default: 'PT5S', limit: 'PT1S' },
            windowedCount: { default: '1/PT10S', limit: '1/PT1S' },
        },
    },
};

function renderPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return render(<ApiLoggingSettingsPage />, { wrapper: Wrapper });
}

describe('ApiLoggingSettingsPage', () => {
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

    it('renders API logging fields from settings', () => {
        renderPage();

        expect(screen.getByRole('heading', { name: 'API Logging' })).not.toBeNull();
        expect((screen.getByLabelText('Max Duration (in ms)') as HTMLInputElement).value).toBe('15000');
        expect((screen.getByLabelText('Probabilistic default') as HTMLInputElement).value).toBe('0.25');
        expect((screen.getByLabelText('Temporal default') as HTMLInputElement).value).toBe('PT5S');
    });

    it('posts logging without wiping cors or email', () => {
        const mutate = jest.fn();
        mockUseSaveOrgConsoleSettings.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useSaveOrgConsoleSettings>);
        renderPage();

        fireEvent.change(screen.getByLabelText('Max Duration (in ms)'), { target: { value: '20000' } });
        fireEvent.click(screen.getByRole('button', { name: /Save changes/i }));

        expect(mutate).toHaveBeenCalledWith(
            expect.objectContaining({
                cors: SETTINGS.cors,
                email: SETTINGS.email,
                logging: expect.objectContaining({
                    maxDurationMillis: 20000,
                }),
            }),
            expect.any(Object),
        );
    });

    it('discards unsaved changes and hides the action bar', () => {
        renderPage();

        fireEvent.change(screen.getByLabelText('Max Duration (in ms)'), { target: { value: '20000' } });
        expect(screen.getByRole('button', { name: /Save changes/i })).not.toBeNull();

        fireEvent.click(screen.getByRole('button', { name: /Discard/i }));

        expect((screen.getByLabelText('Max Duration (in ms)') as HTMLInputElement).value).toBe('15000');
        expect(screen.queryByRole('button', { name: /Save changes/i })).toBeNull();
    });

    it('keeps Save disabled when sampling validation fails', () => {
        const mutate = jest.fn();
        mockUseSaveOrgConsoleSettings.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useSaveOrgConsoleSettings>);
        renderPage();

        fireEvent.change(screen.getByLabelText('Probabilistic default'), { target: { value: '0.6' } });
        fireEvent.change(screen.getByLabelText('Probabilistic limit'), { target: { value: '0.5' } });

        expect(screen.getByRole('button', { name: /Save changes/i })).toHaveProperty('disabled', true);
        fireEvent.click(screen.getByRole('button', { name: /Save changes/i }));
        expect(mutate).not.toHaveBeenCalled();
    });
});
