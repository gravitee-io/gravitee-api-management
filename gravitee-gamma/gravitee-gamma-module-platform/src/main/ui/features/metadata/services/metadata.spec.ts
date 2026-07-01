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
import { createEnvironmentMetadata, deleteEnvironmentMetadata, listEnvironmentMetadata, updateEnvironmentMetadata } from './metadata';
import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV1Env: jest.fn(),
}));

const mockApimFetchJsonV1Env = jest.mocked(apimFetchJsonV1Env);

describe('metadata service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonV1Env.mockResolvedValue(undefined);
    });

    describe('listEnvironmentMetadata', () => {
        it('calls GET on the configuration/metadata resource', async () => {
            await listEnvironmentMetadata('env-1');
            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/metadata');
        });
    });

    describe('createEnvironmentMetadata', () => {
        it('calls POST with the serialized payload', async () => {
            const payload = { key: 'support-email', name: 'Support Email', format: 'MAIL' as const, value: 'help@example.com' };
            await createEnvironmentMetadata('env-1', payload);
            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/metadata', {
                method: 'POST',
                body: JSON.stringify(payload),
            });
        });
    });

    describe('updateEnvironmentMetadata', () => {
        it('calls PUT with the serialized payload', async () => {
            const payload = { key: 'support-email', name: 'Support Email', format: 'MAIL' as const, value: 'new@example.com' };
            await updateEnvironmentMetadata('env-1', payload);
            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/metadata', {
                method: 'PUT',
                body: JSON.stringify(payload),
            });
        });
    });

    describe('deleteEnvironmentMetadata', () => {
        it('calls DELETE on the keyed resource path', async () => {
            await deleteEnvironmentMetadata('env-1', 'support-email');
            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/metadata/support-email', {
                method: 'DELETE',
            });
        });

        it('URL-encodes the key in the path', async () => {
            await deleteEnvironmentMetadata('env-1', 'key with spaces');
            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/metadata/key%20with%20spaces', {
                method: 'DELETE',
            });
        });
    });
});
