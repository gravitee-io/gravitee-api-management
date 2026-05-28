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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiDynamicPropertiesPage } from './ApiDynamicPropertiesPage';
import { ApiDetailContext } from '../../../../context/ApiDetailContext';
import { updateDynamicProperties } from '../../../../services/apis';
import type { ApiDetailDto, DynamicPropertyConfig } from '../../../../types';

// ─── SDK / context mocks ──────────────────────────────────────────────────────

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
    useHasPermission: jest.fn(() => true),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

// ─── React Query mock ─────────────────────────────────────────────────────────

jest.mock('@tanstack/react-query', () => ({
    useMutation: jest.fn(config => ({
        mutate: jest.fn(async (args: unknown) => {
            await config.mutationFn(args);
            config.onSuccess?.();
        }),
        isPending: false,
        isError: false,
    })),
    useQueryClient: jest.fn(() => ({ invalidateQueries: jest.fn() })),
}));

// ─── Service mock ─────────────────────────────────────────────────────────────

jest.mock('../../../../services/apis', () => ({
    updateDynamicProperties: jest.fn(() => Promise.resolve()),
}));

// ─── Helpers ──────────────────────────────────────────────────────────────────

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUpdateDynamicProperties = updateDynamicProperties as jest.Mock;

const DYNAMIC_CONFIG: DynamicPropertyConfig = {
    enabled: true,
    provider: 'HTTP',
    schedule: '0 */5 * * * *',
    configuration: {
        method: 'GET',
        url: 'https://api.example.com/properties',
        specification: '[{"operation":"default","spec":{}}]',
    },
};

function makeApi(overrides: Partial<ApiDetailDto> = {}): ApiDetailDto {
    return {
        id: 'api-1',
        name: 'Test API',
        ...overrides,
    };
}

function renderPage(api: ApiDetailDto, isLoading = false) {
    render(
        <ApiDetailContext.Provider value={{ api, isLoading, permissionsReady: true }}>
            <MemoryRouter initialEntries={['/apis/api-1/properties/dynamic']}>
                <Routes>
                    <Route path="apis/:apiId/properties/dynamic" element={<ApiDynamicPropertiesPage />} />
                </Routes>
            </MemoryRouter>
        </ApiDetailContext.Provider>,
    );
}

beforeEach(() => {
    jest.clearAllMocks();
    mockUseHasPermission.mockReturnValue(true);
    mockUpdateDynamicProperties.mockResolvedValue(undefined);
});

/** Save / Discard bar is shown only after the user edits the form (isDirty). */
async function revealSaveBar(user: ReturnType<typeof userEvent.setup>) {
    const enabledSwitch = screen.getByRole('switch', { name: /enable dynamic properties/i });
    await user.click(enabledSwitch);
    await user.click(enabledSwitch);
}

// ─── 1. Loading: renders skeletons ────────────────────────────────────────────

it('renders loading skeletons while isLoading is true', () => {
    renderPage(makeApi(), true);
    // No page heading rendered while loading
    expect(screen.queryByText(/dynamic properties/i)).toBeNull();
});

// ─── 2. Renders: existing config seeds the form ───────────────────────────────

it('seeds URL and schedule fields from existing dynamic property config', () => {
    renderPage(makeApi({ services: { dynamicProperty: DYNAMIC_CONFIG } }));

    expect((screen.getByPlaceholderText(/https:\/\/api\.example\.com/i) as HTMLInputElement).value).toBe(
        'https://api.example.com/properties',
    );
    expect((screen.getByDisplayValue('0 */5 * * * *') as HTMLInputElement).value).toBe('0 */5 * * * *');
});

// ─── 3. Permission: Save bar hidden for read-only users ───────────────────────

it('does not render the Save button when user lacks api-definition-u', () => {
    mockUseHasPermission.mockReturnValue(false);
    renderPage(makeApi({ services: { dynamicProperty: DYNAMIC_CONFIG } }));

    expect(screen.queryByRole('button', { name: /save changes/i })).toBeNull();
});

// ─── 4. Kubernetes: read-only banner shown ────────────────────────────────────

it('shows the Kubernetes read-only banner when origin is KUBERNETES', () => {
    renderPage(
        makeApi({
            services: { dynamicProperty: DYNAMIC_CONFIG },
            definitionContext: { origin: 'KUBERNETES' },
        }),
    );

    expect(screen.getByText(/kubernetes operator/i)).not.toBeNull();
    expect(screen.queryByRole('button', { name: /save changes/i })).toBeNull();
});

// ─── 5. Validation: URL required when enabled ─────────────────────────────────

it('shows URL error and does not call service when enabled=true and URL is empty', async () => {
    const user = userEvent.setup();
    // Start with enabled=true but no URL
    renderPage(
        makeApi({
            services: {
                dynamicProperty: { enabled: true, provider: 'HTTP', schedule: '0 */5 * * * *', configuration: { url: '' } },
            },
        }),
    );

    await revealSaveBar(user);
    await user.click(screen.getByRole('button', { name: /save changes/i }));

    expect(screen.getByText(/url is required/i)).not.toBeNull();
    expect(mockUpdateDynamicProperties).not.toHaveBeenCalled();
});

// ─── 6. Validation: cron error shown for invalid expression ──────────────────

it('shows cron format error and blocks save for an invalid cron expression', async () => {
    const user = userEvent.setup();
    renderPage(makeApi({ services: { dynamicProperty: DYNAMIC_CONFIG } }));

    const cronInput = screen.getByDisplayValue('0 */5 * * * *');
    await user.clear(cronInput);
    await user.type(cronInput, 'not-a-cron');

    await user.click(screen.getByRole('button', { name: /save changes/i }));

    expect(screen.getByText(/must have exactly 6 fields/i)).not.toBeNull();
    expect(mockUpdateDynamicProperties).not.toHaveBeenCalled();
});

// ─── 7. Save: correct DTO sent to service ─────────────────────────────────────

it('calls updateDynamicProperties with the correct payload on successful save', async () => {
    const user = userEvent.setup();
    renderPage(makeApi({ services: { dynamicProperty: DYNAMIC_CONFIG } }));

    await revealSaveBar(user);
    await user.click(screen.getByRole('button', { name: /save changes/i }));

    await waitFor(() => expect(mockUpdateDynamicProperties).toHaveBeenCalledTimes(1));

    const [, , sentDto] = mockUpdateDynamicProperties.mock.calls[0];
    expect(sentDto.enabled).toBe(true);
    expect(sentDto.provider).toBe('HTTP');
    expect(sentDto.configuration.url).toBe('https://api.example.com/properties');
    expect(sentDto.configuration.method).toBe('GET');
});

// ─── 8. Enable toggle: config fields disabled when feature is off ─────────────

it('disables the URL input while enabled=false', () => {
    renderPage(
        makeApi({
            services: {
                dynamicProperty: { ...DYNAMIC_CONFIG, enabled: false },
            },
        }),
    );

    expect(screen.getByPlaceholderText(/https:\/\/api\.example\.com/i)).toBeDisabled();
});

// ─── 9. SSL trust-all: warning shown ─────────────────────────────────────────

it('shows the trust-all security warning when trustAll is enabled', async () => {
    const user = userEvent.setup();
    renderPage(makeApi({ services: { dynamicProperty: DYNAMIC_CONFIG } }));

    // Expand the SSL section
    await user.click(screen.getByRole('button', { name: /ssl \/ tls/i }));

    // Toggle trust-all on
    const trustAllSwitch = screen.getByRole('switch', { name: /trust all certificates/i });
    await user.click(trustAllSwitch);

    expect(screen.getByText(/man-in-the-middle/i)).not.toBeNull();
});

// ─── 10. No stale data: form seeds from DEFAULT_FORM when no config exists ────

it('seeds form from defaults when the API has no dynamic property config', () => {
    renderPage(makeApi({ services: undefined }));

    // Default schedule is present
    expect(screen.getByDisplayValue('0 */5 * * * *')).not.toBeNull();
    // Enabled toggle is off by default
    expect(screen.getByRole('switch', { name: /enable dynamic properties/i })).not.toBeChecked();
});
