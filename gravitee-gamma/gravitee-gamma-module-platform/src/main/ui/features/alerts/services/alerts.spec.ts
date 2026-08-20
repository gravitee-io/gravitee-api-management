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
import { listPlatformAlerts, updatePlatformAlert } from './alerts';
import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';
import type { AlertTrigger } from '../types/alert';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV1Env: jest.fn(),
}));

const mockApimFetchJsonV1Env = jest.mocked(apimFetchJsonV1Env);

const ALERT: AlertTrigger = {
    id: 'alert-1',
    name: 'Node down',
    description: 'Gateway stopped',
    severity: 'CRITICAL',
    enabled: true,
    source: 'NODE_LIFECYCLE',
    type: 'NODE_LIFECYCLE_CHANGED',
    conditions: [],
    filters: [],
    notifications: [],
    notificationPeriods: [],
    dampening: { mode: 'STRICT_COUNT', trueEvaluations: 1 },
};

describe('platform alerts service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonV1Env.mockResolvedValue(undefined);
    });

    describe('listPlatformAlerts', () => {
        it('GETs /platform/alerts with event counts enabled', async () => {
            await listPlatformAlerts('env-1');

            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/platform/alerts?event_counts=true');
        });
    });

    describe('updatePlatformAlert', () => {
        it('PUTs the alert entity to /platform/alerts/{id}', async () => {
            const updated = { ...ALERT, enabled: false };
            await updatePlatformAlert('env-1', updated);

            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/platform/alerts/alert-1', {
                method: 'PUT',
                body: JSON.stringify(updated),
            });
        });

        it('URL-encodes the alert id', async () => {
            await updatePlatformAlert('env-1', { ...ALERT, id: 'a/b' });

            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/platform/alerts/a%2Fb', expect.any(Object));
        });
    });
});
