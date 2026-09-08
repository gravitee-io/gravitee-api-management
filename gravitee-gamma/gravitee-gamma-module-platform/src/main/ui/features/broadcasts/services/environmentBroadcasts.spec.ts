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

import { listEnvironmentRoles, sendEnvironmentBroadcast } from './environmentBroadcasts';
import { apimFetchJsonOrg, apimFetchJsonV1Env } from '../../../shared/api/apimClient';
import type { BroadcastPayload } from '../types';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonOrg: jest.fn(),
    apimFetchJsonV1Env: jest.fn(),
}));

const mockApimFetchJsonOrg = jest.mocked(apimFetchJsonOrg);
const mockApimFetchJsonV1Env = jest.mocked(apimFetchJsonV1Env);

describe('environmentBroadcasts service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonOrg.mockResolvedValue([]);
        mockApimFetchJsonV1Env.mockResolvedValue(0);
    });

    it('lists ENVIRONMENT-scoped roles from the organization configuration resource', async () => {
        await listEnvironmentRoles();
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/rolescopes/ENVIRONMENT/roles');
    });

    it('POSTs a PORTAL payload to the environment messages resource', async () => {
        const payload: BroadcastPayload = {
            channel: 'PORTAL',
            title: 'Maintenance',
            text: 'Gateway restart tonight.',
            recipient: { role_scope: 'ENVIRONMENT', role_value: ['ADMIN', 'USER'] },
        };
        mockApimFetchJsonV1Env.mockResolvedValue(4);

        await expect(sendEnvironmentBroadcast('DEFAULT', payload)).resolves.toBe(4);
        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('DEFAULT', '/messages', {
            method: 'POST',
            body: JSON.stringify(payload),
        });
    });

    it('does not POST when the environment id is missing', async () => {
        const payload: BroadcastPayload = {
            channel: 'PORTAL',
            title: 'Maintenance',
            text: 'Gateway restart tonight.',
            recipient: { role_scope: 'ENVIRONMENT', role_value: ['ADMIN'] },
        };

        await expect(sendEnvironmentBroadcast('', payload)).rejects.toThrow('Environment is not available.');
        expect(mockApimFetchJsonV1Env).not.toHaveBeenCalled();
    });

    it('POSTs an HTTP payload without role_value', async () => {
        const payload: BroadcastPayload = {
            channel: 'HTTP',
            text: 'Webhook ping',
            recipient: { url: 'https://hooks.example.com/notify' },
            params: { Accept: 'application/json' },
            useSystemProxy: false,
        };

        await sendEnvironmentBroadcast('env-1', payload);
        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/messages', {
            method: 'POST',
            body: JSON.stringify(payload),
        });
    });
});
