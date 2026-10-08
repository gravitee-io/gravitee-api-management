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
import { act, screen, waitFor } from '@testing-library/react';
import userEvent, { type UserEvent } from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';

import { CreateGatewayIntegration } from './CreateGatewayIntegration';
import { ApimApiError, apimFetchJsonV2 } from '../../../shared/api/apimClient';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));

jest.mock('../../../shared/api/apimClient', () => ({
    ...jest.requireActual('../../../shared/api/apimClient'),
    apimFetchJsonV2: jest.fn(),
}));

jest.mock('../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn(), info: jest.fn() },
}));

const mockApimFetchJsonV2 = jest.mocked(apimFetchJsonV2);

interface PendingResponse {
    resolve: (value: unknown) => void;
    reject: (error: unknown) => void;
}

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

function holdCreateResponses(): PendingResponse[] {
    const pending: PendingResponse[] = [];
    mockApimFetchJsonV2.mockImplementation(
        () =>
            new Promise((resolve, reject) => {
                pending.push({ resolve, reject });
            }) as never,
    );
    return pending;
}

function renderPage() {
    const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return (
            <QueryClientProvider client={queryClient}>
                <MemoryRouter>{children}</MemoryRouter>
            </QueryClientProvider>
        );
    }
    renderWithGraphene(<CreateGatewayIntegration provider="solace" onCancel={jest.fn()} />, { wrapper: Wrapper });
    return userEvent.setup();
}

async function fillName(user: UserEvent, name: string) {
    await user.click(screen.getByRole('textbox', { name: /^Name/ }));
    await user.paste(name);
}

function createButton(): HTMLElement {
    return screen.getByRole('button', { name: 'Create integration' });
}

function createRequests() {
    return mockApimFetchJsonV2.mock.calls.filter(([, path, init]) => path === '/integrations' && init?.method === 'POST');
}

describe('CreateGatewayIntegration while a create request is in flight', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('sends one create request when submit is activated twice before the first response arrives', async () => {
        const pending = holdCreateResponses();
        const user = renderPage();
        await fillName(user, 'Solace Prod');

        act(() => {
            createButton().click();
            createButton().click();
        });
        await waitFor(() => expect(pending.length).toBeGreaterThan(0));
        await act(async () => {
            pending.forEach(response => response.resolve({ id: 'gw-created-id', name: 'Solace Prod', provider: 'solace' }));
        });

        await waitFor(() => expect(mockNavigate).toHaveBeenCalled());
        expect(createRequests()).toHaveLength(1);
    });

    it('sends a second create request when the user retries after a failed create', async () => {
        const pending = holdCreateResponses();
        const user = renderPage();
        await fillName(user, 'Solace Prod');
        await user.click(createButton());
        await waitFor(() => expect(createButton()).toBeDisabled());

        await act(async () => {
            pending[0].reject(new ApimApiError(400, 'Invalid integration'));
        });

        await waitFor(() => expect(createButton()).toBeEnabled());
        await user.click(createButton());
        await waitFor(() => expect(createRequests()).toHaveLength(2));
    });
});
