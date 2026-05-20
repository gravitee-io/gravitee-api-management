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
import { render, screen, waitFor, within, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiPropertiesPage } from './ApiPropertiesPage';
import { useApiDetail } from '../../../hooks/useApiDetail';
import { updateApiProperties } from '../../../services/apis';
import type { Property } from '../../../types/api';

// ─── SDK / context mocks ──────────────────────────────────────────────────────

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
    useHasPermission: jest.fn(() => true),
}));

jest.mock('../../../hooks/useApiDetail', () => ({
    useApiDetail: jest.fn(),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

// ─── React Query mock — simulates mutate calling mutationFn synchronously ─────

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

// ─── Service mock — captures what the component sends to the backend ──────────

jest.mock('../../../services/apis', () => ({
    updateApiProperties: jest.fn(() => Promise.resolve()),
}));

// ─── Helpers ──────────────────────────────────────────────────────────────────

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseApiDetail = useApiDetail as jest.Mock;
const mockUpdateApiProperties = updateApiProperties as jest.Mock;

const PLAIN_PROP = { key: 'backend.timeout', value: '5000', encrypted: false };
const ENCRYPTED_PROP = { key: 'secret.key', value: '***', encrypted: true };
const DYNAMIC_PROP = { key: 'dynamic.host', value: 'http://source', dynamic: true };

function renderPage(properties: Property[] = []) {
    mockUseApiDetail.mockReturnValue({
        data: { id: 'api-1', name: 'Test API', properties },
        isLoading: false,
    });
    render(
        <MemoryRouter initialEntries={['/apis/api-1/properties']}>
            <Routes>
                <Route path="apis/:apiId/properties" element={<ApiPropertiesPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

beforeEach(() => {
    jest.clearAllMocks();
    mockUseHasPermission.mockReturnValue(true);
    mockUpdateApiProperties.mockResolvedValue(undefined);
});

// ─── 1. Permission: read-only renders no write controls ───────────────────────

it('hides Add button and action dropdowns when user lacks api-definition-u', () => {
    mockUseHasPermission.mockReturnValue(false);
    renderPage([PLAIN_PROP]);

    expect(screen.queryByRole('button', { name: /add property/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /open actions/i })).toBeNull();
});

// ─── 2. Dynamic property: only Delete allowed ─────────────────────────────────

it('shows only Remove for a dynamic property — no Encrypt or Renew options', async () => {
    const user = userEvent.setup();
    renderPage([DYNAMIC_PROP]);

    await user.click(screen.getByRole('button', { name: /open actions/i }));

    expect(screen.queryByText(/encrypt value/i)).toBeNull();
    expect(screen.queryByText(/renew encryption/i)).toBeNull();
    expect(screen.getByText(/remove/i)).not.toBeNull();
});

// ─── 3. Encrypted property: only Renew encryption shown ──────────────────────

it('shows only Renew encryption for an encrypted property — no Edit value or Encrypt value', async () => {
    const user = userEvent.setup();
    renderPage([ENCRYPTED_PROP]);

    await user.click(screen.getByRole('button', { name: /open actions/i }));

    expect(screen.queryByText(/edit value/i)).toBeNull();
    expect(screen.queryByText(/encrypt value/i)).toBeNull();
    expect(screen.getByText(/renew encryption/i)).not.toBeNull();
});

// ─── 5. Add property: correct payload sent to service ─────────────────────────

it('adds a plain-text property and calls updateApiProperties with the new key/value', async () => {
    const user = userEvent.setup();
    renderPage([]);

    await user.click(screen.getByRole('button', { name: /add property/i }));

    const dialog = screen.getByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText(/key/i), { target: { value: 'my.key' } });
    fireEvent.change(within(dialog).getByLabelText(/value/i), { target: { value: 'my-value' } });
    await user.click(within(dialog).getByRole('button', { name: /add property/i }));

    await waitFor(() => expect(mockUpdateApiProperties).toHaveBeenCalledTimes(1));

    const [, , sentProperties] = mockUpdateApiProperties.mock.calls[0];
    expect(sentProperties).toEqual(
        expect.arrayContaining([expect.objectContaining({ key: 'my.key', value: 'my-value', encryptable: false })]),
    );
});

// ─── 6. Encrypt value: sets encryptable=true on the property ─────────────────

it('calls updateApiProperties with encryptable=true when Encrypt value is clicked', async () => {
    const user = userEvent.setup();
    renderPage([PLAIN_PROP]);

    await user.click(screen.getByRole('button', { name: /open actions/i }));
    await user.click(screen.getByText(/encrypt value/i));

    await waitFor(() => expect(mockUpdateApiProperties).toHaveBeenCalledTimes(1));

    const [, , sentProperties] = mockUpdateApiProperties.mock.calls[0];
    expect(sentProperties).toEqual(expect.arrayContaining([expect.objectContaining({ key: 'backend.timeout', encryptable: true })]));
});
