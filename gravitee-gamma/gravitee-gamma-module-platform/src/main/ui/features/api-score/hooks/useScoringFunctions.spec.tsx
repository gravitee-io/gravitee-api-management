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

import { useScoringFunctions } from './useScoringFunctions';
import { listScoringFunctions } from '../services/scoringFunctions';
import { environmentScoringKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));

jest.mock('../services/scoringFunctions', () => ({
    listScoringFunctions: jest.fn(),
    createScoringFunction: jest.fn(),
    deleteScoringFunction: jest.fn(),
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockList = jest.mocked(listScoringFunctions);

const FN = {
    name: 'checkTag.js',
    payload: 'module.exports = {}',
    createdAt: '2026-01-01T00:00:00Z',
    referenceId: 'DEFAULT',
    referenceType: 'ENVIRONMENT',
};

function createHarness() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return { queryClient, Wrapper };
}

describe('useScoringFunctions', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT' } as ReturnType<typeof useEnvironment>);
        mockList.mockResolvedValue([FN]);
    });

    it('loads GET /scoring/functions under the environment query key', async () => {
        const { queryClient, Wrapper } = createHarness();
        const { result } = renderHook(() => useScoringFunctions(), { wrapper: Wrapper });

        await waitFor(() => expect(result.current.isLoading).toBe(false));
        expect(mockList).toHaveBeenCalledWith('DEFAULT');
        expect(result.current.functions).toEqual([FN]);
        expect(queryClient.getQueryData(environmentScoringKeys.functions('DEFAULT'))).toEqual([FN]);
    });

    it('does not fetch without env.id', () => {
        mockUseEnvironment.mockReturnValue({ id: undefined } as unknown as ReturnType<typeof useEnvironment>);
        const { Wrapper } = createHarness();
        renderHook(() => useScoringFunctions(), { wrapper: Wrapper });
        expect(mockList).not.toHaveBeenCalled();
    });
});
