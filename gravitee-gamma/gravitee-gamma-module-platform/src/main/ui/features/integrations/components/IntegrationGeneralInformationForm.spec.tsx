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
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { IntegrationGeneralInformationForm } from './IntegrationGeneralInformationForm';
import { ApimApiError } from '../../../shared/api/apimClient';
import { notify } from '../../../shared/notify';
import { updateIntegration } from '../services/integrationUpdate';
import type { Integration } from '../types/integration';

jest.mock('../services/integrationUpdate', () => ({ updateIntegration: jest.fn() }));

jest.mock('../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn(), info: jest.fn() },
}));

const mockUpdateIntegration = jest.mocked(updateIntegration);
const mockNotify = jest.mocked(notify);

const integration: Integration = {
    id: 'int-1',
    name: 'Old name',
    description: 'Old description',
    provider: 'solace',
    groups: ['Platform Team'],
};

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

function renderForm(loaded: Integration = integration) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    renderWithGraphene(
        <QueryClientProvider client={queryClient}>
            <IntegrationGeneralInformationForm integration={loaded} />
        </QueryClientProvider>,
    );
}

function nameField() {
    return screen.getByRole('textbox', { name: /^Name/ });
}

function descriptionField() {
    return screen.getByRole('textbox', { name: /^Description/ });
}

async function replaceField(user: ReturnType<typeof userEvent.setup>, field: HTMLElement, value: string) {
    await user.clear(field);
    if (value) {
        await user.paste(value);
    }
}

describe('IntegrationGeneralInformationForm', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUpdateIntegration.mockImplementation(async (_environmentId, _integrationId, request) => ({
            ...integration,
            name: request.name,
            description: request.description,
        }));
    });

    it('shows the saved name in the Name field after a successful save', async () => {
        mockUpdateIntegration.mockResolvedValueOnce({ ...integration, name: 'Saved name' });
        const user = userEvent.setup();
        renderForm();

        await replaceField(user, nameField(), 'New name');
        await user.click(screen.getByRole('button', { name: 'Save' }));

        await waitFor(() => expect(mockNotify.success).toHaveBeenCalledTimes(1));
        expect(nameField()).toHaveValue('Saved name');
    });

    it('shows the saved description in the Description field after a successful save', async () => {
        mockUpdateIntegration.mockResolvedValueOnce({ ...integration, description: 'Saved description' });
        const user = userEvent.setup();
        renderForm();

        await replaceField(user, descriptionField(), 'New description');
        await user.click(screen.getByRole('button', { name: 'Save' }));

        await waitFor(() => expect(mockNotify.success).toHaveBeenCalledTimes(1));
        expect(descriptionField()).toHaveValue('Saved description');
    });

    it('notifies that the integration was updated after a successful save', async () => {
        const user = userEvent.setup();
        renderForm();

        await replaceField(user, nameField(), 'New name');
        await user.click(screen.getByRole('button', { name: 'Save' }));

        await waitFor(() => expect(mockNotify.success).toHaveBeenCalledWith('Integration successfully updated!'));
        expect(mockNotify.error).not.toHaveBeenCalled();
    });

    it('offers no save action once a changed name has been saved', async () => {
        const user = userEvent.setup();
        renderForm();

        await replaceField(user, nameField(), 'New name');
        await user.click(screen.getByRole('button', { name: 'Save' }));

        await waitFor(() => expect(mockNotify.success).toHaveBeenCalledTimes(1));
        expect(nameField()).toHaveValue('New name');
        expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument();
    });

    it('notifies the API error message when the update is rejected', async () => {
        mockUpdateIntegration.mockRejectedValue(new ApimApiError(400, 'Update rejected', { message: 'Update rejected' }));
        const user = userEvent.setup();
        renderForm();

        await replaceField(user, nameField(), 'New name');
        await user.click(screen.getByRole('button', { name: 'Save' }));

        await waitFor(() => expect(mockNotify.error).toHaveBeenCalledWith('Something went wrong! Update rejected'));
        expect(mockNotify.success).not.toHaveBeenCalled();
    });

    it.each([
        {
            kind: 'an integration with groups, keeping its groups',
            loaded: integration,
            expectedRequest: { name: 'New name', description: 'Old description', groups: ['Platform Team'] },
        },
        {
            kind: 'an A2A integration, keeping its well-known URLs',
            loaded: {
                id: 'int-a2a',
                name: 'Old name',
                description: 'Old description',
                provider: 'A2A',
                groups: ['Platform Team'],
                wellKnownUrls: [{ url: 'https://agent.example.com/.well-known/agent.json' }],
            },
            expectedRequest: {
                name: 'New name',
                description: 'Old description',
                groups: ['Platform Team'],
                wellKnownUrls: [{ url: 'https://agent.example.com/.well-known/agent.json' }],
            },
        },
        {
            kind: 'an integration without groups or description',
            loaded: { id: 'int-1', name: 'Old name', provider: 'solace' },
            expectedRequest: { name: 'New name', description: '', groups: [] },
        },
    ])('sends exactly the edited values and the loaded associations for $kind', async ({ loaded, expectedRequest }) => {
        const user = userEvent.setup();
        renderForm(loaded);

        await replaceField(user, nameField(), 'New name');
        await user.click(screen.getByRole('button', { name: 'Save' }));

        await waitFor(() => expect(mockUpdateIntegration).toHaveBeenCalledTimes(1));
        expect(mockUpdateIntegration.mock.calls[0][1]).toBe(loaded.id);
        expect(mockUpdateIntegration.mock.calls[0][2]).toStrictEqual(expectedRequest);
    });

    it('keeps the edited values and the save action after a failed save', async () => {
        mockUpdateIntegration.mockRejectedValue(new ApimApiError(400, 'Update rejected', { message: 'Update rejected' }));
        const user = userEvent.setup();
        renderForm();

        await replaceField(user, nameField(), 'New name');
        await replaceField(user, descriptionField(), 'New description');
        await user.click(screen.getByRole('button', { name: 'Save' }));

        await waitFor(() => expect(mockNotify.error).toHaveBeenCalledTimes(1));
        expect(nameField()).toHaveValue('New name');
        expect(descriptionField()).toHaveValue('New description');
        expect(screen.getByRole('button', { name: 'Save' })).toBeEnabled();
    });

    it('disables the save action while the update is in flight', async () => {
        mockUpdateIntegration.mockReturnValue(new Promise(() => {}));
        const user = userEvent.setup();
        renderForm();

        await replaceField(user, nameField(), 'New name');
        await user.click(screen.getByRole('button', { name: 'Save' }));

        await waitFor(() => expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled());
        expect(mockUpdateIntegration).toHaveBeenCalledTimes(1);
    });

    it('offers no save action while nothing has changed', () => {
        renderForm();

        expect(nameField()).toHaveValue('Old name');
        expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument();
    });

    it.each([
        { invalidName: 'an emptied name', name: '' },
        { invalidName: 'a 51-character name', name: 'N'.repeat(51) },
    ])('offers no save action for $invalidName', async ({ name }) => {
        const user = userEvent.setup();
        renderForm();

        await replaceField(user, nameField(), name);

        expect(nameField()).toHaveValue(name);
        expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument();
    });

    it('offers an enabled save action for a 50-character name', async () => {
        const user = userEvent.setup();
        renderForm();

        await replaceField(user, nameField(), 'N'.repeat(50));

        expect(screen.getByRole('button', { name: 'Save' })).toBeEnabled();
    });

    it('keeps only the first 250 characters typed into the Description field', async () => {
        const user = userEvent.setup();
        renderForm({ ...integration, description: '' });

        await user.type(descriptionField(), 'd'.repeat(251));

        expect(descriptionField()).toHaveValue('d'.repeat(250));
    });
});
