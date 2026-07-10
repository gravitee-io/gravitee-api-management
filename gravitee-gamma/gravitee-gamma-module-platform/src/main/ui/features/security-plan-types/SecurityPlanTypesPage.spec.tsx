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

import { usePortalSettings } from './hooks/usePortalSettings';
import { useSavePortalSettings } from './hooks/useSavePortalSettings';
import { SecurityPlanTypesPage } from './SecurityPlanTypesPage';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
    useEnvironment: jest.fn(),
}));

jest.mock('./hooks/usePortalSettings', () => ({
    usePortalSettings: jest.fn(),
}));

jest.mock('./hooks/useSavePortalSettings', () => ({
    useSavePortalSettings: jest.fn(),
}));

const mockNotifySuccess = jest.fn();
const mockNotifyError = jest.fn();
jest.mock('../../shared/notify', () => ({
    notify: {
        success: (msg: string) => mockNotifySuccess(msg),
        error: (err: unknown, fallback?: string) => mockNotifyError(err, fallback),
    },
}));

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseEnvironment = jest.mocked(useEnvironment);
const mockUsePortalSettings = jest.mocked(usePortalSettings);
const mockUseSavePortalSettings = jest.mocked(useSavePortalSettings);

beforeAll(() => {
    global.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    } as typeof ResizeObserver;
});

const SETTINGS_ALL_ENABLED = {
    company: { name: 'Acme Corp' },
    cors: { allowOrigin: ['https://portal.example.com'] },
    plan: {
        security: {
            keyless: { enabled: true },
            apikey: { enabled: true },
            customApiKey: { enabled: true },
            customApiKeyReuse: { enabled: true },
            sharedApiKey: { enabled: true },
            oauth2: { enabled: true },
            jwt: { enabled: true },
            push: { enabled: true },
            mtls: { enabled: true },
        },
        validation: { enabled: true },
    },
};

const SETTINGS_PARTIAL = {
    plan: {
        security: {
            keyless: { enabled: true },
            apikey: { enabled: true },
            customApiKey: { enabled: false },
            customApiKeyReuse: { enabled: false },
            sharedApiKey: { enabled: true },
            oauth2: { enabled: true },
            jwt: { enabled: false },
            push: { enabled: false },
            mtls: { enabled: true },
        },
    },
};

function createTestContext() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return { Wrapper };
}

function getToggle(label: string) {
    return screen.getByLabelText(label);
}

function expectChecked(label: string, checked: boolean) {
    expect(getToggle(label).getAttribute('aria-checked')).toBe(checked ? 'true' : 'false');
}

function expectDisabled(label: string) {
    expect(getToggle(label).hasAttribute('disabled')).toBe(true);
}

let mockMutate: jest.Mock;

function renderPage() {
    const { Wrapper } = createTestContext();
    return render(
        <Wrapper>
            <SecurityPlanTypesPage />
        </Wrapper>,
    );
}

describe('SecurityPlanTypesPage', () => {
    beforeEach(() => {
        mockMutate = jest.fn();
        mockUseHasPermission.mockReturnValue(true);
        mockUseEnvironment.mockReturnValue({ id: 'env-1', organizationId: 'org-1' });
        mockUsePortalSettings.mockReturnValue({ data: SETTINGS_ALL_ENABLED, isLoading: false, isError: false } as unknown as ReturnType<
            typeof usePortalSettings
        >);
        mockUseSavePortalSettings.mockReturnValue({
            mutate: mockMutate.mockImplementation((_payload: unknown, opts?: { onSuccess?: () => void }) => opts?.onSuccess?.()),
            isPending: false,
        } as unknown as ReturnType<typeof useSavePortalSettings>);
        mockNotifySuccess.mockClear();
        mockNotifyError.mockClear();
    });

    afterEach(() => jest.clearAllMocks());

    it('renders the page title and description', () => {
        renderPage();
        expect(screen.queryByText('Security Plan Types')).not.toBeNull();
        expect(screen.queryByText('Configure which security plan types are available for APIs across the environment.')).not.toBeNull();
    });

    it('renders all security plan type toggles', () => {
        renderPage();
        expect(screen.queryByText('Keyless plans')).not.toBeNull();
        expect(screen.queryByText('API Key plans')).not.toBeNull();
        expect(screen.queryByText('Allow custom API Key')).not.toBeNull();
        expect(screen.queryByText('Allow custom API Key reuse')).not.toBeNull();
        expect(screen.queryByText('Allow to share API Key on an application')).not.toBeNull();
        expect(screen.queryByText('OAuth2 plans')).not.toBeNull();
        expect(screen.queryByText('JWT plans')).not.toBeNull();
        expect(screen.queryByText('Push plans')).not.toBeNull();
        expect(screen.queryByText('mTLS plans')).not.toBeNull();
    });

    it('shows V4-only descriptions for Push and mTLS', () => {
        renderPage();
        const descriptions = screen.getAllByText('Only available for API V4');
        expect(descriptions.length).toBe(2);
    });

    it('reflects loaded settings state', () => {
        mockUsePortalSettings.mockReturnValue({ data: SETTINGS_PARTIAL, isLoading: false } as unknown as ReturnType<
            typeof usePortalSettings
        >);
        renderPage();

        expectChecked('Keyless plans', true);
        expectChecked('API Key plans', true);
        expectChecked('Allow custom API Key', false);
        expectChecked('Allow custom API Key reuse', false);
        expectChecked('Allow to share API Key on an application', true);
        expectChecked('OAuth2 plans', true);
        expectChecked('JWT plans', false);
        expectChecked('Push plans', false);
        expectChecked('mTLS plans', true);
    });

    it('does not show Save/Discard buttons when no changes are made', () => {
        renderPage();
        expect(screen.queryByRole('button', { name: /Save changes/i })).toBeNull();
        expect(screen.queryByRole('button', { name: /Discard/i })).toBeNull();
    });

    it('shows Save/Discard after toggling', () => {
        renderPage();
        fireEvent.click(getToggle('JWT plans'));
        expect(screen.queryByText('Save changes')).not.toBeNull();
        expect(screen.queryByText('Discard')).not.toBeNull();
    });

    it('reverts changes when Discard is clicked', () => {
        renderPage();
        expectChecked('JWT plans', true);
        fireEvent.click(getToggle('JWT plans'));
        expectChecked('JWT plans', false);
        fireEvent.click(screen.getByText('Discard'));
        expectChecked('JWT plans', true);
        expect(screen.queryByText('Save changes')).toBeNull();
    });

    it('calls save mutation with merged full settings payload', () => {
        renderPage();
        fireEvent.click(getToggle('Push plans'));
        fireEvent.click(screen.getByText('Save changes'));

        expect(mockMutate).toHaveBeenCalledWith(
            {
                company: { name: 'Acme Corp' },
                cors: { allowOrigin: ['https://portal.example.com'] },
                plan: {
                    security: {
                        keyless: { enabled: true },
                        apikey: { enabled: true },
                        customApiKey: { enabled: true },
                        customApiKeyReuse: { enabled: true },
                        sharedApiKey: { enabled: true },
                        oauth2: { enabled: true },
                        jwt: { enabled: true },
                        push: { enabled: false },
                        mtls: { enabled: true },
                    },
                    validation: { enabled: true },
                },
            },
            expect.objectContaining({ onSuccess: expect.any(Function) }),
        );
    });

    describe('API Key cascade logic', () => {
        it('disables sub-toggles when API Key is turned off', () => {
            renderPage();
            fireEvent.click(getToggle('API Key plans'));

            expectChecked('API Key plans', false);
            expectChecked('Allow custom API Key', false);
            expectChecked('Allow custom API Key reuse', false);
            expectChecked('Allow to share API Key on an application', false);
        });

        it('disables custom API Key reuse when custom API Key is turned off', () => {
            renderPage();
            fireEvent.click(getToggle('Allow custom API Key'));

            expectChecked('Allow custom API Key', false);
            expectChecked('Allow custom API Key reuse', false);
        });

        it('sub-toggles are disabled (non-interactive) when API Key is off', () => {
            renderPage();
            fireEvent.click(getToggle('API Key plans'));

            expectDisabled('Allow custom API Key');
            expectDisabled('Allow custom API Key reuse');
            expectDisabled('Allow to share API Key on an application');
        });

        it('custom API Key reuse is disabled when custom API Key is off', () => {
            mockUsePortalSettings.mockReturnValue({ data: SETTINGS_PARTIAL, isLoading: false } as unknown as ReturnType<
                typeof usePortalSettings
            >);
            renderPage();

            expectDisabled('Allow custom API Key reuse');
        });
    });

    describe('permission gating', () => {
        beforeEach(() => {
            mockUseHasPermission.mockReturnValue(false);
        });

        it('disables all toggles when user lacks edit permission', () => {
            renderPage();
            expectDisabled('Keyless plans');
            expectDisabled('API Key plans');
            expectDisabled('Allow custom API Key');
            expectDisabled('Allow custom API Key reuse');
            expectDisabled('Allow to share API Key on an application');
            expectDisabled('OAuth2 plans');
            expectDisabled('JWT plans');
            expectDisabled('Push plans');
            expectDisabled('mTLS plans');
        });

        it('does not show Save/Discard when user lacks permission', () => {
            renderPage();
            expect(screen.queryByText('Save changes')).toBeNull();
            expect(screen.queryByText('Discard')).toBeNull();
        });

        it('shows read-only alert banner', () => {
            renderPage();
            expect(
                screen.queryByText('You do not have permission to modify these settings. Contact your administrator for access.'),
            ).not.toBeNull();
        });
    });

    it('shows loading skeleton while settings are fetched', () => {
        mockUsePortalSettings.mockReturnValue({ data: undefined, isLoading: true, isError: false } as unknown as ReturnType<
            typeof usePortalSettings
        >);
        renderPage();
        expect(screen.queryByText('Security Plan Types')).toBeNull();
        expect(screen.queryByText('Keyless plans')).toBeNull();
    });

    it('shows an error message and blocks editing when settings fail to load', () => {
        mockUsePortalSettings.mockReturnValue({ data: undefined, isLoading: false, isError: true } as unknown as ReturnType<
            typeof usePortalSettings
        >);
        renderPage();
        expect(screen.queryByText('Security Plan Types')).not.toBeNull();
        expect(screen.queryByText('Failed to load settings. Please refresh and try again.')).not.toBeNull();
        expect(screen.queryByText('Keyless plans')).toBeNull();
        expect(screen.queryByRole('button', { name: /Save changes/i })).toBeNull();
    });

    it('shows Enabled/Disabled labels next to toggles', () => {
        mockUsePortalSettings.mockReturnValue({ data: SETTINGS_PARTIAL, isLoading: false } as unknown as ReturnType<
            typeof usePortalSettings
        >);
        renderPage();
        const enabledLabels = screen.getAllByText('Enabled');
        const disabledLabels = screen.getAllByText('Disabled');
        expect(enabledLabels.length).toBe(5);
        expect(disabledLabels.length).toBe(4);
    });

    it('re-initializes toggles when the environment changes', () => {
        const { Wrapper } = createTestContext();
        const { rerender } = render(
            <Wrapper>
                <SecurityPlanTypesPage />
            </Wrapper>,
        );

        expectChecked('JWT plans', true);

        mockUseEnvironment.mockReturnValue({ id: 'env-2', organizationId: 'org-1' });
        mockUsePortalSettings.mockReturnValue({ data: SETTINGS_PARTIAL, isLoading: false } as unknown as ReturnType<
            typeof usePortalSettings
        >);

        rerender(
            <Wrapper>
                <SecurityPlanTypesPage />
            </Wrapper>,
        );

        expectChecked('JWT plans', false);
        expectChecked('Push plans', false);
    });

    describe('system readonly settings', () => {
        const SETTINGS_WITH_READONLY = {
            ...SETTINGS_ALL_ENABLED,
            metadata: {
                readonly: ['plan.security.jwt.enabled', 'plan.security.push.enabled'],
            },
        };

        beforeEach(() => {
            mockUsePortalSettings.mockReturnValue({
                data: SETTINGS_WITH_READONLY,
                isLoading: false,
                isError: false,
            } as unknown as ReturnType<typeof usePortalSettings>);
        });

        it('disables toggles locked by metadata.readonly', () => {
            renderPage();

            expectDisabled('JWT plans');
            expectDisabled('Push plans');
            expect(getToggle('Keyless plans').hasAttribute('disabled')).toBe(false);
        });

        it('shows Save when an editable key changes even if cascade updates a readonly key', () => {
            mockUsePortalSettings.mockReturnValue({
                data: {
                    ...SETTINGS_ALL_ENABLED,
                    metadata: {
                        readonly: ['plan.security.apikey.allowCustom.enabled'],
                    },
                },
                isLoading: false,
                isError: false,
            } as unknown as ReturnType<typeof usePortalSettings>);

            renderPage();
            fireEvent.click(getToggle('API Key plans'));

            expectChecked('Allow custom API Key', false);
            expect(screen.queryByText('Save changes')).not.toBeNull();
        });

        it('preserves readonly values in the save payload', () => {
            renderPage();

            fireEvent.click(getToggle('Keyless plans'));
            fireEvent.click(screen.getByText('Save changes'));

            expect(mockMutate).toHaveBeenCalledWith(
                expect.objectContaining({
                    plan: expect.objectContaining({
                        security: expect.objectContaining({
                            keyless: { enabled: false },
                            jwt: { enabled: true },
                            push: { enabled: true },
                        }),
                    }),
                }),
                expect.objectContaining({ onSuccess: expect.any(Function) }),
            );
        });

        it('disables shared API Key when metadata uses the backend readonly key', () => {
            mockUsePortalSettings.mockReturnValue({
                data: {
                    ...SETTINGS_ALL_ENABLED,
                    metadata: {
                        readonly: ['plan.security.apikey.allowShared.enabled'],
                    },
                },
                isLoading: false,
                isError: false,
            } as unknown as ReturnType<typeof usePortalSettings>);

            renderPage();

            expectDisabled('Allow to share API Key on an application');
            expect(getToggle('API Key plans').hasAttribute('disabled')).toBe(false);
        });

        it('wraps gravitee-locked toggles with the system readonly tooltip trigger', () => {
            mockUsePortalSettings.mockReturnValue({
                data: {
                    ...SETTINGS_ALL_ENABLED,
                    plan: {
                        ...SETTINGS_ALL_ENABLED.plan,
                        security: {
                            ...SETTINGS_ALL_ENABLED.plan.security,
                            customApiKeyReuse: { enabled: false },
                        },
                    },
                    metadata: {
                        readonly: ['plan.security.apikey.allowShared.enabled', 'plan.security.apikey.allowCustomReuse.enabled'],
                    },
                },
                isLoading: false,
                isError: false,
            } as unknown as ReturnType<typeof usePortalSettings>);

            renderPage();

            expect(getToggle('Allow to share API Key on an application').closest('[data-system-readonly="true"]')).not.toBeNull();
            expect(getToggle('Allow custom API Key reuse').closest('[data-system-readonly="true"]')).not.toBeNull();
            expect(getToggle('Keyless plans').closest('[data-system-readonly="true"]')).toBeNull();
        });

        it('does not wrap cascade-disabled toggles with the system readonly tooltip trigger', () => {
            renderPage();
            fireEvent.click(getToggle('API Key plans'));

            expect(getToggle('Allow custom API Key').closest('[data-system-readonly="true"]')).toBeNull();
        });
    });
});
