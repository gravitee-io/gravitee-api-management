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
    createEnvironmentNotification,
    deleteEnvironmentNotification,
    listEnvironmentNotificationHooks,
    listEnvironmentNotifications,
    listEnvironmentNotifiers,
    updateEnvironmentNotification,
} from './environmentNotifications';
import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV1Env: jest.fn(),
}));

const mockApimFetchJsonV1Env = jest.mocked(apimFetchJsonV1Env);

describe('environmentNotifications service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonV1Env.mockResolvedValue(undefined);
    });

    describe('listEnvironmentNotifications', () => {
        it('calls GET on the environment notification settings resource', async () => {
            await listEnvironmentNotifications('DEFAULT');

            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('DEFAULT', '/configuration/notificationsettings');
        });
    });

    describe('listEnvironmentNotifiers', () => {
        it('calls GET on the environment notifiers resource', async () => {
            await listEnvironmentNotifiers('DEFAULT');

            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('DEFAULT', '/configuration/notifiers');
        });
    });

    describe('listEnvironmentNotificationHooks', () => {
        it('calls GET on the environment hooks resource', async () => {
            await listEnvironmentNotificationHooks('DEFAULT');

            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('DEFAULT', '/configuration/hooks');
        });
    });

    describe('createEnvironmentNotification', () => {
        it('POSTs a GENERIC ENVIRONMENT notification', async () => {
            await createEnvironmentNotification('DEFAULT', {
                name: 'My webhook',
                notifier: 'default-webhook',
                referenceType: 'ENVIRONMENT',
                referenceId: 'DEFAULT',
                config_type: 'GENERIC',
                hooks: [],
            });

            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('DEFAULT', '/configuration/notificationsettings', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    name: 'My webhook',
                    notifier: 'default-webhook',
                    referenceType: 'ENVIRONMENT',
                    referenceId: 'DEFAULT',
                    config_type: 'GENERIC',
                    hooks: [],
                }),
            });
        });
    });

    describe('updateEnvironmentNotification', () => {
        it('uses the trailing-slash root path for PORTAL notifications (no id)', async () => {
            await updateEnvironmentNotification('DEFAULT', {
                config_type: 'PORTAL',
                name: 'Console Notification',
                referenceType: 'ENVIRONMENT',
                referenceId: 'DEFAULT',
            });

            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith(
                'DEFAULT',
                '/configuration/notificationsettings/',
                expect.objectContaining({ method: 'PUT' }),
            );
        });

        it('includes the notification id for GENERIC updates', async () => {
            await updateEnvironmentNotification('DEFAULT', {
                id: 'n-1',
                config_type: 'GENERIC',
                name: 'Email alerts',
                referenceType: 'ENVIRONMENT',
                referenceId: 'DEFAULT',
            });

            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith(
                'DEFAULT',
                '/configuration/notificationsettings/n-1',
                expect.objectContaining({ method: 'PUT' }),
            );
        });

        it('does not PUT GENERIC updates to the PORTAL path when id is missing', async () => {
            await expect(
                updateEnvironmentNotification('DEFAULT', {
                    config_type: 'GENERIC',
                    name: 'Email alerts',
                    referenceType: 'ENVIRONMENT',
                    referenceId: 'DEFAULT',
                }),
            ).rejects.toThrow('Cannot update a GENERIC notification without an id');

            expect(mockApimFetchJsonV1Env).not.toHaveBeenCalled();
        });
    });

    describe('deleteEnvironmentNotification', () => {
        it('calls DELETE on the notification id resource', async () => {
            await deleteEnvironmentNotification('DEFAULT', 'notif id');

            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('DEFAULT', '/configuration/notificationsettings/notif%20id', {
                method: 'DELETE',
            });
        });
    });
});
