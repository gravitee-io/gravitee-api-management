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
import { createApiMetadata, deleteApiMetadata, searchApiMetadata, updateApiMetadata } from './apiMetadata';
import { apimFetchJsonV1Env, apimFetchJsonV2 } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV1Env: jest.fn(),
    apimFetchJsonV2: jest.fn(),
}));

const mockApimFetchJsonV1Env = jest.mocked(apimFetchJsonV1Env);
const mockApimFetchJsonV2 = jest.mocked(apimFetchJsonV2);

describe('apiMetadata service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonV1Env.mockResolvedValue(undefined);
        mockApimFetchJsonV2.mockResolvedValue({ data: [] });
    });

    describe('searchApiMetadata', () => {
        it('calls the v2 metadata search with default pagination', async () => {
            await searchApiMetadata('env-1', 'api-1');
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/apis/api-1/metadata?page=1&perPage=10');
        });

        it('appends source and sortBy when provided', async () => {
            await searchApiMetadata('env-1', 'api-1', { page: 2, perPage: 25, source: 'GLOBAL', sortBy: '-name' });
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/apis/api-1/metadata?page=2&perPage=25&source=GLOBAL&sortBy=-name');
        });

        it('normalizes a missing data array to an empty list', async () => {
            mockApimFetchJsonV2.mockResolvedValueOnce({} as Awaited<ReturnType<typeof searchApiMetadata>>);
            const result = await searchApiMetadata('env-1', 'api-1');
            expect(result.data).toEqual([]);
        });

        it('URL-encodes the API id', async () => {
            await searchApiMetadata('env-1', 'api/with spaces');
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/apis/api%2Fwith%20spaces/metadata?page=1&perPage=10');
        });
    });

    describe('createApiMetadata', () => {
        it('POSTs the payload to the v1 metadata collection', async () => {
            const payload = { name: 'Team', format: 'STRING' as const, value: 'Platform' };
            await createApiMetadata('env-1', 'api-1', payload);
            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/apis/api-1/metadata/', {
                method: 'POST',
                body: JSON.stringify(payload),
            });
        });
    });

    describe('updateApiMetadata', () => {
        it('PUTs the payload to the keyed v1 resource', async () => {
            const payload = { key: 'team', name: 'Team', format: 'STRING' as const, value: 'Platform' };
            await updateApiMetadata('env-1', 'api-1', payload);
            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/apis/api-1/metadata/team', {
                method: 'PUT',
                body: JSON.stringify(payload),
            });
        });
    });

    describe('deleteApiMetadata', () => {
        it('DELETEs the keyed v1 resource', async () => {
            await deleteApiMetadata('env-1', 'api-1', 'team');
            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/apis/api-1/metadata/team', {
                method: 'DELETE',
            });
        });

        it('URL-encodes the metadata key', async () => {
            await deleteApiMetadata('env-1', 'api-1', 'key with spaces');
            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/apis/api-1/metadata/key%20with%20spaces', {
                method: 'DELETE',
            });
        });
    });
});
