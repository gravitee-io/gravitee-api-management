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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiFailoverPage } from './ApiFailoverPage';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import { updateApiFailover } from '../../../services/apis';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
    useHasPermission: jest.fn(() => true),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('@tanstack/react-query', () => ({
    useMutation: jest.fn(config => ({
        mutate: jest.fn((args: unknown) => {
            void config.mutationFn(args);
        }),
        isPending: false,
    })),
    useQueryClient: jest.fn(() => ({ invalidateQueries: jest.fn() })),
}));

jest.mock('../../../services/apis', () => ({
    updateApiFailover: jest.fn(() => Promise.resolve()),
}));

jest.mock('../../../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

jest.mock('../../../context/ApiDetailContext', () => ({
    useApiDetailContext: jest.fn(),
}));

const mockContext = jest.mocked(useApiDetailContext);
const mockUpdateApiFailover = updateApiFailover as jest.Mock;

function renderPage(perSubscription: boolean) {
    mockContext.mockReturnValue({
        api: {
            id: 'api-1',
            name: 'Test API',
            listeners: [{ type: 'HTTP' }],
            failover: { enabled: true, perSubscription },
        },
        isLoading: false,
        permissionsReady: true,
    } as ReturnType<typeof useApiDetailContext>);

    render(
        <MemoryRouter initialEntries={['/apis/api-1/failover']}>
            <Routes>
                <Route path="apis/:apiId/failover" element={<ApiFailoverPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

const SHARED_WARNING = /circuit breaker will be configured for the whole API/i;

beforeEach(() => {
    jest.clearAllMocks();
});

it('shows the shared circuit-breaker warning when per-subscription is off', () => {
    renderPage(false);
    expect(screen.getByText(SHARED_WARNING)).not.toBeNull();
});

it('hides the shared circuit-breaker warning when per-subscription is on', () => {
    renderPage(true);
    expect(screen.queryByText(SHARED_WARNING)).toBeNull();
});

it('asks for confirmation before saving a shared circuit breaker', async () => {
    const user = userEvent.setup();
    renderPage(false);

    await user.click(screen.getByRole('button', { name: /save changes/i }));

    expect(screen.getByText(/global circuit breaker/i)).not.toBeNull();
    expect(mockUpdateApiFailover).not.toHaveBeenCalled();

    await user.click(screen.getByRole('button', { name: /yes, update it/i }));
    expect(mockUpdateApiFailover).toHaveBeenCalledWith('DEFAULT', 'api-1', expect.objectContaining({ perSubscription: false }));
});

it('saves directly when per-subscription is on', async () => {
    const user = userEvent.setup();
    renderPage(true);

    await user.click(screen.getByRole('button', { name: /save changes/i }));

    expect(screen.queryByText(/global circuit breaker/i)).toBeNull();
    expect(mockUpdateApiFailover).toHaveBeenCalledTimes(1);
});
