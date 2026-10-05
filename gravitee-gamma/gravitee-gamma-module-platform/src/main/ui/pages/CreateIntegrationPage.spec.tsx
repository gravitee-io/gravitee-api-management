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
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import { CreateIntegrationPage } from './CreateIntegrationPage';
import { useCreateIntegration } from '../features/integrations/hooks/useCreateIntegration';
import { integrationProviderLabel } from '../features/integrations/utils/providerLabels';
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

function setCreatePending(isPending: boolean) {
    mockUseCreateIntegration.mockReturnValue({ mutateAsync: mockMutateAsync, isPending } as unknown as ReturnType<
        typeof useCreateIntegration
    >);
}

function checkedProviderNames(): string[] {
    return screen
        .getAllByRole('radio')
        .filter(radio => radio.getAttribute('aria-checked') === 'true')
        .map(radio => radio.textContent ?? '');
}

describe('CreateIntegrationPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockMutateAsync.mockResolvedValue({ id: 'int-42', name: 'My integration', provider: 'solace' });
        mockUseCreateIntegration.mockReturnValue({ mutateAsync: mockMutateAsync, isPending: false } as unknown as ReturnType<
            typeof useCreateIntegration
        >);
    });

    it('opens with no provider selected', () => {
        renderPage();

        expect(checkedProviderNames()).toEqual([]);
    });

    it.each([{ provider: 'MuleSoft' }, { provider: 'A2A Protocol' }])('checks $provider when the user picks it', async ({ provider }) => {
        renderPage();

        await userEvent.click(screen.getByRole('radio', { name: provider }));

        expect(checkedProviderNames()).toEqual([provider]);
    });

    it('goes back to the Integrations list', async () => {
        renderPage();

        await userEvent.click(screen.getByRole('button', { name: 'Back to Integrations' }));

        expect(mockNavigate).toHaveBeenCalledWith('..');
    });

    it.each([
        { label: 'AWS API Gateway', provider: 'aws-api-gateway' },
        { label: 'Solace', provider: 'solace' },
        { label: 'Apigee', provider: 'apigee' },
        { label: 'Azure API Management', provider: 'azure-api-management' },
        { label: 'IBM API Connect', provider: 'ibm-api-connect' },
        { label: 'Confluent Platform', provider: 'confluent-platform' },
        { label: 'MuleSoft', provider: 'mulesoft' },
        { label: 'Edge Stack', provider: 'edge-stack' },
    ])(
        'creates a $label integration with the exact $provider provider token, notifies success, and opens the created integration',
        async ({ label, provider }) => {
            const user = userEvent.setup();
            renderPage();

            await userEvent.click(screen.getByRole('radio', { name: label }));
            await user.type(screen.getByRole('textbox', { name: /^Name/ }), 'My integration');
            await user.click(screen.getByRole('button', { name: 'Create' }));

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

        await user.click(screen.getByRole('radio', { name: integrationProviderLabel('solace') }));
        await user.click(screen.getByRole('textbox', { name: /^Name/ }));
        await user.paste(name);
        if (description) {
            await user.click(screen.getByRole('textbox', { name: /^Description/ }));
            await user.paste(description);
        }
        await user.click(screen.getByRole('button', { name: 'Create' }));

        await waitFor(() => expect(mockMutateAsync).toHaveBeenCalledTimes(1));
        expect(mockMutateAsync).toHaveBeenCalledWith({ name, description, provider: 'solace' });
    });

    it('blocks Create while the creation is in progress', async () => {
        mockUseCreateIntegration.mockReturnValue({ mutateAsync: mockMutateAsync, isPending: true } as unknown as ReturnType<
            typeof useCreateIntegration
        >);
        const user = userEvent.setup();
        renderPage();

        await user.click(screen.getByRole('radio', { name: integrationProviderLabel('solace') }));
        await user.type(screen.getByRole('textbox', { name: /^Name/ }), 'My integration');

        expect(screen.getByRole('button', { name: 'Create' })).toBeDisabled();
    });

    it.each([
        { invalidInput: 'an emptied name', name: '', description: '', field: /^Name/, invalidValue: '', message: 'Name is required.' },
        {
            invalidInput: 'a 51-character name',
            name: 'n'.repeat(51),
            description: '',
            field: /^Name/,
            invalidValue: 'n'.repeat(51),
            message: 'Name can not exceed 50 characters.',
        },
        {
            invalidInput: 'a 251-character description',
            name: 'My integration',
            description: 'd'.repeat(251),
            field: /^Description/,
            invalidValue: 'd'.repeat(251),
            message: 'Description can not exceed 250 characters.',
        },
    ])(
        'flags $invalidInput on its field, keeps the full value, and blocks Create',
        async ({ name, description, field, invalidValue, message }) => {
            const user = userEvent.setup();
            renderPage();

            await user.click(screen.getByRole('radio', { name: integrationProviderLabel('solace') }));
            const nameInput = screen.getByRole('textbox', { name: /^Name/ });
            await user.type(nameInput, 'x');
            await user.clear(nameInput);
            if (name) {
                await user.paste(name);
            }
            await user.click(screen.getByRole('textbox', { name: /^Description/ }));
            if (description) {
                await user.paste(description);
            }
            await user.tab();

            const invalidField = screen.getByRole('textbox', { name: field });
            expect(invalidField).toHaveValue(invalidValue);
            expect(invalidField).toHaveAttribute('aria-invalid', 'true');
            expect(screen.getByText(message)).toBeInTheDocument();
            const createButton = screen.getByRole('button', { name: 'Create' });
            expect(createButton).toBeDisabled();
            await user.click(createButton);
            expect(mockMutateAsync).not.toHaveBeenCalled();
        },
    );

    it('does not flag the empty Name before the user touches it, but blocks Create', async () => {
        const user = userEvent.setup();
        renderPage();

        await user.click(screen.getByRole('radio', { name: integrationProviderLabel('solace') }));

        expect(screen.getByRole('textbox', { name: /^Name/ })).not.toHaveAttribute('aria-invalid', 'true');
        expect(screen.queryByText('Name is required.')).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Create' })).toBeDisabled();
    });

    it.each([
        { failure: 'a rejected request', error: new ApimApiError(400, 'Validation error') },
        { failure: 'an unexpected error', error: new Error('Network down') },
    ])('notifies $failure and stays on the filled form', async ({ error }) => {
        mockMutateAsync.mockRejectedValue(error);
        const user = userEvent.setup();
        renderPage();

        await user.click(screen.getByRole('radio', { name: integrationProviderLabel('solace') }));
        await user.type(screen.getByRole('textbox', { name: /^Name/ }), 'My integration');
        await user.type(screen.getByRole('textbox', { name: /^Description/ }), 'Ingests the EU gateways');
        await user.click(screen.getByRole('button', { name: 'Create' }));

        await waitFor(() => expect(mockNotify.error).toHaveBeenCalledTimes(1));
        expect(mockNotify.error).toHaveBeenCalledWith(error, 'Failed to create integration.');
        expect(mockNotify.success).not.toHaveBeenCalled();
        expect(mockNavigate).not.toHaveBeenCalled();
        expect(screen.getByRole('textbox', { name: /^Name/ })).toHaveValue('My integration');
        expect(screen.getByRole('textbox', { name: /^Description/ })).toHaveValue('Ingests the EU gateways');
        expect(screen.getByRole('radio', { name: integrationProviderLabel('solace') })).toBeChecked();
    });

    it('locks the provider choice while an A2A create is in flight and unlocks it once the create ends', async () => {
        const user = userEvent.setup();
        const view = renderPage();
        await user.click(screen.getByRole('radio', { name: 'A2A Protocol' }));

        setCreatePending(true);
        view.rerender(pageElement());

        await waitFor(() => screen.getAllByRole('radio').forEach(radio => expect(radio).toBeDisabled()));
        await user.click(screen.getByRole('radio', { name: 'MuleSoft' }));
        expect(checkedProviderNames()).toEqual(['A2A Protocol']);

        setCreatePending(false);
        view.rerender(pageElement());

        await waitFor(() => screen.getAllByRole('radio').forEach(radio => expect(radio).toBeEnabled()));
    });

    it('asks for well-known URLs only once A2A Protocol is picked', async () => {
        renderPage();

        await userEvent.click(screen.getByRole('radio', { name: 'Solace' }));
        expect(screen.queryByRole('button', { name: 'Add another URL' })).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('radio', { name: 'A2A Protocol' }));
        expect(screen.getByRole('button', { name: 'Add another URL' })).toBeInTheDocument();
        expect(screen.getByRole('textbox', { name: /^Name/ })).toBeInTheDocument();
        expect(screen.getByRole('textbox', { name: /^Description/ })).toBeInTheDocument();
    });

    it('creates an A2A integration with provider A2A and its well-known URLs, notifies success, and opens it', async () => {
        mockMutateAsync.mockResolvedValue({ id: 'a2a-created-id', name: 'Billing Agents', provider: 'A2A' });
        const user = userEvent.setup();
        renderPage();

        await user.click(screen.getByRole('radio', { name: 'A2A Protocol' }));
        await user.click(screen.getByRole('textbox', { name: /^Name/ }));
        await user.paste('Billing Agents');
        await user.click(screen.getByRole('button', { name: 'Add another URL' }));
        await user.click(screen.getByRole('textbox', { name: 'Well-known URL 1' }));
        await user.paste('https://billing.example.com/.well-known/agent-card.json');
        await user.click(screen.getByRole('button', { name: 'Create' }));

        await waitFor(() => expect(mockNavigate).toHaveBeenCalledTimes(1));
        expect(mockMutateAsync).toHaveBeenCalledTimes(1);
        expect(mockMutateAsync).toHaveBeenCalledWith({
            name: 'Billing Agents',
            description: '',
            provider: 'A2A',
            wellKnownUrls: ['https://billing.example.com/.well-known/agent-card.json'],
        });
        expect(mockNotify.success).toHaveBeenCalledWith('Integration Billing Agents created successfully');
        expect(mockNavigate).toHaveBeenCalledWith('../a2a-created-id');
    });

    it('drops back to the gateway form without well-known URLs when the user switches from A2A to another provider', async () => {
        const user = userEvent.setup();
        renderPage();

        await user.click(screen.getByRole('radio', { name: 'A2A Protocol' }));
        await user.click(screen.getByRole('radio', { name: 'Solace' }));

        expect(screen.queryByRole('button', { name: 'Add another URL' })).not.toBeInTheDocument();
        expect(checkedProviderNames()).toEqual(['Solace']);
    });
});
