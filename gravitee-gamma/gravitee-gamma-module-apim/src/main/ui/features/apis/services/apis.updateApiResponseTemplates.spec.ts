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
import { updateApiResponseTemplates } from './apis';
import { apimFetchJsonV2, apimFetchJsonV2WithMeta } from '../../../shared/api/apimClient';
import type { ResponseTemplatesMap } from '../types';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV2: jest.fn(),
    apimFetchJsonV2WithMeta: jest.fn(),
    apimFetchBlobV2: jest.fn(),
}));

const mockWithMeta = jest.mocked(apimFetchJsonV2WithMeta);
const mockFetchV2 = jest.mocked(apimFetchJsonV2);

const identity = (current: ResponseTemplatesMap) => current;

describe('updateApiResponseTemplates', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockFetchV2.mockResolvedValue(undefined);
    });

    it('PATCHes with If-Match from the GET ETag', async () => {
        mockWithMeta.mockResolvedValue({
            data: { responseTemplates: {}, updatedAt: '2026-01-01T00:00:00.000Z' },
            etag: '"1704067200000"',
        });

        await updateApiResponseTemplates('env-1', 'api-1', identity);

        expect(mockWithMeta).toHaveBeenCalledWith('env-1', '/apis/api-1');
        expect(mockFetchV2).toHaveBeenCalledWith('env-1', '/apis/api-1', {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json-patch+json',
                'If-Match': '"1704067200000"',
            },
            body: JSON.stringify([{ op: 'add', path: '/responseTemplates', value: {} }]),
        });
    });

    it('falls back to updatedAt millis when ETag is missing', async () => {
        mockWithMeta.mockResolvedValue({
            data: { responseTemplates: {}, updatedAt: '2026-01-01T00:00:00.000Z' },
            etag: null,
        });

        await updateApiResponseTemplates('env-1', 'api-1', identity);

        expect(mockFetchV2).toHaveBeenCalledWith(
            'env-1',
            '/apis/api-1',
            expect.objectContaining({
                headers: expect.objectContaining({
                    'If-Match': `"${Date.parse('2026-01-01T00:00:00.000Z')}"`,
                }),
            }),
        );
    });

    it('omits If-Match when neither ETag nor updatedAt is available', async () => {
        mockWithMeta.mockResolvedValue({
            data: { responseTemplates: {} },
            etag: null,
        });

        await updateApiResponseTemplates('env-1', 'api-1', identity);

        expect(mockFetchV2).toHaveBeenCalledWith('env-1', '/apis/api-1', {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json-patch+json' },
            body: JSON.stringify([{ op: 'add', path: '/responseTemplates', value: {} }]),
        });
    });
});
