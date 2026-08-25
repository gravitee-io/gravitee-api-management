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

import { useSavePlatformPolicies } from './useSavePlatformPolicies';
import { notify } from '../../../shared/notify';
import { getOrganization, updateOrganization } from '../services/platformPolicies';
import type { Organization } from '../types/platformPolicies';

jest.mock('../services/platformPolicies', () => ({
    getOrganization: jest.fn(),
    updateOrganization: jest.fn(),
}));

jest.mock('../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

const mockGetOrganization = jest.mocked(getOrganization);
const mockUpdateOrganization = jest.mocked(updateOrganization);

const STORED_ORGANIZATION: Organization = {
    id: 'DEFAULT',
    name: 'Gravitee',
    description: 'Default organization',
    flowMode: 'DEFAULT',
    flows: [{ id: 'flow-1', name: 'Stored flow', enabled: true, 'path-operator': { path: '/', operator: 'STARTS_WITH' } }],
};

function renderSaveHook() {
    const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return renderHook(() => useSavePlatformPolicies(), { wrapper: Wrapper });
}

async function save(output: Parameters<ReturnType<typeof useSavePlatformPolicies>['mutateAsync']>[0]) {
    const { result } = renderSaveHook();
    await result.current.mutateAsync(output);
    await waitFor(() => expect(mockUpdateOrganization).toHaveBeenCalled());
    return mockUpdateOrganization.mock.calls[0][0];
}

describe('useSavePlatformPolicies', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockGetOrganization.mockResolvedValue(STORED_ORGANIZATION);
        mockUpdateOrganization.mockResolvedValue(undefined);
    });

    it('writes the studio flows back in the stored format', async () => {
        const sent = await save({
            commonFlows: [
                {
                    id: 'flow-1',
                    name: 'Partner traffic',
                    enabled: true,
                    selectors: [{ type: 'HTTP', path: '/partners', pathOperator: 'STARTS_WITH' }],
                    request: [{ policy: 'rate-limit', name: 'Rate limit', enabled: true }],
                    tags: ['tag-eu'],
                },
            ],
        });

        expect(sent.flows).toEqual([
            expect.objectContaining({
                id: 'flow-1',
                name: 'Partner traffic',
                'path-operator': { path: '/partners', operator: 'STARTS_WITH' },
                pre: [expect.objectContaining({ policy: 'rate-limit' })],
                post: [],
                consumers: [{ consumerType: 'TAG', consumerId: 'tag-eu' }],
            }),
        ]);
    });

    it('keeps the organization fields the studio does not own', async () => {
        const sent = await save({ flowExecution: { mode: 'BEST_MATCH' } });
        expect(sent).toEqual(expect.objectContaining({ id: 'DEFAULT', name: 'Gravitee', description: 'Default organization' }));
    });

    it('sends the stored flows back untouched when only the flow mode changed', async () => {
        const sent = await save({ flowExecution: { mode: 'BEST_MATCH' } });
        expect(sent.flowMode).toBe('BEST_MATCH');
        expect(sent.flows).toBe(STORED_ORGANIZATION.flows);
    });

    it('re-reads the organization before writing so a concurrent change is not overwritten', async () => {
        await save({ flowExecution: { mode: 'DEFAULT' } });
        expect(mockGetOrganization).toHaveBeenCalledTimes(1);
    });

    it('rejects and reports the failure when the write is refused', async () => {
        const failure = new Error('Gateway unreachable');
        mockUpdateOrganization.mockRejectedValue(failure);
        const { result } = renderSaveHook();

        await expect(result.current.mutateAsync({ flowExecution: { mode: 'BEST_MATCH' } })).rejects.toThrow('Gateway unreachable');

        expect(notify.error).toHaveBeenCalledWith(failure, 'An error occurred while updating the platform policies.');
        expect(notify.success).not.toHaveBeenCalled();
    });
});
