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

import { fireEvent, render, screen } from '@testing-library/react';
import { useState } from 'react';

import {
    DictionaryPropertiesEditor,
    hasValidProperties,
    propertiesRowsToRecord,
    type DictionaryPropertyRow,
} from './DictionaryPropertiesEditor';
import { notify } from '../../../shared/notify';

jest.mock('../../../shared/notify', () => ({
    notify: {
        error: jest.fn(),
        success: jest.fn(),
        info: jest.fn(),
    },
}));

beforeAll(() => {
    let id = 0;
    Object.defineProperty(globalThis, 'crypto', {
        value: { randomUUID: () => `row-${++id}` },
    });
});

function Harness({ initial = [] as DictionaryPropertyRow[] }) {
    const [properties, setProperties] = useState(initial);
    return <DictionaryPropertiesEditor properties={properties} onChange={setProperties} />;
}

describe('DictionaryPropertiesEditor', () => {
    beforeEach(() => {
        jest.mocked(notify.error).mockClear();
    });

    it('shows empty state when there are no properties', () => {
        render(<Harness />);
        expect(screen.queryByText('No properties yet. Add at least one key-value pair.')).not.toBeNull();
    });

    it('adds a property from the draft row', () => {
        render(<Harness />);
        fireEvent.change(screen.getByLabelText('New property key'), { target: { value: 'FR' } });
        fireEvent.change(screen.getByLabelText('New property value'), { target: { value: 'France' } });
        fireEvent.click(screen.getByRole('button', { name: 'Add property' }));

        expect((screen.getByLabelText('Property key FR') as HTMLInputElement).value).toBe('FR');
        expect((screen.getByLabelText('Property value for FR') as HTMLInputElement).value).toBe('France');
        expect((screen.getByLabelText('New property key') as HTMLInputElement).value).toBe('');
    });

    it('toasts when adding a property with an empty key', () => {
        render(<Harness />);
        fireEvent.click(screen.getByRole('button', { name: 'Add property' }));
        expect(notify.error).toHaveBeenCalledWith('Property key cannot be empty');
    });

    it('removes a property row', () => {
        render(<Harness initial={[{ id: '1', key: 'FR', value: 'France' }]} />);
        fireEvent.click(screen.getByRole('button', { name: 'Remove property FR' }));
        expect(screen.queryByLabelText('Property key FR')).toBeNull();
    });
});

describe('propertiesRowsToRecord / hasValidProperties', () => {
    it('maps trimmed keys and ignores empty keys', () => {
        expect(
            propertiesRowsToRecord([
                { id: '1', key: ' FR ', value: 'France' },
                { id: '2', key: '  ', value: 'skip' },
            ]),
        ).toEqual({ FR: 'France' });
    });

    it('requires at least one non-empty key', () => {
        expect(hasValidProperties([])).toBe(false);
        expect(hasValidProperties([{ id: '1', key: '  ', value: 'x' }])).toBe(false);
        expect(hasValidProperties([{ id: '1', key: 'k', value: '' }])).toBe(true);
    });
});
