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
import { ApimApiError } from '@gravitee/gamma-ui-shared/api';

import { createPortalsQueryClient } from './queryClient';

describe('portals query retries', () => {
    let queryClient: ReturnType<typeof createPortalsQueryClient>;

    beforeEach(() => {
        queryClient = createPortalsQueryClient();
    });

    afterEach(() => queryClient.clear());

    it.each([400, 403, 499])('does not retry HTTP %s', async status => {
        const error = new ApimApiError(status, 'Client error');
        const queryFn = jest.fn().mockRejectedValue(error);

        await expect(queryClient.fetchQuery({ queryKey: ['portals'], queryFn, retryDelay: 0 })).rejects.toBe(error);

        expect(queryFn).toHaveBeenCalledTimes(1);
    });

    it.each([new ApimApiError(500, 'Server error'), new TypeError('Network error')])('stops after two retries for %s', async error => {
        const queryFn = jest.fn().mockRejectedValue(error);

        await expect(queryClient.fetchQuery({ queryKey: ['portals'], queryFn, retryDelay: 0 })).rejects.toBe(error);

        expect(queryFn).toHaveBeenCalledTimes(3);
    });

    it('returns a successful response after transient failures', async () => {
        const queryFn = jest
            .fn()
            .mockRejectedValueOnce(new TypeError('Network error'))
            .mockRejectedValueOnce(new ApimApiError(503, 'Unavailable'))
            .mockResolvedValue('Portals loaded');

        await expect(queryClient.fetchQuery({ queryKey: ['portals'], queryFn, retryDelay: 0 })).resolves.toBe('Portals loaded');

        expect(queryFn).toHaveBeenCalledTimes(3);
    });
});
