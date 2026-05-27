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
import { deleteApplicationMetadata, deleteApplicationNotification, updateApplicationNotification } from './applicationNotifications';
import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV1Env: jest.fn(),
}));

const mockApimFetchJsonV1Env = jest.mocked(apimFetchJsonV1Env);

describe('applicationNotifications service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonV1Env.mockResolvedValue(undefined);
    });

    describe('deleteApplicationNotification', () => {
        it('calls DELETE on the notification settings resource', async () => {
            await deleteApplicationNotification('DEFAULT', 'app/1', 'notif id');

            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('DEFAULT', '/applications/app%2F1/notificationsettings/notif%20id', {
                method: 'DELETE',
            });
        });
    });

    describe('updateApplicationNotification', () => {
        it('uses root path for PORTAL notifications', async () => {
            await updateApplicationNotification('DEFAULT', 'app-1', {
                config_type: 'PORTAL',
                name: 'Portal',
            });

            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith(
                'DEFAULT',
                '/applications/app-1/notificationsettings/',
                expect.objectContaining({ method: 'PUT' }),
            );
        });

        it('includes notification id for non-PORTAL updates', async () => {
            await updateApplicationNotification('DEFAULT', 'app-1', {
                id: 'n-1',
                config_type: 'DEFAULT',
                name: 'Email alerts',
            });

            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith(
                'DEFAULT',
                '/applications/app-1/notificationsettings/n-1',
                expect.objectContaining({ method: 'PUT' }),
            );
        });
    });

    describe('deleteApplicationMetadata', () => {
        it('calls DELETE on the metadata key resource', async () => {
            await deleteApplicationMetadata('DEFAULT', 'app-1', 'my-key');

            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('DEFAULT', '/applications/app-1/metadata/my-key', {
                method: 'DELETE',
            });
        });
    });
});
