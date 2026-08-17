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

import { CorsSettingsPage } from './CorsSettingsPage';
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
    cors: {
        allowOrigin: ['https://console.example.com'],
        allowMethods: ['GET', 'POST'],
        allowHeaders: ['Authorization'],
        exposedHeaders: ['ETag'],
        maxAge: 1728000,
    },
    email: { host: 'smtp.example.com' },
};

function renderPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return render(<CorsSettingsPage />, { wrapper: Wrapper });
}

describe('CorsSettingsPage', () => {
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

    it('renders CORS fields from settings', () => {
        renderPage();
        expect(screen.getByRole('heading', { name: 'CORS' })).not.toBeNull();
        expect(screen.getByText('https://console.example.com')).not.toBeNull();
        expect((screen.getByLabelText('Max age') as HTMLInputElement).value).toBe('1728000');
    });

    it('saves only the cors overlay and keeps email', () => {
        const mutate = jest.fn();
        mockUseSaveOrgConsoleSettings.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useSaveOrgConsoleSettings>);
        renderPage();
        fireEvent.change(screen.getByLabelText('Max age'), { target: { value: '60' } });
        fireEvent.click(screen.getByRole('button', { name: /Save changes/i }));
        expect(mutate).toHaveBeenCalledWith(
            expect.objectContaining({
                cors: expect.objectContaining({ maxAge: 60 }),
                email: SETTINGS.email,
            }),
            expect.any(Object),
        );
    });
});
