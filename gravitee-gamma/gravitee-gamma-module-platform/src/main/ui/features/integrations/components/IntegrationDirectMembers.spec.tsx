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

import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { screen } from '@testing-library/react';

import { IntegrationDirectMembers } from './IntegrationDirectMembers';
import { ApimApiError } from '../../../shared/api/apimClient';
import { listIntegrationMembers } from '../services/integrationMembers';

jest.mock('../services/integrationMembers', () => ({ listIntegrationMembers: jest.fn() }));

const mockListIntegrationMembers = jest.mocked(listIntegrationMembers);

function renderSection() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    renderWithGraphene(
        <QueryClientProvider client={queryClient}>
            <IntegrationDirectMembers integrationId="int-1" />
        </QueryClientProvider>,
    );
}

describe('IntegrationDirectMembers', () => {
    beforeAll(() => {
        Object.defineProperty(window, 'matchMedia', {
            writable: true,
            value: jest.fn().mockImplementation((query: string) => ({
                matches: false,
                media: query,
                onchange: null,
                addListener: jest.fn(),
                removeListener: jest.fn(),
                addEventListener: jest.fn(),
                removeEventListener: jest.fn(),
                dispatchEvent: jest.fn(),
            })),
        });
    });

    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('shows a loading placeholder, with no Direct Members table, empty card or alert, while the members listing is pending', () => {
        mockListIntegrationMembers.mockReturnValue(new Promise(() => {}));

        renderSection();

        expect(document.querySelector('[data-slot="skeleton"]')).not.toBeNull();
        expect(screen.queryByRole('table')).toBeNull();
        expect(screen.queryByText('No direct members')).toBeNull();
        expect(screen.queryByRole('alert')).toBeNull();
    });

    it('shows a No direct members empty card instead of the Direct Members table when the members listing is empty', async () => {
        mockListIntegrationMembers.mockResolvedValue([]);

        renderSection();

        expect(await screen.findByText('No direct members')).toBeInTheDocument();
        expect(screen.queryByRole('table')).toBeNull();
    });

    it.each<[scenario: string, failure: unknown, expectedAlertText: string]>([
        [
            'an error response carrying a message',
            new ApimApiError(500, 'Members could not be loaded', { message: 'Members could not be loaded' }),
            'Members could not be loaded',
        ],
        // The client fills ApimApiError.message with the raw body text when the body has no message field.
        ['an HTTP 500 error response whose body has no message', new ApimApiError(500, '{}', {}), 'Failed to load members.'],
        ['an HTTP 502 error response whose body is not JSON', new ApimApiError(502, 'Bad Gateway', undefined), 'Failed to load members.'],
        ['a network error with no HTTP response', new TypeError('Failed to fetch'), 'Failed to load members.'],
    ])('shows only an alert reading the failure, with no Direct Members table, for %s', async (_scenario, failure, expectedAlertText) => {
        mockListIntegrationMembers.mockRejectedValue(failure);

        renderSection();

        const alert = await screen.findByRole('alert');
        expect(alert.textContent?.trim()).toBe(expectedAlertText);
        expect(screen.queryByRole('table')).toBeNull();
        expect(screen.queryByText('No direct members')).toBeNull();
    });
});
