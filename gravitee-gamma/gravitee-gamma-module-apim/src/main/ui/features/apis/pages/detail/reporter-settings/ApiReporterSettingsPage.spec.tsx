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
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiReporterSettingsPage } from './ApiReporterSettingsPage';
import { useApiDetail } from '../../../hooks/useApiDetail';
import { updateApiAnalytics } from '../../../services/apis';
import type { Analytics } from '../../../types';

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
    updateApiAnalytics: jest.fn(() => Promise.resolve()),
}));

// ─── Helpers ──────────────────────────────────────────────────────────────────

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseApiDetail = useApiDetail as jest.Mock;
const mockUpdateApiAnalytics = updateApiAnalytics as jest.Mock;

const BASE_ANALYTICS: Analytics = {
    enabled: true,
    logging: {
        mode: { entrypoint: true, endpoint: false },
        phase: { request: true, response: false },
        content: { headers: false, payload: false },
    },
    tracing: { enabled: false, verbose: false },
    otelLogs: { enabled: false },
    sampling: { type: 'COUNT', value: '100' },
};

function renderPage(analytics: Analytics = BASE_ANALYTICS) {
    mockUseApiDetail.mockReturnValue({
        data: { id: 'api-1', name: 'Test API', analytics },
        isLoading: false,
        isError: false,
    });
    render(
        <MemoryRouter initialEntries={['/apis/api-1/reporter-settings']}>
            <Routes>
                <Route path="apis/:apiId/reporter-settings" element={<ApiReporterSettingsPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

beforeEach(() => {
    jest.clearAllMocks();
    mockUseHasPermission.mockReturnValue(true);
    mockUpdateApiAnalytics.mockResolvedValue(undefined);
});

/** Save / Discard bar is shown only after the user edits the form (isDirty). */
async function revealSaveBar(user: ReturnType<typeof userEvent.setup>) {
    await user.click(screen.getByRole('switch', { name: /enable analytics/i }));
}

// ─── 1. Renders page heading ──────────────────────────────────────────────────

it('renders the Reporter Settings page heading', () => {
    renderPage();

    expect(screen.getByRole('heading', { name: /reporter settings/i })).not.toBeNull();
});

// ─── 2. Permission: hides Save button when read-only ─────────────────────────

it('hides the Save button when the user lacks api-definition-u', () => {
    mockUseHasPermission.mockReturnValue(false);
    renderPage();

    expect(screen.queryByRole('button', { name: /save changes/i })).toBeNull();
});

// ─── 3. Verbose disabled when tracing not enabled ────────────────────────────

it('keeps the Verbose switch disabled when analytics is on but tracing is off', () => {
    renderPage({ ...BASE_ANALYTICS, tracing: { enabled: false, verbose: false } });

    const verboseSwitch = screen.getByRole('switch', { name: /verbose/i });
    expect(verboseSwitch).toBeDisabled();
});

// ─── 4. Verbose warning visible when both tracing and verbose are on ──────────

it('shows the verbose warning banner when tracing and verbose are both enabled', () => {
    renderPage({ ...BASE_ANALYTICS, tracing: { enabled: true, verbose: true } });

    expect(screen.getByText(/verbose mode significantly increases trace size/i)).not.toBeNull();
});

// ─── 5. Save preserves sampling ───────────────────────────────────────────────

it('calls updateApiAnalytics with sampling preserved from the loaded API', async () => {
    const user = userEvent.setup();
    renderPage();
    await revealSaveBar(user);

    await user.click(screen.getByRole('button', { name: /save changes/i }));

    await waitFor(() => expect(mockUpdateApiAnalytics).toHaveBeenCalledTimes(1));

    const [, , sentAnalytics] = mockUpdateApiAnalytics.mock.calls[0];
    expect(sentAnalytics.sampling).toEqual({ type: 'COUNT', value: '100' });
});

// ─── 6. OTel Logs disabled when tracing not enabled ──────────────────────────

it('keeps the OTel Logs switch disabled when tracing is off', () => {
    renderPage({ ...BASE_ANALYTICS, tracing: { enabled: false } });

    const otelSwitch = screen.getByRole('switch', { name: /otel logs/i });
    expect(otelSwitch).toBeDisabled();
});

// ─── 7. Save / Discard bar (isDirty) ─────────────────────────────────────────

it('does not show Save or Discard when the form is clean', () => {
    renderPage();

    expect(screen.queryByRole('button', { name: /save changes/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /discard/i })).toBeNull();
});

it('shows Save and Discard after the form is edited', async () => {
    const user = userEvent.setup();
    renderPage();
    await revealSaveBar(user);

    expect(screen.getByRole('button', { name: /save changes/i })).not.toBeNull();
    expect(screen.getByRole('button', { name: /discard/i })).not.toBeNull();
});

it('restores saved values and hides Save and Discard when Discard is clicked', async () => {
    const user = userEvent.setup();
    renderPage();
    await revealSaveBar(user);

    expect(screen.getByRole('switch', { name: /enable analytics/i })).not.toBeChecked();

    fireEvent.click(screen.getByRole('button', { name: /discard/i }));

    expect(screen.getByRole('switch', { name: /enable analytics/i })).toBeChecked();
    expect(screen.queryByRole('button', { name: /save changes/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /discard/i })).toBeNull();
});

it('does not show Discard for read-only users even after edits', async () => {
    mockUseHasPermission.mockReturnValue(false);
    const user = userEvent.setup();
    renderPage();

    await user.click(screen.getByRole('switch', { name: /enable analytics/i }));

    expect(screen.queryByRole('button', { name: /discard/i })).toBeNull();
});
