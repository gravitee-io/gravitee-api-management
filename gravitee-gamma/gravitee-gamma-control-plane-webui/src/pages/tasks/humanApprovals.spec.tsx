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
import { http, HttpResponse } from 'msw';
import type { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';

import { permissionService } from '@gravitee/gamma-modules-sdk';

import { useHumanApprovals, type HumanApprovals } from './humanApprovals';
import { useAuthStore } from '../../features/auth/auth.store';
import { useEnvironmentStore } from '../../features/environment/environment.store';
import { useGammaModules } from '../../features/modules/hooks/useGammaModules';
import { useModulesStore } from '../../features/modules/modules.store';
import { startPermissionSync } from '../../features/permissions/permission-sync';
import { TEST_ENVIRONMENTS, TEST_GAMMA_BASE, TEST_MANAGEMENT_BASE } from '../../testing/factories';
import { TEST_HUMAN_APPROVALS_RESPONSE } from '../../testing/handlers/tasks.handlers';
import { respondWith, seedEnvironments, seedHumanApprovalDecider, seedModules, seedUser, trackHandler } from '../../testing/helpers';
import { server } from '../../testing/server';

jest.mock('@module-federation/runtime', () => ({ registerRemotes: jest.fn() }));

const AIM_URL = `${TEST_GAMMA_BASE}/environments/env-1-id/modules/aim`;
const SETTINGS_URL = `${AIM_URL}/_settings`;
const APPROVALS_URL = `${AIM_URL}/approvals`;
const NO_APPROVALS = { data: [], pagination: { page: 1, perPage: 25, totalCount: 0 } };
const AIM_READ_TIMEOUT_MS = 10_000;
const DECIDER_PERMISSIONS = ['environment-hitl_decisions-r', 'environment-hitl_decisions-u'];

function wrapper({ children }: { readonly children: ReactNode }) {
    return <MemoryRouter>{children}</MemoryRouter>;
}

function renderApprovals() {
    return renderHook(() => useHumanApprovals(), { wrapper });
}

function trackAim() {
    return {
        settings: trackHandler('get', SETTINGS_URL, { features: ['hitl'] }),
        approvals: trackHandler('get', APPROVALS_URL, TEST_HUMAN_APPROVALS_RESPONSE),
    };
}

/** An approvals read whose answer the test gives when it chooses. */
function holdApprovals(url = APPROVALS_URL): { answer: (body: unknown) => void; readonly calls: number } {
    const held = { answer: (_body: unknown) => undefined as void, calls: 0 };
    server.use(
        http.get(url, () => {
            held.calls += 1;
            return new Promise<Response>(resolve => (held.answer = body => resolve(HttpResponse.json(body as never))));
        }),
    );
    return held;
}

function returnToTab() {
    act(() => {
        document.dispatchEvent(new Event('visibilitychange'));
        window.dispatchEvent(new Event('focus'));
    });
}

/** Lets every answer already given reach the store, so that what is asserted next is final. */
async function settleReads() {
    await act(() => jest.runOnlyPendingTimersAsync());
}

describe('useHumanApprovals', () => {
    beforeEach(() => {
        jest.useFakeTimers({ advanceTimers: true });
        seedEnvironments();
        seedHumanApprovalDecider();
    });

    afterEach(() => {
        jest.useRealTimers();
        jest.restoreAllMocks();
    });

    describe('when off', () => {
        it.each([
            ['may not decide them', () => permissionService.load('environment', ['environment-hitl_decisions-r'])],
            ['may not read them', () => permissionService.load('environment', ['environment-hitl_decisions-u'])],
            ['has no aim module', () => seedModules('apim')],
        ])('should send AIM no request at all for a user who %s', async (_case, arrange) => {
            arrange();
            const { settings, approvals } = trackAim();

            const { result } = renderApprovals();
            await settleReads();

            expect(result.current.humanApprovals.status).toBe('off');
            expect(settings.callCount).toBe(0);
            expect(approvals.callCount).toBe(0);
        });

        it('should not read the approvals when the hitl feature is off', async () => {
            const settings = trackHandler('get', SETTINGS_URL, { features: ['llm-providers'] });
            const approvals = trackHandler('get', APPROVALS_URL, TEST_HUMAN_APPROVALS_RESPONSE);

            const { result } = renderApprovals();

            await waitFor(() => expect(result.current.humanApprovals.status).toBe('off'));
            expect(settings.callCount).toBe(1);
            expect(approvals.callCount).toBe(0);
        });
    });

    describe('until what the user may do here is known', () => {
        it('should be loading, and ask AIM nothing, while the permissions are awaited, then read once they allow it', async () => {
            permissionService.clear('environment');
            const { settings, approvals } = trackAim();

            const { result } = renderApprovals();
            await settleReads();

            expect(result.current.humanApprovals.status).toBe('loading');
            expect(settings.callCount + approvals.callCount).toBe(0);

            act(() => permissionService.load('environment', DECIDER_PERMISSIONS));

            await waitFor(() => expect(result.current.humanApprovals.status).toBe('loaded'));
        });

        it('should be off at once with no aim module, whatever the permissions', () => {
            permissionService.clear('environment');
            seedModules('apim');

            expect(renderApprovals().result.current.humanApprovals.status).toBe('off');
        });

        it('should fail, with no request, when the permissions could not be read', async () => {
            permissionService.clear('environment');
            const { approvals } = trackAim();
            const { result } = renderApprovals();

            act(() => permissionService.markLoadFailed('environment'));
            await settleReads();

            expect(result.current.humanApprovals).toMatchObject({
                status: 'failed',
                error: { message: expect.stringContaining('permissions') },
            });
            expect(approvals.callCount).toBe(0);
        });

        it('should be loading, and ask AIM nothing, while the list of modules is awaited', async () => {
            useModulesStore.setState({ modules: [], listStatus: 'pending' });
            const { settings, approvals } = trackAim();

            const { result } = renderApprovals();
            await settleReads();

            expect(result.current.humanApprovals.status).toBe('loading');
            expect(settings.callCount + approvals.callCount).toBe(0);
        });

        it('should fail when the list of modules could not be read, and read it again on refresh', async () => {
            jest.spyOn(console, 'error').mockImplementation(() => undefined);
            useModulesStore.setState({ modules: [], listStatus: 'pending' });
            seedUser();
            respondWith('get', `${TEST_GAMMA_BASE}/modules`, { message: 'boom' }, 503);
            const { result } = renderHook(
                () => {
                    useGammaModules();
                    return useHumanApprovals();
                },
                { wrapper },
            );

            await waitFor(() =>
                expect(result.current.humanApprovals).toMatchObject({
                    status: 'failed',
                    error: { message: expect.stringContaining('applications') },
                }),
            );

            respondWith('get', `${TEST_GAMMA_BASE}/modules`, [{ id: 'aim', name: 'AIM', version: '1.0.0', mfManifest: { name: 'aim' } }]);
            act(() => result.current.refresh());

            await waitFor(() => expect(result.current.humanApprovals.status).toBe('loaded'));
        });
    });

    it('should read the first page of the approvals the user can decide, capped, and the total the server counts', async () => {
        const { approvals } = trackAim();

        const { result } = renderApprovals();
        expect(result.current.humanApprovals.status).toBe('loading');

        await waitFor(() =>
            expect(result.current.humanApprovals).toEqual({
                status: 'loaded',
                environmentId: 'env-1-id',
                items: TEST_HUMAN_APPROVALS_RESPONSE.data,
                totalCount: 12,
            }),
        );
        const query = new URL(approvals.lastCall!.url).searchParams;
        expect(query.get('status')).toBe('PENDING');
        expect(query.get('decidable')).toBe('true');
        expect(query.get('page')).toBe('1');
        expect(query.get('perPage')).toBe('25');
    });

    it.each([
        ['one of them', [{ status: 'EXPIRED' }, { status: 'PENDING' }], 12, 11],
        ['more of them than the server counts', [{ status: 'EXPIRED' }, { status: 'EXPIRED' }], 1, 0],
    ])(
        'should keep only the approvals still pending, not counting those it dropped, when AIM shows %s expired',
        async (_case, rows, serverTotal, total) => {
            const [approval] = TEST_HUMAN_APPROVALS_RESPONSE.data;
            respondWith('get', APPROVALS_URL, {
                data: rows.map((row, index) => ({ ...approval, id: `apv-${index}`, ...row })),
                pagination: { page: 1, perPage: 25, totalCount: serverTotal },
            });

            const { result } = renderApprovals();

            await waitFor(() => expect(result.current.humanApprovals.status).toBe('loaded'));
            expect(result.current.humanApprovals).toMatchObject({
                items: rows.filter(row => row.status === 'PENDING').map(() => expect.anything()),
                totalCount: total,
            });
        },
    );

    describe('when AIM cannot answer', () => {
        it('should fail, not read as none, when the approvals answer an error, and not read the settings again to retry', async () => {
            const settings = trackHandler('get', SETTINGS_URL, { features: ['hitl'] });
            respondWith('get', APPROVALS_URL, { message: 'AIM is down' }, 503);
            const { result } = renderApprovals();

            await waitFor(() =>
                expect(result.current.humanApprovals).toMatchObject({ status: 'failed', error: { message: 'AIM is down' } }),
            );

            respondWith('get', APPROVALS_URL, TEST_HUMAN_APPROVALS_RESPONSE);
            act(() => result.current.refresh());

            await waitFor(() => expect(result.current.humanApprovals.status).toBe('loaded'));
            expect(settings.callCount).toBe(1);
        });

        it('should fail, not read as off, when the settings answer an error, and ask for them again', async () => {
            respondWith('get', SETTINGS_URL, { message: 'AIM is down' }, 500);
            const approvals = trackHandler('get', APPROVALS_URL, TEST_HUMAN_APPROVALS_RESPONSE);
            const { result } = renderApprovals();

            await waitFor(() => expect(result.current.humanApprovals.status).toBe('failed'));
            expect(approvals.callCount).toBe(0);

            respondWith('get', SETTINGS_URL, { features: ['hitl'] });
            act(() => result.current.refresh());

            await waitFor(() => expect(result.current.humanApprovals.status).toBe('loaded'));
        });

        const [valid] = TEST_HUMAN_APPROVALS_RESPONSE.data;
        const pagination = { page: 1, perPage: 25, totalCount: 1 };

        it.each<[string, unknown]>([
            ['no data', { pagination }],
            ['no pagination', { data: [] }],
            ['an empty pagination', { data: [], pagination: {} }],
            ['a total that is not a whole number', { data: [], pagination: { totalCount: 1.5 } }],
            ['a negative total', { data: [], pagination: { totalCount: -1 } }],
            ['an item with no caller', { data: [{ ...valid, caller: undefined }], pagination }],
            ['an item with no rule name', { data: [{ ...valid, rule: {} }], pagination }],
            ['an item with a date that is not one', { data: [{ ...valid, createdAt: 'soon' }], pagination }],
        ])('should fail when the answer holds %s', async (_case, answer) => {
            respondWith('get', APPROVALS_URL, answer as never);

            const { result } = renderApprovals();

            await waitFor(() => expect(result.current.humanApprovals.status).toBe('failed'));
        });

        it('should accept an application with no name', async () => {
            respondWith('get', APPROVALS_URL, {
                data: [{ ...valid, caller: { applicationId: 'app-7', applicationName: null } }],
                pagination,
            });

            const { result } = renderApprovals();

            await waitFor(() => expect(result.current.humanApprovals.status).toBe('loaded'));
        });

        it('should fail, saying so, once AIM has not answered in time, and not before', async () => {
            // The runtime's own timeout does not follow the fake clock.
            jest.spyOn(AbortSignal, 'timeout').mockImplementation(ms => {
                const controller = new AbortController();
                setTimeout(() => controller.abort(new DOMException('signal timed out', 'TimeoutError')), ms);
                return controller.signal;
            });
            const approvals = holdApprovals();

            const { result } = renderApprovals();
            await waitFor(() => expect(approvals.calls).toBe(1));

            await act(() => jest.advanceTimersByTimeAsync(AIM_READ_TIMEOUT_MS - 1_000));
            expect(result.current.humanApprovals.status).toBe('loading');

            await act(() => jest.advanceTimersByTimeAsync(1_000));
            expect(result.current.humanApprovals).toMatchObject({ status: 'failed', error: { message: 'AIM did not answer in time.' } });
        });

        it('should keep the rows of an earlier read, and say it failed, when a later one fails', async () => {
            const { result } = renderApprovals();
            await waitFor(() => expect(result.current.humanApprovals.status).toBe('loaded'));

            respondWith('get', APPROVALS_URL, { message: 'AIM is down' }, 503);
            act(() => result.current.refresh());

            await waitFor(() => expect(result.current.humanApprovals).toHaveProperty('refreshError.message', 'AIM is down'));
            expect(result.current.humanApprovals).toMatchObject({ status: 'loaded', totalCount: 12, items: [{ id: 'apv-1' }] });

            respondWith('get', APPROVALS_URL, TEST_HUMAN_APPROVALS_RESPONSE);
            act(() => result.current.refresh());

            await waitFor(() => expect(result.current.humanApprovals).not.toHaveProperty('refreshError'));
        });
    });

    describe('when reading again', () => {
        it('should read again when the tab comes back, once for its two events, keeping the rows in the meantime', async () => {
            const { approvals } = trackAim();
            const { result } = renderApprovals();
            await waitFor(() => expect(result.current.humanApprovals.status).toBe('loaded'));

            returnToTab();
            expect(result.current.humanApprovals.status).toBe('loaded');
            await settleReads();

            expect(approvals.callCount).toBe(2);
        });

        it('should not read when the tab goes to the background', async () => {
            const { approvals } = trackAim();
            const { result } = renderApprovals();
            await waitFor(() => expect(result.current.humanApprovals.status).toBe('loaded'));

            jest.spyOn(document, 'hidden', 'get').mockReturnValue(true);
            act(() => {
                document.dispatchEvent(new Event('visibilitychange'));
            });
            await settleReads();

            expect(approvals.callCount).toBe(1);
        });

        it('should stop reading once unmounted', async () => {
            const { approvals } = trackAim();
            const { result, unmount } = renderApprovals();
            await waitFor(() => expect(result.current.humanApprovals.status).toBe('loaded'));

            unmount();
            returnToTab();
            await settleReads();

            expect(approvals.callCount).toBe(1);
        });

        it('should join a read already running when the tab comes back', async () => {
            const approvals = holdApprovals();
            const { result } = renderApprovals();
            await waitFor(() => expect(approvals.calls).toBe(1));

            returnToTab();
            act(() => approvals.answer(TEST_HUMAN_APPROVALS_RESPONSE));
            await waitFor(() => expect(result.current.humanApprovals.status).toBe('loaded'));
            await settleReads();

            expect(approvals.calls).toBe(1);
        });

        it('should read once more after a running read when asked for an answer from now, however many times', async () => {
            const approvals = holdApprovals();
            const { result } = renderApprovals();
            await waitFor(() => expect(approvals.calls).toBe(1));
            expect(result.current.refreshing).toBe(true);

            act(() => {
                result.current.refresh();
                result.current.refresh();
            });
            act(() => approvals.answer(TEST_HUMAN_APPROVALS_RESPONSE));
            await waitFor(() => expect(approvals.calls).toBe(2));

            act(() => approvals.answer(NO_APPROVALS));
            await waitFor(() => expect(result.current.humanApprovals).toMatchObject({ totalCount: 0 }));
            await settleReads();
            expect(approvals.calls).toBe(2);
            expect(result.current.refreshing).toBe(false);
        });
    });

    it('should share one read and its answer between the badge and the page', async () => {
        const { approvals } = trackAim();

        const { result: badge } = renderApprovals();
        const { result: page } = renderApprovals();

        await waitFor(() => expect(badge.current.humanApprovals.status).toBe('loaded'));
        expect(page.current.humanApprovals).toEqual(badge.current.humanApprovals);
        expect(approvals.callCount).toBe(1);
    });

    describe('through the real sequence of the permissions', () => {
        const secondEnvironmentApprovals = `${TEST_GAMMA_BASE}/environments/env-2-id/modules/aim/approvals`;
        const permissionsUrl = (environmentId: string) => `${TEST_MANAGEMENT_BASE}/environments/${environmentId}/permissions`;
        const mayDecide = { HITL_DECISIONS: ['R', 'U'] };
        let stopPermissionSync: () => void;

        function signIn() {
            act(() => seedUser());
        }

        function signOut() {
            act(() => useAuthStore.setState({ user: null }));
        }

        function switchToSecondEnvironment() {
            act(() => useEnvironmentStore.getState().setCurrentEnvironment(TEST_ENVIRONMENTS[1]!));
        }

        beforeEach(() => {
            stopPermissionSync = startPermissionSync();
            jest.spyOn(console, 'error').mockImplementation(() => undefined);
            respondWith('get', permissionsUrl('env-1-id'), mayDecide);
            respondWith('get', permissionsUrl('env-2-id'), mayDecide);
        });

        afterEach(() => {
            stopPermissionSync();
        });

        it('should drop the rows of the old environment, never rendering them under the new one, and not read the settings again', async () => {
            const settings = trackHandler('get', `${TEST_GAMMA_BASE}/environments/:envId/modules/aim/_settings`, { features: ['hitl'] });
            const secondEnvironment = trackHandler('get', secondEnvironmentApprovals, NO_APPROVALS);
            signIn();
            const renders: HumanApprovals[] = [];
            const { result } = renderHook(
                () => {
                    const approvals = useHumanApprovals();
                    renders.push(approvals.humanApprovals);
                    return approvals;
                },
                { wrapper },
            );
            await waitFor(() => expect(result.current.humanApprovals).toMatchObject({ environmentId: 'env-1-id' }));
            renders.length = 0;

            switchToSecondEnvironment();

            expect(result.current.humanApprovals.status).toBe('loading');
            await waitFor(() =>
                expect(result.current.humanApprovals).toEqual({ status: 'loaded', environmentId: 'env-2-id', items: [], totalCount: 0 }),
            );
            expect(renders).not.toContainEqual(expect.objectContaining({ environmentId: 'env-1-id' }));
            expect(secondEnvironment.callCount).toBe(1);
            expect(settings.callCount).toBe(1);
        });

        it('should ask nothing of an environment where the user may not decide', async () => {
            respondWith('get', permissionsUrl('env-2-id'), {});
            const secondEnvironment = trackHandler('get', secondEnvironmentApprovals, NO_APPROVALS);
            signIn();
            const { result } = renderApprovals();
            await waitFor(() => expect(result.current.humanApprovals.status).toBe('loaded'));

            switchToSecondEnvironment();

            await waitFor(() => expect(result.current.humanApprovals.status).toBe('off'));
            expect(secondEnvironment.callCount).toBe(0);
        });

        it('should drop an answer for the old environment that arrives late', async () => {
            const firstEnvironment = holdApprovals();
            respondWith('get', secondEnvironmentApprovals, NO_APPROVALS);
            signIn();
            const { result } = renderApprovals();
            await waitFor(() => expect(firstEnvironment.calls).toBe(1));

            switchToSecondEnvironment();
            await waitFor(() => expect(result.current.humanApprovals).toMatchObject({ environmentId: 'env-2-id', totalCount: 0 }));

            act(() => firstEnvironment.answer(TEST_HUMAN_APPROVALS_RESPONSE));
            await settleReads();

            expect(result.current.humanApprovals).toMatchObject({ environmentId: 'env-2-id', totalCount: 0 });
        });

        it('should drop, on sign-out, the rows and what is known of the settings, so the next user starts from nothing', async () => {
            const settings = trackHandler('get', SETTINGS_URL, { features: ['hitl'] });
            signIn();
            const { result } = renderApprovals();
            await waitFor(() => expect(result.current.humanApprovals.status).toBe('loaded'));

            signOut();
            expect(result.current.humanApprovals.status).toBe('loading');

            signIn();
            await waitFor(() => expect(result.current.humanApprovals.status).toBe('loaded'));
            expect(settings.callCount).toBe(2);
        });

        it('should let no read made for the user before write anything, before or after its settings are read', async () => {
            const settings = trackHandler('get', SETTINGS_URL, { features: ['hitl'] });
            let answerSettings: ((response: Response) => void) | undefined;
            server.use(http.get(SETTINGS_URL, () => new Promise<Response>(resolve => (answerSettings = resolve)), { once: true }));
            const approvals = holdApprovals();
            signIn();
            const { result } = renderApprovals();
            await waitFor(() => expect(answerSettings).toBeDefined());

            signOut();
            act(() => answerSettings?.(HttpResponse.json({ features: [] })));
            await settleReads();
            expect(approvals.calls).toBe(0);

            signIn();
            await waitFor(() => expect(settings.callCount).toBe(1));
            await waitFor(() => expect(approvals.calls).toBe(1));
            act(() => approvals.answer(NO_APPROVALS));
            await waitFor(() => expect(result.current.humanApprovals).toMatchObject({ status: 'loaded', totalCount: 0 }));
        });

        it('should drop an answer to a read made for the user before, in the same environment', async () => {
            const firstRead = holdApprovals();
            signIn();
            const { result } = renderApprovals();
            await waitFor(() => expect(firstRead.calls).toBe(1));

            signOut();
            const secondRead = holdApprovals();
            signIn();
            await waitFor(() => expect(secondRead.calls).toBe(1));

            act(() => firstRead.answer(TEST_HUMAN_APPROVALS_RESPONSE));
            await settleReads();
            expect(result.current.humanApprovals.status).toBe('loading');

            act(() => secondRead.answer(NO_APPROVALS));
            await waitFor(() => expect(result.current.humanApprovals).toMatchObject({ totalCount: 0 }));
        });

        it('should read the permissions again on refresh when they could not be read', async () => {
            respondWith('get', permissionsUrl('env-1-id'), { message: 'boom' }, 500);
            signIn();
            const { result } = renderApprovals();
            await waitFor(() => expect(result.current.humanApprovals.status).toBe('failed'));

            respondWith('get', permissionsUrl('env-1-id'), mayDecide);
            act(() => result.current.refresh());

            await waitFor(() => expect(result.current.humanApprovals.status).toBe('loaded'));
        });
    });
});
