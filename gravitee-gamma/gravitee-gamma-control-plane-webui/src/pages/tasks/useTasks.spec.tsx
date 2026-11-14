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
import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';

import { useTasks } from './useTasks';
import { useEnvironmentStore } from '../../features/environment/environment.store';
import { TEST_ENVIRONMENTS, TEST_GAMMA_BASE, TEST_MANAGEMENT_BASE, TEST_MANAGEMENT_V2_ORGANIZATION_BASE } from '../../testing/factories';
import { TEST_HUMAN_APPROVALS_RESPONSE } from '../../testing/handlers/tasks.handlers';
import {
    resetAllStores,
    respondWith,
    seedBootstrap,
    seedEnvironments,
    seedHumanApprovalDecider,
    trackHandler,
} from '../../testing/helpers';

const USER_REGISTRATION_RESPONSE = {
    data: [{ type: 'USER_REGISTRATION_APPROVAL', created_at: 1_700_000_000_000, data: { id: 'user-1', displayName: 'Ada' } }],
    metadata: {},
    page: { total_elements: 1 },
};

function renderAt(path: string) {
    return renderHook(() => useTasks(), {
        wrapper: ({ children }: { readonly children: ReactNode }) => <MemoryRouter initialEntries={[path]}>{children}</MemoryRouter>,
    });
}

describe('useTasks', () => {
    beforeEach(() => {
        resetAllStores();
        seedBootstrap();
        seedEnvironments();
        respondWith('get', `${TEST_MANAGEMENT_BASE}/user/tasks`, USER_REGISTRATION_RESPONSE);
    });

    it('should link a task with no environment to the environment of the address', async () => {
        const { result } = renderAt('/environments/env-2/home');

        await waitFor(() => expect(result.current.tasks[0]?.to).toBe('/environments/env-2/platform/users/user-1'));
    });

    it('should link a task with no environment to the one of the session from Account Management', async () => {
        useEnvironmentStore.setState({ currentEnvironment: TEST_ENVIRONMENTS[1], environmentId: TEST_ENVIRONMENTS[1]!.id });

        const { result } = renderAt('/account/overview');

        await waitFor(() => expect(result.current.tasks[0]?.to).toBe('/environments/env-2/platform/users/user-1'));
    });

    describe('human approvals', () => {
        const approvalsUrl = `${TEST_GAMMA_BASE}/environments/env-1-id/modules/aim/approvals`;

        beforeEach(() => {
            seedHumanApprovalDecider();
        });

        it('should add the approvals to the tasks, counted by the server total rather than the page', async () => {
            const { result } = renderAt('/environments/env-1/home');

            await waitFor(() =>
                expect(result.current.tasks.map(task => task.type)).toEqual(['HUMAN_APPROVAL', 'USER_REGISTRATION_APPROVAL']),
            );
            expect(result.current.tasks[0]?.to).toBe('/environments/env-1/aim/govern/hitl/apv-1');
            expect(result.current.tasks[0]?.subtitle).toBe('Support Agent · Refunds over 100');
            expect(result.current.totalCount).toBe(13);
        });

        it('should link to the environment the approvals were read for, from Account Management', async () => {
            useEnvironmentStore.setState({ currentEnvironment: TEST_ENVIRONMENTS[1], environmentId: TEST_ENVIRONMENTS[1]!.id });
            respondWith('get', `${TEST_GAMMA_BASE}/environments/env-2-id/modules/aim/approvals`, TEST_HUMAN_APPROVALS_RESPONSE);

            const { result } = renderAt('/account/overview');

            await waitFor(() => expect(result.current.tasks[0]?.to).toBe('/environments/env-2/aim/govern/hitl/apv-1'));
            expect(result.current.approvalsOverflow).toEqual({
                listed: 1,
                total: 12,
                inboxPath: '/environments/env-2/aim/govern/hitl',
                environmentName: 'Environment 2',
            });
        });

        it('should report no overflow when the list holds every approval', async () => {
            respondWith('get', approvalsUrl, { ...TEST_HUMAN_APPROVALS_RESPONSE, pagination: { page: 1, perPage: 25, totalCount: 1 } });

            const { result } = renderAt('/environments/env-1/home');

            await waitFor(() => expect(result.current.tasks).toHaveLength(2));
            expect(result.current.approvalsOverflow).toBeNull();
        });

        it('should keep the other tasks and their count when AIM fails', async () => {
            respondWith('get', approvalsUrl, { message: 'AIM is down' }, 503);

            const { result } = renderAt('/environments/env-1/home');

            await waitFor(() => expect(result.current.humanApprovals.status).toBe('failed'));
            await waitFor(() => expect(result.current.tasks.map(task => task.type)).toEqual(['USER_REGISTRATION_APPROVAL']));
            expect(result.current.error).toBeNull();
            expect(result.current.totalCount).toBe(1);
            expect(result.current.approvalsOverflow).toBeNull();
        });

        it('should ask AIM again on reload, but not after a promotion is processed', async () => {
            const approvals = trackHandler('get', approvalsUrl, TEST_HUMAN_APPROVALS_RESPONSE);
            respondWith('post', `${TEST_MANAGEMENT_V2_ORGANIZATION_BASE}/promotions/promo-1/_process`, undefined, 204);
            const { result } = renderAt('/environments/env-1/home');
            await waitFor(() => expect(result.current.humanApprovals.status).toBe('loaded'));
            expect(approvals.callCount).toBe(1);

            await act(() => result.current.processPromotion('promo-1', true));
            expect(approvals.callCount).toBe(1);

            act(() => result.current.reload());
            await waitFor(() => expect(approvals.callCount).toBe(2));
        });
    });
});
