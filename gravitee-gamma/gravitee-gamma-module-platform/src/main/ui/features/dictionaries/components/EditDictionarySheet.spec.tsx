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

import { fireEvent, render, screen, waitFor } from '@testing-library/react';

import { querySheetHeading } from '../../applications/components/test/sheetSpecHelpers';
import { EditDictionarySheet } from './EditDictionarySheet';
import type { Dictionary } from '../types/dictionary';

const MANUAL_DICTIONARY: Dictionary = {
    id: 'dict-1',
    key: 'airport-iata',
    name: 'Airport IATA Codes',
    description: 'IATA codes for airports',
    type: 'MANUAL',
    properties: { CDG: 'Paris Charles de Gaulle', LHR: 'London Heathrow' },
};

const DYNAMIC_DICTIONARY: Dictionary = {
    id: 'dict-2',
    key: 'remote-codes',
    name: 'Remote Codes',
    description: 'Pulled from HTTP',
    type: 'DYNAMIC',
    properties: { a: '1' },
    trigger: { rate: 5, unit: 'MINUTES' },
    provider: {
        type: 'HTTP',
        configuration: {
            url: 'https://example.com/codes',
            method: 'GET',
            specification: '[{"operation":"shift","spec":{"*":"&"}}]',
            useSystemProxy: false,
            headers: [{ name: 'Accept', value: 'application/json' }],
        },
    },
};

function renderSheet({
    open = true,
    dictionary = MANUAL_DICTIONARY,
    isLoading = false,
    isSaving = false,
    onSubmit = jest.fn().mockResolvedValue(undefined),
}: {
    open?: boolean;
    dictionary?: Dictionary | undefined;
    isLoading?: boolean;
    isSaving?: boolean;
    onSubmit?: jest.Mock;
} = {}) {
    const onClose = jest.fn();
    render(
        <EditDictionarySheet
            open={open}
            dictionary={dictionary}
            isLoading={isLoading}
            onClose={onClose}
            onSubmit={onSubmit}
            isSaving={isSaving}
        />,
    );
    return { onClose, onSubmit };
}

describe('EditDictionarySheet', () => {
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
        expect(querySheetHeading('Edit Dictionary')).toBeNull();
    });

    it('prefills name, description, key and shows immutable type helper', () => {
        renderSheet();
        expect(screen.getByRole('heading', { name: 'Edit Dictionary' })).not.toBeNull();
        expect(screen.queryByText('Update dictionary details and configuration.')).not.toBeNull();
        expect((screen.getByLabelText(/^Key/) as HTMLInputElement).value).toBe('airport-iata');
        expect((screen.getByLabelText(/^Key/) as HTMLInputElement).readOnly).toBe(true);
        expect((screen.getByLabelText(/^Name/) as HTMLInputElement).value).toBe('Airport IATA Codes');
        expect((screen.getByLabelText(/^Description/) as HTMLTextAreaElement).value).toBe('IATA codes for airports');
        expect(screen.queryByText('Type cannot be changed after creation.')).not.toBeNull();
        expect(screen.queryByText('Key cannot be changed after creation.')).not.toBeNull();
        expect(screen.getByDisplayValue('CDG')).not.toBeNull();
        expect(screen.getByDisplayValue('Paris Charles de Gaulle')).not.toBeNull();
    });

    it('keeps Save Changes disabled when name becomes invalid', () => {
        renderSheet();
        const saveBtn = screen.getByRole('button', { name: 'Save Changes' }) as HTMLButtonElement;
        expect(saveBtn.disabled).toBe(false);

        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'ab' } });
        expect((screen.getByRole('button', { name: 'Save Changes' }) as HTMLButtonElement).disabled).toBe(true);
    });

    it('submits MANUAL update payload with properties via PUT contract shape', async () => {
        const { onSubmit } = renderSheet();
        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'Airport Codes Updated' } });
        fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

        await waitFor(() => {
            expect(onSubmit).toHaveBeenCalledWith({
                name: 'Airport Codes Updated',
                description: 'IATA codes for airports',
                type: 'MANUAL',
                properties: { CDG: 'Paris Charles de Gaulle', LHR: 'London Heathrow' },
            });
        });
    });

    it('prefills DYNAMIC trigger and provider fields and submits them', async () => {
        const { onSubmit } = renderSheet({ dictionary: DYNAMIC_DICTIONARY });
        expect((screen.getByLabelText(/HTTP Service URL/) as HTMLInputElement).value).toBe('https://example.com/codes');
        expect((screen.getByLabelText(/^Interval/) as HTMLInputElement).value).toBe('5');

        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'Remote Codes Updated' } });
        fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

        await waitFor(() => {
            expect(onSubmit).toHaveBeenCalledWith(
                expect.objectContaining({
                    name: 'Remote Codes Updated',
                    type: 'DYNAMIC',
                    trigger: { rate: 5, unit: 'MINUTES' },
                    provider: expect.objectContaining({
                        type: 'HTTP',
                        configuration: expect.objectContaining({
                            url: 'https://example.com/codes',
                            method: 'GET',
                        }),
                    }),
                }),
            );
        });
    });
});
