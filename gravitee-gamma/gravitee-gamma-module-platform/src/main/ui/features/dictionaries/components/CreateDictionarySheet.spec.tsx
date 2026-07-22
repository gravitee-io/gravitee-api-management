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

import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';

import { querySheetHeading } from '../../applications/components/test/sheetSpecHelpers';
import { CreateDictionarySheet } from './CreateDictionarySheet';

function renderSheet({
    open = true,
    isSaving = false,
    onSubmit = jest.fn().mockResolvedValue(undefined),
}: {
    open?: boolean;
    isSaving?: boolean;
    onSubmit?: jest.Mock;
} = {}) {
    const onClose = jest.fn();
    render(<CreateDictionarySheet open={open} onClose={onClose} onSubmit={onSubmit} isSaving={isSaving} />);
    return { onClose, onSubmit };
}

function selectType(typeLabel: 'Manual' | 'Dynamic') {
    fireEvent.click(screen.getByLabelText(/^Type/));
    fireEvent.click(screen.getByRole('option', { name: typeLabel }));
}

describe('CreateDictionarySheet', () => {
    beforeEach(() => {
        let id = 0;
        Object.defineProperty(globalThis, 'crypto', {
            configurable: true,
            value: { randomUUID: () => `row-${++id}` },
        });
        Element.prototype.scrollIntoView = jest.fn();
        global.ResizeObserver = class ResizeObserver {
            observe() {}
            unobserve() {}
            disconnect() {}
        } as typeof ResizeObserver;
    });

    it('does not show sheet content when closed', () => {
        renderSheet({ open: false });
        expect(querySheetHeading('Create Dictionary')).toBeNull();
    });

    it('shows create title and type selector', () => {
        renderSheet();
        expect(screen.getByRole('heading', { name: 'Create Dictionary' })).not.toBeNull();
        expect(
            screen.queryByText('Define a new dictionary. For dynamic dictionaries, configure the trigger and HTTP provider.'),
        ).not.toBeNull();
        expect(screen.getByLabelText(/^Type/)).not.toBeNull();
    });

    it('keeps Create disabled until name is valid for MANUAL', () => {
        renderSheet();
        const createBtn = screen.getByRole('button', { name: 'Create' }) as HTMLButtonElement;
        expect(createBtn.disabled).toBe(true);

        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'ab' } });
        expect(createBtn.disabled).toBe(true);
        expect(screen.queryByText('Name must be at least 3 characters')).not.toBeNull();

        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'Airport IATA Codes' } });
        expect((screen.getByRole('button', { name: 'Create' }) as HTMLButtonElement).disabled).toBe(false);
    });

    it('submits a MANUAL payload without provider or trigger', async () => {
        const { onSubmit } = renderSheet();
        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'Airport IATA Codes' } });
        fireEvent.change(screen.getByLabelText(/^Description/), {
            target: { value: 'IATA codes for airports' },
        });
        fireEvent.click(screen.getByRole('button', { name: 'Create' }));

        await waitFor(() => {
            expect(onSubmit).toHaveBeenCalledWith({
                name: 'Airport IATA Codes',
                description: 'IATA codes for airports',
                type: 'MANUAL',
            });
        });
    });

    it('shows dynamic fields when type is Dynamic and keeps Create disabled until required fields are valid', () => {
        renderSheet();
        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'gamma-dict-dynamix' } });
        selectType('Dynamic');

        expect(screen.queryByText('Trigger')).not.toBeNull();
        expect(screen.queryByText('HTTP Provider')).not.toBeNull();
        expect((screen.getByRole('button', { name: 'Create' }) as HTMLButtonElement).disabled).toBe(true);

        fireEvent.change(screen.getByLabelText(/HTTP Service URL/), {
            target: { value: 'not-a-url' },
        });
        expect(screen.queryByText('Enter a valid URL')).not.toBeNull();

        fireEvent.change(screen.getByLabelText(/HTTP Service URL/), {
            target: { value: 'https://service.internal/dictionary' },
        });
        expect((screen.getByRole('button', { name: 'Create' }) as HTMLButtonElement).disabled).toBe(false);
    });

    it('rejects trigger rate of 0 for DYNAMIC', () => {
        renderSheet();
        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'gamma-dict-dynamix' } });
        selectType('Dynamic');
        fireEvent.change(screen.getByLabelText(/HTTP Service URL/), {
            target: { value: 'https://service.internal/dictionary' },
        });
        fireEvent.change(screen.getByLabelText(/^Interval/), { target: { value: '0' } });

        expect(screen.queryByText('Interval must be greater than 0')).not.toBeNull();
        expect((screen.getByRole('button', { name: 'Create' }) as HTMLButtonElement).disabled).toBe(true);
    });

    it('submits a DYNAMIC payload with HTTP provider and trigger', async () => {
        const { onSubmit } = renderSheet();
        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'gamma-dict-dynamix' } });
        fireEvent.change(screen.getByLabelText(/^Description/), { target: { value: 'Dynamic lookup' } });
        selectType('Dynamic');

        fireEvent.change(screen.getByLabelText(/^Interval/), { target: { value: '5' } });
        fireEvent.click(screen.getByLabelText(/^Time Unit/));
        fireEvent.click(screen.getByRole('option', { name: 'Hours' }));

        fireEvent.change(screen.getByLabelText(/HTTP Service URL/), {
            target: { value: 'https://service.internal/dictionary' },
        });
        fireEvent.click(screen.getByLabelText(/^HTTP Method/));
        fireEvent.click(screen.getByRole('option', { name: 'POST' }));
        fireEvent.click(screen.getByLabelText(/Use system proxy/));
        fireEvent.change(screen.getByLabelText(/^Request body/), { target: { value: '{"ok":true}' } });

        fireEvent.click(screen.getByRole('button', { name: 'Add' }));
        const headerNameInputs = screen.getAllByLabelText('Header name');
        const headerValueInputs = screen.getAllByLabelText('Header value');
        fireEvent.change(headerNameInputs[0], { target: { value: 'X-Api-Key' } });
        fireEvent.change(headerValueInputs[0], { target: { value: 'secret' } });

        const specification = screen.getByLabelText(/JOLT Specification/);
        fireEvent.change(specification, {
            target: { value: '[{"operation":"shift","spec":{"*":"&"}}]' },
        });

        fireEvent.click(screen.getByRole('button', { name: 'Create' }));

        await waitFor(() => {
            expect(onSubmit).toHaveBeenCalledWith({
                name: 'gamma-dict-dynamix',
                description: 'Dynamic lookup',
                type: 'DYNAMIC',
                trigger: { rate: 5, unit: 'HOURS' },
                provider: {
                    type: 'HTTP',
                    configuration: {
                        url: 'https://service.internal/dictionary',
                        method: 'POST',
                        body: '{"ok":true}',
                        headers: [{ name: 'X-Api-Key', value: 'secret' }],
                        specification: '[{"operation":"shift","spec":{"*":"&"}}]',
                        useSystemProxy: true,
                    },
                },
            });
        });
    });

    it('shows inline API error when create fails', async () => {
        const onSubmit = jest
            .fn()
            .mockRejectedValue(new Error('A dictionary with name [Airport IATA Codes] already exists in this environment.'));
        renderSheet({ onSubmit });
        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'Airport IATA Codes' } });
        fireEvent.click(screen.getByRole('button', { name: 'Create' }));

        await waitFor(() => {
            expect(screen.queryByText('A dictionary with name [Airport IATA Codes] already exists in this environment.')).not.toBeNull();
        });
    });

    it('shows Creating… while saving', () => {
        renderSheet({ isSaving: true });
        expect(screen.queryByRole('button', { name: 'Creating…' })).not.toBeNull();
        expect(within(screen.getByRole('button', { name: 'Creating…' })).getByText('Creating…')).not.toBeNull();
    });
});
