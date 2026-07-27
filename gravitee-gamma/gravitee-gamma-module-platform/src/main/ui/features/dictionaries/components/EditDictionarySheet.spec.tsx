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

import { EditDictionarySheet } from './EditDictionarySheet';
import { querySheetHeading } from '../../applications/components/test/sheetSpecHelpers';
import type { Dictionary } from '../types/dictionary';

const MANUAL_DICTIONARY: Dictionary = {
    id: 'dict-1',
    name: 'Airport IATA Codes',
    description: 'IATA codes for airports',
    type: 'MANUAL',
    properties: { CDG: 'Paris Charles de Gaulle', LHR: 'London Heathrow' },
};

const DYNAMIC_DICTIONARY: Dictionary = {
    id: 'dict-2',
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
            body: '{"include":"all"}',
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

    it('prefills name and description for MANUAL without key or properties', () => {
        renderSheet();
        expect(screen.getByRole('heading', { name: 'Edit Dictionary' })).not.toBeNull();
        expect((screen.getByLabelText(/^Name/) as HTMLInputElement).value).toBe('Airport IATA Codes');
        expect((screen.getByLabelText(/^Description/) as HTMLTextAreaElement).value).toBe('IATA codes for airports');
        expect(screen.queryByLabelText(/^Key/)).toBeNull();
        expect(screen.queryByText('Key cannot be changed after creation.')).toBeNull();
        expect(screen.queryByText('Type cannot be changed after creation.')).not.toBeNull();
        expect(screen.queryByDisplayValue('CDG')).toBeNull();
        expect(screen.queryByText('Properties')).toBeNull();
        expect(screen.queryByLabelText(/HTTP Service URL/)).toBeNull();
    });

    it('prefills when dictionary arrives after the sheet opens', () => {
        const { rerender } = render(
            <EditDictionarySheet
                open
                dictionary={undefined}
                isLoading
                onClose={jest.fn()}
                onSubmit={jest.fn().mockResolvedValue(undefined)}
                isSaving={false}
            />,
        );

        expect(screen.queryByLabelText(/^Name/)).toBeNull();

        rerender(
            <EditDictionarySheet
                open
                dictionary={MANUAL_DICTIONARY}
                isLoading={false}
                onClose={jest.fn()}
                onSubmit={jest.fn().mockResolvedValue(undefined)}
                isSaving={false}
            />,
        );

        expect((screen.getByLabelText(/^Name/) as HTMLInputElement).value).toBe('Airport IATA Codes');
        expect((screen.getByLabelText(/^Description/) as HTMLTextAreaElement).value).toBe('IATA codes for airports');
    });

    it('keeps Save Changes disabled when name becomes invalid', () => {
        renderSheet();
        const saveBtn = screen.getByRole('button', { name: 'Save Changes' }) as HTMLButtonElement;
        expect(saveBtn.disabled).toBe(false);

        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'ab' } });
        expect((screen.getByRole('button', { name: 'Save Changes' }) as HTMLButtonElement).disabled).toBe(true);
    });

    it('submits MANUAL update with existing properties preserved', async () => {
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

    it('submits empty string when description is cleared', async () => {
        const { onSubmit } = renderSheet();
        fireEvent.change(screen.getByLabelText(/^Description/), { target: { value: '   ' } });
        fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

        await waitFor(() => {
            expect(onSubmit).toHaveBeenCalledWith(
                expect.objectContaining({
                    description: '',
                }),
            );
        });
    });

    it('prefills DYNAMIC trigger and provider without key field', async () => {
        const { onSubmit } = renderSheet({ dictionary: DYNAMIC_DICTIONARY });
        expect(screen.queryByLabelText(/^Key/)).toBeNull();
        expect((screen.getByLabelText(/HTTP Service URL/) as HTMLInputElement).value).toBe('https://example.com/codes');
        expect((screen.getByLabelText(/^Interval/) as HTMLInputElement).value).toBe('5');

        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'Remote Codes Updated' } });
        fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

        await waitFor(() => {
            expect(onSubmit).toHaveBeenCalledWith(
                expect.objectContaining({
                    name: 'Remote Codes Updated',
                    type: 'DYNAMIC',
                    properties: { a: '1' },
                    trigger: { rate: 5, unit: 'MINUTES' },
                    provider: expect.objectContaining({
                        type: 'HTTP',
                        configuration: expect.objectContaining({
                            url: 'https://example.com/codes',
                            method: 'GET',
                            body: '{"include":"all"}',
                        }),
                    }),
                }),
            );
        });
    });

    it('submits empty properties object when DYNAMIC dictionary has no properties', async () => {
        const { onSubmit } = renderSheet({
            dictionary: { ...DYNAMIC_DICTIONARY, properties: undefined },
        });
        fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

        await waitFor(() => {
            expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ properties: {} }));
        });
    });

    it('submits empty string when HTTP body is cleared', async () => {
        const { onSubmit } = renderSheet({ dictionary: DYNAMIC_DICTIONARY });
        fireEvent.change(screen.getByLabelText(/Request body/), { target: { value: '   ' } });
        fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

        await waitFor(() => {
            expect(onSubmit).toHaveBeenCalledWith(
                expect.objectContaining({
                    provider: expect.objectContaining({
                        configuration: expect.objectContaining({ body: '' }),
                    }),
                }),
            );
        });
    });
});
