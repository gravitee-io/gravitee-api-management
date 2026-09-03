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

import { EnvironmentApiLoggingSettingsPage } from './EnvironmentApiLoggingSettingsPage';
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
    cors: { allowOrigin: ['https://portal.example.com'] },
    email: { host: 'smtp.example.com' },
    logging: {
        maxDurationMillis: 15000,
        audit: { enabled: true, trail: { enabled: true } },
        user: { displayed: true },
        messageSampling: {
            probabilistic: { default: 0.01, limit: 0.5 },
            count: { default: 100, limit: 10 },
            temporal: { default: 'PT1S', limit: 'PT1S' },
            windowedCount: { default: '1/PT10S', limit: '1/PT1S' },
        },
    },
};

function renderPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return render(<EnvironmentApiLoggingSettingsPage />, { wrapper: Wrapper });
}

describe('EnvironmentApiLoggingSettingsPage', () => {
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

    it('renders API Logging fields and the environment description', () => {
        renderPage();
        expect(screen.getByRole('heading', { name: 'API Logging' })).not.toBeNull();
        expect(screen.getByText(/Cap how long API may log full payloads/)).not.toBeNull();
        expect((screen.getByLabelText('Max Duration (in ms)') as HTMLInputElement).value).toBe('15000');
        expect((screen.getByTestId('api-logging-count-default') as HTMLInputElement).value).toBe('100');
    });

    it('saves only the logging overlay and keeps cors and email', () => {
        const mutate = jest.fn();
        mockUseSaveEnvironmentPortalSettings.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useSaveEnvironmentPortalSettings>);
        renderPage();
        fireEvent.change(screen.getByLabelText('Max Duration (in ms)'), { target: { value: '20000' } });
        fireEvent.click(screen.getByRole('button', { name: /Save changes/i }));
        expect(mutate).toHaveBeenCalledWith(
            expect.objectContaining({
                logging: expect.objectContaining({
                    maxDurationMillis: 20000,
                    audit: { enabled: true, trail: { enabled: true } },
                    messageSampling: expect.objectContaining({
                        probabilistic: { default: 0.01, limit: 0.5 },
                    }),
                }),
                cors: SETTINGS.cors,
                email: SETTINGS.email,
            }),
            expect.any(Object),
        );
    });

    it('does not save when probabilistic default exceeds the limit', () => {
        const mutate = jest.fn();
        mockUseSaveEnvironmentPortalSettings.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useSaveEnvironmentPortalSettings>);
        renderPage();
        fireEvent.change(screen.getByTestId('api-logging-probabilistic-default'), { target: { value: '0.9' } });
        expect(screen.getByRole('button', { name: /Save changes/i })).toHaveProperty('disabled', true);
        fireEvent.click(screen.getByRole('button', { name: /Save changes/i }));
        expect(mutate).not.toHaveBeenCalled();
    });

    it('keeps other logging fields editable when only max duration is system-provided', () => {
        mockUsePortalSettings.mockReturnValue({
            data: {
                ...SETTINGS,
                metadata: { readonly: ['logging.default.max.duration'] },
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof usePortalSettings>);

        renderPage();
        expect((screen.getByLabelText('Max Duration (in ms)') as HTMLInputElement).disabled).toBe(true);
        expect((screen.getByTestId('api-logging-count-limit') as HTMLInputElement).disabled).toBe(false);
        expect((screen.getByLabelText('Enable audit on API Logging consultation') as HTMLButtonElement).disabled).toBe(false);
    });

    it('resets form state when the environment changes', () => {
        const { rerender } = renderPage();
        fireEvent.change(screen.getByLabelText('Max Duration (in ms)'), { target: { value: '60' } });
        expect((screen.getByLabelText('Max Duration (in ms)') as HTMLInputElement).value).toBe('60');

        mockUseEnvironment.mockReturnValue({ id: 'env-2' } as ReturnType<typeof useEnvironment>);
        mockUsePortalSettings.mockReturnValue({
            data: {
                ...SETTINGS,
                logging: { ...SETTINGS.logging, maxDurationMillis: 90 },
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof usePortalSettings>);

        rerender(<EnvironmentApiLoggingSettingsPage />);
        expect((screen.getByLabelText('Max Duration (in ms)') as HTMLInputElement).value).toBe('90');
    });

    it('does not clobber in-progress edits when the same environment refetches', () => {
        const { rerender } = renderPage();
        fireEvent.change(screen.getByLabelText('Max Duration (in ms)'), { target: { value: '60' } });

        mockUsePortalSettings.mockReturnValue({
            data: {
                ...SETTINGS,
                logging: { ...SETTINGS.logging, maxDurationMillis: 15000 },
            },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof usePortalSettings>);

        rerender(<EnvironmentApiLoggingSettingsPage />);
        expect((screen.getByLabelText('Max Duration (in ms)') as HTMLInputElement).value).toBe('60');
    });

    it('disables logging fields when the user cannot update environment settings', () => {
        mockUseHasPermission.mockReturnValue(false);
        renderPage();
        expect((screen.getByLabelText('Max Duration (in ms)') as HTMLInputElement).disabled).toBe(true);
        expect(screen.getByText(/You do not have permission to modify these settings/)).not.toBeNull();
    });

    it('discards in-progress edits back to the last saved state', () => {
        renderPage();
        fireEvent.change(screen.getByLabelText('Max Duration (in ms)'), { target: { value: '60' } });
        fireEvent.click(screen.getByRole('button', { name: /Discard/i }));
        expect((screen.getByLabelText('Max Duration (in ms)') as HTMLInputElement).value).toBe('15000');
        expect(screen.queryByRole('button', { name: /Save changes/i })).toBeNull();
    });

    it('shows a loading skeleton while portal settings are fetching', () => {
        mockUsePortalSettings.mockReturnValue({
            data: undefined,
            isLoading: true,
            isError: false,
        } as ReturnType<typeof usePortalSettings>);
        renderPage();
        expect(screen.queryByRole('heading', { name: 'API Logging' })).toBeNull();
        expect(screen.queryByLabelText('Max Duration (in ms)')).toBeNull();
    });

    it('shows an error when portal settings fail to load', () => {
        mockUsePortalSettings.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
        } as ReturnType<typeof usePortalSettings>);
        renderPage();
        expect(screen.getByText(/Failed to load settings/)).not.toBeNull();
        expect(screen.queryByLabelText('Max Duration (in ms)')).toBeNull();
    });

    it('saves audit trail and user toggles with the logging overlay', () => {
        const mutate = jest.fn();
        mockUseSaveEnvironmentPortalSettings.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useSaveEnvironmentPortalSettings>);
        renderPage();
        fireEvent.click(screen.getByLabelText('Enable audit on API Logging consultation'));
        fireEvent.click(screen.getByRole('button', { name: /Save changes/i }));
        expect(mutate).toHaveBeenCalledWith(
            expect.objectContaining({
                logging: expect.objectContaining({
                    audit: { enabled: false, trail: { enabled: true } },
                }),
            }),
            expect.any(Object),
        );
    });
});
