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

import { ManagementAndSchedulersPage } from './ManagementAndSchedulersPage';
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
    management: {
        title: 'Gravitee.io Management',
        url: 'https://apim.example.com',
        support: { enabled: true },
        userCreation: { enabled: true },
        automaticValidation: { enabled: true },
    },
    scheduler: { tasks: 10, notifications: 10 },
};

function renderPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return render(<ManagementAndSchedulersPage />, { wrapper: Wrapper });
}

describe('ManagementAndSchedulersPage', () => {
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

    it('renders management and scheduler fields from settings', () => {
        renderPage();
        expect(screen.getByRole('heading', { name: 'Management & Schedulers' })).not.toBeNull();
        expect((screen.getByLabelText('Title') as HTMLInputElement).value).toBe('Gravitee.io Management');
        expect((screen.getByLabelText('Management URL') as HTMLInputElement).value).toBe('https://apim.example.com');
        expect((screen.getByLabelText('Tasks (in seconds)') as HTMLInputElement).value).toBe('10');
        expect(screen.getByLabelText('Activate Support')).not.toBeNull();
        expect(screen.getByLabelText('Allow User Registration')).not.toBeNull();
        expect(screen.getByLabelText('Enable automatic validation of registration requests')).not.toBeNull();
    });

    it('saves a merged management payload after an edit', () => {
        const mutate = jest.fn();
        mockUseSaveOrgConsoleSettings.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useSaveOrgConsoleSettings>);
        renderPage();

        fireEvent.change(screen.getByLabelText('Title'), { target: { value: 'Acme Console' } });
        fireEvent.click(screen.getByRole('button', { name: /Save changes/i }));

        expect(mutate).toHaveBeenCalledWith(
            expect.objectContaining({
                management: expect.objectContaining({ title: 'Acme Console' }),
                scheduler: { tasks: 10, notifications: 10 },
            }),
            expect.any(Object),
        );
    });

    it('shows a read-only banner and disables fields without update permission', () => {
        mockUseHasPermission.mockReturnValue(false);
        renderPage();
        expect(screen.getByText(/You do not have permission to modify these settings/)).not.toBeNull();
        expect((screen.getByLabelText('Title') as HTMLInputElement).disabled).toBe(true);
        expect(screen.queryByRole('button', { name: /Save changes/i })).toBeNull();
    });
});
