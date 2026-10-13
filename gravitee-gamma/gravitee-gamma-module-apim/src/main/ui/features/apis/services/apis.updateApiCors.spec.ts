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
import { updateApiCors } from './apis';
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type { Cors } from '../types';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV2: jest.fn(),
    apimFetchJsonV2WithMeta: jest.fn(),
    apimFetchBlobV2: jest.fn(),
    apimFetchJsonOrg: jest.fn(),
    apimFetchJsonV1Env: jest.fn(),
}));

const mockFetch = jest.mocked(apimFetchJsonV2);

const cors: Cors = { enabled: true, allowPrivateNetwork: true };

function putListeners(): Record<string, unknown>[] {
    const init = mockFetch.mock.calls[1]?.[2] as { body?: string } | undefined;
    return JSON.parse(init?.body ?? '{}').listeners;
}

describe('updateApiCors', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockFetch.mockResolvedValue({});
    });

    it('merges rejectNullByte into an existing requestValidation object', async () => {
        mockFetch.mockResolvedValueOnce({
            id: 'api-1',
            listeners: [
                {
                    type: 'HTTP',
                    cors: { enabled: false },
                    requestValidation: { rejectNullByte: true, preserved: 'keep' },
                },
                { type: 'SUBSCRIPTION', entrypoints: [] },
            ],
        });

        await updateApiCors('env-1', 'api-1', cors, { rejectNullByte: false });

        const listeners = putListeners();
        expect(listeners[0]).toEqual({
            type: 'HTTP',
            cors,
            requestValidation: { rejectNullByte: false, preserved: 'keep' },
        });
        expect(listeners[1]).toEqual({ type: 'SUBSCRIPTION', entrypoints: [] });
    });

    it('does not add requestValidation when the listener has none and rejectNullByte is false', async () => {
        mockFetch.mockResolvedValueOnce({
            id: 'api-1',
            listeners: [{ type: 'HTTP', cors: { enabled: true } }],
        });

        await updateApiCors('env-1', 'api-1', cors, { rejectNullByte: false });

        expect(putListeners()[0]).toEqual({ type: 'HTTP', cors });
    });

    it('adds requestValidation when rejectNullByte is turned on', async () => {
        mockFetch.mockResolvedValueOnce({
            id: 'api-1',
            listeners: [{ type: 'HTTP', cors: { enabled: true } }],
        });

        await updateApiCors('env-1', 'api-1', cors, { rejectNullByte: true });

        expect(putListeners()[0].requestValidation).toEqual({ rejectNullByte: true });
    });
});
