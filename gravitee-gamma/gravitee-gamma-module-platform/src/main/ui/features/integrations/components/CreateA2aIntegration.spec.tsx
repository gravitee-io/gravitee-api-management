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
import { act, screen, waitFor } from '@testing-library/react';
import userEvent, { type UserEvent } from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import { CreateA2aIntegration } from './CreateA2aIntegration';
import { ApimApiError } from '../../../shared/api/apimClient';
import { notify } from '../../../shared/notify';
import { useCreateIntegration } from '../hooks/useCreateIntegration';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));

jest.mock('../hooks/useCreateIntegration', () => ({
    useCreateIntegration: jest.fn(),
}));

jest.mock('../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn(), info: jest.fn() },
}));

const mockUseCreateIntegration = jest.mocked(useCreateIntegration);
const mockNotify = jest.mocked(notify);
const mockMutateAsync = jest.fn();

const CREATE_FAILED_MESSAGE = 'An error occurred. Integration not created';

const NOTIFIED_FAILURES = [
    {
        failure: 'an error response',
        error: new ApimApiError(400, 'Invalid integration'),
        name: 'Orders Agent',
        url: 'https://orders.example.com/.well-known/agent-card.json',
    },
    {
        failure: 'a network error with no response',
        error: new TypeError('Failed to fetch'),
        name: 'Travel Planner',
        url: 'https://travel.example.net/agents/.well-known/agent-card.json',
    },
];

const FORM_KEEPING_FAILURES = [
    {
        failure: 'an error response',
        error: new ApimApiError(400, 'Invalid integration'),
        name: 'Inventory Agent',
        description: 'Tracks stock levels across warehouses',
        url: 'https://inventory.example.com/.well-known/agent-card.json',
    },
    {
        failure: 'a network error with no response',
        error: new TypeError('Failed to fetch'),
        name: 'Docs Search',
        description: 'Finds pages in the product documentation',
        url: 'https://docs.example.net/.well-known/agent-card.json',
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

function renderPage() {
    renderWithGraphene(
        <MemoryRouter>
            <CreateA2aIntegration onCancel={jest.fn()} />
        </MemoryRouter>,
    );
    return userEvent.setup();
}

function nameInput(): HTMLElement {
    return screen.getByRole('textbox', { name: /^Name/ });
}

function descriptionInput(): HTMLElement {
    return screen.getByRole('textbox', { name: /^Description/ });
}

function urlInputs(): HTMLInputElement[] {
    return screen.queryAllByRole<HTMLInputElement>('textbox', { name: /^Well-known URL \d+$/ });
}

async function enterText(user: UserEvent, input: HTMLElement, value: string) {
    await user.click(input);
    await user.paste(value);
}

async function fillForm(user: UserEvent, { name, description, urls }: { name: string; description: string; urls: string[] }) {
    await enterText(user, nameInput(), name);
    if (description) await enterText(user, descriptionInput(), description);
    for (const url of urls) {
        await user.click(screen.getByRole('button', { name: 'Add another URL' }));
        const inputs = urlInputs();
        await enterText(user, inputs[inputs.length - 1], url);
    }
}

async function submit(user: UserEvent) {
    await user.click(screen.getByRole('button', { name: 'Create integration' }));
}

describe('CreateA2aIntegration', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockMutateAsync.mockResolvedValue({ id: 'a2a-created-id', name: 'A2A Agents', provider: 'A2A' });
        mockUseCreateIntegration.mockReturnValue({ mutateAsync: mockMutateAsync, isPending: false } as unknown as ReturnType<
            typeof useCreateIntegration
        >);
    });

    it('notifies a successful create with the integration name', async () => {
        const user = renderPage();
        await fillForm(user, { name: 'A2A Agents', description: '', urls: ['https://agent.example.com/.well-known/agent.json'] });

        await submit(user);

        await waitFor(() => expect(mockNotify.success).toHaveBeenCalledTimes(1));
        expect(mockNotify.success.mock.calls[0][0]).toContain('A2A Agents');
    });

    it.each([
        {
            name: 'Weather Agents',
            description: 'Forecast agents',
            urls: ['https://weather.example.com/.well-known/agent-card.json'],
        },
        {
            name: 'Billing Agents',
            description: 'Invoice agents',
            urls: ['https://billing.example.com/.well-known/agent-card.json', 'https://search.example.com/.well-known/agent-card.json'],
        },
    ])(
        'creates $name with the entered description, provider A2A and its $urls.length well-known URLs in order',
        async ({ name, description, urls }) => {
            const user = renderPage();
            await fillForm(user, { name, description, urls });

            await submit(user);

            await waitFor(() => expect(mockMutateAsync).toHaveBeenCalledTimes(1));
            expect(mockMutateAsync).toHaveBeenCalledWith({ name, description, provider: 'A2A', wellKnownUrls: urls });
        },
    );

    it.each(NOTIFIED_FAILURES)('notifies the fixed create error message on $failure', async ({ error, name, url }) => {
        mockMutateAsync.mockRejectedValue(error);
        const user = renderPage();
        await fillForm(user, { name, description: '', urls: [url] });

        await submit(user);

        await waitFor(() => expect(mockNotify.error).toHaveBeenCalledTimes(1));
        expect(mockNotify.error.mock.calls[0][0]).toBe(CREATE_FAILED_MESSAGE);
        expect(mockNotify.success).not.toHaveBeenCalled();
    });

    it.each(FORM_KEEPING_FAILURES)('keeps the user on the filled create form on $failure', async ({ error, name, description, url }) => {
        mockMutateAsync.mockRejectedValue(error);
        const user = renderPage();
        await fillForm(user, { name, description, urls: [url] });

        await submit(user);
        await waitFor(() => expect(mockMutateAsync).toHaveBeenCalledTimes(1));
        await act(async () => {});

        expect(mockNavigate).not.toHaveBeenCalled();
        expect(nameInput()).toHaveValue(name);
        expect(descriptionInput()).toHaveValue(description);
        expect(urlInputs().map(input => input.value)).toEqual([url]);
    });
});
