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
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { TasksPage } from './TasksPage';
import { useModulesStore } from '../../features/modules/modules.store';
import { startPermissionSync } from '../../features/permissions/permission-sync';
import { TEST_GAMMA_BASE, TEST_MANAGEMENT_BASE, TEST_MANAGEMENT_V2_ORGANIZATION_BASE } from '../../testing/factories';
import { TEST_HUMAN_APPROVALS_RESPONSE, TEST_TASKS_RESPONSE } from '../../testing/handlers/tasks.handlers';
import {
    resetAllStores,
    respondWith,
    seedBootstrap,
    seedEnvironments,
    seedHumanApprovalDecider,
    seedUser,
    trackHandler,
} from '../../testing/helpers';
import { PathnameProbe } from '../../testing/PathnameProbe';
import { server } from '../../testing/server';

/** The number on a stat card: the card is titled, and its count is the heading under the title. */
function statCount(title: string): string | undefined {
    const card = screen.getByText(title).closest('[data-slot="card"]');
    return card?.querySelector('p.text-2xl')?.textContent ?? undefined;
}

const APPROVALS_CARD = 'Approvals in Environment 1';

function promotionsCount(): string | undefined {
    return statCount('Promotions');
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

    describe('human approvals', () => {
        const aimUrl = `${TEST_GAMMA_BASE}/environments/env-1-id/modules/aim`;
        const approvalsUrl = `${aimUrl}/approvals`;
        const noUserTasks = { data: [], metadata: {}, page: { total_elements: 0 } };
        const noApprovals = { data: [], pagination: { page: 1, perPage: 25, totalCount: 0 } };

        beforeEach(() => {
            seedHumanApprovalDecider();
        });

        it('lists the approvals beside the other tasks and counts every one on the cards and the chips', async () => {
            renderTasksPage();

            expect(await screen.findByText('refund_order')).toBeTruthy();
            expect(screen.getByText('Support Agent · Refunds over 100')).toBeTruthy();
            expect(screen.getByText('Human Approval')).toBeTruthy();
            expect(screen.getByText('Agent Management')).toBeTruthy();
            expect(await screen.findByText(/Loyalty API/)).toBeTruthy();
            expect(statCount(APPROVALS_CARD)).toBe('12');
            expect(statCount('Pending tasks')).toBe('15');
            expect(screen.getByText('Approval (12)')).toBeTruthy();
            expect(screen.getByText('All (15)')).toBeTruthy();
        });

        it('filters the list down to the approvals when the Approval chip is selected', async () => {
            renderTasksPage();
            await screen.findByText('refund_order');
            await screen.findByText(/Loyalty API/);

            fireEvent.click(screen.getByText('Approval (12)'));

            expect(screen.getByText('refund_order')).toBeTruthy();
            expect(screen.queryByText(/Loyalty API/)).toBeNull();
        });

        it('opens the record of the approval in the aim module', async () => {
            const onPath = jest.fn();
            render(
                <MemoryRouter initialEntries={['/environments/env-1/tasks']}>
                    <Routes>
                        <Route path="/environments/:envHrid/tasks" element={<TasksPage />} />
                        <Route path="*" element={<PathnameProbe onPath={onPath} />} />
                    </Routes>
                </MemoryRouter>,
            );
            await screen.findByText('refund_order');

            fireEvent.click(screen.getByRole('button', { name: /Review approval/ }));

            await waitFor(() => expect(onPath).toHaveBeenCalledWith('/environments/env-1/aim/govern/hitl/apv-1'));
        });

        it('links to the inbox when AIM holds more approvals than the list shows', async () => {
            respondWith('get', approvalsUrl, { ...TEST_HUMAN_APPROVALS_RESPONSE, pagination: { page: 1, perPage: 25, totalCount: 1_200 } });
            renderTasksPage();

            const link = await screen.findByRole('link', { name: 'Open the inbox' });

            expect(link.getAttribute('href')).toBe('/environments/env-1/aim/govern/hitl');
            expect(screen.getByText(/Showing 1 of 1,200 human approvals in Environment 1/)).toBeTruthy();
        });

        it('offers no inbox link when the list shows every approval', async () => {
            respondWith('get', approvalsUrl, { ...TEST_HUMAN_APPROVALS_RESPONSE, pagination: { page: 1, perPage: 25, totalCount: 1 } });
            renderTasksPage();
            await screen.findByText('refund_order');

            expect(screen.queryByRole('link', { name: 'Open the inbox' })).toBeNull();
        });

        it('says the approvals wait in the inbox, never that all is done, when the page AIM sent holds none still pending', async () => {
            const [approval] = TEST_HUMAN_APPROVALS_RESPONSE.data;
            server.use(
                http.get(`${TEST_MANAGEMENT_BASE}/user/tasks`, () => HttpResponse.json(noUserTasks)),
                http.get(approvalsUrl, () =>
                    HttpResponse.json({ data: [{ ...approval, status: 'EXPIRED' }], pagination: { page: 1, perPage: 25, totalCount: 6 } }),
                ),
            );
            renderTasksPage();

            expect(await screen.findByText(/5 human approvals in Environment 1 wait in the inbox/)).toBeTruthy();
            expect(screen.getByRole('link', { name: 'Open the inbox' })).toBeTruthy();
            expect(screen.queryByText(/Showing 0/)).toBeNull();
            expect(screen.queryByText('No pending tasks')).toBeNull();
        });

        describe('when AIM fails', () => {
            beforeEach(() => {
                respondWith('get', approvalsUrl, { message: 'AIM is down' }, 503);
            });

            it('keeps the other tasks and their counts, and shows no count where the approvals are part of it', async () => {
                renderTasksPage();

                expect(await screen.findByText(/Failed to load human approvals: AIM is down/)).toBeTruthy();
                expect(await screen.findByText(/Loyalty API/)).toBeTruthy();
                expect(statCount('Promotions')).toBe('1');
                expect(statCount(APPROVALS_CARD)).toContain('—');
                expect(screen.getByText(`${APPROVALS_CARD} unavailable`)).toBeTruthy();
                expect(statCount('Pending tasks')).toContain('—');
                expect(screen.queryByText(/^Approval \(/)).toBeNull();
            });

            it('does not say the user is all caught up when nothing else is pending', async () => {
                respondWith('get', `${TEST_MANAGEMENT_BASE}/user/tasks`, noUserTasks);
                renderTasksPage();

                await screen.findByText(/Failed to load human approvals/);
                await waitFor(() => expect(statCount('Promotions')).toBe('0'));

                expect(screen.queryByText('No pending tasks')).toBeNull();
            });

            it('asks AIM again when retried, and then lists the approvals', async () => {
                renderTasksPage();
                await screen.findByText(/Failed to load human approvals/);
                respondWith('get', approvalsUrl, TEST_HUMAN_APPROVALS_RESPONSE);

                fireEvent.click(screen.getByRole('button', { name: /Retry/ }));

                expect(await screen.findByText('refund_order')).toBeTruthy();
                expect(screen.queryByText(/Failed to load human approvals/)).toBeNull();
            });

            it('says so for each source, with one Retry, when the other tasks fail as well', async () => {
                respondWith('get', `${TEST_MANAGEMENT_BASE}/user/tasks`, { message: 'boom' }, 500);
                renderTasksPage();

                expect(await screen.findByText(/Failed to load tasks: boom/)).toBeTruthy();
                expect(await screen.findByText(/Failed to load human approvals: AIM is down/)).toBeTruthy();
                expect(screen.getAllByRole('button', { name: /Retry/ })).toHaveLength(1);
            });
        });

        it('still says the other tasks failed when only approvals are on screen', async () => {
            respondWith('get', `${TEST_MANAGEMENT_BASE}/user/tasks`, { message: 'boom' }, 500);
            renderTasksPage();

            expect(await screen.findByText('refund_order')).toBeTruthy();
            expect(await screen.findByText(/Failed to load tasks: boom/)).toBeTruthy();
        });

        it('shows a skeleton rather than an empty list or a zero while AIM has not answered', async () => {
            server.use(
                http.get(`${TEST_MANAGEMENT_BASE}/user/tasks`, () => HttpResponse.json(noUserTasks)),
                http.get(approvalsUrl, () => new Promise<never>(() => undefined)),
            );
            renderTasksPage();

            await waitFor(() => expect(statCount('Subscriptions')).toBe('0'));

            expect(screen.queryByText('No pending tasks')).toBeNull();
            const approvalsCard = screen.getByText(APPROVALS_CARD).closest('[data-slot="card"]');
            expect(approvalsCard?.querySelector('[data-slot="skeleton"]')).toBeTruthy();
            expect(statCount('Pending tasks')).toBeUndefined();
        });

        it('says the list is empty once AIM answered none', async () => {
            server.use(
                http.get(`${TEST_MANAGEMENT_BASE}/user/tasks`, () => HttpResponse.json(noUserTasks)),
                http.get(approvalsUrl, () => HttpResponse.json(noApprovals)),
            );
            renderTasksPage();

            expect(await screen.findByText('No pending tasks')).toBeTruthy();
            expect(statCount(APPROVALS_CARD)).toBe('0');
            expect(statCount('Pending tasks')).toBe('0');
        });

        it('keeps the page and says so when an answer of AIM is not what it should be', async () => {
            const [approval] = TEST_HUMAN_APPROVALS_RESPONSE.data;
            respondWith('get', approvalsUrl, { ...TEST_HUMAN_APPROVALS_RESPONSE, data: [{ ...approval, caller: undefined }] });
            renderTasksPage();

            expect(await screen.findByText(/Failed to load human approvals/)).toBeTruthy();
            expect(await screen.findByText(/Loyalty API/)).toBeTruthy();
        });

        it('keeps the rows and says they may be out of date when a later read fails', async () => {
            renderTasksPage();
            await screen.findByText('refund_order');
            respondWith('get', approvalsUrl, { message: 'AIM is down' }, 503);

            fireEvent.click(screen.getByRole('button', { name: 'Refresh' }));

            expect(await screen.findByText(/may be out of date/)).toBeTruthy();
            expect(screen.getByText('refund_order')).toBeTruthy();
            expect(statCount(APPROVALS_CARD)).toBe('12');
        });

        it('does not say the user is all caught up when a later read fails and nothing is listed', async () => {
            server.use(
                http.get(`${TEST_MANAGEMENT_BASE}/user/tasks`, () => HttpResponse.json(noUserTasks)),
                http.get(approvalsUrl, () => HttpResponse.json(noApprovals)),
            );
            renderTasksPage();
            await screen.findByText('No pending tasks');
            respondWith('get', approvalsUrl, { message: 'AIM is down' }, 503);

            fireEvent.click(screen.getByRole('button', { name: 'Refresh' }));

            expect(await screen.findByText(/may be out of date/)).toBeTruthy();
            expect(screen.queryByText('No pending tasks')).toBeNull();
        });

        it('shows as without approvals, with no read of them, when the hitl feature is off', async () => {
            respondWith('get', `${aimUrl}/_settings`, { features: ['llm-providers'] });
            const approvals = trackHandler('get', approvalsUrl, TEST_HUMAN_APPROVALS_RESPONSE);
            renderTasksPage();
            await screen.findByText(/Loyalty API/);

            await waitFor(() => expect(statCount('Pending tasks')).toBe('3'));
            expect(approvals.callCount).toBe(0);
            expect(screen.queryByText(APPROVALS_CARD)).toBeNull();
            expect(screen.queryByText(/^Approval/)).toBeNull();
        });

        it('says the approvals cannot be known, never that all is done, when the list of modules could not be read', async () => {
            respondWith('get', `${TEST_MANAGEMENT_BASE}/user/tasks`, noUserTasks);
            act(() => useModulesStore.setState({ modules: [], listStatus: 'failed' }));
            renderTasksPage();

            expect(await screen.findByText(/Failed to load human approvals: .*applications/)).toBeTruthy();
            await waitFor(() => expect(statCount('Subscriptions')).toBe('0'));
            expect(screen.queryByText('No pending tasks')).toBeNull();
        });

        describe('through the real sequence of the permissions', () => {
            let stopPermissionSync: () => void;
            const permissionsUrl = `${TEST_MANAGEMENT_BASE}/environments/env-1-id/permissions`;
            const mayDecide = { HITL_DECISIONS: ['R', 'U'] };

            beforeEach(() => {
                stopPermissionSync = startPermissionSync();
                jest.spyOn(console, 'error').mockImplementation(() => undefined);
                respondWith('get', `${TEST_MANAGEMENT_BASE}/user/tasks`, noUserTasks);
            });

            afterEach(() => {
                stopPermissionSync();
            });

            it('shows a skeleton, never that all is done, until the permissions arrive', async () => {
                let answerPermissions: ((response: Response) => void) | undefined;
                server.use(http.get(permissionsUrl, () => new Promise<Response>(resolve => (answerPermissions = resolve))));
                act(() => seedUser());
                renderTasksPage();
                await waitFor(() => expect(answerPermissions).toBeDefined());
                await waitFor(() => expect(statCount('Subscriptions')).toBe('0'));

                expect(screen.queryByText('No pending tasks')).toBeNull();

                act(() => answerPermissions?.(HttpResponse.json(mayDecide)));

                expect(await screen.findByText('refund_order')).toBeTruthy();
            });

            it('says the permissions could not be read, never that all is done, and reads them again on Retry', async () => {
                respondWith('get', permissionsUrl, { message: 'boom' }, 500);
                act(() => seedUser());
                renderTasksPage();

                expect(await screen.findByText(/Failed to load human approvals: .*permissions/)).toBeTruthy();
                await waitFor(() => expect(statCount('Subscriptions')).toBe('0'));
                expect(screen.queryByText('No pending tasks')).toBeNull();

                respondWith('get', permissionsUrl, mayDecide);
                fireEvent.click(screen.getByRole('button', { name: /Retry/ }));

                expect(await screen.findByText('refund_order')).toBeTruthy();
            });
        });

        it('reads both the approvals and the other tasks once when Refresh is clicked, however many times while it runs', async () => {
            const approvals = trackHandler('get', approvalsUrl, TEST_HUMAN_APPROVALS_RESPONSE);
            const userTasks = trackHandler('get', `${TEST_MANAGEMENT_BASE}/user/tasks`, TEST_TASKS_RESPONSE);
            renderTasksPage();
            await screen.findByText('refund_order');
            await waitFor(() => expect(userTasks.callCount).toBe(1));
            respondWith('get', approvalsUrl, noApprovals);

            fireEvent.click(screen.getByRole('button', { name: 'Refresh' }));
            fireEvent.click(screen.getByRole('button', { name: 'Refresh' }));

            await waitFor(() => expect(screen.queryByText('refund_order')).toBeNull());
            await waitFor(() => expect(screen.getByRole('button', { name: 'Refresh' }).hasAttribute('disabled')).toBe(false));
            expect(approvals.callCount).toBe(1);
            expect(userTasks.callCount).toBe(2);
        });
    });

    it('refreshes the tasks alone for a user with no approvals', async () => {
        const userTasks = trackHandler('get', `${TEST_MANAGEMENT_BASE}/user/tasks`, TEST_TASKS_RESPONSE);
        renderTasksPage();
        await screen.findByText(/Loyalty API/);

        fireEvent.click(screen.getByRole('button', { name: 'Refresh' }));

        await waitFor(() => expect(userTasks.callCount).toBe(2));
    });

    it('shows an error state with a retry action when the request fails', async () => {
        respondWith('get', `${TEST_MANAGEMENT_BASE}/user/tasks`, { message: 'boom' }, 500);
        renderTasksPage();

        expect(await screen.findByRole('alert')).toBeTruthy();
        expect(screen.getByRole('button', { name: /Retry/ })).toBeTruthy();
    });
});
