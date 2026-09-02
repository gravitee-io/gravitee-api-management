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
import { notificationSettingsUpdatePath } from './notificationSettingsUpdatePath';

describe('notificationSettingsUpdatePath', () => {
    it('uses the trailing-slash root path for PORTAL notifications', () => {
        expect(notificationSettingsUpdatePath('/configuration', { config_type: 'PORTAL' })).toBe('/configuration/notificationsettings/');
        expect(notificationSettingsUpdatePath('/applications/app-1', { config_type: 'PORTAL', id: 'ignored' })).toBe(
            '/applications/app-1/notificationsettings/',
        );
    });

    it('appends the encoded id for GENERIC notifications', () => {
        expect(notificationSettingsUpdatePath('/configuration', { config_type: 'GENERIC', id: 'n 1' })).toBe(
            '/configuration/notificationsettings/n%201',
        );
    });

    it('throws when a non-PORTAL notification has no id, so GENERIC updates cannot hit the PORTAL path', () => {
        expect(() => notificationSettingsUpdatePath('/configuration', { config_type: 'GENERIC' })).toThrow(
            'Cannot update a GENERIC notification without an id',
        );
        expect(() => notificationSettingsUpdatePath('/configuration', { config_type: 'GENERIC', id: '  ' })).toThrow(
            'Cannot update a GENERIC notification without an id',
        );
    });
});
