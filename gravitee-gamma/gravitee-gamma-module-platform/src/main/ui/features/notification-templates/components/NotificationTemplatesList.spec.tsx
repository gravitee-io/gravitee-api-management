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

import { NotificationTemplatesList } from './NotificationTemplatesList';
import type { NotificationTemplateCategory } from '../types/notificationTemplate';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));

const CATEGORIES: NotificationTemplateCategory[] = [
    {
        scope: 'API',
        label: 'API',
        description: 'Sent when an API changes state, or when someone subscribes to one.',
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
    {
        scope: 'APPLICATION',
        label: 'Application',
        description: 'Sent when an application subscription changes.',
        customCount: 0,
        rows: [
            {
                scope: 'APPLICATION',
                name: 'New Subscription',
                hook: 'SUBSCRIPTION_NEW',
                description: 'A subscription is created.',
                overridden: false,
                templateSegment: 'SUBSCRIPTION_NEW',
            },
        ],
    },
];

function renderList(categories: NotificationTemplateCategory[] = CATEGORIES, isLoading = false) {
    return render(
        <MemoryRouter>
            <NotificationTemplatesList
                categories={categories}
                isLoading={isLoading}
                templateCount={categories.reduce((sum, category) => sum + category.rows.length, 0)}
                customCount={categories.reduce((sum, category) => sum + category.customCount, 0)}
            />
        </MemoryRouter>,
    );
}

describe('NotificationTemplatesList', () => {
    beforeEach(() => {
        mockNavigate.mockReset();
    });

    it('shows a Custom badge on overridden rows and the category custom count', () => {
        renderList();
        expect(screen.getByText('API Started')).not.toBeNull();
        expect(screen.getAllByText('Custom')).toHaveLength(1);
        expect(screen.getByText('1 custom')).not.toBeNull();
    });

    it('puts the summary count on the same row as Collapse all', () => {
        renderList();
        const summary = screen.getByText('2 templates · 1 custom');
        const collapse = screen.getByRole('button', { name: 'Collapse all' });
        expect(summary.parentElement).toBe(collapse.parentElement);
    });

    it('navigates to the scope and hook segment when a row is clicked', () => {
        renderList();
        fireEvent.click(screen.getByRole('button', { name: /API Started/ }));
        expect(mockNavigate).toHaveBeenCalledWith('API/API_STARTED');
    });

    it('expands every scope by default and collapses them from the toggle', () => {
        renderList();
        expect(screen.getByText('API Started')).not.toBeNull();
        expect(screen.getByText('New Subscription')).not.toBeNull();
        fireEvent.click(screen.getByRole('button', { name: 'Collapse all' }));
        expect(screen.queryByRole('button', { name: /API Started/ })).toBeNull();
        expect(screen.queryByRole('button', { name: /New Subscription/ })).toBeNull();
        fireEvent.click(screen.getByRole('button', { name: 'Expand all' }));
        expect(screen.getByText('API Started')).not.toBeNull();
        expect(screen.getByText('New Subscription')).not.toBeNull();
    });

    it('shows an empty message when there are no categories', () => {
        renderList([]);
        expect(screen.getByText('This organization has no notification templates configured.')).not.toBeNull();
    });
});
