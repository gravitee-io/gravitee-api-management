/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { ApiSelectionDialog } from './ApiSelectionDialog';

const mockApis = [
    {
        id: 'api-payments',
        name: 'Payments API',
        version: '1.2.0',
        type: 'PROXY' as const,
        definitionVersion: 'V4' as const,
        description: 'Process payments',
        entrypoints: ['/payments/v1'],
        owner: { id: 'user-1', displayName: 'Platform Team' },
        categories: ['Payments'],
    },
    {
        id: 'api-accounts',
        name: 'Accounts API',
        version: '2.0.0',
        type: 'PROXY' as const,
        definitionVersion: 'V4' as const,
        description: 'Manage accounts',
        entrypoints: ['/accounts/v2'],
        owner: { id: 'user-2', displayName: 'Core Services' },
        categories: ['Accounts'],
    },
];

jest.mock('../../editor/services/api.service', () => ({
    searchApis: jest.fn().mockImplementation(({ q = '' } = {}) => {
        const query = q.toLowerCase();
        const data = mockApis.filter(api => api.name.toLowerCase().includes(query));
        return Promise.resolve({
            data,
            metadata: { pagination: { total: data.length, current_page: 1, size: 20, total_pages: 1 } },
        });
    }),
}));

describe('ApiSelectionDialog', () => {
    it('should filter APIs by search query', async () => {
        const user = userEvent.setup();

        renderWithGraphene(<ApiSelectionDialog open onOpenChange={jest.fn()} onSelect={jest.fn()} />);

        await waitFor(() => {
            expect(screen.getByRole('option', { name: /Payments API/i })).toBeInTheDocument();
        });

        await user.type(screen.getByRole('textbox', { name: 'Search APIs' }), 'Accounts');

        await waitFor(() => {
            expect(screen.queryByRole('option', { name: /Payments API/i })).not.toBeInTheDocument();
            expect(screen.getByRole('option', { name: /Accounts API/i })).toBeInTheDocument();
        });
    });

    it('should call onSelect when an API is chosen', async () => {
        const user = userEvent.setup();
        const onSelect = jest.fn();

        renderWithGraphene(<ApiSelectionDialog open onOpenChange={jest.fn()} onSelect={onSelect} />);

        await waitFor(() => {
            expect(screen.getByRole('option', { name: /Payments API/i })).toBeInTheDocument();
        });

        await user.click(screen.getByRole('option', { name: /Payments API/i }));
        expect(onSelect).toHaveBeenCalledWith('api-payments', 'Payments API');
    });
});
