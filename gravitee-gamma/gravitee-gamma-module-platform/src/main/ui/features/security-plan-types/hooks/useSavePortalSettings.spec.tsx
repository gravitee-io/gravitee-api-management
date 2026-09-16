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

import { useSavePortalSettings } from './useSavePortalSettings';
import { notify } from '../../../shared/notify';
import { environmentPortalKeys } from '../../../shared/utils/queryKeys';
import { savePortalSettings } from '../services/portalSettings';
import type { PortalSettings } from '../services/portalSettings';
import { portalSettingsKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));

jest.mock('../services/portalSettings', () => ({
    savePortalSettings: jest.fn(),
}));

jest.mock('../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockSavePortalSettings = jest.mocked(savePortalSettings);

describe('useSavePortalSettings', () => {
    let queryClient: QueryClient;
    let invalidateQueries: jest.SpiedFunction<QueryClient['invalidateQueries']>;

    beforeEach(() => {
        queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false }, queries: { retry: false } } });
        invalidateQueries = jest.spyOn(queryClient, 'invalidateQueries').mockResolvedValue(undefined);
        mockUseEnvironment.mockReturnValue({ id: 'env-1', organizationId: 'org-1' });
        mockSavePortalSettings.mockResolvedValue({ api: { primaryOwnerMode: 'USER' } });
    });

    afterEach(() => {
        queryClient.clear();
        jest.clearAllMocks();
    });

    function wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }

    it('invalidates portal settings and the environment portal snapshot after a successful save', async () => {
        const payload: PortalSettings = { api: { primaryOwnerMode: 'GROUP' } };
        const { result } = renderHook(
            () =>
                useSavePortalSettings({
                    successMessage: 'Primary owner mode saved successfully.',
                    errorMessage: 'Failed to save primary owner mode.',
                }),
            { wrapper },
        );

        await result.current.mutateAsync(payload);

        await waitFor(() => {
            expect(mockSavePortalSettings).toHaveBeenCalledWith('env-1', payload);
            expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: portalSettingsKeys.env('env-1') });
            expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: environmentPortalKeys.detail('env-1') });
        });
        expect(notify.success).toHaveBeenCalledWith('Primary owner mode saved successfully.');
    });
});
