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
import { getNotifierSchema, listNotifiers } from './notifiers';
import { ApimApiError, apimFetchJsonV1Env } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    ...jest.requireActual('../../../shared/api/apimClient'),
    apimFetchJsonV1Env: jest.fn(),
}));

const mockApimFetchJsonV1Env = jest.mocked(apimFetchJsonV1Env);

describe('notifiers service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('lists notifiers from GET /notifiers/', async () => {
        mockApimFetchJsonV1Env.mockResolvedValue([{ id: 'default-email', name: 'System e-mail' }]);

        await listNotifiers('env-1');

        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/notifiers/');
    });

    it('loads a notifier schema from GET /notifiers/{id}/schema', async () => {
        mockApimFetchJsonV1Env.mockResolvedValue({ type: 'object', properties: { to: { type: 'string' } } });

        const schema = await getNotifierSchema('env-1', 'default-email');

        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/notifiers/default-email/schema');
        expect(schema).toEqual({ type: 'object', properties: { to: { type: 'string' } } });
    });

    it('parses a double-encoded schema string', async () => {
        mockApimFetchJsonV1Env.mockResolvedValue(JSON.stringify({ type: 'object' }));

        const schema = await getNotifierSchema('env-1', 'webhook-notifier');

        expect(schema).toEqual({ type: 'object' });
    });

    it('URL-encodes the notifier id', async () => {
        mockApimFetchJsonV1Env.mockResolvedValue({});

        await getNotifierSchema('env-1', 'email/notifier');

        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/notifiers/email%2Fnotifier/schema');
    });

    it('returns an empty schema when the notifier schema is missing (404)', async () => {
        mockApimFetchJsonV1Env.mockRejectedValue(new ApimApiError(404, 'Not found'));

        await expect(getNotifierSchema('env-1', 'missing-notifier')).resolves.toEqual({});
    });

    it('rethrows non-404 schema errors', async () => {
        mockApimFetchJsonV1Env.mockRejectedValue(new ApimApiError(500, 'Internal Server Error'));

        await expect(getNotifierSchema('env-1', 'default-email')).rejects.toBeInstanceOf(ApimApiError);
    });
});
