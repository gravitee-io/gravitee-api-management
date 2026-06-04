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
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { authzApiService } from '../../api/authz-api.service';
import { authzQueryKeys } from '../../api/query-keys';
import { useUpdateSchema } from '../useUpdateSchema';

function makeWrapper(client: QueryClient) {
    return ({ children }: { children: ReactNode }) => <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('useUpdateSchema', () => {
    beforeEach(() => vi.restoreAllMocks());

    it('saves and invalidates the schema query', async () => {
        const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        const invalidate = vi.spyOn(client, 'invalidateQueries');
        vi.spyOn(authzApiService, 'updateSchema').mockResolvedValue({ environmentId: 'env-1', schemaText: 'x', updatedAt: null });

        const { result } = renderHook(() => useUpdateSchema('env-1'), { wrapper: makeWrapper(client) });
        result.current.mutate('entity Edited {};');

        await waitFor(() => expect(authzApiService.updateSchema).toHaveBeenCalledWith('env-1', 'entity Edited {};'));
        await waitFor(() => expect(invalidate).toHaveBeenCalledWith({ queryKey: authzQueryKeys.schema('env-1') }));
    });
});
