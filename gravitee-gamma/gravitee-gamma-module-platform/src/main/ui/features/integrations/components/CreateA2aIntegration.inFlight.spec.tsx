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

import { CreateA2aIntegration } from './CreateA2aIntegration';
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

const FAILED_CREATES = [
    {
        failure: 'an HTTP 400 response',
        error: new ApimApiError(400, 'Invalid integration'),
        name: 'Pricing Agent',
        url: 'https://pricing.example.com/.well-known/agent-card.json',
    },
    {
        failure: 'a network error with no response',
        error: new TypeError('Failed to fetch'),
        name: 'Calendar Agent',
        url: 'https://calendar.example.net/.well-known/agent-card.json',
    },
];

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
    renderWithGraphene(<CreateA2aIntegration />, { wrapper: Wrapper });
    return userEvent.setup();
}

async function enterText(user: UserEvent, input: HTMLElement, value: string) {
    await user.click(input);
    await user.paste(value);
}

async function fillForm(user: UserEvent, { name, url }: { name: string; url: string }) {
    await enterText(user, screen.getByRole('textbox', { name: /^Name/ }), name);
    await user.click(screen.getByRole('button', { name: 'Add another URL' }));
    await enterText(user, screen.getByRole('textbox', { name: 'Well-known URL 1' }), url);
}

function createButton(): HTMLElement {
    return screen.getByRole('button', { name: 'Create' });
}

function createRequests() {
    return mockApimFetchJsonV2.mock.calls.filter(([, path, init]) => path === '/integrations' && init?.method === 'POST');
}

describe('CreateA2aIntegration while a create request is in flight', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('sends one create request when submit is activated twice before the first response arrives', async () => {
        const pending = holdCreateResponses();
        const user = renderPage();
        await fillForm(user, { name: 'A2A Agents', url: 'https://agent.example.com/.well-known/agent.json' });

        act(() => {
            createButton().click();
            createButton().click();
        });
        await waitFor(() => expect(pending.length).toBeGreaterThan(0));
        await act(async () => {
            pending.forEach(response => response.resolve({ id: 'a2a-created-id', name: 'A2A Agents', provider: 'A2A' }));
        });

        await waitFor(() => expect(mockNavigate).toHaveBeenCalled());
        expect(createRequests()).toHaveLength(1);
    });

    it('keeps the submit control disabled until the create response arrives', async () => {
        const pending = holdCreateResponses();
        const user = renderPage();
        await fillForm(user, { name: 'A2A Agents', url: 'https://agent.example.com/.well-known/agent.json' });

        await user.click(createButton());

        await waitFor(() => expect(createRequests()).toHaveLength(1));
        await waitFor(() => expect(createButton()).toBeDisabled());
        expect(mockNavigate).not.toHaveBeenCalled();
        await act(async () => {
            pending[0].resolve({ id: 'a2a-created-id', name: 'A2A Agents', provider: 'A2A' });
        });
        await waitFor(() => expect(mockNavigate).toHaveBeenCalled());
    });

    it.each(FAILED_CREATES)('makes the submit control usable again after $failure', async ({ error, name, url }) => {
        const pending = holdCreateResponses();
        const user = renderPage();
        await fillForm(user, { name, url });
        await user.click(createButton());
        await waitFor(() => expect(createButton()).toBeDisabled());

        await act(async () => {
            pending[0].reject(error);
        });

        await waitFor(() => expect(createButton()).toBeEnabled());
        await user.click(createButton());
        await waitFor(() => expect(createRequests()).toHaveLength(2));
    });
});
