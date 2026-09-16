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
import { toast } from '@gravitee/graphene-core';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { TasksPage } from './TasksPage';
import { TEST_MANAGEMENT_BASE, TEST_MANAGEMENT_V2_ORGANIZATION_BASE } from '../../testing/factories';
import { TEST_TASKS_RESPONSE } from '../../testing/handlers/tasks.handlers';
import { resetAllStores, respondWith, seedBootstrap, seedEnvironments } from '../../testing/helpers';
import { server } from '../../testing/server';

/** The number on the Promotions stat card: the card is titled, and its count is the heading under the title. */
function promotionsCount(): string | undefined {
    const card = screen.getByText('Promotions').closest('[data-slot="card"]');
    return card?.querySelector('p.text-2xl')?.textContent ?? undefined;
}

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

    it.each([
        ['accepted', /^Accept$/, true],
        ['rejected', /^Confirm reject$/, false],
    ])('sends the %s decision as a raw boolean and refreshes the list', async (_decision, confirmButton, expectedBody) => {
        jest.spyOn(toast, 'success').mockImplementation(() => '');
        const bodies: unknown[] = [];
        let taskRequests = 0;
        server.use(
            http.get(`${TEST_MANAGEMENT_BASE}/user/tasks`, () => {
                taskRequests += 1;
                return HttpResponse.json(TEST_TASKS_RESPONSE);
            }),
            // `false` is the case that matters: a client treating a falsy body as "no body" sends nothing and
            // the gateway reads a reject as an accept, which no assertion on the call itself would catch.
            http.post(`${TEST_MANAGEMENT_V2_ORGANIZATION_BASE}/promotions/:promotionId/_process`, async ({ request }) => {
                bodies.push(await request.json());
                return new HttpResponse(null, { status: 204 });
            }),
        );

        renderTasksPage();
        await screen.findByText(/Loyalty API/);
        await waitFor(() => expect(taskRequests).toBe(1));

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));
        if (expectedBody === false) {
            fireEvent.click(screen.getByRole('button', { name: /^Reject$/ }));
        }
        fireEvent.click(screen.getByRole('button', { name: confirmButton }));

        await waitFor(() => expect(bodies).toEqual([expectedBody]));
        // The count cards and the list both read the same response, so one refetch covers both.
        await waitFor(() => expect(taskRequests).toBe(2));
    });

    it('drops the processed promotion from the list and the Promotions count, with no reload', async () => {
        // The count cards and the list read the same response, so what a reader sees after accepting is the
        // refetch's answer — not the row they just acted on, still sitting there until they reload.
        jest.spyOn(toast, 'success').mockImplementation(() => '');
        const remaining = { ...TEST_TASKS_RESPONSE, data: TEST_TASKS_RESPONSE.data.filter(task => task.type !== 'PROMOTION_APPROVAL') };
        let processed = false;
        server.use(
            http.get(`${TEST_MANAGEMENT_BASE}/user/tasks`, () => HttpResponse.json(processed ? remaining : TEST_TASKS_RESPONSE)),
            http.post(`${TEST_MANAGEMENT_V2_ORGANIZATION_BASE}/promotions/:promotionId/_process`, () => {
                processed = true;
                return new HttpResponse(null, { status: 204 });
            }),
        );

        renderTasksPage();
        await screen.findByText(/Loyalty API/);
        expect(promotionsCount()).toBe('1');

        fireEvent.click(screen.getByRole('button', { name: /Review promotion/ }));
        fireEvent.click(screen.getByRole('button', { name: /^Accept$/ }));

        await waitFor(() => expect(screen.queryByText(/Loyalty API/)).toBeNull());
        await waitFor(() => expect(promotionsCount()).toBe('0'));
    });

    it('shows an error state with a retry action when the request fails', async () => {
        respondWith('get', `${TEST_MANAGEMENT_BASE}/user/tasks`, { message: 'boom' }, 500);
        renderTasksPage();

        expect(await screen.findByRole('alert')).toBeTruthy();
        expect(screen.getByRole('button', { name: /Retry/ })).toBeTruthy();
    });
});
