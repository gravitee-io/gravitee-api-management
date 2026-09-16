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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { useScoringOverview } from './useScoringOverview';
import { getScoringOverview } from '../services/scoring';
import { environmentScoringKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));

jest.mock('../services/scoring', () => ({
    getScoringOverview: jest.fn(),
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockGetScoringOverview = jest.mocked(getScoringOverview);

function createHarness() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return { queryClient, Wrapper };
}

describe('useScoringOverview', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT' } as ReturnType<typeof useEnvironment>);
        mockGetScoringOverview.mockResolvedValue({
            id: 'DEFAULT',
            score: 0.83,
            errors: 3,
            warnings: 5,
            infos: 2,
            hints: 1,
        });
    });

    it('loads GET /scoring/overview under the environment query key', async () => {
        const { queryClient, Wrapper } = createHarness();
        const { result } = renderHook(() => useScoringOverview(), { wrapper: Wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(mockGetScoringOverview).toHaveBeenCalledWith('DEFAULT');
        expect(queryClient.getQueryData(environmentScoringKeys.overview('DEFAULT'))).toEqual({
            id: 'DEFAULT',
            score: 0.83,
            errors: 3,
            warnings: 5,
            infos: 2,
            hints: 1,
        });
    });

    it('does not fetch without env.id', () => {
        mockUseEnvironment.mockReturnValue({ id: undefined } as unknown as ReturnType<typeof useEnvironment>);
        const { Wrapper } = createHarness();
        renderHook(() => useScoringOverview(), { wrapper: Wrapper });

        expect(mockGetScoringOverview).not.toHaveBeenCalled();
    });
});
