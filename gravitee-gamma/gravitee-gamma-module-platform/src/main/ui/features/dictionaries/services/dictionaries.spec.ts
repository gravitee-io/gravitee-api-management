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
import {
    createEnvironmentDictionary,
    deleteEnvironmentDictionary,
    deployEnvironmentDictionary,
    getEnvironmentDictionary,
    listEnvironmentDictionaries,
    startEnvironmentDictionary,
    stopEnvironmentDictionary,
    undeployEnvironmentDictionary,
    updateEnvironmentDictionary,
} from './dictionaries';
import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';
import type { NewDictionaryPayload, UpdateDictionaryPayload } from '../types/dictionary';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV1Env: jest.fn(),
}));

const mockApimFetchJsonV1Env = jest.mocked(apimFetchJsonV1Env);

describe('dictionaries service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonV1Env.mockResolvedValue(undefined);
    });

    describe('listEnvironmentDictionaries', () => {
        it('calls GET on the configuration/dictionaries resource', async () => {
            await listEnvironmentDictionaries('env-1');
            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/dictionaries');
        });
    });

    describe('getEnvironmentDictionary', () => {
        it('calls GET on the keyed resource path', async () => {
            await getEnvironmentDictionary('env-1', 'dict-1');
            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/dictionaries/dict-1');
        });

        it('URL-encodes the dictionary id in the path', async () => {
            await getEnvironmentDictionary('env-1', 'id with spaces');
            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/dictionaries/id%20with%20spaces');
        });
    });

    describe('createEnvironmentDictionary', () => {
        it('calls POST with a MANUAL payload', async () => {
            const payload: NewDictionaryPayload = {
                key: 'countries',
                name: 'Countries',
                description: 'Country codes',
                type: 'MANUAL',
                properties: { FR: 'France' },
            };
            await createEnvironmentDictionary('env-1', payload);
            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/dictionaries', {
                method: 'POST',
                body: JSON.stringify(payload),
            });
        });

        it('calls POST with a DYNAMIC payload including provider and trigger', async () => {
            const payload: NewDictionaryPayload = {
                key: 'remote-codes',
                name: 'Remote Codes',
                type: 'DYNAMIC',
                provider: {
                    type: 'HTTP',
                    configuration: {
                        url: 'https://example.com/dict',
                        method: 'GET',
                        specification: '[]',
                        useSystemProxy: false,
                    },
                },
                trigger: { rate: 5, unit: 'MINUTES' },
            };
            await createEnvironmentDictionary('env-1', payload);
            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/dictionaries', {
                method: 'POST',
                body: JSON.stringify(payload),
            });
        });
    });

    describe('updateEnvironmentDictionary', () => {
        it('calls PUT with the serialized payload', async () => {
            const payload: UpdateDictionaryPayload = {
                name: 'Countries',
                description: 'Updated',
                type: 'MANUAL',
                properties: { FR: 'France', DE: 'Germany' },
            };
            await updateEnvironmentDictionary('env-1', 'dict-1', payload);
            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/dictionaries/dict-1', {
                method: 'PUT',
                body: JSON.stringify(payload),
            });
        });
    });

    describe('deleteEnvironmentDictionary', () => {
        it('calls DELETE on the keyed resource path', async () => {
            await deleteEnvironmentDictionary('env-1', 'dict-1');
            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/dictionaries/dict-1', {
                method: 'DELETE',
            });
        });
    });

    describe('deployEnvironmentDictionary', () => {
        it('calls POST on the _deploy sub-resource', async () => {
            await deployEnvironmentDictionary('env-1', 'dict-1');
            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/dictionaries/dict-1/_deploy', {
                method: 'POST',
                body: JSON.stringify({}),
            });
        });
    });

    describe('undeployEnvironmentDictionary', () => {
        it('calls POST on the _undeploy sub-resource', async () => {
            await undeployEnvironmentDictionary('env-1', 'dict-1');
            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/dictionaries/dict-1/_undeploy', {
                method: 'POST',
                body: JSON.stringify({}),
            });
        });
    });

    describe('startEnvironmentDictionary', () => {
        it('calls POST with action=START', async () => {
            await startEnvironmentDictionary('env-1', 'dict-1');
            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/dictionaries/dict-1?action=START', {
                method: 'POST',
                body: JSON.stringify({}),
            });
        });
    });

    describe('stopEnvironmentDictionary', () => {
        it('calls POST with action=STOP', async () => {
            await stopEnvironmentDictionary('env-1', 'dict-1');
            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/dictionaries/dict-1?action=STOP', {
                method: 'POST',
                body: JSON.stringify({}),
            });
        });
    });
});
