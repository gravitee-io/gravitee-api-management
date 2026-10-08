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
import { createMemoryRouter, MemoryRouter, RouterProvider } from 'react-router-dom';

import { CreateIntegrationPage } from './CreateIntegrationPage';
import { useCreateIntegration } from '../features/integrations/hooks/useCreateIntegration';
import { ApimApiError } from '../shared/api/apimClient';
import { notify } from '../shared/notify';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));

jest.mock('../features/integrations/hooks/useCreateIntegration', () => ({
    useCreateIntegration: jest.fn(),
}));

jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn(), info: jest.fn() },
}));

const mockUseCreateIntegration = jest.mocked(useCreateIntegration);
const mockNotify = jest.mocked(notify);
const mockMutateAsync = jest.fn();

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

function pageElement() {
    return (
        <MemoryRouter>
            <CreateIntegrationPage />
        </MemoryRouter>
    );
}

function renderPage() {
    return renderWithGraphene(pageElement());
}

function renderPageAt(initialEntries: string[]) {
    const router = createMemoryRouter([{ path: '/', element: <CreateIntegrationPage /> }], {
        initialEntries,
        initialIndex: initialEntries.length - 1,
    });
    renderWithGraphene(<RouterProvider router={router} />);
    return router;
}

function setCreatePending(isPending: boolean) {
    mockUseCreateIntegration.mockReturnValue({ mutateAsync: mockMutateAsync, isPending } as unknown as ReturnType<
        typeof useCreateIntegration
    >);
}

function pickProvider(label: string) {
    return userEvent.click(screen.getByRole('button', { name: new RegExp(`^${label}`) }));
}

function nameInput() {
    return screen.getByRole('textbox', { name: /^Name/ });
}

function descriptionInput() {
    return screen.getByRole('textbox', { name: /^Description/ });
}

function createButton() {
    return screen.getByRole('button', { name: 'Create integration' });
}

async function fillFields(user: UserEvent, { name, description }: { name: string; description: string }) {
    await user.click(nameInput());
    await user.paste(name);
    await user.click(descriptionInput());
    await user.paste(description);
    await user.tab();
}

const INVALID_INPUTS = [
    { invalidInput: 'an emptied name', name: '', description: '', field: /^Name/, invalidValue: '', message: 'Name is required.' },
    {
        invalidInput: 'a 251-character description',
        name: 'My integration',
        description: 'd'.repeat(251),
        field: /^Description/,
        invalidValue: 'd'.repeat(251),
        message: 'Description can not exceed 250 characters.',
    },
];

describe('CreateIntegrationPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockMutateAsync.mockResolvedValue({ id: 'int-42', name: 'My integration', provider: 'solace' });
        mockUseCreateIntegration.mockReturnValue({ mutateAsync: mockMutateAsync, isPending: false } as unknown as ReturnType<
            typeof useCreateIntegration
        >);
    });

    it('opens on the provider choice, without any integration field', () => {
        renderPage();

        expect(screen.getByRole('textbox', { name: 'Filter providers' })).toBeInTheDocument();
        expect(screen.queryByRole('textbox', { name: /^Name/ })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Create integration' })).not.toBeInTheDocument();
    });

    it('goes back to the Integrations list from the provider choice', async () => {
        renderPage();

        await userEvent.click(screen.getByRole('button', { name: 'Back to Integrations' }));

        expect(mockNavigate).toHaveBeenCalledWith('..');
    });

    it('shows the chosen provider with a Change button above its form once a provider is picked', async () => {
        renderPage();

        await pickProvider('Solace');

        expect(screen.getByText('Solace')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Change' })).toBeInTheDocument();
        expect(screen.queryByRole('textbox', { name: 'Filter providers' })).not.toBeInTheDocument();
        expect(nameInput()).toBeInTheDocument();
    });

    it.each([
        { label: 'AWS API Gateway', provider: 'aws-api-gateway' },
        { label: 'Solace', provider: 'solace' },
    ])(
        'creates a $label integration with the exact $provider provider token, notifies success, and opens the created integration',
        async ({ label, provider }) => {
            const user = userEvent.setup();
            renderPage();

            await pickProvider(label);
            await user.type(nameInput(), 'My integration');
            await user.click(createButton());

            await waitFor(() => expect(mockNavigate).toHaveBeenCalledTimes(1));
            expect(mockMutateAsync).toHaveBeenCalledTimes(1);
            expect(mockMutateAsync).toHaveBeenCalledWith({ name: 'My integration', description: '', provider });
            expect(mockNotify.success).toHaveBeenCalledWith('Integration My integration created successfully');
            expect(mockNavigate).toHaveBeenCalledWith('../int-42');
        },
    );

    it.each([
        { input: 'a 50-character name', name: 'n'.repeat(50), description: '' },
        { input: 'a 250-character description', name: 'My integration', description: 'd'.repeat(250) },
        { input: 'a name with surrounding spaces', name: '  My integration  ', description: '' },
    ])('submits $input exactly as entered', async ({ name, description }) => {
        const user = userEvent.setup();
        renderPage();

        await pickProvider('Solace');
        await user.click(nameInput());
        await user.paste(name);
        await user.click(descriptionInput());
        await user.paste(description);
        await user.click(createButton());

        await waitFor(() => expect(mockMutateAsync).toHaveBeenCalledTimes(1));
        expect(mockMutateAsync).toHaveBeenCalledWith({ name, description, provider: 'solace' });
    });

    it('blocks Create integration while the creation is in progress', async () => {
        mockUseCreateIntegration.mockReturnValue({ mutateAsync: mockMutateAsync, isPending: true } as unknown as ReturnType<
            typeof useCreateIntegration
        >);
        const user = userEvent.setup();
        renderPage();

        await pickProvider('Solace');
        await user.type(nameInput(), 'My integration');

        expect(createButton()).toBeDisabled();
    });

    it.each(INVALID_INPUTS)(
        'flags $invalidInput on its field and keeps the full value',
        async ({ name, description, field, invalidValue, message }) => {
            const user = userEvent.setup();
            renderPage();
            await pickProvider('Solace');

            await fillFields(user, { name, description });

            const invalidField = screen.getByRole('textbox', { name: field });
            expect(invalidField).toHaveValue(invalidValue);
            expect(invalidField).toHaveAttribute('aria-invalid', 'true');
            expect(screen.getByText(message)).toBeInTheDocument();
        },
    );

    it.each(INVALID_INPUTS)('blocks Create integration for $invalidInput', async ({ name, description }) => {
        const user = userEvent.setup();
        renderPage();
        await pickProvider('Solace');

        await fillFields(user, { name, description });

        expect(createButton()).toBeDisabled();
    });

    it('does not flag the empty Name before the user touches it, but blocks Create integration', async () => {
        renderPage();

        await pickProvider('Solace');

        expect(nameInput()).not.toHaveAttribute('aria-invalid', 'true');
        expect(screen.queryByText('Name is required.')).not.toBeInTheDocument();
        expect(createButton()).toBeDisabled();
    });

    it.each([
        { failure: 'a rejected request', error: new ApimApiError(400, 'Validation error') },
        { failure: 'an unexpected error', error: new Error('Network down') },
    ])('notifies $failure and stays on the filled form', async ({ error }) => {
        mockMutateAsync.mockRejectedValue(error);
        const user = userEvent.setup();
        renderPage();

        await pickProvider('Solace');
        await user.type(nameInput(), 'My integration');
        await user.type(screen.getByRole('textbox', { name: /^Description/ }), 'Ingests the EU gateways');
        await user.click(createButton());

        await waitFor(() => expect(mockNotify.error).toHaveBeenCalledTimes(1));
        expect(mockNotify.error).toHaveBeenCalledWith(error, 'Failed to create integration.');
        expect(mockNotify.success).not.toHaveBeenCalled();
        expect(mockNavigate).not.toHaveBeenCalled();
        expect(nameInput()).toHaveValue('My integration');
        expect(screen.getByRole('textbox', { name: /^Description/ })).toHaveValue('Ingests the EU gateways');
    });

    it('starts the next form empty after Change', async () => {
        const user = userEvent.setup();
        renderPage();
        await pickProvider('Solace');
        await user.type(nameInput(), 'Typed before changing');
        await user.click(screen.getByRole('button', { name: 'Change' }));

        await pickProvider('Apigee');

        expect(nameInput()).toHaveValue('');
    });

    it('goes back to the Integrations list from Cancel', async () => {
        renderPage();
        await pickProvider('Solace');

        await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));

        expect(mockNavigate).toHaveBeenCalledWith('..');
    });

    it('locks Change, Back to providers and Cancel while a create is in flight', async () => {
        const view = renderPage();
        await pickProvider('Solace');

        setCreatePending(true);
        view.rerender(pageElement());

        await waitFor(() => expect(screen.getByRole('button', { name: 'Change' })).toBeDisabled());
        expect(screen.getByRole('button', { name: 'Back to providers' })).toBeDisabled();
        expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    });

    it('unlocks Change, Back to providers and Cancel once a create ends', async () => {
        const view = renderPage();
        await pickProvider('Solace');
        setCreatePending(true);
        view.rerender(pageElement());
        await waitFor(() => expect(screen.getByRole('button', { name: 'Change' })).toBeDisabled());

        setCreatePending(false);
        view.rerender(pageElement());

        await waitFor(() => expect(screen.getByRole('button', { name: 'Change' })).toBeEnabled());
        expect(screen.getByRole('button', { name: 'Back to providers' })).toBeEnabled();
        expect(screen.getByRole('button', { name: 'Cancel' })).toBeEnabled();
    });

    it('opens on the connection step of the provider named in the URL', () => {
        renderPageAt(['/?provider=apigee']);

        expect(screen.getByText('Apigee')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Change' })).toBeInTheDocument();
        expect(nameInput()).toBeInTheDocument();
        expect(screen.queryByRole('textbox', { name: 'Filter providers' })).not.toBeInTheDocument();
    });

    it('records the picked provider in the URL and returns to the provider choice on browser Back', async () => {
        const router = renderPageAt(['/']);
        await pickProvider('Apigee');

        expect(new URLSearchParams(router.state.location.search).get('provider')).toBe('apigee');

        await act(() => router.navigate(-1));

        expect(screen.getByRole('textbox', { name: 'Filter providers' })).toBeInTheDocument();
        expect(screen.queryByRole('textbox', { name: /^Name/ })).not.toBeInTheDocument();
    });

    it.each([{ action: 'Change' }, { action: 'Back to providers' }])('removes the provider from the URL on $action', async ({ action }) => {
        const router = renderPageAt(['/?provider=solace']);

        await userEvent.click(screen.getByRole('button', { name: action }));

        expect(new URLSearchParams(router.state.location.search).has('provider')).toBe(false);
        expect(screen.getByRole('textbox', { name: 'Filter providers' })).toBeInTheDocument();
        expect(screen.queryByRole('textbox', { name: /^Name/ })).not.toBeInTheDocument();
    });

    it('moves focus to the page heading once a provider is picked', async () => {
        renderPage();

        await pickProvider('Solace');

        expect(screen.getByRole('heading', { name: 'Create a new integration' })).toHaveFocus();
    });

    it.each([{ action: 'Change' }, { action: 'Back to providers' }])('moves focus to the page heading on $action', async ({ action }) => {
        renderPageAt(['/?provider=solace']);

        await userEvent.click(screen.getByRole('button', { name: action }));

        expect(screen.getByRole('heading', { name: 'Create a new integration' })).toHaveFocus();
    });

    it.each([{ entry: '/' }, { entry: '/?provider=apigee' }])('leaves focus alone on first render at $entry', ({ entry }) => {
        renderPageAt([entry]);

        expect(screen.getByRole('heading', { name: 'Create a new integration' })).not.toHaveFocus();
        expect(document.body).toHaveFocus();
    });
});
