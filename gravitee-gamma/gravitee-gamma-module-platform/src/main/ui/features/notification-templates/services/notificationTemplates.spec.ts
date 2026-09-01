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
    createNotificationTemplate,
    listNotificationTemplates,
    persistNotificationTemplate,
    searchNotificationTemplates,
    updateNotificationTemplate,
} from './notificationTemplates';
import { apimFetchJsonOrg } from '../../../shared/api/apimClient';
import type { NotificationTemplate } from '../types/notificationTemplate';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonOrg: jest.fn(),
}));

const mockApimFetchJsonOrg = jest.mocked(apimFetchJsonOrg);

const EMAIL: NotificationTemplate = {
    name: 'API Started',
    scope: 'API',
    type: 'EMAIL',
    hook: 'API_STARTED',
    content: '<p>started</p>',
    title: 'API started',
};

describe('notificationTemplates service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonOrg.mockResolvedValue([]);
    });

    it('lists all organization notification templates', async () => {
        await listNotificationTemplates();
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/notification-templates');
    });

    it('searches by scope and hook, including an empty hook for include-fragments', async () => {
        await searchNotificationTemplates({ scope: 'API', hook: 'API_STARTED' });
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/notification-templates?scope=API&hook=API_STARTED');

        await searchNotificationTemplates({ scope: 'TEMPLATES_TO_INCLUDE', hook: '' });
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/notification-templates?scope=TEMPLATES_TO_INCLUDE&hook=');
    });

    it('POSTs a first override without an id', async () => {
        await createNotificationTemplate({ ...EMAIL, id: 'should-strip' });
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/notification-templates', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(EMAIL),
        });
    });

    it('PUTs an existing customization by id', async () => {
        const existing = { ...EMAIL, id: 'tmpl-1', enabled: true };
        await updateNotificationTemplate(existing);
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/notification-templates/tmpl-1', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(existing),
        });
    });

    it('persists with POST when there is no id and PUT when there is', async () => {
        await persistNotificationTemplate(EMAIL);
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith(
            '/configuration/notification-templates',
            expect.objectContaining({ method: 'POST' }),
        );

        await persistNotificationTemplate({ ...EMAIL, id: 'tmpl-2' });
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith(
            '/configuration/notification-templates/tmpl-2',
            expect.objectContaining({ method: 'PUT' }),
        );
    });
});
