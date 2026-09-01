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

import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { NotificationTemplatesPage } from './NotificationTemplatesPage';
import { useNotificationTemplates } from '../features/notification-templates/hooks/useNotificationTemplates';
import type { NotificationTemplateCategory } from '../features/notification-templates/types/notificationTemplate';

jest.mock('../features/notification-templates/hooks/useNotificationTemplates');

const mockUseNotificationTemplates = jest.mocked(useNotificationTemplates);

const CATEGORIES: NotificationTemplateCategory[] = [
    {
        scope: 'API',
        label: 'API',
        description: 'Sent when an API changes state.',
        customCount: 1,
        rows: [
            {
                scope: 'API',
                name: 'API Started',
                hook: 'API_STARTED',
                description: 'Triggered when an API is started.',
                overridden: true,
                templateSegment: 'API_STARTED',
            },
        ],
    },
];

function renderPage() {
    return render(
        <MemoryRouter>
            <NotificationTemplatesPage />
        </MemoryRouter>,
    );
}

describe('NotificationTemplatesPage', () => {
    it('renders grouped templates and the custom count', () => {
        mockUseNotificationTemplates.mockReturnValue({
            categories: CATEGORIES,
            templateCount: 1,
            customCount: 1,
            isLoading: false,
            isError: false,
            error: null,
            refetch: jest.fn(),
        });

        renderPage();

        expect(screen.getByText('Templates')).not.toBeNull();
        expect(screen.getByText('1 templates · 1 custom')).not.toBeNull();
        expect(screen.getByText('API Started')).not.toBeNull();
    });

    it('offers a retry when the list request fails', () => {
        const refetch = jest.fn();
        mockUseNotificationTemplates.mockReturnValue({
            categories: [],
            templateCount: 0,
            customCount: 0,
            isLoading: false,
            isError: true,
            error: new Error('boom'),
            refetch,
        });

        renderPage();

        expect(screen.getByText('Failed to load notification templates. Please refresh and try again.')).not.toBeNull();
        fireEvent.click(screen.getByRole('button', { name: 'Try again' }));
        expect(refetch).toHaveBeenCalled();
    });
});
