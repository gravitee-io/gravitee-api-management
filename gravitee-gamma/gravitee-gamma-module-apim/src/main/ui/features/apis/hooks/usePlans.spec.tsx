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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { usePlanStatusCounts, usePlanTransition, useReorderPlan, useUpdatePlan } from './usePlans';
import { listPlans, transitionPlan, updatePlan } from '../services/plans';
import { EMPTY_GENERAL, EMPTY_RESTRICTIONS, EMPTY_SECURITY } from '../types/plan';
import type { ManagedPlan, ManagedPlanPage, PlanContext, PlanFormValue, PlanStatus } from '../types/plan';
import { apiDetailKeys, apiPlanKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));
jest.mock('../services/plans', () => ({ listPlans: jest.fn(), transitionPlan: jest.fn(), updatePlan: jest.fn() }));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockUpdatePlan = jest.mocked(updatePlan);
const mockListPlans = jest.mocked(listPlans);
const mockTransitionPlan = jest.mocked(transitionPlan);

const CTX: PlanContext = { type: 'api', entityId: 'api-1' };

const RENAMED_PLAN: ManagedPlan = {
    id: 'plan-1',
    name: 'Renamed by operator',
    description: 'Plan owned by the upstream provider',
    status: 'PUBLISHED',
    security: { type: 'API_KEY' },
    order: 1,
    validation: 'AUTO',
    definitionVersion: 'FEDERATED',
};

const FORM: PlanFormValue = {
    securityType: 'API_KEY',
    general: { ...EMPTY_GENERAL, name: 'Renamed by operator' },
    security: { ...EMPTY_SECURITY },
    restrictions: { ...EMPTY_RESTRICTIONS },
};

function renderUpdatePlanHook(ctx: PlanContext = CTX) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    const invalidateQueries = jest.spyOn(queryClient, 'invalidateQueries');
    const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    const { result } = renderHook(() => useUpdatePlan(ctx), { wrapper });
    return { result, queryClient, invalidateQueries };
}

describe('useUpdatePlan once the mutation settles', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT', hrids: ['DEFAULT'] });
        mockUpdatePlan.mockResolvedValue(RENAMED_PLAN);
    });

    it('stores the saved plan under its detail key so the plan page shows the new value', async () => {
        const { result, queryClient } = renderUpdatePlanHook();

        result.current.mutate({ planId: 'plan-1', form: FORM, definitionVersion: 'FEDERATED' });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(queryClient.getQueryData(apiPlanKeys.detail('DEFAULT', CTX, 'plan-1'))).toEqual(RENAMED_PLAN);
    });

    it('invalidates the plan list of the edited API', async () => {
        const { result, invalidateQueries } = renderUpdatePlanHook();

        result.current.mutate({ planId: 'plan-1', form: FORM, definitionVersion: 'FEDERATED' });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: ['api-plans', 'api', 'api-1'] });
    });

    it.each([
        { ctx: { type: 'api', entityId: 'api-1' } as PlanContext, invalidatesApiDetail: true },
        { ctx: { type: 'api-product', entityId: 'api-1' } as PlanContext, invalidatesApiDetail: false },
    ])('invalidates the API detail only for an API context ($ctx.type)', async ({ ctx, invalidatesApiDetail }) => {
        const { result, invalidateQueries } = renderUpdatePlanHook(ctx);

        result.current.mutate({ planId: 'plan-1', form: FORM, definitionVersion: 'FEDERATED' });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        const apiDetailInvalidation = { queryKey: apiDetailKeys.detail('DEFAULT', 'api-1') };
        if (invalidatesApiDetail) {
            expect(invalidateQueries).toHaveBeenCalledWith(apiDetailInvalidation);
        } else {
            expect(invalidateQueries).not.toHaveBeenCalledWith(apiDetailInvalidation);
        }
    });
});

function renderReorderPlanHook() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    const invalidateQueries = jest.spyOn(queryClient, 'invalidateQueries');
    const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    const { result } = renderHook(() => useReorderPlan(CTX), { wrapper });
    return { result, invalidateQueries };
}

describe('useReorderPlan', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT', hrids: ['DEFAULT'] });
        mockUpdatePlan.mockResolvedValue(RENAMED_PLAN);
    });

    it('sends the full plan with the new order but without id and status', async () => {
        const { result } = renderReorderPlanHook();

        result.current.mutate({ planId: 'plan-1', fullPlan: RENAMED_PLAN, newOrder: 3 });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        const payload = mockUpdatePlan.mock.calls[0][3];
        expect(payload).toEqual(expect.objectContaining({ order: 3, definitionVersion: 'FEDERATED' }));
        expect(payload).not.toHaveProperty('id');
        expect(payload).not.toHaveProperty('status');
    });

    it('invalidates the plan list of the reordered API', async () => {
        const { result, invalidateQueries } = renderReorderPlanHook();

        result.current.mutate({ planId: 'plan-1', fullPlan: RENAMED_PLAN, newOrder: 3 });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: ['api-plans', 'api', 'api-1'] });
    });
});

const DEPRECATED_PLAN: ManagedPlan = { ...RENAMED_PLAN, status: 'DEPRECATED' };

function renderPlanTransitionHook() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    const invalidateQueries = jest.spyOn(queryClient, 'invalidateQueries');
    const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    const { result } = renderHook(() => usePlanTransition(CTX), { wrapper });
    return { result, queryClient, invalidateQueries };
}

describe('usePlanTransition once the transition succeeds', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT', hrids: ['DEFAULT'] });
        mockTransitionPlan.mockResolvedValue(DEPRECATED_PLAN);
    });

    it('stores the returned plan under its detail key', async () => {
        const { result, queryClient } = renderPlanTransitionHook();

        result.current.mutate({ planId: 'plan-1', action: 'deprecate' });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(queryClient.getQueryData(apiPlanKeys.detail('DEFAULT', CTX, 'plan-1'))).toEqual(DEPRECATED_PLAN);
    });

    it('invalidates the plan list and the API detail', async () => {
        const { result, invalidateQueries } = renderPlanTransitionHook();

        result.current.mutate({ planId: 'plan-1', action: 'deprecate' });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: ['api-plans', 'api', 'api-1'] });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: apiDetailKeys.detail('DEFAULT', 'api-1') });
    });
});

function planPageWithTotal(totalCount: number): ManagedPlanPage {
    return { data: [], pagination: { totalCount, pageIndex: 1, pageSize: 1 } };
}

function mockTotalsByStatus(totals: Partial<Record<PlanStatus, number>>) {
    mockListPlans.mockImplementation((_envId, _ctx, statuses) => {
        const total = totals[statuses[0]];
        return total === undefined ? new Promise<ManagedPlanPage>(() => {}) : Promise.resolve(planPageWithTotal(total));
    });
}

function renderPlanStatusCountsHook() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    return renderHook(() => usePlanStatusCounts(CTX), { wrapper });
}

describe('usePlanStatusCounts', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT', hrids: ['DEFAULT'] });
    });

    it('reports each status count and their sum once every status has loaded', async () => {
        mockTotalsByStatus({ STAGING: 1, PUBLISHED: 2, DEPRECATED: 3, CLOSED: 4 });

        const { result } = renderPlanStatusCountsHook();

        await waitFor(() => expect(result.current.isLoading).toBe(false));
        expect(result.current).toEqual({ staging: 1, published: 2, deprecated: 3, closed: 4, total: 10, isLoading: false });
    });

    it('reports a zero total when every status is empty', async () => {
        mockTotalsByStatus({ STAGING: 0, PUBLISHED: 0, DEPRECATED: 0, CLOSED: 0 });

        const { result } = renderPlanStatusCountsHook();

        await waitFor(() => expect(result.current.isLoading).toBe(false));
        expect(result.current.total).toBe(0);
    });

    it('stays loading and counts the unresolved status as zero while one status query is pending', async () => {
        mockTotalsByStatus({ STAGING: 1, PUBLISHED: 2, DEPRECATED: 3 });

        const { result } = renderPlanStatusCountsHook();

        await waitFor(() =>
            expect(result.current).toEqual({ staging: 1, published: 2, deprecated: 3, closed: 0, total: 6, isLoading: true }),
        );
    });
});
