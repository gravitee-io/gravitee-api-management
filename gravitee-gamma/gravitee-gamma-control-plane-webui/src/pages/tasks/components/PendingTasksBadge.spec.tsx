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
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, useNavigate, type NavigateFunction } from 'react-router-dom';

import { PendingTasksBadge } from './PendingTasksBadge';
import { useEnvironmentStore } from '../../../features/environment/environment.store';
import { startPermissionSync } from '../../../features/permissions/permission-sync';
import { TEST_ENVIRONMENTS, TEST_GAMMA_BASE, TEST_MANAGEMENT_BASE } from '../../../testing/factories';
import { TEST_HUMAN_APPROVALS_RESPONSE, TEST_TASKS_RESPONSE } from '../../../testing/handlers/tasks.handlers';
import { respondWith, seedEnvironments, seedHumanApprovalDecider, seedUser, trackHandler } from '../../../testing/helpers';
import { server } from '../../../testing/server';

const APPROVALS_URL = `${TEST_GAMMA_BASE}/environments/env-1-id/modules/aim/approvals`;
const NO_APPROVALS = { data: [], pagination: { page: 1, perPage: 25, totalCount: 0 } };

let navigate: NavigateFunction;

function NavigateProbe() {
    navigate = useNavigate();
    return null;
}

function renderBadge(path = '/environments/env-1/home') {
    return render(
        <MemoryRouter initialEntries={[path]}>
            <NavigateProbe />
            <PendingTasksBadge />
        </MemoryRouter>,
    );
}

function badgeButton(name: RegExp) {
    return screen.findByRole('button', { name });
}

/** Lets every answer already given reach the screen, so that what is asserted next is final. */
async function settleReads() {
    await act(() => jest.runOnlyPendingTimersAsync());
}

beforeAll(() => {
    // Radix's Select drives pointer capture and scrolling, neither of which jsdom implements.
    Element.prototype.hasPointerCapture = jest.fn();
    Element.prototype.setPointerCapture = jest.fn();
    Element.prototype.releasePointerCapture = jest.fn();
    Element.prototype.scrollIntoView = jest.fn();
});

describe('PendingTasksBadge', () => {
    beforeEach(() => {
        jest.useFakeTimers({ advanceTimers: true });
        seedEnvironments();
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    it('counts the tasks only for a user with no approvals to decide', async () => {
        renderBadge();

        expect(await badgeButton(/3 pending/)).toBeTruthy();
    });

    describe('for a user who may decide approvals', () => {
        beforeEach(() => {
            seedHumanApprovalDecider();
        });

        it('adds every approval the server counts to the tasks', async () => {
            renderBadge();

            expect(await badgeButton(/15 pending/)).toBeTruthy();
        });

        it('counts the tasks only when AIM fails, and the approvals only when the tasks fail', async () => {
            respondWith('get', APPROVALS_URL, { message: 'AIM is down' }, 503);
            const { unmount } = renderBadge();
            await settleReads();
            expect(await badgeButton(/3 pending/)).toBeTruthy();
            unmount();

            respondWith('get', APPROVALS_URL, TEST_HUMAN_APPROVALS_RESPONSE);
            respondWith('get', `${TEST_MANAGEMENT_BASE}/user/tasks`, { message: 'boom' }, 500);
            renderBadge();

            expect(await badgeButton(/12 pending/)).toBeTruthy();
        });

        it('shows no number, not a zero, when nothing is known', async () => {
            respondWith('get', `${TEST_MANAGEMENT_BASE}/user/tasks`, { message: 'boom' }, 500);
            respondWith('get', APPROVALS_URL, { message: 'AIM is down' }, 503);
            renderBadge();
            await settleReads();

            expect((await badgeButton(/^Tasks and approvals$/)).textContent).toBe('');
        });

        it('reads AIM on mount and never on the interval the tasks are polled on', async () => {
            const userTasks = trackHandler('get', `${TEST_MANAGEMENT_BASE}/user/tasks`, TEST_TASKS_RESPONSE);
            const approvals = trackHandler('get', APPROVALS_URL, TEST_HUMAN_APPROVALS_RESPONSE);
            renderBadge();
            await badgeButton(/15 pending/);
            const userTasksReadsBefore = userTasks.callCount;

            await act(() => jest.advanceTimersByTimeAsync(5 * 60 * 1000));

            expect(userTasks.callCount).toBeGreaterThan(userTasksReadsBefore);
            expect(approvals.callCount).toBe(1);
        });

        it('follows an approval decided in the inbox once the user leaves it, even while a read runs', async () => {
            let answerFirstRead: ((response: Response) => void) | undefined;
            const approvals = trackHandler('get', APPROVALS_URL, NO_APPROVALS);
            server.use(http.get(APPROVALS_URL, () => new Promise<Response>(resolve => (answerFirstRead = resolve)), { once: true }));
            renderBadge('/environments/env-1/aim/govern/hitl/apv-1');
            await waitFor(() => expect(answerFirstRead).toBeDefined());

            act(() => navigate('/environments/env-1/home'));
            act(() => answerFirstRead?.(HttpResponse.json(TEST_HUMAN_APPROVALS_RESPONSE)));

            await waitFor(() => expect(approvals.callCount).toBe(1));
            expect(await badgeButton(/3 pending/)).toBeTruthy();
        });

        it('does not read AIM on a navigation that does not leave the approval pages', async () => {
            renderBadge();
            await badgeButton(/15 pending/);
            const approvals = trackHandler('get', APPROVALS_URL, TEST_HUMAN_APPROVALS_RESPONSE);

            act(() => navigate('/environments/env-1/aim/catalog/tools'));
            act(() => navigate('/environments/env-1/aim/govern/hitl'));
            act(() => navigate('/environments/env-1/aim/govern/hitl/apv-2'));
            await settleReads();

            expect(approvals.callCount).toBe(0);
        });

        it('lists the approvals in the sheet it opens', async () => {
            renderBadge();

            fireEvent.click(await badgeButton(/15 pending/));

            expect(await screen.findByText('refund_order')).toBeTruthy();
        });

        it('follows a read made by the sheet, with no read of its own', async () => {
            renderBadge();
            const button = await badgeButton(/15 pending/);
            const approvals = trackHandler('get', APPROVALS_URL, NO_APPROVALS);

            fireEvent.click(button);

            await waitFor(() => expect(approvals.callCount).toBe(1));
            // The open sheet hides the page from the accessibility tree, the badge with it.
            expect(await screen.findByLabelText(/3 pending/)).toBeTruthy();
        });

        describe('when the environment changes with the sheet open', () => {
            let stopPermissionSync: () => void;
            const permissionsUrl = (environmentId: string) => `${TEST_MANAGEMENT_BASE}/environments/${environmentId}/permissions`;

            beforeEach(() => {
                stopPermissionSync = startPermissionSync();
                respondWith('get', permissionsUrl('env-1-id'), { HITL_DECISIONS: ['R', 'U'] });
                respondWith('get', permissionsUrl('env-2-id'), {});
            });

            afterEach(() => {
                stopPermissionSync();
            });

            it('shows every task again when the Approval filter was selected and the new environment has no approvals for the user', async () => {
                const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
                act(() => seedUser());
                renderBadge();
                await user.click(await badgeButton(/15 pending/));
                await screen.findByText('refund_order');
                await user.click(screen.getByRole('combobox', { name: 'Filter tasks by category' }));
                await user.click(screen.getByRole('option', { name: 'Approval (12)' }));
                expect(screen.queryByText(/Loyalty API/)).toBeNull();

                act(() => useEnvironmentStore.getState().setCurrentEnvironment(TEST_ENVIRONMENTS[1]!));

                expect(await screen.findByText(/Loyalty API/)).toBeTruthy();
                await settleReads();
                expect(screen.getByText(/Loyalty API/)).toBeTruthy();
                expect(screen.queryByText('refund_order')).toBeNull();
            });
        });
    });
});
