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

function renderForm() {
    const onSubmit = jest.fn();
    renderWithGraphene(<A2aIntegrationForm onSubmit={onSubmit} />);
    return { onSubmit, user: userEvent.setup() };
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

async function submit(user: UserEvent) {
    await user.click(screen.getByRole('button', { name: 'Create' }));
}

describe('A2aIntegrationForm', () => {
    describe('well-known URL entries', () => {
        it('opens with no well-known URL entry', () => {
            renderForm();

            expect(urlInputs()).toHaveLength(0);
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
        it.each(['https://agent.example.com/.well-known/agent.json', 'http://localhost:8080/a', 'HTTPS://Agent.Example.com/a'])(
            'accepts the URL %s',
            async url => {
                const { user } = renderForm();

                await addUrl(user, url);

                expect(urlInputs()[0]).not.toHaveAttribute('aria-invalid', 'true');
            },
        );

        it.each([
            '://x',
            'see ://x',
            'https://',
            'example.com/agent.json',
            'https://a b',
            'ftp://host/x',
            'gopher://host/x',
            'javascript://x',
        ])('marks the URL %s invalid', async url => {
            const { user } = renderForm();

            await addUrl(user, url);

            expect(urlInputs()[0]).toHaveAttribute('aria-invalid', 'true');
        });

        it.each(['example.com/agent.json', 'https://', 'ftp://host/x'])(
            'does not submit while the only entry holds the invalid URL %s',
            async url => {
                const { onSubmit, user } = renderForm();
                await enterText(user, nameInput(), 'Weather Agents');
                await addUrl(user, url);

                await submit(user);

                expect(onSubmit).not.toHaveBeenCalled();
            },
        );

        it.each([
            { urls: ['example.com/agent.json'], message: 'Enter a valid http:// or https:// URL.' },
            { urls: ['https://a.com/x', 'HTTPS://A.COM/x'], message: 'This URL is already in the list.' },
        ])('shows the message $message for the offending entry when the user attempts to submit', async ({ urls, message }) => {
            const { user } = renderForm();
            await enterText(user, nameInput(), 'Weather Agents');
            await addUrls(user, urls);

            await submit(user);

            expect(screen.getAllByText(message)).toHaveLength(1);
        });

        it('does not mark a newly added empty entry invalid before it is edited or submitted', async () => {
            const { user } = renderForm();

            await user.click(screen.getByRole('button', { name: 'Add another URL' }));

            expect(urlInputs()[0]).not.toHaveAttribute('aria-invalid', 'true');
            expect(screen.queryByText('Enter a valid http:// or https:// URL.')).toBeNull();
        });

        const PADDED_URLS = [
            { padding: 'one leading space', url: ` ${WEATHER_URL}` },
            { padding: 'one trailing space', url: `${WEATHER_URL} ` },
            { padding: 'a leading tab', url: `\t${WEATHER_URL}` },
        ];

        it.each(PADDED_URLS)('marks an entry with $padding invalid with its message', async ({ url }) => {
            const { user } = renderForm();

            await addUrl(user, url);

            expect(urlInputs()[0]).toHaveAttribute('aria-invalid', 'true');
            expect(screen.getByText('Enter a valid http:// or https:// URL.')).toBeVisible();
        });

        it.each(PADDED_URLS)('does not submit while the only entry has $padding', async ({ url }) => {
            const { onSubmit, user } = renderForm();
            await enterText(user, nameInput(), 'Weather Agents');
            await addUrl(user, url);

            await submit(user);

            expect(urlValues()).toEqual([url]);
            expect(onSubmit).not.toHaveBeenCalled();
        });

        it('does not submit when no well-known URL entry was added', async () => {
            const { onSubmit, user } = renderForm();
            await enterText(user, nameInput(), 'Weather Agents');

            await submit(user);

            expect(onSubmit).not.toHaveBeenCalled();
        });

        it('asks for at least one well-known URL when the user attempts to submit with no entry', async () => {
            const { user } = renderForm();
            await enterText(user, nameInput(), 'Weather Agents');

            await submit(user);

            expect(screen.getByText('Add at least one well-known URL.')).toBeVisible();
        });
    });

    describe('duplicate well-known URLs', () => {
        it.each([
            { first: 'https://a.com/x', second: 'HTTPS://A.COM/x' },
            { first: 'http://a.com', second: 'http://a.com:80/' },
            { first: 'https://a.com/x%3a', second: 'https://a.com/x%3A' },
        ])('marks only the later of $first and $second invalid', async ({ first, second }) => {
            const { user } = renderForm();
            await enterText(user, nameInput(), 'Weather Agents');
            await addUrls(user, [first, second]);

            await submit(user);

            expect(urlInputs()[0]).not.toHaveAttribute('aria-invalid', 'true');
            expect(urlInputs()[1]).toHaveAttribute('aria-invalid', 'true');
        });

        it('does not submit while two entries are duplicates', async () => {
            const { onSubmit, user } = renderForm();
            await enterText(user, nameInput(), 'Weather Agents');
            await addUrls(user, ['https://a.com/x', 'HTTPS://A.COM/x']);

            await submit(user);

            expect(onSubmit).not.toHaveBeenCalled();
        });

        it.each([
            { first: 'https://a.com/x', second: 'https://a.com/X' },
            { first: 'https://a.com/x?id=A', second: 'https://a.com/x?id=a' },
            { first: 'https://a.com:8443/x', second: 'https://a.com/x' },
        ])('treats $first and $second as distinct', async ({ first, second }) => {
            const { user } = renderForm();
            await enterText(user, nameInput(), 'Weather Agents');
            await addUrls(user, [first, second]);

            await submit(user);

            expect(urlInputs()[0]).not.toHaveAttribute('aria-invalid', 'true');
            expect(urlInputs()[1]).not.toHaveAttribute('aria-invalid', 'true');
        });
    });

    describe('name', () => {
        it.each([
            { input: 'an empty name', name: '' },
            { input: 'a whitespace-only name', name: '   ' },
            { input: 'a 51-character name', name: 'a'.repeat(51) },
        ])('rejects $input and does not submit', async ({ name }) => {
            const { onSubmit, user } = renderForm();
            if (name) await enterText(user, nameInput(), name);
            await enterText(user, descriptionInput(), 'Forecast agents');
            await addUrl(user, WEATHER_URL);

            await submit(user);

            expect(onSubmit).not.toHaveBeenCalled();
            expect(nameInput()).toHaveAttribute('aria-invalid', 'true');
        });

        it.each([
            { input: 'a 1-character name', name: 'A' },
            { input: 'a 50-character name', name: 'a'.repeat(50) },
        ])('accepts $input and submits', async ({ name }) => {
            const { onSubmit, user } = renderForm();
            await enterText(user, nameInput(), name);
            await enterText(user, descriptionInput(), 'Forecast agents');
            await addUrl(user, WEATHER_URL);

            await submit(user);

            expect(nameInput()).not.toHaveAttribute('aria-invalid', 'true');
            expect(onSubmit).toHaveBeenCalledWith({ name, description: 'Forecast agents', wellKnownUrls: [WEATHER_URL] });
        });
    });

    describe('description', () => {
        it('rejects a 251-character description and does not submit', async () => {
            const { onSubmit, user } = renderForm();
            await enterText(user, nameInput(), 'Weather Agents');
            await enterText(user, descriptionInput(), 'd'.repeat(251));
            await addUrl(user, WEATHER_URL);

            await submit(user);

            expect(onSubmit).not.toHaveBeenCalled();
            expect(descriptionInput()).toHaveAttribute('aria-invalid', 'true');
        });

        it.each([
            { input: 'an empty description', description: '' },
            { input: 'a 250-character description', description: 'd'.repeat(250) },
        ])('accepts $input and submits', async ({ description }) => {
            const { onSubmit, user } = renderForm();
            await enterText(user, nameInput(), 'Weather Agents');
            if (description) await enterText(user, descriptionInput(), description);
            await addUrl(user, WEATHER_URL);

            await submit(user);

            expect(descriptionInput()).not.toHaveAttribute('aria-invalid', 'true');
            expect(onSubmit).toHaveBeenCalledWith({ name: 'Weather Agents', description, wellKnownUrls: [WEATHER_URL] });
        });
    });
});
