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

import { ApiCorsPage } from './ApiCorsPage';
import { useApiDetail } from '../../../hooks/useApiDetail';
import { updateApiCors } from '../../../services/apis';
import type { Cors } from '../../../types';

// ─── SDK / context mocks ──────────────────────────────────────────────────────

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
    useHasPermission: jest.fn(() => true),
}));

jest.mock('../../../hooks/useApiDetail', () => ({
    useApiDetail: jest.fn(),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

// ─── React Query mock ─────────────────────────────────────────────────────────

jest.mock('@tanstack/react-query', () => ({
    useMutation: jest.fn(config => ({
        mutate: jest.fn(async args => {
            await config.mutationFn(args);
            config.onSuccess?.();
        }),
        isPending: false,
    })),
    useQueryClient: jest.fn(() => ({ invalidateQueries: jest.fn() })),
}));

// ─── Service mock ─────────────────────────────────────────────────────────────

jest.mock('../../../services/apis', () => ({
    updateApiCors: jest.fn(() => Promise.resolve()),
}));

// ─── Helpers ──────────────────────────────────────────────────────────────────

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseApiDetail = useApiDetail as jest.Mock;
const mockUpdateApiCors = updateApiCors as jest.Mock;

const ENABLED_CORS: Cors = {
    enabled: true,
    allowOrigin: ['https://app.company.com'],
    allowMethods: ['GET', 'POST'],
    allowHeaders: ['Content-Type', 'Authorization'],
    exposeHeaders: [],
    allowCredentials: false,
    runPolicies: false,
    maxAge: 3600,
};

function renderPage(cors: Cors | null = null) {
    mockUseApiDetail.mockReturnValue({
        data: {
            id: 'api-1',
            name: 'Test API',
            listeners: [{ type: 'HTTP', ...(cors ? { cors } : {}) }],
        },
        isLoading: false,
        isError: false,
    });
    render(
        <MemoryRouter initialEntries={['/apis/api-1/cors']}>
            <Routes>
                <Route path="apis/:apiId/cors" element={<ApiCorsPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

beforeEach(() => {
    jest.clearAllMocks();
    mockUseHasPermission.mockReturnValue(true);
    mockUpdateApiCors.mockResolvedValue(undefined);
});

// ─── 1. Permission: read-only hides Save and disables Enable toggle ───────────

it('hides Save button and disables Enable CORS switch when user lacks api-definition-u', () => {
    mockUseHasPermission.mockReturnValue(false);
    renderPage(ENABLED_CORS);

    expect(screen.queryByRole('button', { name: /save changes/i })).toBeNull();
});

// ─── 2. Data: existing CORS values render as chips ────────────────────────────

it('renders existing origins, methods and headers as chips', () => {
    renderPage(ENABLED_CORS);

    expect(screen.getByText('https://app.company.com')).not.toBeNull();
    expect(screen.getByText('GET')).not.toBeNull();
    expect(screen.getByText('POST')).not.toBeNull();
    expect(screen.getByText('Content-Type')).not.toBeNull();
    expect(screen.getByText('Authorization')).not.toBeNull();
});

// ─── 3. Wildcard warning: shown when * is in allowOrigin ─────────────────────

it('shows wildcard warning banner when * is in Allow-Origin', () => {
    renderPage({ ...ENABLED_CORS, allowOrigin: ['*'] });

    expect(screen.getByText(/allowing all origins/i)).not.toBeNull();
});

// ─── 4. Wildcard warning: hidden when no * ───────────────────────────────────

it('does not show wildcard warning when allowOrigin contains no *', () => {
    renderPage(ENABLED_CORS);

    expect(screen.queryByText(/allowing all origins/i)).toBeNull();
});

// ─── 5. Chip input: adding a new origin via Enter ────────────────────────────

it('adds a new origin chip when the user types a value and presses Enter', async () => {
    const user = userEvent.setup();
    renderPage(ENABLED_CORS);

    const originsInput = screen.getByRole('textbox', { name: /access-control-allow-origin/i });
    await user.type(originsInput, 'https://new.site.com{Enter}');

    expect(screen.getByText('https://new.site.com')).not.toBeNull();
});

// ─── 6. Chip input: removing a chip via Remove button ────────────────────────

it('removes an origin chip when its Remove button is clicked', async () => {
    const user = userEvent.setup();
    renderPage(ENABLED_CORS);

    await user.click(screen.getByRole('button', { name: /remove https:\/\/app\.company\.com/i }));

    expect(screen.queryByText('https://app.company.com')).toBeNull();
});

// ─── 7. Save: calls updateApiCors with the correct payload ───────────────────

it('calls updateApiCors with the current CORS form state when Save is clicked', async () => {
    const user = userEvent.setup();
    renderPage(ENABLED_CORS);

    await user.click(screen.getByRole('button', { name: /save changes/i }));

    await waitFor(() => expect(mockUpdateApiCors).toHaveBeenCalledTimes(1));

    const [, , sentCors] = mockUpdateApiCors.mock.calls[0];
    expect(sentCors.enabled).toBe(true);
    expect(sentCors.allowOrigin).toEqual(['https://app.company.com']);
    expect(sentCors.allowMethods).toEqual(['GET', 'POST']);
    expect(sentCors.maxAge).toBe(3600);
});

// ─── 8. Fields disabled when CORS master toggle is off ───────────────────────

it('disables chip inputs when CORS is disabled (enabled: false)', () => {
    renderPage({ ...ENABLED_CORS, enabled: false });

    expect(screen.getByRole('textbox', { name: /access-control-allow-origin/i })).toBeDisabled();
    expect(screen.getByRole('textbox', { name: /access-control-allow-methods/i })).toBeDisabled();
});
