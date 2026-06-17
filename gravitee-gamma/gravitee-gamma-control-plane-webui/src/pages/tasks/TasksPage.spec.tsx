/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { TasksPage } from './TasksPage';
import { TEST_MANAGEMENT_BASE } from '../../testing/factories';
import { resetAllStores, respondWith, seedBootstrap, seedEnvironments } from '../../testing/helpers';

function renderTasksPage() {
    return render(
        <MemoryRouter initialEntries={['/environments/env-1/tasks']}>
            <Routes>
                <Route path="/environments/:envHrid/tasks" element={<TasksPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('TasksPage', () => {
    beforeEach(() => {
        resetAllStores();
        seedBootstrap();
        seedEnvironments();
    });

    it('renders pending tasks served by the API with their surface labels', async () => {
        renderTasksPage();

        expect(await screen.findByText(/Booking MCP Server/)).toBeTruthy();
        expect(screen.getByText(/Baggage Tracking API/)).toBeTruthy();
        expect(screen.getByText(/Loyalty API/)).toBeTruthy();
        expect(screen.getByText('MCP')).toBeTruthy();
    });

    it('filters the list when a category chip is selected', async () => {
        renderTasksPage();
        await screen.findByText(/Loyalty API/);

        fireEvent.click(screen.getByText(/Promotion \(1\)/));

        expect(screen.getByText(/Loyalty API/)).toBeTruthy();
        expect(screen.queryByText(/Booking MCP Server/)).toBeNull();
    });

    it('shows an error state with a retry action when the request fails', async () => {
        respondWith('get', `${TEST_MANAGEMENT_BASE}/user/tasks`, { message: 'boom' }, 500);
        renderTasksPage();

        expect(await screen.findByRole('alert')).toBeTruthy();
        expect(screen.getByRole('button', { name: /Retry/ })).toBeTruthy();
    });
});
