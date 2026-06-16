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
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, useNavigate } from 'react-router-dom';

import { ApiListTable } from './ApiListTable';
import type { ApiListItem } from '../../types';

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: jest.fn(),
}));

const mockNavigate = jest.fn();

function makeApi(overrides: Partial<ApiListItem> = {}): ApiListItem {
    return { id: 'api-1', name: 'Test API', apiVersion: '1.0', type: 'PROXY', definitionVersion: 'V4', ...overrides };
}

function renderTable(props: Partial<Parameters<typeof ApiListTable>[0]> = {}) {
    return render(
        <MemoryRouter>
            <ApiListTable apis={[]} isLoading={false} {...props} />
        </MemoryRouter>,
    );
}

describe('ApiListTable', () => {
    beforeEach(() => {
        (useNavigate as jest.Mock).mockReturnValue(mockNavigate);
    });

    afterEach(() => jest.clearAllMocks());

    it('renders skeleton rows when loading', async () => {
        const { container } = renderTable({ isLoading: true, skeletonRowCount: 3 });
        // Each skeleton row has 5 Skeleton elements. DataTable defers skeletons by ~200ms (loadingDelay)
        // to prevent flash on fast requests, so we have to wait for them.
        await waitFor(() => {
            const skeletons = container.querySelectorAll('[class*="animate-pulse"]');
            expect(skeletons.length).toBeGreaterThan(0);
        });
    });

    it('renders the empty state when no APIs are present', () => {
        renderTable({ apis: [], isLoading: false });
        expect(screen.queryByText(/no apis found/i)).not.toBeNull();
    });

    it('renders a row for each API with the name', () => {
        const api = makeApi({ name: 'My Service' });
        renderTable({ apis: [api] });
        expect(screen.queryByText('My Service')).not.toBeNull();
    });

    it('navigates to the overview page on row click', () => {
        const api = makeApi();
        renderTable({ apis: [api] });
        fireEvent.click(screen.getByText('Test API'));
        expect(mockNavigate).toHaveBeenCalledWith('api-1/overview');
    });

    it('renders the API actions button for each row', () => {
        const api = makeApi();
        renderTable({ apis: [api] });
        expect(screen.queryByRole('button', { name: 'API actions' })).not.toBeNull();
    });

    it('stops row click propagation when the actions button is clicked', () => {
        const api = makeApi();
        renderTable({ apis: [api] });
        fireEvent.click(screen.getByRole('button', { name: 'API actions' }));
        expect(mockNavigate).not.toHaveBeenCalledWith('api-1/overview');
    });

    it('opens the actions dropdown and shows navigation items', async () => {
        const user = userEvent.setup();
        const api = makeApi();
        renderTable({ apis: [api] });
        await user.click(screen.getByRole('button', { name: 'API actions' }));
        await waitFor(() => expect(screen.queryByText('View Details')).not.toBeNull());
        expect(screen.queryByText('Edit Configuration')).not.toBeNull();
        expect(screen.queryByText('View Analytics')).not.toBeNull();
    });

    describe('RuntimeStatusBadge', () => {
        it('shows "Started" badge for STARTED state', () => {
            renderTable({ apis: [makeApi({ state: 'STARTED' })] });
            expect(screen.queryByText('Started')).not.toBeNull();
        });

        it('shows "Stopped" badge for STOPPED state', () => {
            renderTable({ apis: [makeApi({ state: 'STOPPED' })] });
            expect(screen.queryByText('Stopped')).not.toBeNull();
        });

        it('shows "Closed" badge for CLOSED state', () => {
            renderTable({ apis: [makeApi({ state: 'CLOSED' })] });
            expect(screen.queryByText('Closed')).not.toBeNull();
        });
    });

    describe('SyncStatusBadge', () => {
        it('shows "In sync" badge for DEPLOYED state', () => {
            renderTable({ apis: [makeApi({ deploymentState: 'DEPLOYED' })] });
            expect(screen.queryByText('In sync')).not.toBeNull();
        });

        it('shows "Out of sync" badge for NEED_REDEPLOY state', () => {
            renderTable({ apis: [makeApi({ deploymentState: 'NEED_REDEPLOY' })] });
            expect(screen.queryByText('Out of sync')).not.toBeNull();
        });
    });

    it('renders access path when listener has an HTTP path', () => {
        const api = makeApi({ listeners: [{ type: 'HTTP', paths: [{ path: '/my-api' }] }] });
        renderTable({ apis: [api] });
        expect(screen.queryByText('/my-api')).not.toBeNull();
    });

    it('renders owner display name', () => {
        const api = makeApi({ primaryOwner: { displayName: 'Jane Doe' } });
        renderTable({ apis: [api] });
        expect(screen.queryByText('Jane Doe')).not.toBeNull();
    });
});
