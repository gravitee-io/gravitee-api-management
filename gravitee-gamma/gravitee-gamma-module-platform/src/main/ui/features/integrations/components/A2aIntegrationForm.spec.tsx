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
import { screen } from '@testing-library/react';
import userEvent, { type UserEvent } from '@testing-library/user-event';

import { A2aIntegrationForm } from './A2aIntegrationForm';

const WEATHER_URL = 'https://weather.example.com/.well-known/agent-card.json';
const BILLING_URL = 'https://billing.example.com/.well-known/agent-card.json';
const SEARCH_URL = 'https://search.example.com/.well-known/agent-card.json';

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

function renderForm({ isSubmitting = false }: { isSubmitting?: boolean } = {}) {
    const onSubmit = jest.fn();
    const onCancel = jest.fn();
    renderWithGraphene(<A2aIntegrationForm onSubmit={onSubmit} onCancel={onCancel} isSubmitting={isSubmitting} />);
    return { onSubmit, onCancel, user: userEvent.setup() };
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

function urlValues(): string[] {
    return urlInputs().map(input => input.value);
}

function removeButtons(): HTMLElement[] {
    return screen.queryAllByRole('button', { name: /^Remove well-known URL/ });
}

async function enterText(user: UserEvent, input: HTMLElement, value: string) {
    await user.click(input);
    await user.paste(value);
}

async function addUrl(user: UserEvent, value: string) {
    await user.click(screen.getByRole('button', { name: 'Add another URL' }));
    const inputs = urlInputs();
    await enterText(user, inputs[inputs.length - 1], value);
}

async function addUrls(user: UserEvent, values: string[]) {
    for (const value of values) {
        await addUrl(user, value);
    }
}

function createButton(): HTMLElement {
    return screen.getByRole('button', { name: 'Create integration' });
}

function cancelButton(): HTMLElement {
    return screen.getByRole('button', { name: 'Cancel' });
}

async function submit(user: UserEvent) {
    await user.click(createButton());
}

async function fillValidForm(user: UserEvent) {
    await enterText(user, nameInput(), 'Weather Agents');
    await addUrl(user, WEATHER_URL);
}

describe('A2aIntegrationForm', () => {
    describe('Create button', () => {
        it('is disabled on the empty form', () => {
            renderForm();

            expect(createButton()).toBeDisabled();
        });

        it('is enabled once the name and a valid well-known URL are set', async () => {
            const { user } = renderForm();

            await fillValidForm(user);

            expect(createButton()).toBeEnabled();
        });

        it.each([
            { invalidInput: 'a 51-character name', name: 'n'.repeat(51), description: '', urls: [WEATHER_URL] },
            { invalidInput: 'a 251-character description', name: 'Weather Agents', description: 'd'.repeat(251), urls: [WEATHER_URL] },
            { invalidInput: 'no name', name: '', description: '', urls: [WEATHER_URL] },
            { invalidInput: 'no well-known URL', name: 'Weather Agents', description: '', urls: [] },
            { invalidInput: 'an invalid well-known URL', name: 'Weather Agents', description: '', urls: ['example.com/agent.json'] },
            { invalidInput: 'a duplicate well-known URL', name: 'Weather Agents', description: '', urls: [WEATHER_URL, WEATHER_URL] },
        ])('stays disabled with $invalidInput', async ({ name, description, urls }) => {
            const { user } = renderForm();
            if (name) await enterText(user, nameInput(), name);
            if (description) await enterText(user, descriptionInput(), description);
            await addUrls(user, urls);

            expect(createButton()).toBeDisabled();
        });

        it('is disabled while a create request is pending', async () => {
            const { user } = renderForm({ isSubmitting: true });

            await fillValidForm(user);

            expect(createButton()).toBeDisabled();
        });
    });

    describe('Cancel button', () => {
        it('cancels without submitting the form', async () => {
            const { onCancel, onSubmit, user } = renderForm();
            await fillValidForm(user);

            await user.click(cancelButton());

            expect(onCancel).toHaveBeenCalledTimes(1);
            expect(onSubmit).not.toHaveBeenCalled();
        });

        it('is disabled while a create request is pending', () => {
            renderForm({ isSubmitting: true });

            expect(cancelButton()).toBeDisabled();
        });

        it('is enabled while the form is still empty', () => {
            renderForm();

            expect(cancelButton()).toBeEnabled();
        });
    });

    describe('well-known URL entries', () => {
        it('opens with no well-known URL entry', () => {
            renderForm();

            expect(urlInputs()).toHaveLength(0);
            expect(screen.queryByText('Add at least one well-known URL.')).toBeNull();
        });

        it('offers no remove control when a single entry is shown', async () => {
            const { user } = renderForm();

            await user.click(screen.getByRole('button', { name: 'Add another URL' }));

            expect(urlInputs()).toHaveLength(1);
            expect(removeButtons()).toHaveLength(0);
        });

        it.each([{ existing: [] }, { existing: [WEATHER_URL] }, { existing: [WEATHER_URL, BILLING_URL, SEARCH_URL] }])(
            'adds exactly one empty, editable entry after $existing.length existing entries',
            async ({ existing }) => {
                const { user } = renderForm();
                await addUrls(user, existing);

                await user.click(screen.getByRole('button', { name: 'Add another URL' }));

                expect(urlValues()).toEqual([...existing, '']);
                await user.type(urlInputs()[existing.length], 'https://new.example.com');
                expect(urlInputs()[existing.length]).toHaveValue('https://new.example.com');
            },
        );

        it('offers no remove control once a second entry is removed', async () => {
            const { user } = renderForm();
            await addUrls(user, [WEATHER_URL, BILLING_URL]);

            await user.click(screen.getByRole('button', { name: 'Remove well-known URL 2' }));

            expect(urlValues()).toEqual([WEATHER_URL]);
            expect(removeButtons()).toHaveLength(0);
        });

        it.each([
            { existing: [WEATHER_URL, BILLING_URL], removedPosition: 1, remaining: [BILLING_URL] },
            { existing: [WEATHER_URL, BILLING_URL, SEARCH_URL], removedPosition: 2, remaining: [WEATHER_URL, SEARCH_URL] },
            { existing: [WEATHER_URL, BILLING_URL, SEARCH_URL], removedPosition: 3, remaining: [WEATHER_URL, BILLING_URL] },
        ])(
            'keeps the other values in order when entry $removedPosition of $existing.length is removed',
            async ({ existing, removedPosition, remaining }) => {
                const { user } = renderForm();
                await addUrls(user, existing);

                await user.click(screen.getByRole('button', { name: `Remove well-known URL ${removedPosition}` }));

                expect(urlValues()).toEqual(remaining);
            },
        );
    });

    describe('well-known URL validation', () => {
        it('accepts a valid URL', async () => {
            const { user } = renderForm();

            await addUrl(user, WEATHER_URL);

            expect(urlInputs()[0]).not.toHaveAttribute('aria-invalid', 'true');
        });

        it('marks an invalid URL invalid once it is edited', async () => {
            const { user } = renderForm();
            await enterText(user, nameInput(), 'Weather Agents');
            await addUrl(user, 'example.com/agent.json');

            expect(urlInputs()[0]).toHaveAttribute('aria-invalid', 'true');
        });

        it.each([
            { urls: ['example.com/agent.json'], message: 'Enter a valid http:// or https:// URL.' },
            { urls: ['https://a.com/x', 'HTTPS://A.COM/x'], message: 'This URL is already in the list.' },
        ])('shows the message $message for the offending entry once it is edited', async ({ urls, message }) => {
            const { user } = renderForm();
            await enterText(user, nameInput(), 'Weather Agents');
            await addUrls(user, urls);

            expect(screen.getAllByText(message)).toHaveLength(1);
        });

        it('does not mark a newly added empty entry invalid before it is edited or submitted', async () => {
            const { user } = renderForm();

            await user.click(screen.getByRole('button', { name: 'Add another URL' }));

            expect(urlInputs()[0]).not.toHaveAttribute('aria-invalid', 'true');
            expect(screen.queryByText('Enter a valid http:// or https:// URL.')).toBeNull();
        });

        it('keeps a padded URL unchanged and blocks submission', async () => {
            const { onSubmit, user } = renderForm();
            await enterText(user, nameInput(), 'Weather Agents');
            await addUrl(user, ` ${WEATHER_URL}`);

            await submit(user);

            expect(urlValues()).toEqual([` ${WEATHER_URL}`]);
            expect(onSubmit).not.toHaveBeenCalled();
        });

        it('blocks submission when no well-known URL entry was added', async () => {
            const { onSubmit, user } = renderForm();
            await enterText(user, nameInput(), 'Weather Agents');

            await submit(user);

            expect(onSubmit).not.toHaveBeenCalled();
        });

        it('asks for at least one well-known URL once the name has been touched and no entry exists', async () => {
            const { user } = renderForm();
            await enterText(user, nameInput(), 'Weather Agents');

            expect(screen.getByText('Add at least one well-known URL.')).toBeVisible();
        });

        it('asks for at least one well-known URL once the description has been touched and no entry exists', async () => {
            const { user } = renderForm();
            await enterText(user, descriptionInput(), 'Forecast agents');

            expect(screen.getByText('Add at least one well-known URL.')).toBeVisible();
        });
    });

    describe('duplicate well-known URLs', () => {
        it('marks only the later of two duplicate entries invalid', async () => {
            const { user } = renderForm();
            await enterText(user, nameInput(), 'Weather Agents');
            await addUrls(user, ['https://a.com/x', 'HTTPS://A.COM/x']);

            expect(urlInputs()[0]).not.toHaveAttribute('aria-invalid', 'true');
            expect(urlInputs()[1]).toHaveAttribute('aria-invalid', 'true');
        });

        it('does not mark two distinct entries invalid', async () => {
            const { user } = renderForm();
            await enterText(user, nameInput(), 'Weather Agents');
            await addUrls(user, ['https://a.com/x', 'https://a.com/X']);

            expect(urlInputs()[0]).not.toHaveAttribute('aria-invalid', 'true');
            expect(urlInputs()[1]).not.toHaveAttribute('aria-invalid', 'true');
            expect(createButton()).toBeEnabled();
        });
    });

    describe('name and description', () => {
        it('marks an empty name invalid once the user leaves the field', async () => {
            const { user } = renderForm();

            await user.click(nameInput());
            await user.tab();

            expect(nameInput()).toHaveAttribute('aria-invalid', 'true');
            expect(screen.getByText('Name is required.')).toBeVisible();
        });

        it('marks a 251-character description invalid', async () => {
            const { user } = renderForm();
            await enterText(user, nameInput(), 'Weather Agents');
            await enterText(user, descriptionInput(), 'd'.repeat(251));
            await addUrl(user, WEATHER_URL);

            expect(descriptionInput()).toHaveAttribute('aria-invalid', 'true');
        });

        it('submits a typed name and description with a valid well-known URL', async () => {
            const { onSubmit, user } = renderForm();
            await enterText(user, nameInput(), 'Weather Agents');
            await enterText(user, descriptionInput(), 'Forecast agents');
            await addUrl(user, WEATHER_URL);

            await submit(user);

            expect(nameInput()).not.toHaveAttribute('aria-invalid', 'true');
            expect(descriptionInput()).not.toHaveAttribute('aria-invalid', 'true');
            expect(onSubmit).toHaveBeenCalledWith({ name: 'Weather Agents', description: 'Forecast agents', wellKnownUrls: [WEATHER_URL] });
        });
    });
});
