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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, fireEvent, render, screen } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useHasPermission } from '@gravitee/gamma-modules-sdk';

import { ApiReviewSettingsPage } from './ApiReviewSettingsPage';
import { usePortalSettings } from '../features/security-plan-types/hooks/usePortalSettings';
import { useSavePortalSettings } from '../features/security-plan-types/hooks/useSavePortalSettings';
import type { PortalSettings } from '../features/security-plan-types/services/portalSettings';
import { ApimApiError } from '../shared/api/apimClient';
import { useForbiddenResourceRedirect } from '../shared/hooks/useForbiddenResourceRedirect';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
    useEnvironment: () => ({ id: 'env-1' }),
}));

jest.mock('../shared/hooks/useForbiddenResourceRedirect');

jest.mock('../features/security-plan-types/hooks/usePortalSettings', () => ({
    usePortalSettings: jest.fn(),
}));

jest.mock('../features/security-plan-types/hooks/useSavePortalSettings', () => ({
    useSavePortalSettings: jest.fn(),
}));

// The rules card has its own suite; here it only needs to be present on the page.
jest.mock('../features/api-review/components/QualityRulesCard', () => ({
    QualityRulesCard: () => <div data-testid="quality-rules-card" />,
}));

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUsePortalSettings = jest.mocked(usePortalSettings);
const mockUseSavePortalSettings = jest.mocked(useSavePortalSettings);
const mockUseForbiddenResourceRedirect = jest.mocked(useForbiddenResourceRedirect);

const SETTINGS: PortalSettings = {
    company: { name: 'Acme' },
    apiScore: { enabled: true },
    apiReview: { enabled: false },
    plan: { security: { keyless: { enabled: true } } },
};

let queryClient: QueryClient;

function renderPage() {
    queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return render(<ApiReviewSettingsPage />, { wrapper: Wrapper });
}

function mockSettings(settings: PortalSettings | undefined, state: Partial<{ isLoading: boolean; isError: boolean; error: unknown }> = {}) {
    mockUsePortalSettings.mockReturnValue({
        data: settings,
        isLoading: false,
        isError: false,
        error: null,
        ...state,
    } as unknown as ReturnType<typeof usePortalSettings>);
}

describe('ApiReviewSettingsPage', () => {
    let mutate: jest.Mock;

    beforeAll(() => {
        global.ResizeObserver = class ResizeObserver {
            observe() {}
            unobserve() {}
            disconnect() {}
        } as typeof ResizeObserver;
    });

    beforeEach(() => {
        mutate = jest.fn();
        mockUseForbiddenResourceRedirect.mockImplementation(() => undefined);
        mockUseHasPermission.mockReturnValue(true);
        mockSettings(SETTINGS);
        mockUseSavePortalSettings.mockReturnValue({ mutate, isPending: false } as unknown as ReturnType<typeof useSavePortalSettings>);
    });

    afterEach(() => jest.clearAllMocks());

    it('renders the title, both toggles from the settings, and the rules card', () => {
        renderPage();
        expect(screen.getByRole('heading', { name: 'API Review' })).toBeInTheDocument();
        expect(screen.getByLabelText('Enable API Score')).toHaveAttribute('aria-checked', 'true');
        expect(screen.getByLabelText('Enable API Review')).toHaveAttribute('aria-checked', 'false');
        expect(screen.getByTestId('quality-rules-card')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /save changes/i })).not.toBeInTheDocument();
    });

    it('saves both toggles on top of the existing portal settings', () => {
        renderPage();
        fireEvent.click(screen.getByLabelText('Enable API Review'));
        fireEvent.click(screen.getByRole('button', { name: /save changes/i }));

        expect(mutate).toHaveBeenCalledWith(
            {
                company: { name: 'Acme' },
                apiScore: { enabled: true },
                apiReview: { enabled: true },
                plan: { security: { keyless: { enabled: true } } },
            },
            expect.objectContaining({ onSuccess: expect.any(Function) }),
        );
    });

    it('refreshes the portal configuration after a save so the API Score nav item follows the toggle', () => {
        renderPage();
        const invalidate = jest.spyOn(queryClient, 'invalidateQueries');
        fireEvent.click(screen.getByLabelText('Enable API Score'));
        fireEvent.click(screen.getByRole('button', { name: /save changes/i }));

        const options = mutate.mock.calls[0]?.[1] as { onSuccess: () => void };
        act(() => options.onSuccess());

        expect(invalidate).toHaveBeenCalledWith({ queryKey: ['environment-portal-configuration'] });
        expect(screen.queryByRole('button', { name: /save changes/i })).not.toBeInTheDocument();
    });

    it('discards local edits', () => {
        renderPage();
        fireEvent.click(screen.getByLabelText('Enable API Review'));
        expect(screen.getByLabelText('Enable API Review')).toHaveAttribute('aria-checked', 'true');
        fireEvent.click(screen.getByRole('button', { name: /discard/i }));
        expect(screen.getByLabelText('Enable API Review')).toHaveAttribute('aria-checked', 'false');
        expect(mutate).not.toHaveBeenCalled();
    });

    it('locks the toggles and hides the save bar without environment-settings-u', () => {
        mockUseHasPermission.mockReturnValue(false);
        renderPage();
        expect(screen.getByText(/do not have permission to modify these settings/i)).toBeInTheDocument();
        expect(screen.getByLabelText('Enable API Score')).toBeDisabled();
        expect(screen.getByLabelText('Enable API Review')).toBeDisabled();
    });

    it('keeps a system-pinned toggle read-only', () => {
        mockSettings({ ...SETTINGS, metadata: { readonly: ['api.review.enabled'] } });
        renderPage();
        expect(screen.getByLabelText('Enable API Review')).toBeDisabled();
        expect(screen.getByLabelText('Enable API Score')).toBeEnabled();
    });

    it('shows the loading skeleton and the load error', () => {
        mockSettings(undefined, { isLoading: true });
        const { unmount } = renderPage();
        expect(screen.queryByRole('heading', { name: 'API Review' })).not.toBeInTheDocument();
        unmount();

        mockSettings(undefined, { isError: true, error: new Error('boom') });
        renderPage();
        expect(screen.getByText(/failed to load settings/i)).toBeInTheDocument();
    });

    it('hands a 403 to the forbidden redirect instead of rendering an error', () => {
        mockSettings(undefined, { isError: true, error: new ApimApiError(403, 'Forbidden') });
        renderPage();
        expect(mockUseForbiddenResourceRedirect).toHaveBeenCalledWith(
            expect.objectContaining({ isForbidden: true, navItemKey: 'api-review', redirectTo: '../applications' }),
        );
        expect(screen.queryByText(/failed to load settings/i)).not.toBeInTheDocument();
    });
});
