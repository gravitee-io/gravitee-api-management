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
import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useApiScoring } from './useApiScoring';
import { notify } from '../../../shared/notify';
import { evaluateApiScoring, getApiScoring, listScoringJobs } from '../services/apiScoring';
import type { ApiScoring, ScoringAsyncJob } from '../types/scoring';
import { apiScoringKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('../services/apiScoring', () => ({
    evaluateApiScoring: jest.fn(),
    getApiScoring: jest.fn(),
    listScoringJobs: jest.fn(),
}));

jest.mock('../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn(), info: jest.fn() },
}));

const mockUseEnvironment = useEnvironment as jest.MockedFunction<typeof useEnvironment>;
const mockGetApiScoring = getApiScoring as jest.MockedFunction<typeof getApiScoring>;
const mockListScoringJobs = listScoringJobs as jest.MockedFunction<typeof listScoringJobs>;
const mockEvaluateApiScoring = evaluateApiScoring as jest.MockedFunction<typeof evaluateApiScoring>;

const OLD_SCORE: ApiScoring = {
    createdAt: '2026-01-01T00:00:00Z',
    summary: { all: 1, errors: 1, warnings: 0, infos: 0, hints: 0, score: 0.5 },
    assets: [],
};

const NEW_SCORE: ApiScoring = {
    createdAt: '2026-09-10T12:00:00Z',
    summary: { all: 0, errors: 0, warnings: 0, infos: 0, hints: 0, score: 0.9 },
    assets: [],
};

function job(status: ScoringAsyncJob['status']): ScoringAsyncJob {
    return {
        id: `job-${status}`,
        sourceId: 'api-1',
        type: 'SCORING_REQUEST',
        status,
        createdAt: '2026-09-10T11:59:00.000Z',
        updatedAt: '2026-09-10T11:59:00.000Z',
    };
}

function createHarness() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return { queryClient, Wrapper };
}

describe('useApiScoring', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT' } as ReturnType<typeof useEnvironment>);
        mockGetApiScoring.mockResolvedValue(OLD_SCORE);
        mockListScoringJobs.mockResolvedValue({ data: [] });
        mockEvaluateApiScoring.mockResolvedValue({ status: 'PENDING' });
    });

    it('loads the scoring report and jobs for the API', async () => {
        const { Wrapper } = createHarness();
        const { result } = renderHook(() => useApiScoring('api-1'), { wrapper: Wrapper });

        await waitFor(() => expect(result.current.isLoading).toBe(false));

        expect(mockGetApiScoring).toHaveBeenCalledWith('DEFAULT', 'api-1');
        expect(mockListScoringJobs).toHaveBeenCalledWith('DEFAULT', 'api-1');
        expect(result.current.scoring?.summary?.score).toBe(0.5);
        expect(result.current.pending).toBe(false);
    });

    it('treats a missing report as never scored instead of an error', async () => {
        mockGetApiScoring.mockResolvedValue(null);
        const { Wrapper } = createHarness();
        const { result } = renderHook(() => useApiScoring('api-1'), { wrapper: Wrapper });

        await waitFor(() => expect(result.current.isLoading).toBe(false));
        expect(result.current.isError).toBe(false);
        expect(result.current.scoring).toBeNull();
    });

    it('is pending when the latest scoring job is PENDING', async () => {
        mockListScoringJobs.mockResolvedValue({ data: [job('PENDING')] });
        const { Wrapper } = createHarness();
        const { result } = renderHook(() => useApiScoring('api-1'), { wrapper: Wrapper });

        await waitFor(() => expect(result.current.pending).toBe(true));
    });

    it('treats a failed jobs query as a page error', async () => {
        mockListScoringJobs.mockRejectedValueOnce(new Error('jobs failed'));
        const { Wrapper } = createHarness();
        const { result } = renderHook(() => useApiScoring('api-1'), { wrapper: Wrapper });

        await waitFor(() => expect(result.current.isError).toBe(true));
        expect(result.current.error).toEqual(expect.any(Error));
    });

    it('stays pending after Evaluate until the jobs refetch completes', async () => {
        const { Wrapper } = createHarness();
        const { result } = renderHook(() => useApiScoring('api-1'), { wrapper: Wrapper });
        await waitFor(() => expect(result.current.isLoading).toBe(false));

        let resolveJobs: (value: { data: ScoringAsyncJob[] }) => void;
        mockListScoringJobs.mockImplementation(
            () =>
                new Promise(resolve => {
                    resolveJobs = resolve;
                }),
        );

        await act(async () => {
            result.current.evaluate();
        });

        await waitFor(() => expect(mockEvaluateApiScoring).toHaveBeenCalledWith('DEFAULT', 'api-1'));
        await waitFor(() => expect(result.current.pending).toBe(true));
        expect(result.current.jobs).toEqual([]);

        await act(async () => {
            resolveJobs!({ data: [job('PENDING')] });
        });

        await waitFor(() => expect(result.current.jobs[0]?.status).toBe('PENDING'));
        expect(result.current.pending).toBe(true);
    });

    it('POSTs evaluate and then reloads jobs', async () => {
        const { Wrapper } = createHarness();
        const { result } = renderHook(() => useApiScoring('api-1'), { wrapper: Wrapper });
        await waitFor(() => expect(result.current.isLoading).toBe(false));

        await act(async () => {
            result.current.evaluate();
        });

        await waitFor(() => expect(mockEvaluateApiScoring).toHaveBeenCalledWith('DEFAULT', 'api-1'));
        await waitFor(() => expect(mockListScoringJobs.mock.calls.length).toBeGreaterThan(1));
    });

    it('toasts a failed Evaluate and reloads jobs so an ERROR job is visible', async () => {
        let jobs: ScoringAsyncJob[] = [];
        mockListScoringJobs.mockImplementation(async () => ({ data: jobs }));
        mockEvaluateApiScoring.mockRejectedValueOnce(new Error('provider unreachable'));

        const { Wrapper } = createHarness();
        const { result } = renderHook(() => useApiScoring('api-1'), { wrapper: Wrapper });
        await waitFor(() => expect(result.current.isLoading).toBe(false));

        jobs = [job('ERROR')];
        await act(async () => {
            result.current.evaluate();
        });

        await waitFor(() => expect(result.current.jobs[0]?.status).toBe('ERROR'));
        expect(notify.error).toHaveBeenCalledWith(expect.any(Error), 'An error occurred while evaluating API Scoring.');
    });

    it('reloads the scoring report after Evaluate even if the job is already SUCCESS', async () => {
        let jobs: ScoringAsyncJob[] = [];
        let report: ApiScoring | null = OLD_SCORE;
        mockListScoringJobs.mockImplementation(async () => ({ data: jobs }));
        mockGetApiScoring.mockImplementation(async () => report);
        mockEvaluateApiScoring.mockImplementation(async () => {
            jobs = [job('SUCCESS')];
            report = NEW_SCORE;
            return { status: 'SUCCESS' };
        });

        const { Wrapper } = createHarness();
        const { result } = renderHook(() => useApiScoring('api-1'), { wrapper: Wrapper });
        await waitFor(() => expect(result.current.scoring?.summary?.score).toBe(0.5));

        await act(async () => {
            result.current.evaluate();
        });

        await waitFor(() => expect(result.current.scoring?.summary?.score).toBe(0.9));
    });

    it('reloads the scoring report when the latest job leaves PENDING', async () => {
        mockListScoringJobs.mockResolvedValue({ data: [job('PENDING')] });
        mockGetApiScoring.mockResolvedValue(OLD_SCORE);
        const { queryClient, Wrapper } = createHarness();
        const { result } = renderHook(() => useApiScoring('api-1'), { wrapper: Wrapper });

        await waitFor(() => expect(result.current.pending).toBe(true));
        expect(result.current.scoring?.summary?.score).toBe(0.5);

        mockGetApiScoring.mockResolvedValue(NEW_SCORE);
        await act(async () => {
            queryClient.setQueryData(apiScoringKeys.jobs('DEFAULT', 'api-1'), { data: [job('SUCCESS')] });
        });

        await waitFor(() => expect(result.current.pending).toBe(false));
        await waitFor(() => expect(result.current.scoring?.summary?.score).toBe(0.9));
    });
});
